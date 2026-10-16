package cn.selfstudy.information

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class StorageTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, StudyDatabase::class.java).allowMainThreadQueries().build()
    @After fun close() = db.close()

    @Test fun backupRoundTripAndTransactionalRestore() = runBlocking {
        val data=BackupData(listOf(Progress("lesson",true,123)),listOf(Review("q",true,0,20000,19999,"self_information:12")),
            listOf(Attempt(questionId="q",correct=false,response="1/2",answeredAt=1234,variant="seed:12")))
        Backup.restore(db,data)
        val encoded=Backup.encode(Backup.snapshot(db),1)
        val decoded=Backup.decode(encoded,setOf("lesson"),setOf("q"),1)
        assertEquals(data,decoded)
        Backup.restore(db,decoded)
        assertEquals(1,db.dao().attempts().size)
        assertEquals(true,db.dao().progress().single().completed)
        assertEquals("seed:12",db.dao().attempts().single().variant)
    }

    @Test fun failedRestoreRollsBackAllChanges() = runBlocking {
        db.dao().saveProgress(Progress("existing",true,100))
        val duplicate=Progress("duplicate",false,20)
        try {
            Backup.restore(db,BackupData(listOf(duplicate,duplicate),emptyList(),emptyList()))
            fail("Duplicate rows should reject the transaction")
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            assertEquals("existing",db.dao().progress().single().lessonId)
        }
    }

    @Test fun unknownIdsDuplicateRecordsAndFutureFormatsAreRejected() {
        val base=Backup.encode(BackupData(listOf(Progress("known",false,0)),emptyList(),emptyList()),1)
        assertThrows(IllegalArgumentException::class.java) {Backup.decode(base,emptySet(),emptySet(),1)}
        assertThrows(IllegalArgumentException::class.java) {Backup.decode(base.replace("\"version\": 1","\"version\": 2"),setOf("known"),emptySet(),1)}
        val duplicate=Backup.encode(BackupData(listOf(Progress("known"),Progress("known")),emptyList(),emptyList()),1)
        assertThrows(IllegalArgumentException::class.java) {Backup.decode(duplicate,setOf("known"),emptySet(),1)}
        assertThrows(IllegalArgumentException::class.java) {Backup.readLimited(ByteArrayInputStream(ByteArray(Backup.MAX_BYTES+1)))}
    }

    @Test fun fullBundledCourseHasValidSourceLinksAndGeneratesSafeVariants() {
        val course=Course(context)
        assertEquals(49,course.lessons.size)
        assertEquals(98,course.questions.size)
        assertEquals(235,course.coverage.size)
        course.coverage.forEach {
            assertTrue(it.page in 1..course.documentById.getValue(it.document).pages)
            assertTrue(it.lessons.all(course.lessonById::containsKey))
        }
        course.questions.filter {it.variant!=null}.forEach {q ->
            repeat(20) {seed ->
                val v=QuestionVariants.make(q,seed)
                assertEquals(v,QuestionVariants.make(q,seed))
                assertTrue(v.answer.toDouble().isFinite())
                assertTrue(StudyRules.numericCorrect(v.answer,v.answer.toDouble(),v.tolerance))
            }
        }
        val day=LocalDate.of(2026,10,16).toEpochDay()
        assertEquals(LocalDate.of(2026,10,17),LocalDate.ofEpochDay(StudyRules.schedule(-1,null,day,true).dueDay))
    }
}
