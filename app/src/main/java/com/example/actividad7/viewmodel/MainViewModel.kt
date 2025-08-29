package com.example.actividad7.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.actividad7.data.Appointment
import com.example.actividad7.data.User
import com.example.actividad7.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.actividad7.worker.AppointmentNotificationWorker
import kotlinx.coroutines.withTimeout
import android.content.Context

class MainViewModel : ViewModel() {
    private val repository = FirebaseRepository()
    private var appContext: Context? = null
    
    companion object {
        private const val TAG = "MainViewModel"
    }
    
    fun setContext(context: Context) {
        appContext = context.applicationContext
    }
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments: StateFlow<List<Appointment>> = _appointments.asStateFlow()
    
    init {
        checkAuthState()
    }
    
    private fun checkAuthState() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Verificando estado de autenticación...")
                val user = repository.getCurrentUser()
                if (user != null) {
                    Log.d(TAG, "Usuario encontrado en Auth: ${user.uid}")
                    // Cargar datos del usuario directamente
                    val userData = repository.getUserFromFirestore(user.uid)
                    if (userData != null) {
                        Log.d(TAG, "Datos del usuario cargados: ${userData.name}")
                        _currentUser.value = userData
                        _uiState.value = UiState.Authenticated
                        loadUserAppointments(user.uid)
                    } else {
                        Log.w(TAG, "Usuario no encontrado en Firestore, estableciendo como no autenticado")
                        _uiState.value = UiState.NotAuthenticated
                    }
                } else {
                    Log.d(TAG, "No hay usuario autenticado")
                    _uiState.value = UiState.NotAuthenticated
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en checkAuthState: ${e.message}", e)
                _uiState.value = UiState.NotAuthenticated
            }
        }
    }
    
    fun signUp(email: String, password: String, name: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.signUp(email, password, name)
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _uiState.value = UiState.Authenticated
                    loadUserAppointments(user.id)
                },
                onFailure = { exception ->
                    _uiState.value = UiState.Error(exception.message ?: "Error en el registro")
                }
            )
        }
    }
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Iniciando proceso de inicio de sesión para: $email")
                _uiState.value = UiState.Loading
                
                Log.d(TAG, "Llamando a repository.signIn...")
                val result = repository.signIn(email, password)
                Log.d(TAG, "Resultado recibido de repository.signIn")
                
                result.fold(
                    onSuccess = { user ->
                        Log.d(TAG, "Inicio de sesión exitoso para usuario: ${user.id}")
                        _currentUser.value = user
                        _uiState.value = UiState.Authenticated
                        loadUserAppointments(user.id)
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Error en inicio de sesión: ${exception.message}", exception)
                        _uiState.value = UiState.Error(exception.message ?: "Error en el inicio de sesión")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Excepción no manejada en signIn: ${e.message}", e)
                _uiState.value = UiState.Error("Error inesperado: ${e.message}")
            }
        }
    }
    
    fun signOut() {
        repository.signOut()
        _currentUser.value = null
        _appointments.value = emptyList()
        _uiState.value = UiState.NotAuthenticated
    }
    
    private fun loadUserData(userId: String) {
        viewModelScope.launch {
            try {
                // Cargar datos del usuario desde Firestore
                val userDoc = repository.getUserFromFirestore(userId)
                if (userDoc != null) {
                    _currentUser.value = userDoc
                    _uiState.value = UiState.Authenticated
                    loadUserAppointments(userId)
                } else {
                    _uiState.value = UiState.NotAuthenticated
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar datos del usuario: ${e.message}", e)
                _uiState.value = UiState.NotAuthenticated
            }
        }
    }
    
    fun loadUserAppointments(userId: String) {
        viewModelScope.launch {
            Log.d(TAG, "Cargando citas para usuario: $userId")
            val result = repository.getUserAppointments(userId)
            result.fold(
                onSuccess = { appointments ->
                    Log.d(TAG, "Citas cargadas exitosamente: ${appointments.size} citas")
                    _appointments.value = appointments
                    // Programar notificaciones para todas las citas existentes
                    scheduleNotificationsForExistingAppointments(appointments)
                },
                onFailure = { exception ->
                    Log.e(TAG, "Error al cargar citas: ${exception.message}", exception)
                    // No cambiar el estado UI aquí, solo loggear el error
                    // Las citas pueden fallar sin afectar la autenticación
                }
            )
        }
    }
    
    private fun scheduleNotificationsForExistingAppointments(appointments: List<Appointment>) {
        Log.d(TAG, "Programando notificaciones para ${appointments.size} citas existentes")
        appointments.forEach { appointment ->
            if (appointment.status == com.example.actividad7.data.AppointmentStatus.SCHEDULED) {
                scheduleNotification(appointment)
            }
        }
    }
    
    fun createAppointment(date: Long, time: String, reason: String) {
        val currentUser = _currentUser.value ?: return
        
        val appointment = Appointment(
            userId = currentUser.id,
            date = date,
            time = time,
            reason = reason
        )
        
        viewModelScope.launch {
            val result = repository.createAppointment(appointment)
            result.fold(
                onSuccess = { newAppointment ->
                    _appointments.value = _appointments.value + newAppointment
                    scheduleNotification(newAppointment)
                },
                onFailure = { exception ->
                    _uiState.value = UiState.Error(exception.message ?: "Error al crear la cita")
                }
            )
        }
    }
    
    private fun scheduleNotification(appointment: Appointment) {
        Log.d(TAG, "Programando notificación para cita: ${appointment.id}")
        
        try {
            // Calcular el tiempo para la notificación (15 minutos antes)
            val appointmentCalendar = Calendar.getInstance()
            appointmentCalendar.timeInMillis = appointment.date
            
            // Parsear la hora de la cita
            val timeParts = appointment.time.split(":")
            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            
            // Establecer la hora exacta de la cita
            appointmentCalendar.set(Calendar.HOUR_OF_DAY, hour)
            appointmentCalendar.set(Calendar.MINUTE, minute)
            appointmentCalendar.set(Calendar.SECOND, 0)
            appointmentCalendar.set(Calendar.MILLISECOND, 0)
            
            val appointmentTime = appointmentCalendar.timeInMillis
            
            // Restar 15 minutos para la notificación
            appointmentCalendar.add(Calendar.MINUTE, -15)
            val notificationTime = appointmentCalendar.timeInMillis
            val currentTime = System.currentTimeMillis()
            
            Log.d(TAG, "Hora de la cita: ${Date(appointmentTime)}")
            Log.d(TAG, "Hora de la notificación: ${Date(notificationTime)}")
            Log.d(TAG, "Hora actual: ${Date(currentTime)}")
            
            // Solo programar si la notificación es en el futuro
            if (notificationTime > currentTime) {
                val delay = notificationTime - currentTime
                
                Log.d(TAG, "Programando notificación para: ${Date(notificationTime)}")
                Log.d(TAG, "Delay: ${delay}ms (${delay / 1000 / 60} minutos)")
                
                // Crear datos para el Worker
                val inputData = Data.Builder()
                    .putString(AppointmentNotificationWorker.KEY_APPOINTMENT_ID, appointment.id)
                    .putString(AppointmentNotificationWorker.KEY_APPOINTMENT_DATE, formatDate(appointment.date))
                    .putString(AppointmentNotificationWorker.KEY_APPOINTMENT_TIME, appointment.time)
                    .putString(AppointmentNotificationWorker.KEY_APPOINTMENT_REASON, appointment.reason)
                    .build()
                
                // Crear el trabajo programado con un ID único
                val workName = "notification_${appointment.id}"
                val notificationWork = OneTimeWorkRequestBuilder<AppointmentNotificationWorker>()
                    .setInputData(inputData)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                    .build()
                
                // Programar el trabajo
                val workManager = appContext?.let { WorkManager.getInstance(it) }
                workManager?.enqueueUniqueWork(
                    workName,
                    ExistingWorkPolicy.REPLACE,
                    notificationWork
                )
                
                Log.d(TAG, "Notificación programada exitosamente para cita: ${appointment.id}")
            } else {
                Log.d(TAG, "La notificación debería haber sido enviada ya para cita: ${appointment.id}")
                Log.d(TAG, "Tiempo de notificación: ${Date(notificationTime)}")
                Log.d(TAG, "Tiempo actual: ${Date(currentTime)}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al programar notificación: ${e.message}", e)
        }
    }
    
    // Método para probar notificaciones inmediatamente (para testing)
    fun testNotification() {
        Log.d(TAG, "🧪 Iniciando prueba de notificación")
        
        try {
            val testAppointment = Appointment(
                id = "test_${System.currentTimeMillis()}",
                userId = _currentUser.value?.id ?: "",
                date = System.currentTimeMillis(),
                time = "12:00",
                reason = "🧪 PRUEBA: Esta es una notificación de prueba para verificar que el sistema funciona correctamente"
            )
            
            // Crear datos para el Worker
            val inputData = Data.Builder()
                .putString(AppointmentNotificationWorker.KEY_APPOINTMENT_ID, testAppointment.id)
                .putString(AppointmentNotificationWorker.KEY_APPOINTMENT_DATE, formatDate(testAppointment.date))
                .putString(AppointmentNotificationWorker.KEY_APPOINTMENT_TIME, testAppointment.time)
                .putString(AppointmentNotificationWorker.KEY_APPOINTMENT_REASON, testAppointment.reason)
                .build()
            
            // Crear el trabajo programado para ejecutarse en 1 segundo
            val workName = "test_notification_${System.currentTimeMillis()}"
            val notificationWork = OneTimeWorkRequestBuilder<AppointmentNotificationWorker>()
                .setInputData(inputData)
                .setInitialDelay(1, TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                .build()
            
            // Programar el trabajo
            val workManager = appContext?.let { WorkManager.getInstance(it) }
            workManager?.enqueueUniqueWork(
                workName,
                ExistingWorkPolicy.REPLACE,
                notificationWork
            )
            
            Log.d(TAG, "✅ Notificación de prueba programada para 1 segundo")
            Log.d(TAG, "📋 WorkName: $workName")
            Log.d(TAG, "🆔 Test ID: ${testAppointment.id}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al programar notificación de prueba: ${e.message}", e)
        }
    }
    
    fun updateAppointmentStatus(appointmentId: String, status: String) {
        viewModelScope.launch {
            val result = repository.updateAppointmentStatus(appointmentId, status)
            result.fold(
                onSuccess = {
                    // Actualizar la lista local
                    _appointments.value = _appointments.value.map { appointment ->
                        if (appointment.id == appointmentId) {
                            appointment.copy(status = com.example.actividad7.data.AppointmentStatus.valueOf(status))
                        } else {
                            appointment
                        }
                    }
                },
                onFailure = { exception ->
                    _uiState.value = UiState.Error(exception.message ?: "Error al actualizar la cita")
                }
            )
        }
    }
    
    fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return formatter.format(date)
    }
    
    fun clearError() {
        _uiState.value = UiState.NotAuthenticated
    }
    
    // Método para reprogramar todas las notificaciones existentes
    fun rescheduleAllNotifications() {
        val currentAppointments = _appointments.value
        Log.d(TAG, "Reprogramando notificaciones para ${currentAppointments.size} citas")
        scheduleNotificationsForExistingAppointments(currentAppointments)
    }
}

sealed class UiState {
    object Loading : UiState()
    object NotAuthenticated : UiState()
    object Authenticated : UiState()
    data class Error(val message: String) : UiState()
}

