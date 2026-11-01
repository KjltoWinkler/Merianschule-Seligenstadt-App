package de.kjltowinkler.msapp.server.websocket

import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

object TaskWebSocketManager {
    // Maps userId to set of active sessions
    private val userSessions = ConcurrentHashMap<String, CopyOnWriteArraySet<DefaultWebSocketSession>>()

    fun register(userId: String, session: DefaultWebSocketSession) {
        userSessions.computeIfAbsent(userId) { CopyOnWriteArraySet() }.add(session)
    }

    fun unregister(userId: String, session: DefaultWebSocketSession) {
        val sessions = userSessions[userId] ?: return
        sessions.remove(session)
        if (sessions.isEmpty()) {
            userSessions.remove(userId)
        }
    }

    suspend fun notifyTaskUpdate(userId: String, reason: String = "tasks_updated") {
        val sessions = userSessions[userId] ?: return
        val message = """{"event":"tasks_updated","reason":"$reason","timestamp":${System.currentTimeMillis()}}"""
        coroutineScope {
            for (session in sessions) {
                launch {
                    try {
                        session.send(Frame.Text(message))
                    } catch (_: Exception) {
                        // Ignore dead sessions; will be cleaned up on close
                    }
                }
            }
        }
    }

    fun getActiveSessionCount(userId: String): Int {
        return userSessions[userId]?.size ?: 0
    }
}
