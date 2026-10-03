package com.plaxx.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.plaxx.app.data.models.EventDto
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Generuje plik .ics z podanych eventow i udostepnia przez systemowy share sheet.
 *
 * Wymaga FileProvider w AndroidManifest.xml (patrz Script setup ponizej).
 */
object IcsExporter {

    private const val AUTHORITY = "com.plaxx.app.fileprovider"
    private const val DIR_NAME = "exports"

    private val utcFmt: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.US)

    /**
     * @param events lista eventow do wyeksportowania (zwykle widoczny tydzien)
     * @param titlePrefix przedrostek w nazwie pliku, np. "plaxx-week"
     */
    fun exportAndShare(
        context: Context,
        events: List<EventDto>,
        titlePrefix: String = "plaxx",
    ) {
        if (events.isEmpty()) return

        val ics = buildIcs(events)
        val dir = File(context.cacheDir, DIR_NAME).apply { mkdirs() }
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        val file = File(dir, "$titlePrefix-$stamp.ics")
        file.writeText(ics)

        val uri = FileProvider.getUriForFile(context, AUTHORITY, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/calendar"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Plan Plaxx")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Udostepnij plan"))
    }

    /** Filtruje events do tygodnia zaczynajacego sie od weekStart (Pon) — 7 dni. */
    fun eventsInWeek(events: List<EventDto>, weekStart: LocalDate): List<EventDto> {
        val end = weekStart.plusDays(7)
        return events.filter { ev ->
            val d = runCatching { LocalDateTime.parse(ev.start).toLocalDate() }.getOrNull()
            d != null && !d.isBefore(weekStart) && d.isBefore(end)
        }
    }

    // ─── Build ICS ─────────────────────────────────────────────

    private fun buildIcs(events: List<EventDto>): String {
        val sb = StringBuilder()
        sb.append("BEGIN:VCALENDAR\r\n")
        sb.append("VERSION:2.0\r\n")
        sb.append("PRODID:-//Plaxx//Schedule//PL\r\n")
        sb.append("CALSCALE:GREGORIAN\r\n")
        sb.append("METHOD:PUBLISH\r\n")

        val zone = ZoneId.systemDefault()
        val nowUtc = LocalDateTime.now(zone)
        val dtstamp = java.time.ZonedDateTime.of(nowUtc, zone)
            .withZoneSameInstant(ZoneId.of("UTC"))
            .format(utcFmt)

        events.forEach { ev ->
            val start = runCatching { LocalDateTime.parse(ev.start) }.getOrNull() ?: return@forEach
            val end = runCatching { LocalDateTime.parse(ev.end) }.getOrNull() ?: return@forEach

            val startUtc = java.time.ZonedDateTime.of(start, zone)
                .withZoneSameInstant(ZoneId.of("UTC"))
                .format(utcFmt)
            val endUtc = java.time.ZonedDateTime.of(end, zone)
                .withZoneSameInstant(ZoneId.of("UTC"))
                .format(utcFmt)

            val summary = buildString {
                append(ev.subject)
                if (ev.group.isNotBlank()) append(" (").append(ev.group).append(")")
            }
            val location = ev.room?.takeIf { it.isNotBlank() } ?: ""
            val desc = buildString {
                if (!ev.teacher.isNullOrBlank()) append("Prowadzacy: ").append(ev.teacher)
                if (!ev.desc.isNullOrBlank()) {
                    if (isNotEmpty()) append("\\n\\n")
                    append(ev.desc.replace("\n", "\\n"))
                }
            }

            sb.append("BEGIN:VEVENT\r\n")
            sb.append("UID:").append(ev.id.replace("\u2022", "-").trim()).append("@plaxx.app\r\n")
            sb.append("DTSTAMP:").append(dtstamp).append("\r\n")
            sb.append("DTSTART:").append(startUtc).append("\r\n")
            sb.append("DTEND:").append(endUtc).append("\r\n")
            sb.append("SUMMARY:").append(escape(summary)).append("\r\n")
            if (location.isNotEmpty()) {
                sb.append("LOCATION:").append(escape(location)).append("\r\n")
            }
            if (desc.isNotEmpty()) {
                sb.append("DESCRIPTION:").append(escape(desc)).append("\r\n")
            }
            sb.append("END:VEVENT\r\n")
        }

        sb.append("END:VCALENDAR\r\n")
        return sb.toString()
    }

    private fun escape(s: String): String =
        s.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")
}
