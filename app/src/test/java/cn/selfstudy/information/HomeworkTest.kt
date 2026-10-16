package cn.selfstudy.information

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class HomeworkTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun assignmentMapsToContentChaptersAndStaysOutsidePractice() {
        val course = Course(context)
        assertEquals(4, course.homework.size)
        assertEquals(100, course.homework.sumOf { it.points })
        assertEquals(listOf(1,2,3,4), course.homework.map { it.number })
        assertEquals(4, course.homeworkById.size)
        assertEquals(2, course.homeworkSources.size)
        assertEquals(98, course.questions.size)
        assertTrue(course.homework.all { it.chapter=="c2" })
        assertEquals(listOf("c4"), course.homework.last().relatedChapters)
        course.homework.forEach { problem ->
            assertTrue(problem.id !in course.questionById && problem.id !in course.lessonById)
            assertTrue(problem.id in course.progressIds)
            assertTrue(problem.lessons.isNotEmpty() && problem.lessons.all(course.lessonById::containsKey))
            assertTrue(problem.promptHtml.contains("25 分"))
            assertTrue(problem.solutionHtml.contains("bit"))
        }
    }

    @Test fun homeworkProgressSurvivesBackupWithoutEnteringQuizOrReview() = runBlocking<Unit> {
        val course = Course(context)
        val db = Room.inMemoryDatabaseBuilder(context, StudyDatabase::class.java).build()
        try {
            val data = BackupData(listOf(Progress("hw1-q1", true, 100)), emptyList(), emptyList())
            Backup.restore(db, data)
            val encoded = Backup.encode(Backup.snapshot(db), course.version)
            val decoded = Backup.decode(encoded, course.progressIds, course.questionById.keys, course.version)
            assertEquals(data, decoded)
            assertEquals(0, db.dao().attempts().size)
            assertTrue(db.dao().reviews().isEmpty())
            val legacy = Backup.encode(BackupData(listOf(Progress("p-prob",true,50)),emptyList(),emptyList()),1)
            val restored = Backup.decode(legacy, course.progressIds, course.questionById.keys, course.version)
            assertEquals("p-prob", restored.progress.single().lessonId)
            assertThrows(IllegalArgumentException::class.java) {
                Backup.decode(encoded, course.lessonById.keys, course.questionById.keys, 1)
            }
        } finally { db.close() }
    }
}
