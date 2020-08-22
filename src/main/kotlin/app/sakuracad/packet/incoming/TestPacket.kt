package app.sakuracad.packet.incoming

import app.sakuracad.Session
import app.sakuracad.packet.IncomingPacket
import app.sakuracad.packet.OutgoingPacket
import app.sakuracad.packet.outgoing.MessagePacket

class TestPacket(val test: String) : IncomingPacket() {
    override suspend fun handle(session: Session): OutgoingPacket = MessagePacket("You are connected to SakuraCAD.")
}