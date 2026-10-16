package cn.selfstudy.information

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@LooperMode(LooperMode.Mode.PAUSED)
class MainActivityUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()

    @Test fun newLearnerCanOpenLessonAndItsQuestions() {
        compose.onNodeWithText("从预备知识开始").performClick()
        compose.onNodeWithText("做配套练习").assertIsDisplayed().performClick()
        compose.onNodeWithText("1 / 2").assertIsDisplayed()
    }

    @Test fun materialsAndReviewAreReachableFromNavigation() {
        compose.onNodeWithText("资料",useUnmergedTree=true).performClick()
        compose.onNodeWithText("资料始终在身边").assertIsDisplayed()
        compose.onNodeWithText("复习",useUnmergedTree=true).performClick()
        compose.onNodeWithText("让理解留得更久").assertIsDisplayed()
    }

    @Test fun documentCanTurnPagesAndJumpToTheEnd() {
        val document = Course(compose.activity).documents.first()
        compose.onNodeWithText("资料", useUnmergedTree = true).performClick()
        compose.onNodeWithText(document.title).performClick()
        compose.onNodeWithText("1 / ${document.pages}").assertIsDisplayed()
        compose.onNodeWithText("上一页").assertIsNotEnabled()
        compose.onNodeWithText("下一页").assertIsDisplayed().performClick()
        compose.onNodeWithText("2 / ${document.pages}").assertIsDisplayed()
        compose.onNodeWithText("上一页").performClick()
        compose.onNodeWithText("1 / ${document.pages}").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextReplacement(document.pages.toString())
        compose.onNodeWithText("跳转").performClick()
        compose.onNodeWithText("${document.pages} / ${document.pages}").assertIsDisplayed()
        compose.onNodeWithText("下一页").assertIsNotEnabled()
        compose.onNodeWithText("上一页").assertIsEnabled().performClick()
        compose.onNodeWithText("${document.pages - 1} / ${document.pages}").assertIsDisplayed()
    }

    @Test fun homeworkCompletionIsSeparateAndPersistsAfterRecreation() {
        compose.onNodeWithText("作业",useUnmergedTree=true).performClick()
        compose.onNodeWithText("课程作业").assertIsDisplayed()
        compose.onNodeWithText("02 · 信息量与熵").assertIsDisplayed()
        compose.onNodeWithText("第1题 · 两枚骰子的自信息与熵").performClick()
        compose.onNodeWithText("分步详解").assertIsDisplayed().performClick()
        compose.onNodeWithText("标记本题已完成").performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("作业已完成 ✓ · 点击撤销").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("返回").performClick()
        compose.onNodeWithText("4 道大题 · 每题 25 分 · 已完成 1/4 题").assertIsDisplayed()
        compose.onNodeWithText("学习",useUnmergedTree=true).performClick()
        compose.onNodeWithText("已完成 0/49 节 · 已练习 0 次").assertIsDisplayed()
        compose.onNodeWithText("从预备知识开始").assertIsDisplayed()
        compose.onNodeWithText("复习",useUnmergedTree=true).performClick()
        compose.onNodeWithText("开始今日复习 · 0 题").assertIsDisplayed()
        compose.onNodeWithText("作业",useUnmergedTree=true).performClick()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("4 道大题 · 每题 25 分 · 已完成 1/4 题").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
