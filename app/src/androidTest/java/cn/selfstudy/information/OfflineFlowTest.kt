package cn.selfstudy.information

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun openLessonAndItsPracticeWithoutNetwork() {
        compose.onNodeWithText("从预备知识开始").performClick()
        compose.onNodeWithText("做配套练习").assertIsDisplayed().performClick()
        compose.onNodeWithText("1 / 2").assertIsDisplayed()
    }
    @Test fun materialsAndBackupActionsAreDiscoverable() {
        compose.onNodeWithText("资料",useUnmergedTree=true).performClick()
        compose.onNodeWithText("资料始终在身边").assertIsDisplayed()
    }
}
