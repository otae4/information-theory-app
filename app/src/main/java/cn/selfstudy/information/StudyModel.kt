package cn.selfstudy.information

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.room.withTransaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class StudyModel(app: Application) : AndroidViewModel(app) {
    val course = Course(app)
    val db = Room.databaseBuilder(app, StudyDatabase::class.java, "study.db").build()
    val progress = db.dao().observeProgress().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val reviews = db.dao().observeReviews().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val attemptCount = db.dao().observeAttemptCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun openLesson(id: String) = viewModelScope.launch {
        db.withTransaction {
            val old = db.dao().progressFor(id) ?: Progress(id)
            db.dao().saveProgress(old.copy(lastReadAt = System.currentTimeMillis()))
        }
    }

    fun complete(id: String, completed: Boolean) = viewModelScope.launch {
        db.withTransaction {
            val old = db.dao().progressFor(id) ?: Progress(id)
            db.dao().saveProgress(old.copy(completed = completed, lastReadAt = System.currentTimeMillis()))
        }
    }

    suspend fun completeHomework(id: String, completed: Boolean) {
        require(id in course.homeworkById)
        db.withTransaction {
            db.dao().saveProgress(Progress(id, completed, System.currentTimeMillis()))
        }
    }

    suspend fun answer(question: Question, correct: Boolean, response: String, variant: String) {
        db.withTransaction {
            val today = LocalDate.now().toEpochDay()
            val old = db.dao().review(question.id)
            val schedule = StudyRules.schedule(old?.tier ?: -1, old?.lastAdvancedDay, today, correct)
            db.dao().saveReview(Review(question.id, !correct, schedule.tier, schedule.dueDay, schedule.advancedDay, variant))
            db.dao().saveAttempt(Attempt(questionId = question.id, correct = correct, response = response.take(4000),
                answeredAt = System.currentTimeMillis(), variant = variant))
        }
    }
}
