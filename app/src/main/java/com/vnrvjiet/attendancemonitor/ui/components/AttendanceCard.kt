package com.vnrvjiet.attendancemonitor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vnrvjiet.attendancemonitor.data.model.AttendanceStatus
import com.vnrvjiet.attendancemonitor.ui.theme.*

@Composable
fun AttendanceCard(
    subject: String,
    time: String,
    isAttendancePeriod: Boolean,
    isNow: Boolean = false,
    accentColor: Color = Color(0xFF6200EE),
    currentStatus: AttendanceStatus? = null,
    onStatusSelected: (AttendanceStatus) -> Unit = {},
    onMoreClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(accentColor)
            )

            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = time,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isNow) {
                            Text(
                                text = "NOW",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (currentStatus != null) {
                        StatusChip(status = currentStatus)
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subject,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (isAttendancePeriod) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AttendanceActionButton(
                            label = "PRESENT",
                            icon = Icons.Default.Done,
                            containerColor = if (currentStatus == AttendanceStatus.PRESENT) StatusVerified else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (currentStatus == AttendanceStatus.PRESENT) Color.White else StatusVerified,
                            modifier = Modifier.weight(1f),
                            onClick = { onStatusSelected(AttendanceStatus.PRESENT) }
                        )
                        AttendanceActionButton(
                            label = "ABSENT",
                            icon = Icons.Default.Close,
                            containerColor = if (currentStatus == AttendanceStatus.ABSENT) StatusMismatch else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (currentStatus == AttendanceStatus.ABSENT) Color.White else StatusMismatch,
                            modifier = Modifier.weight(1f),
                            onClick = { onStatusSelected(AttendanceStatus.ABSENT) }
                        )
                        AttendanceActionButton(
                            label = "BUNK",
                            icon = Icons.Default.Bolt,
                            containerColor = if (currentStatus == AttendanceStatus.BUNK) Color(0xFFF9A825) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (currentStatus == AttendanceStatus.BUNK) Color.White else Color(0xFFF9A825),
                            modifier = Modifier.weight(1f),
                            onClick = { onStatusSelected(AttendanceStatus.BUNK) }
                        )
                        AttendanceActionButton(
                            label = "MORE",
                            icon = Icons.Default.MoreHoriz,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
        AttendanceStatus.PRESENT -> StatusVerified to "PRESENT"
        AttendanceStatus.ABSENT -> StatusMismatch to "ABSENT"
        AttendanceStatus.BUNK -> Color(0xFFF9A825) to "BUNK"
        AttendanceStatus.HOLIDAY -> StatusUnexpected to "HOLIDAY"
        AttendanceStatus.CLASS_CANCELLED -> StatusMismatch to "CANCELLED"
        AttendanceStatus.VOLUNTEER -> StatusVerified to "VOLUNTEER"
        AttendanceStatus.COLLEGE_EVENT -> Color(0xFFFFA726) to "EVENT"
        AttendanceStatus.HACKATHON -> Color(0xFF7E57C2) to "HACKATHON"
        AttendanceStatus.SPORTS_EVENT -> Color(0xFFFF7043) to "SPORTS"
        AttendanceStatus.MEDICAL_LEAVE -> Color(0xFFEC407A) to "MEDICAL"
        AttendanceStatus.OTHER -> MaterialTheme.colorScheme.onSurfaceVariant to "OTHER"
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        modifier = Modifier.widthIn(min = 60.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            maxLines = 1,
            softWrap = false,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
