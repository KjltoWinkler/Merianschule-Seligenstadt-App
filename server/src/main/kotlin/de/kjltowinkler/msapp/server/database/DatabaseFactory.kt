package de.kjltowinkler.msapp.server.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.kjltowinkler.msapp.server.config.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import javax.sql.DataSource

object DatabaseFactory {
    private var dataSource: HikariDataSource? = null

    fun init() {
        ServerConfig.ensureDataDirectory()

        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:${ServerConfig.dbPath}"
            driverClassName = "org.sqlite.JDBC"
            maximumPoolSize = 10
            isAutoCommit = true
            transactionIsolation = "TRANSACTION_SERIALIZABLE"
            poolName = "MerianschuleSQLitePool"
        }

        val ds = HikariDataSource(config)
        dataSource = ds

        ds.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("PRAGMA journal_mode = WAL;")
                stmt.execute("PRAGMA busy_timeout = 5000;")
                stmt.execute("PRAGMA foreign_keys = ON;")

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id TEXT PRIMARY KEY,
                        username TEXT NOT NULL UNIQUE COLLATE NOCASE,
                        email TEXT NOT NULL UNIQUE COLLATE NOCASE,
                        password_hash TEXT NOT NULL,
                        password_salt TEXT NOT NULL,
                        full_name TEXT NOT NULL,
                        class_name TEXT NOT NULL,
                        school_type TEXT NOT NULL,
                        class_teacher TEXT NOT NULL,
                        avatar_type TEXT NOT NULL,
                        avatar_color_hex INTEGER NOT NULL,
                        role TEXT NOT NULL DEFAULT 'USER',
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    );
                """.trimIndent())

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS task_lists (
                        id TEXT NOT NULL,
                        user_id TEXT NOT NULL,
                        title TEXT NOT NULL,
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY (id, user_id),
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                    );
                """.trimIndent())

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS tasks (
                        id TEXT NOT NULL,
                        user_id TEXT NOT NULL,
                        list_id TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        subject TEXT NOT NULL DEFAULT '',
                        due_date TEXT NOT NULL DEFAULT '',
                        due_time TEXT NOT NULL DEFAULT '',
                        is_completed INTEGER NOT NULL DEFAULT 0,
                        completed_date TEXT NOT NULL DEFAULT '',
                        created_date TEXT NOT NULL DEFAULT '',
                        deadline_date TEXT NOT NULL DEFAULT '',
                        is_starred INTEGER NOT NULL DEFAULT 0,
                        is_school_task INTEGER NOT NULL DEFAULT 0,
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY (id, user_id),
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                    );
                """.trimIndent())

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS substitutions (
                        id TEXT PRIMARY KEY,
                        date TEXT NOT NULL,
                        day_label TEXT NOT NULL,
                        period TEXT NOT NULL,
                        subject TEXT NOT NULL,
                        class_name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        details TEXT NOT NULL,
                        room_info TEXT NOT NULL,
                        time TEXT NOT NULL,
                        is_new INTEGER NOT NULL DEFAULT 0,
                        is_marked INTEGER NOT NULL DEFAULT 0,
                        updated_at INTEGER NOT NULL
                    );
                """.trimIndent())

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS timetables (
                        class_name TEXT NOT NULL,
                        day_id TEXT NOT NULL,
                        lessons_json TEXT NOT NULL,
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY (class_name, day_id)
                    );
                """.trimIndent())

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS server_metadata (
                        key TEXT PRIMARY KEY,
                        value TEXT NOT NULL,
                        updated_at INTEGER NOT NULL
                    );
                """.trimIndent())
            }
        }
    }

    suspend fun <T> query(block: (Connection) -> T): T = withContext(Dispatchers.IO) {
        val ds = dataSource ?: error("DatabaseFactory is not initialized yet!")
        ds.connection.use { conn ->
            block(conn)
        }
    }
}
