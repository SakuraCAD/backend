package app.sakuracad.packet.incoming

import app.sakuracad.Session
import app.sakuracad.packet.IncomingPacket
import app.sakuracad.packet.OutgoingPacket
import app.sakuracad.packet.outgoing.MessagePacket

class SevenHundredFourPacket : IncomingPacket() {
    override suspend fun handle(session: Session): OutgoingPacket = MessagePacket("Your account doesn't have enough credit to make this call.")
}