package com.plaxx.app.ui.debug

import com.plaxx.app.data.models.EventDto
import com.plaxx.app.data.models.SubjectInfo
import com.plaxx.app.data.models.UserDto
import java.time.LocalDate
import java.time.LocalDateTime

data class MockSession(
    val user: UserDto,
    val planId: Long,
    val planName: String,
    val events: List<EventDto>,
    val selectedGroups: Set<String>
)

object MockData {

    const val DEBUG_OFFLINE = false

    fun buildSession(anchor: LocalDate = LocalDate.now()): MockSession {
        val subjects = listOf(
            "Analiza matematyczna", "Fizyka", "Programowanie w C++",
            "Bazy danych", "Sieci komputerowe", "Jezyk angielski"
        )
        val groups = listOf("W", "C1", "C2", "L1", "L2", "P1", "P2")
        val rooms = listOf("A1", "A2", "A3", "202", "101B", "13")
        val teachers = listOf(
            "dr A. Pudelko", "prof. A. Szromba", "mgr inz. M. Razny",
            "dr inz. G. Pedrak", "mgr J. Firganek"
        )

        val events = mutableListOf<EventDto>()
        val random = java.util.Random(42)

        for (dayOffset in -2..2) {
            val date = anchor.plusDays(dayOffset.toLong())
            val count = 4 + random.nextInt(4)
            val usedSlots = mutableSetOf<Int>()

            repeat(count) {
                val startHour = 7 + random.nextInt(12)
                val startMin = if (random.nextBoolean()) 0 else 30
                val slotKey = startHour * 60 + startMin
                if (slotKey in usedSlots) return@repeat
                usedSlots.add(slotKey)

                val durations = intArrayOf(45, 60, 90, 120)
                val dur = durations[random.nextInt(durations.size)]

                val start = LocalDateTime.of(
                    date.year, date.monthValue, date.dayOfMonth, startHour, startMin
                )
                val end = start.plusMinutes(dur.toLong())

                val subject = subjects[random.nextInt(subjects.size)]
                val group = groups[random.nextInt(groups.size)]
                val room = rooms[random.nextInt(rooms.size)]
                val teacher = teachers[random.nextInt(teachers.size)]

                val logicalId = "$subject \u2022 $group"
                val id = "$logicalId \u2022 ${start.toEpochSecond(java.time.ZoneOffset.UTC)}"

                events.add(
                    EventDto(
                        subject = subject, group = group,
                        teacher = teacher, room = room,
                        desc = "Przedmiot: $subject\nGrupa: $group\nProwadzacy: $teacher\nSala: $room",
                        start = start.toString(), end = end.toString(),
                        day = date.dayOfWeek.value,
                        id = id, logicalId = logicalId
                    )
                )
            }
        }

        val sortedEvents = events.sortedBy { it.start }
        val selected = sortedEvents.map { it.logicalId }.toSet()

        return MockSession(
            user = UserDto(999L, "debug", "Debug", "User"),
            planId = 999L,
            planName = "Mock plan (offline)",
            events = sortedEvents,
            selectedGroups = selected
        )
    }

    fun buildSubjectsMap(events: List<EventDto>): Map<String, SubjectInfo> {
        val map = mutableMapOf<String, SubjectInfo>()
        events.forEach { ev ->
            val info = map[ev.subject]
            if (info == null) {
                map[ev.subject] = SubjectInfo(name = ev.subject, groups = listOf(ev.group))
            } else if (!info.groups.contains(ev.group)) {
                map[ev.subject] = info.copy(groups = info.groups + ev.group)
            }
        }
        return map.mapValues { (_, info) -> info.copy(groups = info.groups.sorted()) }
    }
}
