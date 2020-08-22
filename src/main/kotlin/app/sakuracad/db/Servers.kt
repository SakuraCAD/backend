package app.sakuracad.db

import org.jetbrains.exposed.sql.Table

object Servers : Table() {
    val id = char("id", 36).uniqueIndex() // uuid
    val name = varchar("name", 256)
    val key = varchar("key", 32).uniqueIndex() // keys authenticate servers
}