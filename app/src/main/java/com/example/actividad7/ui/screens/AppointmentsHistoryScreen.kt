package com.example.actividad7.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.actividad7.data.Appointment
import com.example.actividad7.data.AppointmentStatus
import com.example.actividad7.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsHistoryScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val appointments by viewModel.appointments.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
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
                text = "Historial de Citas",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        // Información del usuario
        currentUser?.let { user ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Bienvenido, ${user.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total de citas: ${appointments.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Lista de citas
        if (appointments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No tienes citas programadas",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Reserva tu primera cita para comenzar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(appointments) { appointment ->
                    AppointmentCard(
                        appointment = appointment,
                        viewModel = viewModel,
                        onStatusChange = { status ->
                            viewModel.updateAppointmentStatus(appointment.id, status.name)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentCard(
    appointment: Appointment,
    viewModel: MainViewModel,
    onStatusChange: (AppointmentStatus) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Fecha y hora
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = viewModel.formatDate(appointment.date),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = appointment.time,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Motivo
            Text(
                text = "Motivo:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = appointment.reason,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Estado y acciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Estado
                val statusColor = when (appointment.status) {
                    AppointmentStatus.SCHEDULED -> MaterialTheme.colorScheme.primary
                    AppointmentStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
                    AppointmentStatus.CANCELLED -> MaterialTheme.colorScheme.error
                }
                
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = when (appointment.status) {
                                AppointmentStatus.SCHEDULED -> "Programada"
                                AppointmentStatus.COMPLETED -> "Completada"
                                AppointmentStatus.CANCELLED -> "Cancelada"
                            }
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = statusColor.copy(alpha = 0.1f),
                        labelColor = statusColor
                    )
                )
                
                // Acciones
                if (appointment.status == AppointmentStatus.SCHEDULED) {
                    Row {
                        TextButton(
                            onClick = { onStatusChange(AppointmentStatus.COMPLETED) }
                        ) {
                            Text("Completar")
                        }
                        TextButton(
                            onClick = { onStatusChange(AppointmentStatus.CANCELLED) }
                        ) {
                            Text("Cancelar")
                        }
                    }
                }
            }
        }
    }
}
