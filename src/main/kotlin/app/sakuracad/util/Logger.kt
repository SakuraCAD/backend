package app.sakuracad.util

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.encoder.Encoder
import ch.qos.logback.core.pattern.PatternLayoutEncoderBase
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase
import org.slf4j.LoggerFactory

private const val escape = "\u001B["
private var sequence = 0

private fun hslToRgb(hue: Number, sat: Double, light: Double): List<Int> {
    val hueFixed = (360 + ((hue.toDouble()) % 360)) % 360
    val satFixed = 1.0.coerceAtMost(0.0.coerceAtLeast(sat))
    val lightFixed = 1.0.coerceAtMost(0.0.coerceAtLeast(light))

    val out = mutableListOf(0, 0, 0)

    for (i in 0..2) {
        val n = listOf(0, 8, 4)[i]
        val k = (n + hueFixed / 30) % 12
        val a = satFixed * lightFixed.coerceAtMost(1 - lightFixed)
        out[i] = (((lightFixed - a * (-1.0).coerceAtLeast((k - 3.0).coerceAtMost(9.0 - k).coerceAtMost(1.0))) * 255.0).toInt()) or 0
    }

    return out.toList()
}

private fun getPrefixColors(): List<String> {
    sequence += 47

    val bg1 = hslToRgb(sequence, 0.4, 0.24).joinToString(";")
    val bg2 = hslToRgb(sequence, 0.4, 0.27).joinToString(";")
    val bg3 = hslToRgb(sequence, 0.4, 0.30).joinToString(";")
    val fg1 = hslToRgb(sequence, 1.0, 0.8).joinToString(";")
    val fg2 = hslToRgb(sequence, 0.9, 0.85).joinToString(";")
    val fg3 = hslToRgb(sequence, 0.8, 0.90).joinToString(";")

    return listOf(
        "${escape}48;2;${bg1}m${escape}38;2;${fg1}m",
        "${escape}48;2;${bg2}m${escape}38;2;${fg2}m",
        "${escape}48;2;${bg3}m${escape}38;2;${fg3}m"
    )
}

class CustomConsoleAppender : ConsoleAppender<ILoggingEvent>() {
    private val knownLoggers = mutableMapOf<String, CustomPatternLayoutEncoder>()

    override fun doAppend(eventObject: ILoggingEvent) {
        if (!knownLoggers.contains(eventObject.loggerName)) {
            val colors = getPrefixColors()
            val ple = CustomPatternLayoutEncoder()
            val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext

            ple.context = loggerContext
            ple.pattern = "${colors[0]} %d{HH:mm:ss.SSS} ${colors[1]} %thread ${colors[2]} %logger %highlightbg( %level ) %msg%n"
            ple.start()

            encoder = ple
            knownLoggers[eventObject.loggerName] = ple
        }
        encoder = knownLoggers[eventObject.loggerName]
        super.doAppend(eventObject)
    }
}

class CustomHighlightingCompositeConverter : ForegroundCompositeConverterBase<ILoggingEvent>() {
    override fun getForegroundColorCode(event: ILoggingEvent): String {
        val level = event.level
        return when (level.toInt()) {
            Level.ERROR_INT -> "48;2;255;71;87"
            Level.WARN_INT -> "48;2;247;159;31"
            Level.DEBUG_INT, Level.TRACE_INT -> "48;2;65;66;67"

            else -> "48;2;55;66;250"
        }
    }
}

class CustomPatternLayoutEncoder : PatternLayoutEncoderBase<ILoggingEvent>() {
    override fun start() {
        val patternLayout = PatternLayout()
        patternLayout.defaultConverterMap["highlightbg"] = CustomHighlightingCompositeConverter::class.java.name

        patternLayout.context = context
        patternLayout.pattern = pattern
        patternLayout.isOutputPatternAsHeader = outputPatternAsHeader
        patternLayout.start()
        layout = patternLayout
        super.start()
    }
}