package de.kjltowinkler.msapp.server

import de.kjltowinkler.msapp.data.auth.RegisterRequest
import de.kjltowinkler.msapp.data.substitution.SubstitutionEntry
import de.kjltowinkler.msapp.data.sync.SyncTasksRequest
import de.kjltowinkler.msapp.data.task.TaskItem
import de.kjltowinkler.msapp.data.task.TaskList
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
import kotlin.test.assertFalse
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
    fun testTaskSyncAndUserIsolation() = runBlocking {
        val suffixA = System.currentTimeMillis()
        val userA = UserRepository.createUser(
            RegisterRequest("user_a_$suffixA", "usera_$suffixA@test.de", "Pw123!", "User A", "09AR")
        )
        val userB = UserRepository.createUser(
            RegisterRequest("user_b_$suffixA", "userb_$suffixA@test.de", "Pw123!", "User B", "09AR")
        )

        // User A erzeugt Aufgaben und eine private Liste
        val listA = TaskList(id = "list_chemie", title = "Chemie Projekt")
        val taskA1 = TaskItem(id = "task_a1", title = "Protokoll schreiben", listId = "list_chemie")
        val taskA2 = TaskItem(id = "task_a2", title = "Versuch 2 vorbereiten", listId = "my_tasks")

        val syncResA = TaskRepository.sync(
            userA.id,
            SyncTasksRequest(tasks = listOf(taskA1, taskA2), lists = listOf(listA))
        )
        assertEquals(2, syncResA.tasks.size)
        assertTrue(syncResA.lists.any { it.id == "list_chemie" })

        // User B erzeugt eigene Aufgabe
        val taskB1 = TaskItem(id = "task_b1", title = "Hausaufgabe Englisch", listId = "my_tasks")
        val syncResB = TaskRepository.sync(
            userB.id,
            SyncTasksRequest(tasks = listOf(taskB1))
        )
        assertEquals(1, syncResB.tasks.size)
        assertEquals("task_b1", syncResB.tasks[0].id)
        assertFalse(syncResB.lists.any { it.id == "list_chemie" })

        // User B versucht task_a1 von User A zu löschen -> darf keine Auswirkung auf User A haben
        TaskRepository.sync(
            userB.id,
            SyncTasksRequest(deletedTaskIds = listOf("task_a1"))
        )
        val tasksAfterAttempt = TaskRepository.getTasks(userA.id)
        assertTrue(tasksAfterAttempt.any { it.id == "task_a1" }, "User A's Task darf nicht von User B gelöscht werden")

        // User A löscht task_a1 und list_chemie
        val syncResA2 = TaskRepository.sync(
            userA.id,
            SyncTasksRequest(
                deletedTaskIds = listOf("task_a1"),
                deletedListIds = listOf("list_chemie")
            )
        )
        assertFalse(syncResA2.tasks.any { it.id == "task_a1" })
        assertFalse(syncResA2.lists.any { it.id == "list_chemie" })
        assertTrue(syncResA2.tasks.any { it.id == "task_a2" })

        // User B's Aufgabe existiert weiterhin unverändert
        val tasksB = TaskRepository.getTasks(userB.id)
        assertEquals(1, tasksB.size)
        assertEquals("task_b1", tasksB[0].id)
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

    @Test
    fun testTotpService() {
        // RFC 6238 Key: ASCII "12345678901234567890" = 20 Bytes
        val keyAscii = "12345678901234567890"
        val keyBytes = keyAscii.toByteArray(Charsets.US_ASCII)
        val base32Secret = de.kjltowinkler.msapp.server.security.TotpService.encodeBase32(keyBytes)
        println("Encoded Base32: $base32Secret")
        assertEquals("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", base32Secret)

        val decoded = de.kjltowinkler.msapp.server.security.TotpService.decodeBase32(base32Secret)
        assertNotNull(decoded)
        assertEquals(keyAscii, String(decoded, Charsets.US_ASCII))

        // RFC 6238 Table 1 test vector: Time = 59s -> Code = 287082
        val code59 = de.kjltowinkler.msapp.server.security.TotpService.generateCodeForStep(keyBytes, 59L / 30L)
        assertEquals("287082", code59)

        // RFC 6238 Table 1 test vector: Time = 1111111109s -> Code = 081804
        val code1111111109 = de.kjltowinkler.msapp.server.security.TotpService.generateCodeForStep(keyBytes, 1111111109L / 30L)
        assertEquals("081804", code1111111109)

        // RFC 6238 Table 1 test vector: Time = 1234567890s -> Code = 005924
        val code1234567890 = de.kjltowinkler.msapp.server.security.TotpService.generateCodeForStep(keyBytes, 1234567890L / 30L)
        assertEquals("005924", code1234567890)

        // Verify with verifyCode()
        assertTrue(de.kjltowinkler.msapp.server.security.TotpService.verifyCode(base32Secret, "005924", timeMillis = 1234567890L * 1000L))
    }
}
