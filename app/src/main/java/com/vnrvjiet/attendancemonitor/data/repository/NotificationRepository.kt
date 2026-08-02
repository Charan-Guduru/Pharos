package com.vnrvjiet.attendancemonitor.data.repository

import com.vnrvjiet.attendancemonitor.data.local.dao.NotificationDao
import com.vnrvjiet.attendancemonitor.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val dao: NotificationDao) {
    val allNotifications: Flow<List<NotificationEntity>> = dao.getAllNotifications()
    val unreadCount: Flow<Int> = dao.getUnreadCount()

    suspend fun addNotification(title: String, message: String, type: String) {
        val existing = dao.getLatestUnreadByTypeAndMessage(type, message)
        if (existing != null) {
            // Update timestamp of existing unread notification instead of duplicating
            dao.updateNotification(existing.copy(timestamp = System.currentTimeMillis()))
        } else {
            val notification = NotificationEntity(
                title = title,
                message = message,
                type = type,
                timestamp = System.currentTimeMillis()
            )
            dao.insertNotification(notification)
        }
    }

    suspend fun markAsRead(id: Long) {
        dao.markAsRead(id)
    }

    suspend fun markAllAsRead() {
        dao.markAllAsRead()
    }

    suspend fun delete(notification: NotificationEntity) {
        dao.deleteNotification(notification)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }
}
