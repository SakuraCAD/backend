package app.sakuracad

import app.sakuracad.db.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.natpryce.konfig.*
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.websocket.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.sentry.Sentry
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Duration
import kotlin.time.DurationUnit

object SakuraCAD {
    private val logger = LoggerFactory.getLogger(javaClass)
    val mapper = ObjectMapper().registerKotlinModule()
    val sessions = mutableListOf<Session>()

    @JvmStatic fun main(args: Array<String>) {
        Sentry.init("https://269a1379a3b04da8bb8ce4096dd962a7@sentry.opxl.pw/2")

        val serverPort = Key("server.port", intType)
        val dbUrl = Key("db.url", stringType)
        val dbDriver = Key("db.driver", stringType)
        val dbUsername = Key("db.username", stringType)
        val dbPassword = Key("db.password", stringType)

        val config = try {
            logger.debug("Attempting to load configuration")
            ConfigurationProperties.fromFile(File("./config.properties")) overriding ConfigurationProperties.fromResource("config.properties")
        } catch (e: Misconfiguration) {
            logger.warn("The file `config.properties` was not found. SakuraCAD will start with the default config")
            ConfigurationProperties.fromResource("config.properties")
        }

        logger.info("Launching")
        logger.debug("Connecting to database")
        Database.connect(config[dbUrl], driver = config[dbDriver], user = config[dbUsername], password = config[dbPassword])

        transaction {
            SchemaUtils.createMissingTablesAndColumns(Servers, Users)
        }

        val server = embeddedServer(Netty, config[serverPort], "127.0.0.1") {
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(60) // Disabled (null) by default
                timeout = Duration.ofSeconds(15)
                maxFrameSize = Long.MAX_VALUE // Disabled (max value). The connection will be closed if surpassed this length.
                masking = true
            }
            routing {
                get("/") {
                    call.respondText("{\"message\":\"You are not a WebSocket client.\"}", ContentType.Application.Json)
                }
                webSocket("/") {
                    logger.debug("New session created")
                    val session = Session(this)
                    sessions.add(session)
                    session.run()
                }
            }
        }

        GlobalScope.launch {
            while(true) {
                delay(Duration.ofSeconds(30))

                for (session in sessions.toList()) {
                    if (session.closed) {
                        sessions.remove(session)
                        logger.trace("Cleaned up session ${session.sessionType}:${session.sessionId}")
                    }
                }
            }
        }

        server.start(wait = true)
    }
}