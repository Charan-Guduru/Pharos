package com.vnrvjiet.attendancemonitor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vnrvjiet.attendancemonitor.data.model.AttendanceStatus

data class SituationItem(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val status: AttendanceStatus
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreSituationsSheet(
    onStatusSelected: (AttendanceStatus) -> Unit,
    onMarkWholeDay: (AttendanceStatus) -> Unit,
    onDismiss: () -> Unit
) {
    val situations = listOf(
        SituationItem("Holiday", Icons.Default.Celebration, Color(0xFF2196F3), AttendanceStatus.HOLIDAY),
        SituationItem("Class Cancelled", Icons.Default.EventBusy, Color(0xFFEF5350), AttendanceStatus.CLASS_CANCELLED),
        SituationItem("Volunteer", Icons.Default.VolunteerActivism, Color(0xFF66BB6A), AttendanceStatus.VOLUNTEER),
        SituationItem("College Event", Icons.Default.School, Color(0xFFFFA726), AttendanceStatus.COLLEGE_EVENT),
        SituationItem("Hackathon", Icons.Default.Code, Color(0xFF7E57C2), AttendanceStatus.HACKATHON),
        SituationItem("Sports Event", Icons.Default.SportsBasketball, Color(0xFFFF7043), AttendanceStatus.SPORTS_EVENT),
        SituationItem("Medical Leave", Icons.Default.MedicalServices, Color(0xFFEC407A), AttendanceStatus.MEDICAL_LEAVE),
        SituationItem("Other", Icons.Default.MoreHoriz, Color.Gray, AttendanceStatus.OTHER)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "More Situations", 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Record non-standard attendance for this period.", 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            situations.forEach { situation ->
                SituationButton(
                    item = situation,
                    onClick = {
                        onStatusSelected(situation.status)
                        onDismiss()
                    }
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Text(
                text = "Whole Day Actions", 
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Apply attendance to every period in the current day.", 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            ActionRowButton(
                label = "Mark Entire Day as Holiday",
                icon = Icons.Default.DateRange,
                color = Color(0xFF2196F3),
                onClick = {
                    onMarkWholeDay(AttendanceStatus.HOLIDAY)
                    onDismiss()
                }
            )

            ActionRowButton(
                label = "Mark Entire Day as Absent",
                icon = Icons.Default.EventBusy,
                color = Color(0xFFEF5350),
                onClick = {
                    onMarkWholeDay(AttendanceStatus.ABSENT)
                    onDismiss()
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer, 
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun SituationButton(item: SituationItem, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = item.color.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(item.icon, contentDescription = null, tint = item.color, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = item.label, 
                modifier = Modifier.weight(1f), 
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                Icons.Outlined.ChevronRight, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ActionRowButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label, 
                modifier = Modifier.weight(1f), 
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            )
        }
    }
}
