package app.sakuracad.packet

import app.sakuracad.SakuraCAD
import app.sakuracad.packet.incoming.AuthSPacket
import app.sakuracad.packet.incoming.TestPacket
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.cio.websocket.Frame

class RODPacket(val r: String?, val o: String, val d: Packet) {
    companion object {
        fun fromString(intake: String): RODPacket {
            val node = SakuraCAD.mapper.readValue<ObjectNode>(intake)
            val req = node.get("r")!!.textValue()
            val op = node.get("o")!!.textValue()
            val data = SakuraCAD.mapper.convertValue(
                node.get("d")!!, when (op) {
                    "test" -> TestPacket::class.java
                    "auths" -> AuthSPacket::class.java

                    else -> throw Exception("bad packet type")
                }
            )!!

            return RODPacket(req, op, data as Packet)
        }
    }

    fun toFrame(): Frame {
        var fixedD: Any = d

        if (d is OutgoingPacket) {
            val node = SakuraCAD.mapper.convertValue(d, ObjectNode::class.java)
            node.remove("o")
            fixedD = node
        }

        val outer = this
        return Frame.Text(SakuraCAD.mapper.writeValueAsString(object { val r = outer.r; val o = outer.o; val d = fixedD }))
    }
}