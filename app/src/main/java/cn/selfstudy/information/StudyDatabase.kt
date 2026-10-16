package cn.selfstudy.information

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "progress")
data class Progress(@PrimaryKey val lessonId: String, val completed: Boolean = false, val lastReadAt: Long = 0)

@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey val questionId: String, val wrong: Boolean = false, val tier: Int = -1,
    val dueDay: Long, val lastAdvancedDay: Long? = null, val lastVariant: String = ""
)

@Entity(tableName = "attempts")
data class Attempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: String, val correct: Boolean, val response: String,
    val answeredAt: Long, val variant: String = ""
)

@Dao
interface StudyDao {
    @Query("SELECT * FROM progress") fun observeProgress(): Flow<List<Progress>>
    @Query("SELECT * FROM reviews") fun observeReviews(): Flow<List<Review>>
    @Query("SELECT COUNT(*) FROM attempts") fun observeAttemptCount(): Flow<Int>
    @Query("SELECT * FROM progress") suspend fun progress(): List<Progress>
    @Query("SELECT * FROM reviews") suspend fun reviews(): List<Review>
    @Query("SELECT * FROM attempts ORDER BY id") suspend fun attempts(): List<Attempt>
    @Query("SELECT * FROM reviews WHERE questionId = :id") suspend fun review(id: String): Review?
    @Query("SELECT * FROM progress WHERE lessonId = :id") suspend fun progressFor(id: String): Progress?
    @Upsert suspend fun saveProgress(progress: Progress)
    @Upsert suspend fun saveReview(review: Review)
    @Insert suspend fun saveAttempt(attempt: Attempt)
    @Insert suspend fun insertProgress(progress: List<Progress>)
    @Insert suspend fun insertReviews(reviews: List<Review>)
    @Insert suspend fun insertAttempts(attempts: List<Attempt>)
    @Query("DELETE FROM progress") suspend fun clearProgress()
    @Query("DELETE FROM reviews") suspend fun clearReviews()
    @Query("DELETE FROM attempts") suspend fun clearAttempts()
}

@Database(entities = [Progress::class, Review::class, Attempt::class], version = 1, exportSchema = false)
abstract class StudyDatabase : RoomDatabase() { abstract fun dao(): StudyDao }
