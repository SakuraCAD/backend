package app.sakuracad

import app.sakuracad.packet.IncomingPacket
import app.sakuracad.packet.OutgoingPacket
import app.sakuracad.packet.RODPacket
import app.sakuracad.packet.outgoing.ErrorPacket
import app.sakuracad.packet.outgoing.MessagePacket
import app.sakuracad.util.SecureException
import io.ktor.http.cio.websocket.*
import io.ktor.websocket.DefaultWebSocketServerSession
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule
import kotlin.coroutines.suspendCoroutine

class Session(private val connection: DefaultWebSocketServerSession) {
    var logger: Logger = LoggerFactory.getLogger("${javaClass.name}<NONE:?>")
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
    var closed = false

    suspend fun run() {
        // kick off if they haven't authenticated
        GlobalScope.launch {
            delay(Duration.ofMinutes(1))
            if (closed) return@launch
            if (sessionType == SessionType.NONE) {
                logger.info("Disconnecting session due to not authenticating in time")
                connection.send(
                    RODPacket(
                        null,
                        "message",
                        MessagePacket("You have not authenticated within 1 minute. You will now be automatically disconnected.")
                    ).toFrame()
                )
                connection.close(CloseReason(CloseReason.Codes.NORMAL, "Did not authenticate within 1 minute"))
            }
        }
        
        // wait for a close
        GlobalScope.launch {
            connection.closeReason.await()
            closed = true
        }

        // wrapper to make it so that it stops processing frames when connection is closed
        do {
            // process frames
            for (frame in connection.incoming)
                handler(frame)
        } while (!closed)
        logger.debug("Session disconnected")
    }

    private suspend fun handler(frame: Frame) {
        when (frame) {
            is Frame.Text -> {
                val data = frame.readText()
                var possibleR: String? = null

                try {
                    val packet = RODPacket.fromString(data)
                    possibleR = packet.r
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
                            possibleR,
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