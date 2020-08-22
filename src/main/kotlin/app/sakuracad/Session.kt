package app.sakuracad

import app.sakuracad.packet.IncomingPacket
import app.sakuracad.packet.OutgoingPacket
import app.sakuracad.packet.RODPacket
import app.sakuracad.packet.outgoing.ErrorPacket
import app.sakuracad.util.SecureException
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.websocket.DefaultWebSocketServerSession
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory

class Session(val connection: DefaultWebSocketServerSession) {
    var logger = LoggerFactory.getLogger("${javaClass.name}<NONE:?>")
    var sessionType = SessionType.NONE
        set(value) {
            field = value
            logger = LoggerFactory.getLogger("${javaClass.name}<${sessionType}:${sessionId}>")
        }
    var sessionId = "?"
        set(value) {
            field = value
            logger = LoggerFactory.getLogger("${javaClass.name}<${sessionType}:${sessionId}>")
        }

    suspend fun run() {
        for (frame in connection.incoming) {
            handler(frame)
        }
    }

    private suspend fun handler(frame: Frame) {
        when (frame) {
            is Frame.Text -> {
                val data = frame.readText()

                try {
                    val packet = RODPacket.fromString(data)
                    logger.trace("Received packet of type ${packet.o}")
                    val response = (packet.d as IncomingPacket).handle(this)
                    if (response != null) {
                        connection.send(
                            RODPacket(
                                packet.r,
                                response.o,
                                response
                            ).toFrame()
                        )
                    }
                } catch (e: Exception) {
                    connection.send(
                        RODPacket(
                            null,
                            "error",
                            ErrorPacket("Failed to parse packet.")
                        ).toFrame()
                    )

                    var finalException = e
                    if (e.message?.contains(data) == true)
                        finalException = SecureException(data, e)

                    logger.warn("Failed to parse or handle packet", finalException)
                }
            }
            else -> logger.warn("Session sent a non-text packet, refusing to handle")
        }
    }

    enum class SessionType {
        NONE,
        SERVER,
        USER
    }
}