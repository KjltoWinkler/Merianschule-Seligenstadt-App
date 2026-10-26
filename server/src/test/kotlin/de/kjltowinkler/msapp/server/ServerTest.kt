package de.kjltowinkler.msapp.server

import de.kjltowinkler.msapp.data.auth.RegisterRequest
import de.kjltowinkler.msapp.data.substitution.SubstitutionEntry
import de.kjltowinkler.msapp.data.sync.SyncTasksRequest
import de.kjltowinkler.msapp.data.task.TaskItem
import de.kjltowinkler.msapp.server.config.ServerConfig
import de.kjltowinkler.msapp.server.database.DatabaseFactory
import de.kjltowinkler.msapp.server.repository.SubstitutionServerRepository
import de.kjltowinkler.msapp.server.repository.TaskRepository
import de.kjltowinkler.msapp.server.repository.UserRepository
import de.kjltowinkler.msapp.server.security.JwtService
import de.kjltowinkler.msapp.server.security.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ServerTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun setup() {
            val testDb = File("build/test_merianschule.db")
            if (testDb.exists()) testDb.delete()

            System.setProperty("DB_PATH", testDb.absolutePath)
            DatabaseFactory.init()
        }
    }

    @Test
    fun testPasswordHasherAndJwt() {
        val password = "SuperSecretPassword123!"
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword(password, salt)

        assertTrue(PasswordHasher.verifyPassword(password, salt, hash))

        val token = JwtService.generateToken("user-123", "testuser", "USER")
        val decoded = JwtService.verifier.verify(token)
        assertEquals("user-123", decoded.getClaim("userId").asString())
        assertEquals("testuser", decoded.getClaim("username").asString())
    }

    @Test
    fun testUserAndTaskOperations() = runBlocking {
        val suffix = System.currentTimeMillis()
        val registerReq = RegisterRequest(
            username = "max_$suffix",
            email = "max_$suffix@merianschule-seligenstadt.de",
            password = "Passwort123!",
            fullName = "Max Mustermann",
            className = "09AR"
        )
        val user = UserRepository.createUser(registerReq)
        assertNotNull(user.id)
        assertEquals("max_$suffix", user.username)

        // Add a task
        val task = TaskItem(
            id = "task-1",
            title = "Mathe Hausaufgabe S. 42",
            description = "Aufgabe 1-4",
            isCompleted = false
        )
        TaskRepository.upsertTask(user.id, task)

        val tasks = TaskRepository.getTasks(user.id)
        assertTrue(tasks.any { it.id == "task-1" && it.title == "Mathe Hausaufgabe S. 42" })

        // Sync test
        val syncReq = SyncTasksRequest(
            tasks = listOf(
                task.copy(isCompleted = true)
            )
        )
        val syncRes = TaskRepository.sync(user.id, syncReq)
        assertTrue(syncRes.tasks.first { it.id == "task-1" }.isCompleted)
    }

    @Test
    fun testSubstitutionJsonReplacement() = runBlocking {
        val testEntries = listOf(
            SubstitutionEntry(
                id = "test_sub_1",
                date = "2026-10-25",
                dayLabel = "Freitag, 25. Oktober",
                period = "1. Std",
                subject = "Physik",
                className = "09AR",
                type = "Entfall",
                details = "Lehrer krank",
                roomInfo = "Freistunde",
                time = "07:45"
            )
        )
        val count = SubstitutionServerRepository.replaceEntries(testEntries)
        assertEquals(1, count)

        val retrieved = SubstitutionServerRepository.getByClass("09AR")
        assertEquals(1, retrieved.size)
        assertEquals("Physik", retrieved[0].subject)
    }
}
