package app.sakuracad.db

import org.jetbrains.exposed.sql.Table

object Users : Table() {
    val linkedServers = varchar("linkedServers", (37 * 10) - 1) // serverId,serverId,...
}