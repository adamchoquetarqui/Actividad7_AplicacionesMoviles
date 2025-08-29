package com.example.actividad7.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.actividad7.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookAppointmentScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var selectedHour by remember { mutableStateOf(9) }
    var selectedMinute by remember { mutableStateOf(0) }
    var reason by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    val currentUser by viewModel.currentUser.collectAsState()
    
    // Función para verificar si la hora seleccionada ya pasó (si es hoy)
    fun isTimeValid(): Boolean {
        if (selectedDate == null) return true
        
        val now = Calendar.getInstance()
        val selectedCalendar = Calendar.getInstance()
        selectedCalendar.timeInMillis = selectedDate!!
        selectedCalendar.set(Calendar.HOUR_OF_DAY, selectedHour)
        selectedCalendar.set(Calendar.MINUTE, selectedMinute)
        selectedCalendar.set(Calendar.SECOND, 0)
        selectedCalendar.set(Calendar.MILLISECOND, 0)
        
        // Si es el mismo día, verificar que la hora no haya pasado
        if (now.get(Calendar.YEAR) == selectedCalendar.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == selectedCalendar.get(Calendar.DAY_OF_YEAR)) {
            return selectedCalendar.timeInMillis > now.timeInMillis
        }
        
        return true
    }
    
    // Función para formatear la hora seleccionada
    fun getFormattedTime(): String {
        return String.format("%02d:%02d", selectedHour, selectedMinute)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver"
                )
            }
            Text(
                text = "Reservar Cita",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        // Información del usuario
        currentUser?.let { user ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Usuario: ${user.name}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Email: ${user.email}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Selector de fecha
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Fecha de la cita",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (selectedDate != null) {
                            val date = Date(selectedDate!!)
                            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            "Fecha seleccionada: ${formatter.format(date)}"
                        } else {
                            "Seleccionar fecha"
                        }
                    )
                }
            }
        }
        
        // Selector de hora
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Hora de la cita",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Button(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (selectedDate != null) {
                            "Hora seleccionada: ${getFormattedTime()}"
                        } else {
                            "Seleccionar hora"
                        }
                    )
                }
                
                // Mostrar advertencia si la hora ya pasó
                if (selectedDate != null && !isTimeValid()) {
                    Text(
                        text = "⚠️ La hora seleccionada ya pasó para hoy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        
        // Campo de motivo
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Motivo de la cita",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Describe el motivo de tu cita") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    )
                )
            }
        }
        
        // Botón de confirmar
        Button(
            onClick = {
                if (selectedDate != null && reason.isNotBlank() && isTimeValid()) {
                    viewModel.createAppointment(selectedDate!!, getFormattedTime(), reason)
                    onNavigateBack()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = selectedDate != null && reason.isNotBlank() && isTimeValid()
        ) {
            Text("Confirmar Cita")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
    
    // Calendario visual personalizado
    if (showDatePicker) {
        var currentYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
        var currentMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
        var selectedDay by remember { mutableStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }
        
        val calendar = Calendar.getInstance()
        calendar.set(currentYear, currentMonth, 1)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Domingo
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        val monthNames = listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
        
        val dayNames = listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
        
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (currentMonth == 0) {
                                currentMonth = 11
                                currentYear--
                            } else {
                                currentMonth--
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Mes anterior"
                        )
                    }
                    
                    Text(
                        text = "${monthNames[currentMonth]} $currentYear",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(
                        onClick = {
                            if (currentMonth == 11) {
                                currentMonth = 0
                                currentYear++
                            } else {
                                currentMonth++
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Mes siguiente"
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    // Días de la semana
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        dayNames.forEach { dayName ->
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                                         // Días del mes
                     val days = mutableListOf<Int>()
                     
                     // Agregar espacios vacíos para alinear con los días de la semana
                     repeat(firstDayOfWeek) {
                         days.add(0)
                     }
                     
                     // Agregar los días del mes
                     repeat(daysInMonth) { day ->
                         days.add(day + 1)
                     }
                     
                     // Agregar espacios vacíos al final para completar la última fila
                     val remainingDays = 7 - (days.size % 7)
                     if (remainingDays < 7) {
                         repeat(remainingDays) {
                             days.add(0)
                         }
                     }
                     
                     // Crear filas de 7 días
                     days.chunked(7).forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            week.forEach { day ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                ) {
                                    if (day > 0) {
                                        val isSelected = selectedDay == day
                                        val isToday = day == Calendar.getInstance().get(Calendar.DAY_OF_MONTH) &&
                                                currentMonth == Calendar.getInstance().get(Calendar.MONTH) &&
                                                currentYear == Calendar.getInstance().get(Calendar.YEAR)
                                        
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    when {
                                                        isSelected -> MaterialTheme.colorScheme.primary
                                                        isToday -> MaterialTheme.colorScheme.primaryContainer
                                                        else -> Color.Transparent
                                                    },
                                                    shape = CircleShape
                                                )
                                                .clickable { selectedDay = day },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = day.toString(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                },
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Crear Calendar con la fecha seleccionada
                        val calendar = Calendar.getInstance()
                        calendar.set(currentYear, currentMonth, selectedDay, 0, 0, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        
                        selectedDate = calendar.timeInMillis
                        
                        // Debug: mostrar la fecha seleccionada
                        val debugDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        android.util.Log.d("DatePicker", "Fecha seleccionada: ${debugDate.format(Date(selectedDate!!))}")
                        
                        showDatePicker = false
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
    
    // Time Picker mejorado - Selector de hora y minutos por separado
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Seleccionar hora y minutos") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    // Selector de hora
                    Text(
                        text = "Hora:",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items((0..23).toList()) { hour ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedHour == hour,
                                    onClick = { selectedHour = hour }
                                )
                                Text(
                                    text = String.format("%02d:00", hour),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Selector de minutos
                    Text(
                        text = "Minutos:",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items((0..59).toList()) { minute ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedMinute == minute,
                                    onClick = { selectedMinute = minute }
                                )
                                Text(
                                    text = String.format("%02d", minute),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showTimePicker = false
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
