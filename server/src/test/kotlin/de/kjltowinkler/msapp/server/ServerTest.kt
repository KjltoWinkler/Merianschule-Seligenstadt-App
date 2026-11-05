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
import kotlin.test.assertNull
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

    @Test
    fun testRealtimeWebSocketTokenVerificationAndManager() = runBlocking {
        val userId = "user_ws_test_99"
        val validToken = JwtService.generateToken(userId, "wstest", "USER")
        val verifiedUserId = JwtService.verifyToken(validToken)
        assertEquals(userId, verifiedUserId)

        // Invalid token test
        val invalidToken = validToken + "corrupted"
        val invalidResult = JwtService.verifyToken(invalidToken)
        assertEquals(null, invalidResult)

        // Temp 2FA token darf nicht als reguläres Token für WebSockets verwendet werden
        val tempToken = JwtService.generate2FaTempToken(userId, "wstest")
        val tempResult = JwtService.verifyToken(tempToken)
        assertEquals(null, tempResult)

        // Test TaskWebSocketManager
        val initialCount = de.kjltowinkler.msapp.server.websocket.TaskWebSocketManager.getActiveSessionCount(userId)
        assertEquals(0, initialCount)

        // Notification an User ohne Sessions darf ohne Fehler durchlaufen
        de.kjltowinkler.msapp.server.websocket.TaskWebSocketManager.notifyTaskUpdate(userId, "test_event")
    }

    @Test
    fun testDataEncryptionServiceDirectly() {
        val plaintext = "Geheime Schüler-Notiz für Mathe: 1+1=2"
        val encrypted = de.kjltowinkler.msapp.server.security.DataEncryptionService.encrypt(plaintext)
        assertNotNull(encrypted)
        assertTrue(encrypted.startsWith("enc:v1:"), "Verschlüsselter Text muss Präfix enc:v1: haben")
        assertFalse(encrypted.contains("Geheime"), "Ciphertext darf keinen Klartext enthalten")

        val decrypted = de.kjltowinkler.msapp.server.security.DataEncryptionService.decrypt(encrypted)
        assertEquals(plaintext, decrypted, "Entschlüsselter Text muss dem Original entsprechen")

        // Legacy Klartext ohne Präfix enc:v1: muss transparent zurückgegeben werden (Rückwärtskompatibilität)
        val legacyPlaintext = "Alte unverschlüsselte Aufgabe"
        val decryptedLegacy = de.kjltowinkler.msapp.server.security.DataEncryptionService.decrypt(legacyPlaintext)
        assertEquals(legacyPlaintext, decryptedLegacy)

        // Null- & Empty-Handling
        assertEquals("", de.kjltowinkler.msapp.server.security.DataEncryptionService.encrypt(null))
        assertEquals("", de.kjltowinkler.msapp.server.security.DataEncryptionService.decrypt(null))
        assertEquals("", de.kjltowinkler.msapp.server.security.DataEncryptionService.encrypt(""))
        assertEquals("", de.kjltowinkler.msapp.server.security.DataEncryptionService.decrypt(""))

        // Manipulierter Ciphertext (AES-GCM Auth Tag Failure)
        val tampered = encrypted.dropLast(2) + "=="
        val decryptedTampered = de.kjltowinkler.msapp.server.security.DataEncryptionService.decrypt(tampered)
        // Bei Integritätsverletzung gibt decrypt für Fehlertoleranz den Fallback zurück
        assertEquals(tampered, decryptedTampered)
    }

    @Test
    fun testTaskEncryptionAtRestAndGdprProtection() = runBlocking {
        val suffix = System.currentTimeMillis()
        val user = UserRepository.createUser(
            RegisterRequest("gdpr_user_$suffix", "gdpr_$suffix@school.de", "Pw123!", "Datenschutz Test", "10BR")
        )

        val taskTitle = "Sensible Schüleraufgabe Biologie"
        val taskDesc = "Persönliche Notizen zur Klassenarbeit"
        val listTitle = "Mein Privates Projekt"

        val privateList = TaskList(id = "list_privat_$suffix", title = listTitle)
        val privateTask = TaskItem(
            id = "task_privat_$suffix",
            title = taskTitle,
            description = taskDesc,
            listId = privateList.id,
            subject = "Biologie"
        )

        // Syncen / Speichern
        TaskRepository.sync(user.id, SyncTasksRequest(tasks = listOf(privateTask), lists = listOf(privateList)))

        // 1. Direkte Prüfung in der SQLite-Datenbank: Die Daten müssen dort zwingend AES-256-GCM verschlüsselt liegen!
        DatabaseFactory.query { conn ->
            conn.prepareStatement("SELECT title, description, subject FROM tasks WHERE id = ?").use { stmt ->
                stmt.setString(1, privateTask.id)
                val rs = stmt.executeQuery()
                assertTrue(rs.next(), "Task muss in SQLite-Tabelle tasks existieren")

                val rawTitle = rs.getString("title")
                val rawDesc = rs.getString("description")
                val rawSubject = rs.getString("subject")

                assertTrue(rawTitle.startsWith("enc:v1:"), "Titel in Datenbank muss mit enc:v1: verschlüsselt sein. Ist: $rawTitle")
                assertTrue(rawDesc.startsWith("enc:v1:"), "Beschreibung in Datenbank muss mit enc:v1: verschlüsselt sein. Ist: $rawDesc")
                assertTrue(rawSubject.startsWith("enc:v1:"), "Fach in Datenbank muss mit enc:v1: verschlüsselt sein. Ist: $rawSubject")

                assertFalse(rawTitle.contains("Sensible"), "Rohdaten in DB dürfen keinen Klartext-Titel enthalten")
                assertFalse(rawDesc.contains("Persönliche"), "Rohdaten in DB dürfen keine Klartext-Beschreibung enthalten")
            }

            conn.prepareStatement("SELECT title FROM task_lists WHERE id = ?").use { stmt ->
                stmt.setString(1, privateList.id)
                val rs = stmt.executeQuery()
                assertTrue(rs.next(), "Liste muss in SQLite-Tabelle task_lists existieren")

                val rawListTitle = rs.getString("title")
                assertTrue(rawListTitle.startsWith("enc:v1:"), "Listentitel in DB muss verschlüsselt sein. Ist: $rawListTitle")
                assertFalse(rawListTitle.contains("Privates"), "Rohdaten des Listentitels dürfen keinen Klartext enthalten")
            }
        }

        // 2. Client-Abfrage: Der authentifizierte Schüler erhält seine Daten transparent entschlüsselt zurück
        val userTasks = TaskRepository.getTasks(user.id)
        val retrievedTask = userTasks.first { it.id == privateTask.id }
        assertEquals(taskTitle, retrievedTask.title)
        assertEquals(taskDesc, retrievedTask.description)
        assertEquals("Biologie", retrievedTask.subject)

        val userLists = TaskRepository.getLists(user.id)
        val retrievedList = userLists.first { it.id == privateList.id }
        assertEquals(listTitle, retrievedList.title)

        // 3. DSGVO Schutz: getAllUserTasks() für Admins muss zwingend leer sein (Datensparsamkeit)
        val adminExposedTasks = TaskRepository.getAllUserTasks()
        assertTrue(adminExposedTasks.isEmpty(), "DSGVO-Schutz: Schüleraufgaben dürfen niemals an Admin-Schnittstelle exposed werden!")

        // 4. Verschlüsselung von 2FA-Secrets in der Datenbank
        val base32Secret = "JBSWY3DPEHPK3PXP"
        UserRepository.setTwoFactorTempSecret(user.id, base32Secret)

        DatabaseFactory.query { conn ->
            conn.prepareStatement("SELECT two_factor_temp_secret FROM users WHERE id = ?").use { stmt ->
                stmt.setString(1, user.id)
                val rs = stmt.executeQuery()
                assertTrue(rs.next())
                val rawSecretInDb = rs.getString("two_factor_temp_secret")
                assertTrue(rawSecretInDb.startsWith("enc:v1:"), "2FA-Secret in DB muss verschlüsselt sein. Ist: $rawSecretInDb")
                assertFalse(rawSecretInDb.contains(base32Secret), "2FA-Secret darf nicht als Klartext in der DB liegen")
            }
        }

        val userRecord = UserRepository.findById(user.id)
        assertEquals(base32Secret, userRecord?.twoFactorTempSecret, "UserRepository muss 2FA-Secret transparent entschlüsseln")
    }

    @Test
    fun testInitialPasswordLifecycleAndEncryption() = runBlocking {
        val suffix = System.currentTimeMillis()
        val tempPassword = "InitialStartPassword123!"

        // 1. User created by admin (hasCustomPassword = false)
        val createdUser = UserRepository.createUser(
            RegisterRequest("init_pw_$suffix", "init_pw_$suffix@school.de", tempPassword, "Initial PW Tester", "10BR"),
            hasCustomPassword = false
        )

        assertEquals(tempPassword, createdUser.initialPassword)
        assertFalse(createdUser.hasCustomPassword)

        // Verify DTO exposed to admin panel contains initialPassword
        val dto = createdUser.toDto()
        assertEquals(tempPassword, dto.initialPassword)
        assertFalse(dto.hasCustomPassword)

        // Verify database storage is encrypted at rest (AES-256-GCM)
        DatabaseFactory.query { conn ->
            conn.prepareStatement("SELECT initial_password, has_custom_password FROM users WHERE id = ?").use { stmt ->
                stmt.setString(1, createdUser.id)
                val rs = stmt.executeQuery()
                assertTrue(rs.next())
                val rawDbPw = rs.getString("initial_password")
                val hasCustomDb = rs.getInt("has_custom_password")
                assertEquals(0, hasCustomDb)
                assertTrue(rawDbPw.startsWith("enc:v1:"), "Initiales Passwort muss in SQLite mit enc:v1: verschlüsselt gespeichert sein. Ist: $rawDbPw")
                assertFalse(rawDbPw.contains(tempPassword), "Rohdaten in DB dürfen keinesfalls das Klartext-Passwort enthalten")
            }
        }

        // Verify findById & getAllUsers returns decrypted initial password
        val foundUser = UserRepository.findById(createdUser.id)
        assertNotNull(foundUser)
        assertEquals(tempPassword, foundUser.initialPassword)

        val allUsers = UserRepository.getAllUsers()
        val foundDto = allUsers.first { it.id == createdUser.id }
        assertEquals(tempPassword, foundDto.initialPassword)

        // 2. User changes password (e.g. at onboarding or update-password)
        UserRepository.updatePassword(createdUser.id, "newHash", "newSalt")

        val userAfterChange = UserRepository.findById(createdUser.id)
        assertNotNull(userAfterChange)
        assertTrue(userAfterChange.hasCustomPassword)
        assertNull(userAfterChange.initialPassword, "Initiales Passwort muss nach Passwortänderung null sein")
        assertNull(userAfterChange.toDto().initialPassword, "DTO darf kein initiales Passwort mehr enthalten")

        // Verify DB column is completely wiped (NULL)
        DatabaseFactory.query { conn ->
            conn.prepareStatement("SELECT initial_password, has_custom_password FROM users WHERE id = ?").use { stmt ->
                stmt.setString(1, createdUser.id)
                val rs = stmt.executeQuery()
                assertTrue(rs.next())
                assertNull(rs.getString("initial_password"), "Datenbankspalte initial_password muss NULL sein")
                assertEquals(1, rs.getInt("has_custom_password"))
            }
        }

        // 3. Admin resets password
        val newResetPassword = "ResetTempPw456!"
        val resetSuccess = UserRepository.resetPassword(createdUser.id, newResetPassword)
        assertTrue(resetSuccess)

        val userAfterReset = UserRepository.findById(createdUser.id)
        assertNotNull(userAfterReset)
        assertFalse(userAfterReset.hasCustomPassword)
        assertEquals(newResetPassword, userAfterReset.initialPassword)
        assertEquals(newResetPassword, userAfterReset.toDto().initialPassword)

        // Verify reset password is encrypted in DB
        DatabaseFactory.query { conn ->
            conn.prepareStatement("SELECT initial_password, has_custom_password FROM users WHERE id = ?").use { stmt ->
                stmt.setString(1, createdUser.id)
                val rs = stmt.executeQuery()
                assertTrue(rs.next())
                val rawDbPw = rs.getString("initial_password")
                assertEquals(0, rs.getInt("has_custom_password"))
                assertTrue(rawDbPw.startsWith("enc:v1:"), "Zurückgesetztes Passwort muss verschlüsselt in DB liegen")
            }
        }
    }

    @Test
    fun testAutoGeneratedInitialPassword() = runBlocking {
        val suffix = System.currentTimeMillis()
        // Create user with blank password
        val user = UserRepository.createUser(
            RegisterRequest("auto_init_$suffix", "auto_$suffix@school.de", "", "Auto User", "8aH"),
            hasCustomPassword = false
        )

        // 1. Initial password must be automatically generated
        val initialPw = user.initialPassword
        assertNotNull(initialPw)
        assertTrue(initialPw.length >= 8, "Generiertes Initialpasswort muss mindestens 8 Zeichen lang sein")
        assertFalse(user.hasCustomPassword)

        // 2. Initial password must match the hashed password
        val isValid = PasswordHasher.verifyPassword(initialPw, user.passwordSalt, user.passwordHash)
        assertTrue(isValid, "Das automatisch generierte Initialpasswort muss mit dem Passwort-Hash übereinstimmen")

        // 3. User DTO must expose initial password to admin panel
        val dto = user.toDto()
        assertEquals(initialPw, dto.initialPassword)
        assertFalse(dto.hasCustomPassword)

        // 4. In Database, it must be encrypted
        DatabaseFactory.query { conn ->
            conn.prepareStatement("SELECT initial_password, has_custom_password FROM users WHERE id = ?").use { stmt ->
                stmt.setString(1, user.id)
                val rs = stmt.executeQuery()
                assertTrue(rs.next())
                val rawDbPw = rs.getString("initial_password")
                assertTrue(rawDbPw.startsWith("enc:v1:"), "Muss verschlüsselt gespeichert sein")
                assertFalse(rawDbPw.contains(initialPw))
            }
        }

        // 5. Appears in getAllUsers
        val allUsers = UserRepository.getAllUsers()
        val found = allUsers.first { it.id == user.id }
        assertEquals(user.initialPassword, found.initialPassword)

        // 6. User sets their custom password upon onboarding -> initial password must be wiped
        UserRepository.updatePassword(user.id, "myNewHash", "myNewSalt")
        val updated = UserRepository.findById(user.id)
        assertNotNull(updated)
        assertTrue(updated.hasCustomPassword)
        assertNull(updated.initialPassword, "Initialpasswort muss gelöscht sein sobald der Nutzer sein eigenes PW setzt")
        assertNull(updated.toDto().initialPassword)
    }

    @Test
    fun testAppUpdateLifecycle() {
        val original = de.kjltowinkler.msapp.server.routes.AppUpdateRepository.getUpdateInfo()
        assertNotNull(original)

        val newInfo = original.copy(
            versionCode = 999,
            versionName = "9.9.9",
            releaseNotes = "• Test Update Release Notes"
        )
        de.kjltowinkler.msapp.server.routes.AppUpdateRepository.saveUpdateInfo(newInfo)

        val fetched = de.kjltowinkler.msapp.server.routes.AppUpdateRepository.getUpdateInfo()
        assertEquals(999, fetched.versionCode)
        assertEquals("9.9.9", fetched.versionName)
        assertEquals("• Test Update Release Notes", fetched.releaseNotes)

        // Test update push timestamp update
        val pushedTimestamp = System.currentTimeMillis()
        val pushedInfo = fetched.copy(publishedAt = pushedTimestamp)
        de.kjltowinkler.msapp.server.routes.AppUpdateRepository.saveUpdateInfo(pushedInfo)
        val afterPush = de.kjltowinkler.msapp.server.routes.AppUpdateRepository.getUpdateInfo()
        assertEquals(pushedTimestamp, afterPush.publishedAt)

        // Restore original
        de.kjltowinkler.msapp.server.routes.AppUpdateRepository.saveUpdateInfo(original)
    }
}

