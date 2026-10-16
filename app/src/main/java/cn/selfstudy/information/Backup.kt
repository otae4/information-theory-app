package cn.selfstudy.information

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.ByteArrayOutputStream

data class BackupData(val progress: List<Progress>, val reviews: List<Review>, val attempts: List<Attempt>)

object Backup {
    const val MAX_BYTES = 8 * 1024 * 1024

    fun readLimited(input: InputStream): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size() + count <= MAX_BYTES) { "备份超过 8 MB，无法导入" }
            output.write(buffer, 0, count)
        }
        return output.toString("UTF-8")
    }

    fun encode(data: BackupData, courseVersion: Int): String = JSONObject().apply {
        put("format", "information-theory-backup")
        put("version", 1)
        put("courseVersion", courseVersion)
        put("createdAt", System.currentTimeMillis())
        put("progress", JSONArray(data.progress.map {
            JSONObject().put("lessonId", it.lessonId).put("completed", it.completed).put("lastReadAt", it.lastReadAt)
        }))
        put("reviews", JSONArray(data.reviews.map {
            JSONObject().put("questionId", it.questionId).put("wrong", it.wrong).put("tier", it.tier)
                .put("dueDay", it.dueDay).put("lastAdvancedDay", it.lastAdvancedDay ?: JSONObject.NULL).put("lastVariant", it.lastVariant)
        }))
        put("attempts", JSONArray(data.attempts.map {
            JSONObject().put("questionId", it.questionId).put("correct", it.correct)
                .put("response", it.response).put("answeredAt", it.answeredAt).put("variant", it.variant)
        }))
    }.toString(2).also { require(it.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "备份超过 8 MB，暂无法导出" } }

    fun decode(text: String, lessonIds: Set<String>, questionIds: Set<String>, courseVersion: Int): BackupData {
        require(text.toByteArray().size <= MAX_BYTES) { "备份过大" }
        val j = JSONObject(text)
        require(j.getString("format") == "information-theory-backup" && j.getInt("version") == 1) { "不支持此备份格式" }
        require(j.getInt("courseVersion") <= courseVersion) { "备份来自更新的课程版本，请先更新应用" }
        val progress = j.getJSONArray("progress").objects().map {
            val id = it.getString("lessonId")
            val time = it.getLong("lastReadAt")
            require(id in lessonIds && time >= 0) { "备份包含未知知识点或无效时间" }
            Progress(id, it.getBoolean("completed"), time)
        }
        val reviews = j.getJSONArray("reviews").objects().map {
            val id = it.getString("questionId")
            val tier = it.getInt("tier")
            val due = it.getLong("dueDay")
            val advanced = if (it.isNull("lastAdvancedDay")) null else it.getLong("lastAdvancedDay")
            require(id in questionIds && tier in -1..4 && due in 0..3652500 && (advanced == null || advanced in 0..3652500)) { "复习数据无效" }
            val variant=it.optString("lastVariant")
            require(variant.length<=100) { "复习变式数据无效" }
            Review(id, it.getBoolean("wrong"), tier, due, advanced, variant)
        }
        val raw = j.getJSONArray("attempts")
        require(raw.length() <= 100000) { "答题记录过多" }
        val attempts = raw.objects().map {
            val id = it.getString("questionId")
            val response = it.getString("response")
            val time = it.getLong("answeredAt")
            val variant = it.optString("variant")
            require(id in questionIds && response.length <= 4000 && variant.length <= 100 && time >= 0) { "答题记录无效" }
            Attempt(questionId = id, correct = it.getBoolean("correct"), response = response, answeredAt = time, variant = variant)
        }
        require(progress.map { it.lessonId }.distinct().size == progress.size) { "知识点记录重复" }
        require(reviews.map { it.questionId }.distinct().size == reviews.size) { "复习记录重复" }
        return BackupData(progress, reviews, attempts)
    }

    suspend fun snapshot(db: StudyDatabase): BackupData = db.withTransaction {
        BackupData(db.dao().progress(), db.dao().reviews(), db.dao().attempts())
    }

    suspend fun restore(db: StudyDatabase, data: BackupData) = db.withTransaction {
        db.dao().clearProgress(); db.dao().clearReviews(); db.dao().clearAttempts()
        db.dao().insertProgress(data.progress)
        db.dao().insertReviews(data.reviews)
        db.dao().insertAttempts(data.attempts)
    }
}
