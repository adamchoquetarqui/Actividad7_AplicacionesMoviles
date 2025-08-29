package com.example.actividad7.repository

import android.util.Log
import com.example.actividad7.data.Appointment
import com.example.actividad7.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    companion object {
        private const val TAG = "FirebaseRepository"
    }
    
    // Autenticación
    suspend fun signUp(email: String, password: String, name: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = User(
                id = result.user?.uid ?: "",
                email = email,
                name = name
            )
            
            // Guardar usuario en Firestore
            firestore.collection("users").document(user.id).set(user).await()
            Log.d(TAG, "Usuario creado exitosamente: ${user.id}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear usuario: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            Log.d(TAG, "Iniciando autenticación para email: $email")
            
            // Paso 1: Autenticar con Firebase Auth
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: throw Exception("Usuario no encontrado en Firebase Auth")
            
            Log.d(TAG, "Autenticación exitosa, userId: $userId")
            
            // Paso 2: Obtener datos del usuario desde Firestore
            val userDoc = firestore.collection("users").document(userId).get().await()
            
            if (!userDoc.exists()) {
                Log.w(TAG, "Usuario no existe en Firestore, creando usuario temporal: $userId")
                // Crear usuario temporal en Firestore
                val tempUser = User(
                    id = userId,
                    email = email,
                    name = "Usuario Temporal"
                )
                firestore.collection("users").document(userId).set(tempUser).await()
                Log.d(TAG, "Usuario temporal creado en Firestore")
                return Result.success(tempUser)
            }
            
            val user = userDoc.toObject(User::class.java)
            if (user == null) {
                Log.e(TAG, "Error al convertir documento a User: $userId")
                throw Exception("Error al cargar datos del usuario")
            }
            
            Log.d(TAG, "Usuario autenticado exitosamente: ${user.id} - ${user.name}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error al autenticar usuario: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    fun signOut() {
        auth.signOut()
        Log.d(TAG, "Usuario cerró sesión")
    }
    
    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    
    // Obtener datos del usuario desde Firestore
    suspend fun getUserFromFirestore(userId: String): User? {
        return try {
            Log.d(TAG, "Obteniendo datos del usuario desde Firestore: $userId")
            val userDoc = firestore.collection("users").document(userId).get().await()
            
            if (userDoc.exists()) {
                val user = userDoc.toObject(User::class.java)
                Log.d(TAG, "Usuario obtenido de Firestore: ${user?.name}")
                user
            } else {
                Log.w(TAG, "Usuario no encontrado en Firestore: $userId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener usuario de Firestore: ${e.message}", e)
            null
        }
    }
    
    // Citas
    suspend fun createAppointment(appointment: Appointment): Result<Appointment> {
        return try {
            Log.d(TAG, "Creando cita: ${appointment}")
            
            // Asegurar que la cita tenga un ID único
            val appointmentWithId = if (appointment.id.isEmpty()) {
                appointment.copy(id = UUID.randomUUID().toString())
            } else {
                appointment
            }
            
            // Asegurar que tenga timestamp de creación
            val finalAppointment = appointmentWithId.copy(
                createdAt = System.currentTimeMillis()
            )
            
            Log.d(TAG, "Cita final a guardar: ${finalAppointment}")
            
            // Guardar en Firestore
            firestore.collection("appointments")
                .document(finalAppointment.id)
                .set(finalAppointment)
                .await()
            
            Log.d(TAG, "Cita guardada exitosamente en Firestore con ID: ${finalAppointment.id}")
            Result.success(finalAppointment)
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear cita: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getUserAppointments(userId: String): Result<List<Appointment>> {
        return try {
            Log.d(TAG, "Cargando citas para usuario: $userId")
            
            val snapshot = firestore.collection("appointments")
                .whereEqualTo("userId", userId)
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()
            
            val appointments = snapshot.documents.mapNotNull { doc ->
                try {
                    val appointment = doc.toObject(Appointment::class.java)
                    Log.d(TAG, "Cita cargada: ${appointment}")
                    appointment
                } catch (e: Exception) {
                    Log.e(TAG, "Error al convertir documento a Appointment: ${e.message}")
                    null
                }
            }
            
            Log.d(TAG, "Total de citas cargadas: ${appointments.size}")
            Result.success(appointments)
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar citas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateAppointmentStatus(appointmentId: String, status: String): Result<Unit> {
        return try {
            Log.d(TAG, "Actualizando estado de cita: $appointmentId a $status")
            
            firestore.collection("appointments").document(appointmentId)
                .update("status", status).await()
            
            Log.d(TAG, "Estado de cita actualizado exitosamente")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar estado de cita: ${e.message}", e)
            Result.failure(e)
        }
    }
}

