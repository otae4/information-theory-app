package cn.selfstudy.information

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun HomeworkScreen(course: Course, progress: List<Progress>, onProblem: (String) -> Unit) {
    val completed = progress.filter { it.completed }.map { it.lessonId }.toSet()
    LazyColumn(contentPadding=PaddingValues(20.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {
            Text("课程作业", style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold)
            Text(course.homeworkTitle, Modifier.padding(top=12.dp), style=MaterialTheme.typography.titleLarge)
            Text("4 道大题 · 每题 25 分 · 已完成 ${course.homework.count { it.id in completed }}/4 题", Modifier.padding(top=8.dp))
        }
        item { Text(course.homeworkChapterNote) }
        item { Text("先独立完成，再展开详解核对。作业单独记录完成状态，不加入随机练习、错题本或复习排期。") }
        course.chapters.forEach { chapter ->
            val problems = course.homework.filter { it.chapter==chapter.id }
            if (problems.isNotEmpty()) {
                item { Text(chapter.title, style=MaterialTheme.typography.titleLarge, fontWeight=FontWeight.Bold) }
                items(problems, key={it.id}) { problem ->
                    Card(onClick={onProblem(problem.id)}, modifier=Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text("第${problem.number}题 · ${problem.title}", fontWeight=FontWeight.SemiBold)
                            Text("${problem.points} 分 · ${if(problem.id in completed) "已完成" else "待完成"}")
                            if (problem.relatedChapters.isNotEmpty()) Text("同时关联："+problem.relatedChapters.joinToString { id -> course.chapters.first { it.id==id }.title })
                        }
                    }
                }
            }
        }
        item { Text("题目与解析来源："+course.homeworkSources.joinToString("、"), style=MaterialTheme.typography.bodySmall) }
    }
}

@Composable
fun HomeworkDetailScreen(problem: HomeworkProblem, course: Course, complete: Boolean,
                         onComplete: suspend (Boolean) -> Unit, onLesson: (String) -> Unit,
                         onProblem: (String) -> Unit) {
    var revealed by rememberSaveable(problem.id) { mutableStateOf(false) }
    var busy by remember(problem.id) { mutableStateOf(false) }
    var error by remember(problem.id) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val index = course.homework.indexOfFirst { it.id==problem.id }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            FilterChip(selected=!revealed, onClick={revealed=false}, label={Text("题目")})
            FilterChip(selected=revealed, onClick={revealed=true}, label={Text("分步详解")})
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal=16.dp), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            problem.lessons.forEach { id -> TextButton(onClick={onLesson(id)}) { Text("讲解 · ${course.lessonById.getValue(id).title}") } }
        }
        FormulaReader(problem.promptHtml + if(revealed) problem.solutionHtml else "", Modifier.fillMaxWidth().weight(1f))
        if (error!=null) Text(error!!, Modifier.padding(horizontal=16.dp), color=MaterialTheme.colorScheme.error)
        Button(onClick={
            busy=true; error=null
            scope.launch {
                runCatching { onComplete(!complete) }.onFailure { error="保存失败，请重试：${it.message}" }
                busy=false
            }
        }, enabled=!busy, modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp)) {
            Text(if(busy) "正在保存…" else if(complete) "作业已完成 ✓ · 点击撤销" else "标记本题已完成")
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
            TextButton(onClick={onProblem(course.homework[index-1].id)}, enabled=index>0) { Text("上一题") }
            Text("${index+1} / ${course.homework.size}")
            TextButton(onClick={onProblem(course.homework[index+1].id)}, enabled=index<course.homework.lastIndex) { Text("下一题") }
        }
    }
}
