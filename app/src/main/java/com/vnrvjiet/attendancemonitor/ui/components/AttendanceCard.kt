package com.vnrvjiet.attendancemonitor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vnrvjiet.attendancemonitor.data.model.AttendanceStatus

@Composable
fun AttendanceCard(
    subject: String,
    faculty: String,
    time: String,
    room: String,
    isAttendancePeriod: Boolean,
    isNow: Boolean = false,
    statusText: String? = null,
    accentColor: Color = Color(0xFF6200EE),
    currentStatus: AttendanceStatus? = null,
    onStatusSelected: (AttendanceStatus) -> Unit = {},
    onMoreClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(accentColor)
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = time,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isNow) Color.Blue else Color.Gray
                        )
                        if (isNow) {
                            Text(
                                text = "NOW",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentStatus != null) {
                            StatusChip(status = currentStatus)
                        }
                        
                        if (statusText != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFFFFF3E0),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.WarningAmber,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color(0xFFE65100)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = statusText,
                                        fontSize = 10.sp,
                                        color = Color(0xFFE65100),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subject,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Text(text = " Room $room", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    if (faculty.isNotEmpty() && faculty != "-") {
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                        Text(text = " $faculty", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }

                if (isAttendancePeriod) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AttendanceActionButton(
                            label = "PRESENT",
                            icon = Icons.Default.Done,
                            containerColor = if (currentStatus == AttendanceStatus.PRESENT) Color(0xFF2E7D32) else Color(0xFFE8F5E9),
                            contentColor = if (currentStatus == AttendanceStatus.PRESENT) Color.White else Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f),
                            onClick = { onStatusSelected(AttendanceStatus.PRESENT) }
                        )
                        AttendanceActionButton(
                            label = "ABSENT",
                            icon = Icons.Default.Close,
                            containerColor = if (currentStatus == AttendanceStatus.ABSENT) Color(0xFFC62828) else Color(0xFFFFEBEE),
                            contentColor = if (currentStatus == AttendanceStatus.ABSENT) Color.White else Color(0xFFC62828),
                            modifier = Modifier.weight(1f),
                            onClick = { onStatusSelected(AttendanceStatus.ABSENT) }
                        )
                        AttendanceActionButton(
                            label = "BUNK",
                            icon = Icons.Default.Bolt,
                            containerColor = if (currentStatus == AttendanceStatus.BUNK) Color(0xFFF9A825) else Color(0xFFFFF8E1),
                            contentColor = if (currentStatus == AttendanceStatus.BUNK) Color.White else Color(0xFFF9A825),
                            modifier = Modifier.weight(1f),
                            onClick = { onStatusSelected(AttendanceStatus.BUNK) }
                        )
                        AttendanceActionButton(
                            label = "MORE",
                            icon = Icons.Default.MoreHoriz,
                            containerColor = Color(0xFFF5F5F5),
                            contentColor = Color.Gray,
                            modifier = Modifier.weight(1f),
                            onClick = onMoreClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: AttendanceStatus) {
    val (color, label) = when (status) {
        AttendanceStatus.PRESENT -> Color(0xFF2E7D32) to "PRESENT"
        AttendanceStatus.ABSENT -> Color(0xFFC62828) to "ABSENT"
        AttendanceStatus.BUNK -> Color(0xFFF9A825) to "BUNK"
        AttendanceStatus.HOLIDAY -> Color(0xFF2196F3) to "HOLIDAY"
        AttendanceStatus.CLASS_CANCELLED -> Color(0xFFEF5350) to "CANCELLED"
        AttendanceStatus.VOLUNTEER -> Color(0xFF66BB6A) to "VOLUNTEER"
        AttendanceStatus.COLLEGE_EVENT -> Color(0xFFFFA726) to "EVENT"
        AttendanceStatus.HACKATHON -> Color(0xFF7E57C2) to "HACKATHON"
        AttendanceStatus.SPORTS_EVENT -> Color(0xFFFF7043) to "SPORTS"
        AttendanceStatus.MEDICAL_LEAVE -> Color(0xFFEC407A) to "MEDICAL"
        AttendanceStatus.OTHER -> Color.Gray to "OTHER"
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun AttendanceActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = modifier.height(60.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = contentColor)
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = contentColor)
        }
    }
}
