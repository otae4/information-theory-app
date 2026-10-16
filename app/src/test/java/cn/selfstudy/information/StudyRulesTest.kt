package cn.selfstudy.information

import org.junit.Assert.*
import org.junit.Test

class StudyRulesTest {
    @Test fun entropyHandlesZerosAndBoundaries() {
        assertEquals(0.0, StudyRules.entropy(listOf(0.0, 1.0)), 1e-10)
        assertEquals(1.0, StudyRules.entropy(listOf(0.5, 0.5)), 1e-10)
        assertEquals(2.0, StudyRules.entropy(List(4) { 0.25 }), 1e-10)
        for (invalid in listOf(listOf(-0.1, 1.1), listOf(0.2, 0.2), listOf(Double.NaN))) {
            assertThrows(IllegalArgumentException::class.java) { StudyRules.entropy(invalid) }
        }
    }
    @Test fun numberGradingRejectsInvalidInput() {
        assertTrue(StudyRules.numericCorrect(" 1/2 ", 0.5, .001))
        assertTrue(StudyRules.numericCorrect("1.0005", 1.0, .001))
        assertFalse(StudyRules.numericCorrect("1.01", 1.0, .001))
        listOf("", "1/0", "NaN", "Infinity", "1e999", "1/2/3", "2 bit").forEach { assertNull(StudyRules.parseNumber(it)) }
    }
    @Test fun reviewAdvancesOncePerDayAndResetsAfterFailure() {
        val first = StudyRules.schedule(-1, null, 100, true)
        assertEquals(0, first.tier); assertEquals(101L, first.dueDay)
        assertEquals(first, StudyRules.schedule(first.tier, 100, 100, true))
        assertEquals(104L, StudyRules.schedule(0, 100, 101, true).dueDay)
        val failed = StudyRules.schedule(4, 100, 101, false)
        assertEquals(-1, failed.tier); assertEquals(102L, failed.dueDay)
        assertEquals(0, StudyRules.schedule(-1, 101, 101, true).tier)
        assertEquals(130L, StudyRules.schedule(4, 99, 100, true).dueDay)
    }
}
