package cn.selfstudy.information

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@LooperMode(LooperMode.Mode.PAUSED)
class SubjectsTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()

    @Test fun allSubjectsHaveDisjointIdsAndValidSourceMappings() {
        val app=ApplicationProvider.getApplicationContext<Application>()
        val courses=subjectNames.keys.map { Course(app,it) }
        val ids=courses.flatMap { it.progressIds }
        assertEquals(ids.size, ids.toSet().size)
        val questions=courses.flatMap { it.questionById.keys }
        assertEquals(questions.size,questions.toSet().size)
        assertEquals(listOf(49,11,23),courses.map { it.lessons.size })
        assertEquals(listOf(98,22,46),courses.map { it.questions.size })
        assertEquals(listOf(4,7,8),courses.map { it.homework.size })
        assertEquals(listOf(235,145,1989),courses.map { it.coverage.size })
        courses.forEach { course ->
            assertEquals(course.documents.sumOf { it.pages },course.coverage.size)
            course.lessons.forEach { lesson ->
                assertTrue(course.chapters.any { it.id==lesson.chapter })
                lesson.sources.forEach { ref -> assertTrue(ref.page in 1..course.documentById.getValue(ref.document).pages) }
                assertTrue(lesson.prerequisites.all(course.lessonById::containsKey))
            }
            course.homework.forEach { h ->
                assertTrue(h.lessons.all(course.lessonById::containsKey))
                listOfNotNull(h.source,h.solution).forEach { ref ->
                    assertTrue(ref.page in 1..course.documentById.getValue(ref.document).pages)
                }
            }
        }
    }

    @Test fun combinedBackupAcceptsLegacyAndPreservesAllSubjectRecords() = runBlocking<Unit> {
        val model=StudyModel(ApplicationProvider.getApplicationContext())
        try {
            val data=BackupData(listOf(Progress("p-prob",true,1),Progress("comm-concept",true,2),Progress("dsp-hw-1",true,3)),
                listOf(Review("comm-rate-q1",true,0,20000)),
                listOf(Attempt(questionId="dsp-sequence-q1",correct=true,response="2",answeredAt=4)))
            val text=Backup.encode(data,model.dataVersion)
            assertEquals(data,Backup.decode(text,model.progressIds,model.questionIds,model.dataVersion))
            val old=Backup.encode(BackupData(listOf(Progress("hw1-q1",true,1)),emptyList(),emptyList()),2)
            assertEquals("hw1-q1",Backup.decode(old,model.progressIds,model.questionIds,model.dataVersion).progress.single().lessonId)
        } finally { model.db.close() }
    }

    @Test fun launchHasThreeEntrancesAndHomeworkStaysInItsSubject() {
        compose.onNodeWithText("三科学习室").assertIsDisplayed()
        compose.onNodeWithText("通信原理").performScrollTo().performClick()
        compose.onNodeWithText("从第一节开始").assertIsDisplayed()
        compose.onNodeWithText("作业",useUnmergedTree=true).performClick()
        compose.onNodeWithText("第1题 · 字母编码与平均信息速率").performScrollTo().performClick()
        compose.onNodeWithText("查看原题与图表").assertIsDisplayed()
        compose.onNodeWithText("标记本题已完成").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("作业已完成 ✓ · 点击撤销").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("返回").performClick()
        compose.onNodeWithText("学科").performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("数字信号处理"))
        compose.onNodeWithText("数字信号处理").performClick()
        compose.onNodeWithText("作业",useUnmergedTree=true).performClick()
        compose.onNodeWithText("8 道大题 · 按原作业顺序 · 已完成 0/8 题").assertIsDisplayed()
        compose.onNodeWithText("学科").performClick()
        compose.onNodeWithText("信息论").performScrollTo().performClick()
        compose.onNodeWithText("从预备知识开始").assertIsDisplayed()
        compose.onNodeWithText("已完成 0/49 节 · 已练习 0 次").assertIsDisplayed()
    }
}
