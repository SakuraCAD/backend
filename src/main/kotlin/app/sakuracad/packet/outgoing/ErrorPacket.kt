package app.sakuracad.packet.outgoing

import app.sakuracad.packet.OutgoingPacket

class ErrorPacket(val message: String) : OutgoingPacket("error")