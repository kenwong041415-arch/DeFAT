package com.defat.core.domain.calc

import com.defat.core.domain.fake.testProfile
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.Test

class TargetReferenceDateTest {

    private val profile = testProfile(birthDate = LocalDate.of(2000, 3, 10))

    @Test
    fun `a day before the 18th birthday is evaluated as of that birthday`() {
        assertEquals(LocalDate.of(2018, 3, 10), TargetReferenceDate.forDay(profile, LocalDate.of(2015, 1, 1)))
        assertEquals(LocalDate.of(2018, 3, 10), TargetReferenceDate.forDay(profile, LocalDate.of(2018, 3, 9)))
    }

    @Test
    fun `the 18th birthday and every later day pass through unchanged`() {
        assertEquals(LocalDate.of(2018, 3, 10), TargetReferenceDate.forDay(profile, LocalDate.of(2018, 3, 10)))
        assertEquals(LocalDate.of(2026, 10, 13), TargetReferenceDate.forDay(profile, LocalDate.of(2026, 10, 13)))
    }
}
