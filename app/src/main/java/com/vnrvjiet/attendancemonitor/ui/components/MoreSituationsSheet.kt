package com.vnrvjiet.attendancemonitor.ui.components

import androidx.compose.foundation.layout.*
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

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
            Text("More Situations", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Record non-standard attendance for this period.", color = Color.Gray, fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(16.dp))

            situations.forEach { situation ->
                Surface(
                    onClick = {
                        onStatusSelected(situation.status)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = Color(0xFFF8F9FA)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = situation.color.copy(alpha = 0.1f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(situation.icon, contentDescription = null, tint = situation.color, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = situation.label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3E3E3), contentColor = Color.Black)
            ) {
                Text("Cancel")
            }
        }
    }
}
