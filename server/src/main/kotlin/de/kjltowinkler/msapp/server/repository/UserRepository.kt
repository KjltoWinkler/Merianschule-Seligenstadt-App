package de.kjltowinkler.msapp.server.repository

import de.kjltowinkler.msapp.data.auth.RegisterRequest
import de.kjltowinkler.msapp.data.auth.UpdateProfileRequest
import de.kjltowinkler.msapp.data.auth.UserProfileDto
import de.kjltowinkler.msapp.server.database.DatabaseFactory
import de.kjltowinkler.msapp.server.security.PasswordHasher
import java.sql.ResultSet
import java.util.UUID

data class UserRecord(
    val id: String,
    val username: String,
    val email: String,
    val studentId: String = "",
    val passwordHash: String,
    val passwordSalt: String,
    val fullName: String,
    val className: String,
    val schoolType: String,
    val classTeacher: String,
    val avatarType: String,
    val avatarColorHex: Long,
    val role: String,
    val hasCustomPassword: Boolean = true,
    val isTwoFactorEnabled: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDto(): UserProfileDto = UserProfileDto(
        id = id,
        username = username,
        email = email,
        studentId = studentId,
        fullName = fullName,
        className = className,
        schoolType = schoolType,
        classTeacher = classTeacher,
        avatarType = avatarType,
        avatarColorHex = avatarColorHex,
        role = role,
        hasCustomPassword = hasCustomPassword,
        isTwoFactorEnabled = isTwoFactorEnabled,
        createdAt = createdAt
    )
}

object UserRepository {

    private fun generateUniqueStudentId(conn: java.sql.Connection): String {
        for (i in 0 until 50) {
            val candidate = "#" + (100000 + (Math.random() * 900000).toInt())
            val exists = conn.prepareStatement("SELECT 1 FROM users WHERE student_id = ? LIMIT 1").use { stmt ->
                stmt.setString(1, candidate)
                stmt.executeQuery().use { rs -> rs.next() }
            }
            if (!exists) return candidate
        }
        return "#" + (System.currentTimeMillis() % 1000000L).toString().padStart(6, '0')
    }

    private fun mapResultSet(rs: ResultSet): UserRecord {
        val hasCustom = runCatching { rs.getInt("has_custom_password") == 1 }.getOrDefault(true)
        val is2fa = runCatching { rs.getInt("is_two_factor_enabled") == 1 }.getOrDefault(false)
        val sId = runCatching { rs.getString("student_id") ?: "" }.getOrDefault("")
        return UserRecord(
            id = rs.getString("id"),
            username = rs.getString("username"),
            email = rs.getString("email"),
            studentId = sId,
            passwordHash = rs.getString("password_hash"),
            passwordSalt = rs.getString("password_salt"),
            fullName = rs.getString("full_name"),
            className = rs.getString("class_name"),
            schoolType = rs.getString("school_type"),
            classTeacher = rs.getString("class_teacher"),
            avatarType = rs.getString("avatar_type"),
            avatarColorHex = rs.getLong("avatar_color_hex"),
            role = rs.getString("role"),
            hasCustomPassword = hasCustom,
            isTwoFactorEnabled = is2fa,
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at")
        )
    }

    suspend fun findByUsernameOrEmail(identifier: String): UserRecord? = DatabaseFactory.query { conn ->
        findByUsernameOrEmailSync(conn, identifier)
    }

