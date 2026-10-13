package com.defat.app.fake

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * "Now" for ViewModel tests: Tuesday 2026-10-13 14:00 in Hong Kong. Tests must
 * not read the real clock — with a 04:00 day start, a run between 00:00 and
 * 04:00 local would see a different logical today than the one the fixture
 * data was built for.
 */
object TestClock {
    val zone: ZoneId = ZoneId.of("Asia/Hong_Kong")
    val clock: Clock = Clock.fixed(Instant.parse("2026-10-13T06:00:00Z"), zone)
    val today: LocalDate = LocalDate.of(2026, 10, 13)
}
