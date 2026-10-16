package cn.selfstudy.information

import android.content.Context
import org.json.JSONObject

data class SourceRef(val document: String, val page: Int)
data class Lesson(
    val id: String, val chapter: String, val title: String, val summary: String,
    val minutes: Int, val html: String, val prerequisites: List<String>, val sources: List<SourceRef>
)
data class Question(
    val id: String, val lessonId: String, val kind: String, val prompt: String,
    val options: List<String>, val answer: String, val explanation: String,
    val tolerance: Double = 0.001, val variant: String? = null
)
data class Chapter(val id: String, val title: String, val subtitle: String)
data class Document(val id: String, val title: String, val asset: String, val pages: Int,
                    val kind: String = "课件", val chapter: String = "")
data class CoveragePage(val document: String, val page: Int, val lessons: List<String>, val kind: String)
data class HomeworkProblem(
    val id: String, val number: Int, val title: String, val chapter: String,
    val relatedChapters: List<String>, val points: Int, val lessons: List<String>,
    val promptHtml: String, val solutionHtml: String, val assignment: String = "作业一",
    val source: SourceRef? = null, val solution: SourceRef? = null
)

val subjectNames = linkedMapOf("info" to "信息论", "comm" to "通信原理", "dsp" to "数字信号处理")

class Course(context: Context, val subject: String = "info") {
    val title = subjectNames.getValue(subject)
    private val prefix = if(subject=="info") "" else "subjects/$subject/"
    private val json = JSONObject(context.assets.open(prefix+"course.json").bufferedReader().use { it.readText() })
    private val homeworkJson = JSONObject(context.assets.open(prefix+"homework.json").bufferedReader().use { it.readText() })
    val version = maxOf(json.getInt("version"), homeworkJson.getInt("courseVersion"))
    val homeworkTitle = homeworkJson.getString("title")
    val homeworkChapterNote = homeworkJson.getString("chapterNote")
    val homeworkSources = homeworkJson.getJSONArray("sources").objects().map { it.getString("name") }
    val homework = homeworkJson.getJSONArray("problems").objects().map {
        HomeworkProblem(it.getString("id"), it.getInt("number"), it.getString("title"), it.getString("chapter"),
            it.getJSONArray("relatedChapters").strings(), it.getInt("points"), it.getJSONArray("lessons").strings(),
            it.getString("promptHtml"), it.getString("solutionHtml"), it.optString("assignment", "作业一"),
            it.optJSONObject("source")?.let { s -> SourceRef(s.getString("document"),s.getInt("page")) },
            it.optJSONObject("solution")?.let { s -> SourceRef(s.getString("document"),s.getInt("page")) })
    }
    val chapters = json.getJSONArray("chapters").objects().map {
        Chapter(it.getString("id"), it.getString("title"), it.getString("subtitle"))
    }
    val documents = json.getJSONArray("documents").objects().map {
        Document(it.getString("id"), it.getString("title"), it.getString("asset"), it.getInt("pages"),
            it.optString("kind",if(it.getString("id")=="book") "教材" else "课件"),it.optString("chapter"))
    }
    val lessons = json.getJSONArray("lessons").objects().map {
        Lesson(it.getString("id"), it.getString("chapter"), it.getString("title"), it.getString("summary"),
            it.getInt("minutes"), it.getString("html"), it.getJSONArray("prerequisites").strings(),
            it.getJSONArray("sources").objects().map { s -> SourceRef(s.getString("document"), s.getInt("page")) })
    }
    val questions = json.getJSONArray("questions").objects().map {
        Question(it.getString("id"), it.getString("lessonId"), it.getString("kind"), it.getString("prompt"),
            it.getJSONArray("options").strings(), it.getString("answer"), it.getString("explanation"),
            it.optDouble("tolerance", 0.001), it.optString("variant").takeIf(String::isNotEmpty))
    }
    val coverage = json.getJSONArray("coverage").objects().map {
        CoveragePage(it.getString("document"), it.getInt("page"), it.getJSONArray("lessons").strings(), it.getString("kind"))
    }
    val lessonById = lessons.associateBy { it.id }
    val questionById = questions.associateBy { it.id }
    val documentById = documents.associateBy { it.id }
    val homeworkById = homework.associateBy { it.id }
    val progressIds = lessonById.keys + homeworkById.keys
}

internal fun org.json.JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
internal fun org.json.JSONArray.strings() = (0 until length()).map { getString(it) }