    suspend fun findById(id: String): UserRecord? = DatabaseFactory.query { conn ->
        val sql = "SELECT * FROM users WHERE id = ? LIMIT 1;"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, id)
            stmt.executeQuery().use { rs ->
                if (rs.next()) mapResultSet(rs) else null
            }
        }
    }

    suspend fun createUser(
        request: RegisterRequest,
        role: String = "USER",
        hasCustomPassword: Boolean = true,
        isTwoFactorEnabled: Boolean = false,
        studentId: String = ""
    ): UserRecord = DatabaseFactory.query { conn ->
        val existing = findByUsernameOrEmailSync(conn, request.username) ?: findByUsernameOrEmailSync(conn, request.email)
        if (existing != null) {
            throw IllegalArgumentException("Benutzername oder E-Mail ist bereits vergeben.")
        }

        val finalStudentId = if (studentId.isNotBlank()) {
            if (studentId.startsWith("#")) studentId else "#$studentId"
        } else {
            generateUniqueStudentId(conn)
        }

        val id = UUID.randomUUID().toString()
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword(request.password, salt)
        val now = System.currentTimeMillis()

        val sql = """
            INSERT INTO users (id, username, email, password_hash, password_salt, full_name, class_name, school_type, class_teacher, avatar_type, avatar_color_hex, role, has_custom_password, is_two_factor_enabled, student_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, id)
            stmt.setString(2, request.username.trim())
            stmt.setString(3, request.email.trim())
            stmt.setString(4, hash)
            stmt.setString(5, salt)
            stmt.setString(6, request.fullName.trim())
            stmt.setString(7, request.className.trim())
            stmt.setString(8, request.schoolType.trim())
            stmt.setString(9, request.classTeacher.trim())
            stmt.setString(10, request.avatarType)
            stmt.setLong(11, request.avatarColorHex)
            stmt.setString(12, role)
            stmt.setInt(13, if (hasCustomPassword) 1 else 0)
            stmt.setInt(14, if (isTwoFactorEnabled) 1 else 0)
            stmt.setString(15, finalStudentId)
            stmt.setLong(16, now)
            stmt.setLong(17, now)
            stmt.executeUpdate()
        }

        // Initialize default task list for user
        val listSql = "INSERT OR IGNORE INTO task_lists (id, user_id, title, updated_at) VALUES ('my_tasks', ?, 'Meine Aufgaben', ?);"
        conn.prepareStatement(listSql).use { stmt ->
            stmt.setString(1, id)
            stmt.setLong(2, now)
            stmt.executeUpdate()
        }

        UserRecord(
            id = id,
            username = request.username.trim(),
            email = request.email.trim(),
            studentId = finalStudentId,
            passwordHash = hash,
            passwordSalt = salt,
            fullName = request.fullName.trim(),
            className = request.className.trim(),
            schoolType = request.schoolType.trim(),
            classTeacher = request.classTeacher.trim(),
            avatarType = request.avatarType,
            avatarColorHex = request.avatarColorHex,
            role = role,
            hasCustomPassword = hasCustomPassword,
            isTwoFactorEnabled = isTwoFactorEnabled,
            createdAt = now,
            updatedAt = now
        )
    }

    suspend fun setHasCustomPassword(userId: String, hasCustom: Boolean): Boolean = DatabaseFactory.query { conn ->
        val sql = "UPDATE users SET has_custom_password = ?, updated_at = ? WHERE id = ?;"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, if (hasCustom) 1 else 0)
            stmt.setLong(2, System.currentTimeMillis())
            stmt.setString(3, userId)
            stmt.executeUpdate() > 0
        }
    }

    suspend fun setTwoFactorEnabled(userId: String, enabled: Boolean): Boolean = DatabaseFactory.query { conn ->
        val sql = "UPDATE users SET is_two_factor_enabled = ?, updated_at = ? WHERE id = ?;"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, if (enabled) 1 else 0)
            stmt.setLong(2, System.currentTimeMillis())
            stmt.setString(3, userId)
            stmt.executeUpdate() > 0
        }
    }

    private fun findByUsernameOrEmailSync(conn: java.sql.Connection, identifier: String): UserRecord? {
        val trimmed = identifier.trim()
        val withHash = if (trimmed.startsWith("#")) trimmed else "#$trimmed"
        val withoutHash = if (trimmed.startsWith("#")) trimmed.substring(1) else trimmed

        val sql = "SELECT * FROM users WHERE username = ? OR email = ? OR student_id = ? OR student_id = ? LIMIT 1;"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, trimmed)
            stmt.setString(2, trimmed)
            stmt.setString(3, withHash)
            stmt.setString(4, withoutHash)
            stmt.executeQuery().use { rs ->
                return if (rs.next()) mapResultSet(rs) else null
            }
        }
    }

    suspend fun updateProfile(userId: String, request: UpdateProfileRequest): UserProfileDto? = DatabaseFactory.query { conn ->
        val current = findByIdSync(conn, userId) ?: return@query null
        val now = System.currentTimeMillis()

        val newFullName = request.fullName ?: current.fullName
        val newClassName = request.className ?: current.className
        val newSchoolType = request.schoolType ?: current.schoolType
        val newClassTeacher = request.classTeacher ?: current.classTeacher
        val newAvatarType = request.avatarType ?: current.avatarType
        val newAvatarColorHex = request.avatarColorHex ?: current.avatarColorHex

        val sql = """
            UPDATE users SET 
                full_name = ?, class_name = ?, school_type = ?, 
                class_teacher = ?, avatar_type = ?, avatar_color_hex = ?, updated_at = ?
            WHERE id = ?;
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, newFullName)
            stmt.setString(2, newClassName)
            stmt.setString(3, newSchoolType)
            stmt.setString(4, newClassTeacher)
            stmt.setString(5, newAvatarType)
            stmt.setLong(6, newAvatarColorHex)
            stmt.setLong(7, now)
            stmt.setString(8, userId)
            stmt.executeUpdate()
        }

        UserProfileDto(
            id = current.id,
            username = current.username,
            email = current.email,
            studentId = current.studentId,
            fullName = newFullName,
            className = newClassName,
            schoolType = newSchoolType,
            classTeacher = newClassTeacher,
            avatarType = newAvatarType,
            avatarColorHex = newAvatarColorHex,
            role = current.role,
            createdAt = current.createdAt
        )
    }

    suspend fun updatePassword(userId: String, newPasswordHash: String, newPasswordSalt: String): Boolean = DatabaseFactory.query { conn ->
        val sql = "UPDATE users SET password_hash = ?, password_salt = ?, updated_at = ? WHERE id = ?;"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, newPasswordHash)
            stmt.setString(2, newPasswordSalt)
            stmt.setLong(3, System.currentTimeMillis())
            stmt.setString(4, userId)
            stmt.executeUpdate() > 0
        }
    }

    suspend fun countUsers(): Int = DatabaseFactory.query { conn ->
        val sql = "SELECT COUNT(*) FROM users;"
        conn.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                if (rs.next()) rs.getInt(1) else 0
            }
        }
    }

    suspend fun getAllUsers(): List<UserProfileDto> = DatabaseFactory.query { conn ->
        val sql = "SELECT * FROM users ORDER BY created_at DESC;"
        conn.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                val list = mutableListOf<UserProfileDto>()
                while (rs.next()) {
                    list.add(mapResultSet(rs).toDto())
                }
                list
            }
        }
    }

    suspend fun deleteUser(id: String): Boolean = DatabaseFactory.query { conn ->
        val sql = "DELETE FROM users WHERE id = ?;"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, id)
            stmt.executeUpdate() > 0
        }
    }

    suspend fun resetPassword(userId: String, newPasswordPlain: String): Boolean {
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword(newPasswordPlain, salt)
        return updatePassword(userId, hash, salt)
    }

    private fun findByIdSync(conn: java.sql.Connection, id: String): UserRecord? {
        val sql = "SELECT * FROM users WHERE id = ? LIMIT 1;"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, id)
            stmt.executeQuery().use { rs ->
                return if (rs.next()) mapResultSet(rs) else null
            }
        }
    }
}
