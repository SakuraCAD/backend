package app.sakuracad.packet.incoming

import app.sakuracad.SakuraCAD
import app.sakuracad.Session
import app.sakuracad.packet.IncomingPacket
import app.sakuracad.packet.OutgoingPacket
import app.sakuracad.packet.RODPacket
import app.sakuracad.packet.outgoing.MessagePacket

class BroadcastPacket(val message: String) : IncomingPacket() {
    override suspend fun handle(session: Session): OutgoingPacket {
        SakuraCAD.sessions.filter { s -> s.sessionType == Session.SessionType.USER }.forEach {
            session.connection.send(RODPacket(null, "msg", MessagePacket("broadcast:$message")).toFrame())
        }
        return MessagePacket("sent")
    }
}