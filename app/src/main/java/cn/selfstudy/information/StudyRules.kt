package cn.selfstudy.information

import kotlin.math.abs
import kotlin.math.ln

/** Pure rules shared by grading, review scheduling and tests. Days use the device's local date. */
object StudyRules {
    val intervals = listOf(1L, 3L, 7L, 14L, 30L)

    fun entropy(probabilities: List<Double>): Double {
        require(probabilities.isNotEmpty())
        require(probabilities.all { it.isFinite() && it in 0.0..1.0 })
        require(abs(probabilities.sum() - 1.0) < 1e-9)
        return -probabilities.filter { it > 0.0 }.sumOf { it * ln(it) / ln(2.0) }
    }

    fun parseNumber(raw: String): Double? {
        val text = raw.trim().replace('−', '-').replace('，', ',')
        val parts = text.split('/')
        val value = when (parts.size) {
            1 -> parts[0].toDoubleOrNull()
            2 -> {
                val numerator = parts[0].trim().toDoubleOrNull() ?: return null
                val denominator = parts[1].trim().toDoubleOrNull() ?: return null
                if (denominator == 0.0) return null
                numerator / denominator
            }
            else -> null
        }
        return value?.takeIf { it.isFinite() }
    }

    fun numericCorrect(input: String, answer: Double, tolerance: Double): Boolean =
        parseNumber(input)?.let { abs(it - answer) <= tolerance } ?: false

    data class Schedule(val tier: Int, val dueDay: Long, val advancedDay: Long?)

    fun schedule(previousTier: Int, lastAdvancedDay: Long?, today: Long, correct: Boolean): Schedule {
        require(previousTier in -1..4)
        if (!correct) return Schedule(-1, today + 1, today)
        // Repeating the same question today must not repeatedly increase its interval.
        val tier = if (lastAdvancedDay == today) previousTier.coerceAtLeast(0)
            else (previousTier + 1).coerceIn(0, 4)
        return Schedule(tier, today + intervals[tier], today)
    }
}
