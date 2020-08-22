package app.sakuracad.packet.outgoing

import app.sakuracad.packet.OutgoingPacket

class MessagePacket(val message: String) : OutgoingPacket("message")