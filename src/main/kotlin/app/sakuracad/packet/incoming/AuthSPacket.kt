package app.sakuracad.packet.incoming

import app.sakuracad.Session
import app.sakuracad.db.Servers
import app.sakuracad.packet.IncomingPacket
import app.sakuracad.packet.OutgoingPacket
import app.sakuracad.packet.outgoing.ErrorPacket
import app.sakuracad.packet.outgoing.MessagePacket
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class AuthSPacket(val key: String) : IncomingPacket() {
    override suspend fun handle(session: Session): OutgoingPacket? {
        if (session.sessionType == Session.SessionType.USER)
            return ErrorPacket("Your session is already set as USER")

        session.sessionType = Session.SessionType.SERVER

        val serverId: String = transaction {
            val possibleServer = Servers.select { Servers.key eq key }.toList()
            if (possibleServer.count() < 1) return@transaction null
            possibleServer.first()[Servers.id]
        } ?: return ErrorPacket("Could not find a server by that key")

        session.sessionId = serverId
        session.logger.info("Session authorized")
        return MessagePacket("Authenticated successfully as ${session.sessionId}")
    }
}