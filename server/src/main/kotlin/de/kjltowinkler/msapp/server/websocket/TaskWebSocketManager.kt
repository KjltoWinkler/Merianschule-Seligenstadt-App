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

    suspend fun notifyTaskUpdate(
        userId: String,
        reason: String = "tasks_updated",
        originInstanceId: String? = null
    ) {
        val sessions = userSessions[userId] ?: return
        val originJson = if (originInstanceId != null) "\"$originInstanceId\"" else "null"
        val message = """{"event":"tasks_updated","reason":"$reason","origin":$originJson,"timestamp":${System.currentTimeMillis()}}"""
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

    suspend fun broadcastTaskUpdate(reason: String = "tasks_updated") {
        val message = """{"event":"tasks_updated","reason":"$reason","origin":null,"timestamp":${System.currentTimeMillis()}}"""
        coroutineScope {
            for ((_, sessions) in userSessions) {
                for (session in sessions) {
                    launch {
                        try {
                            session.send(Frame.Text(message))
                        } catch (_: Exception) {
                            // Ignore dead sessions
                        }
                    }
                }
            }
        }
    }

    suspend fun broadcastSubstitutionUpdate(reason: String = "substitutions_updated", count: Int = 0) {
        val message = """{"event":"substitutions_updated","reason":"$reason","count":$count,"timestamp":${System.currentTimeMillis()}}"""
        coroutineScope {
            for ((_, sessions) in userSessions) {
                for (session in sessions) {
                    launch {
                        try {
                            session.send(Frame.Text(message))
                        } catch (_: Exception) {
                            // Ignore dead sessions
                        }
                    }
                }
            }
        }
    }

    suspend fun notifySettingsUpdate(
        userId: String,
        originInstanceId: String? = null
    ) {
        val sessions = userSessions[userId] ?: return
        val originJson = if (originInstanceId != null) "\"$originInstanceId\"" else "null"
        val message = """{"event":"settings_updated","origin":$originJson,"timestamp":${System.currentTimeMillis()}}"""
        coroutineScope {
            for (session in sessions) {
                launch {
                    try {
                        session.send(Frame.Text(message))
                    } catch (_: Exception) {
                        // Ignore dead sessions
                    }
                }
            }
        }
    }

    fun getActiveSessionCount(userId: String): Int {
        return userSessions[userId]?.size ?: 0
    }
}
