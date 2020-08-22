package app.sakuracad.packet

import app.sakuracad.Session

abstract class IncomingPacket : Packet() {
    abstract suspend fun handle(session: Session): OutgoingPacket?
}