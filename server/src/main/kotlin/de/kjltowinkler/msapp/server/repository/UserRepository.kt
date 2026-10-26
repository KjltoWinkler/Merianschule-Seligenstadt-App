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
    val passwordHash: String,
    val passwordSalt: String,
    val fullName: String,
    val className: String,
    val schoolType: String,
    val classTeacher: String,
    val avatarType: String,
    val avatarColorHex: Long,
    val role: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDto(): UserProfileDto = UserProfileDto(
        id = id,
        username = username,
        email = email,
        fullName = fullName,
        className = className,
        schoolType = schoolType,
        classTeacher = classTeacher,
        avatarType = avatarType,
        avatarColorHex = avatarColorHex,
        role = role,
        createdAt = createdAt
    )
}

object UserRepository {

    private fun mapResultSet(rs: ResultSet): UserRecord = UserRecord(
        id = rs.getString("id"),
        username = rs.getString("username"),
        email = rs.getString("email"),
        passwordHash = rs.getString("password_hash"),
        passwordSalt = rs.getString("password_salt"),
        fullName = rs.getString("full_name"),
        className = rs.getString("class_name"),
        schoolType = rs.getString("school_type"),
        classTeacher = rs.getString("class_teacher"),
        avatarType = rs.getString("avatar_type"),
        avatarColorHex = rs.getLong("avatar_color_hex"),
        role = rs.getString("role"),
        createdAt = rs.getLong("created_at"),
        updatedAt = rs.getLong("updated_at")
    )

    suspend fun findByUsernameOrEmail(identifier: String): UserRecord? = DatabaseFactory.query { conn ->
        val sql = "SELECT * FROM users WHERE username = ? OR email = ? LIMIT 1;"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, identifier)
            stmt.setString(2, identifier)
            stmt.executeQuery().use { rs ->
                if (rs.next()) mapResultSet(rs) else null
            }
        }
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

    suspend fun createUser(request: RegisterRequest, role: String = "USER"): UserRecord = DatabaseFactory.query { conn ->
        val existing = findByUsernameOrEmailSync(conn, request.username) ?: findByUsernameOrEmailSync(conn, request.email)
        if (existing != null) {
            throw IllegalArgumentException("Benutzername oder E-Mail ist bereits vergeben.")
        }

        val id = UUID.randomUUID().toString()
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword(request.password, salt)
        val now = System.currentTimeMillis()

        val sql = """
            INSERT INTO users (id, username, email, password_hash, password_salt, full_name, class_name, school_type, class_teacher, avatar_type, avatar_color_hex, role, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
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
            stmt.setLong(13, now)
            stmt.setLong(14, now)
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
            passwordHash = hash,
            passwordSalt = salt,
            fullName = request.fullName.trim(),
            className = request.className.trim(),
            schoolType = request.schoolType.trim(),
            classTeacher = request.classTeacher.trim(),
            avatarType = request.avatarType,
            avatarColorHex = request.avatarColorHex,
            role = role,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun findByUsernameOrEmailSync(conn: java.sql.Connection, identifier: String): UserRecord? {
        val sql = "SELECT * FROM users WHERE username = ? OR email = ? LIMIT 1;"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, identifier)
            stmt.setString(2, identifier)
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
