package cn.selfstudy.information

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.random.Random

private val palette = lightColorScheme(primary=Color(0xFF11695F), secondary=Color(0xFFBA7137),
    background=Color(0xFFF7F8FA), surface=Color.White, surfaceVariant=Color(0xFFEDF3F2),
    onBackground=Color(0xFF20323B), onSurface=Color(0xFF20323B), outline=Color(0xFFB8C9C6))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MaterialTheme(colorScheme=palette) { StudyApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyApp(model: StudyModel = viewModel()) {
    var subject by rememberSaveable { mutableStateOf<String?>(null) }
    if(subject==null) {
        SubjectPicker(model.courses) { subject=it }
    } else key(subject) {
        SubjectStudyApp(model.courses.getValue(subject!!),model) { subject=null }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectStudyApp(course: Course, model: StudyModel, onSubjects:()->Unit) {
    val allProgress by model.progress.collectAsStateWithLifecycle()
    val allReviews by model.reviews.collectAsStateWithLifecycle()
    val allAttempts by model.attempts.collectAsStateWithLifecycle()
    val progress = allProgress.filter { it.lessonId in course.progressIds }
    val reviews = allReviews.filter { it.questionId in course.questionById }
    val attempts = allAttempts.count { it.questionId in course.questionById }
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    var stack by rememberSaveable { mutableStateOf(listOf("home")) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var queue by rememberSaveable { mutableStateOf(listOf<String>()) }
    var quizIndex by rememberSaveable { mutableIntStateOf(0) }
    var quizSeed by rememberSaveable { mutableIntStateOf(0) }
    var reuseVariants by rememberSaveable { mutableStateOf(false) }
    var pendingBackup by remember { mutableStateOf<String?>(null) }
    var importCandidate by remember { mutableStateOf<BackupData?>(null) }
    var backupBusy by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val route = stack.last()
    val screenState = rememberSaveableStateHolder()
    val parts = route.split('|')
    fun push(value: String) { stack = stack + value }
    fun back() { if (stack.size > 1) stack = stack.dropLast(1) }
    fun message(text: String) { scope.launch { snack.showSnackbar(text) } }
    fun startQuiz(ids: List<String>, reuse: Boolean = false) {
        if (ids.isEmpty()) { message("当前没有符合条件的题目"); return }
        queue = ids.shuffled(); quizIndex=0; quizSeed=Random.nextInt(); reuseVariants=reuse; push("quiz")
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val content = pendingBackup
        pendingBackup=null
        if (uri != null) scope.launch {
            val result=withContext(Dispatchers.IO) { runCatching {
                val staging=java.io.File(context.cacheDir,"pending-backup.json")
                val snapshot=content ?: staging.takeIf{it.exists()}?.readText(Charsets.UTF_8) ?: error("备份快照已失效，请重新导出")
                context.contentResolver.openOutputStream(uri,"wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(snapshot) }
                    ?: error("无法写入所选文件")
                staging.delete()
            } }
            message(result.fold({"备份已导出"},{"备份失败：${it.message}"}))
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            backupBusy=true
            val result=withContext(Dispatchers.IO) { runCatching {
                val text=context.contentResolver.openInputStream(uri)?.use(Backup::readLimited) ?: error("无法读取备份")
                Backup.decode(text,model.progressIds,model.questionIds,model.dataVersion)
            } }
            backupBusy=false
            result.onSuccess { importCandidate=it }.onFailure { message("备份未导入：${it.message}") }
        }
    }
    BackHandler { if(stack.size>1) back() else onSubjects() }
    Scaffold(
        topBar={ TopAppBar(title={ Column {
            Text(when(parts[0]) { "lesson" -> course.lessonById[parts[1]]?.title ?: "学习"; "quiz" -> "专注练习";
                "homework" -> course.homeworkById[parts[1]]?.let { "${it.assignment} · 第${it.number}题" } ?: "作业";
                "pdf" -> course.documentById[parts[1]]?.title ?: "资料"; "coverage" -> "资料覆盖清单"; "licenses" -> "关于与开源许可"; else -> course.title },
                style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
            if(route=="home") Text("从理解开始，把知识留下来",style=MaterialTheme.typography.labelMedium,color=palette.primary)
        } }, navigationIcon={TextButton(onClick={if(stack.size>1) back() else onSubjects()}){Text(if(stack.size>1) "返回" else "学科")}},
            colors=TopAppBarDefaults.topAppBarColors(containerColor=palette.background)) },
        bottomBar={if(route=="home") NavigationBar(containerColor=Color.White) {
            listOf("学习","练习","作业","复习","资料").forEachIndexed { i,title ->
                NavigationBarItem(selected=tab==i,onClick={tab=i},icon={ Text(listOf("学","练","作","习","册")[i],fontWeight=FontWeight.Bold) },label={Text(title)})
            }
        }}, snackbarHost={SnackbarHost(snack)},containerColor=palette.background
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            screenState.SaveableStateProvider(route) { when(parts[0]) {
                "home" -> when(tab) {
                    0 -> LearnScreen(course,progress,reviews,attempts) { id -> model.openLesson(id); push("lesson|$id") }
                    1 -> PracticeScreen(course) { startQuiz(it) }
                    2 -> HomeworkScreen(course,progress) { push("homework|$it") }
                    3 -> ReviewScreen(course,reviews,onQuiz={startQuiz(it,true)},onLesson={model.openLesson(it);push("lesson|$it")})
                    4 -> MaterialsScreen(course,backupBusy, onDocument={push("pdf|${it.id}|1")},onCoverage={push("coverage")},onAbout={push("licenses")},
                        onExport={scope.launch {
                            backupBusy=true
                            val result=withContext(Dispatchers.IO){runCatching {
                                Backup.encode(Backup.snapshot(model.db),model.dataVersion).also {
                                    java.io.File(context.cacheDir,"pending-backup.json").writeText(it,Charsets.UTF_8)
                                }
                            }}
                            backupBusy=false
                            result.onSuccess {pendingBackup=it;exportLauncher.launch("三科学习备份-${LocalDate.now()}.json")}
                                .onFailure {message("备份失败：${it.message}")}
                        }},onImport={importLauncher.launch(arrayOf("application/json","application/octet-stream","text/plain"))})
                }
                "lesson" -> course.lessonById[parts[1]]?.let { lesson ->
                    LessonScreen(lesson,course,progress.any {it.lessonId==lesson.id && it.completed},
                        onComplete={model.complete(lesson.id,it)},onPractice={startQuiz(course.questions.filter {it.lessonId==lesson.id}.map {it.id})},
                        onSource={push("pdf|${it.document}|${it.page}")},onPrerequisite={model.openLesson(it);push("lesson|$it")})
                }
                "quiz" -> {
                    if (quizIndex<queue.size) {
                        val original=course.questionById.getValue(queue[quizIndex])
                        val seed=if(reuseVariants) reviews.firstOrNull{it.questionId==original.id}?.lastVariant?.substringAfterLast(':')?.toIntOrNull() ?: quizSeed else quizSeed
                        val question=remember(original.id,seed){QuestionVariants.make(original,seed)}
                        key(original.id,quizSeed) { QuestionScreen(question,quizIndex+1,queue.size,
                            onAnswer={correct,response -> model.answer(question,correct,response,question.variant?.let {"$it:$seed"} ?: "")},
                            onNext={quizIndex++},onLesson={model.openLesson(question.lessonId);push("lesson|${question.lessonId}")}) }
                    } else QuizFinished(queue.size,onAgain={back();startQuiz(queue)},onDone={back()})
                }
                "homework" -> course.homeworkById[parts[1]]?.let { problem ->
                    HomeworkDetailScreen(problem,course,progress.any { it.lessonId==problem.id && it.completed },
                        onComplete={model.completeHomework(problem.id,it)},
                        onLesson={model.openLesson(it);push("lesson|$it")},
                        onSource={push("pdf|${it.document}|${it.page}")},
                        onProblem={stack=stack.dropLast(1)+"homework|$it"})
                }
                "pdf" -> course.documentById[parts[1]]?.let { PdfReader(it,parts[2].toInt(),Modifier.fillMaxSize()) }
                "coverage" -> CoverageScreen(course,onDocument={doc,page->push("pdf|$doc|$page")},onLesson={model.openLesson(it);push("lesson|$it")})
                "licenses" -> AboutScreen()
            } }
        }
    }
    importCandidate?.let { data -> AlertDialog(onDismissRequest={importCandidate=null},title={Text("恢复全部学科学习备份")},
        text={Text("此备份包含 ${data.progress.size} 条学习记录、${data.reviews.size} 条复习记录和 ${data.attempts.size} 次答题。恢复将替换当前记录，建议先导出当前备份。")},
        confirmButton={TextButton(onClick={importCandidate=null;scope.launch {
            val result=withContext(Dispatchers.IO){runCatching{Backup.restore(model.db,data)}}
            message(result.fold({"备份已恢复"},{"恢复失败，原数据保留：${it.message}"}))
        }}){Text("替换并恢复")}},dismissButton={TextButton(onClick={importCandidate=null}){Text("取消")}}) }
}

@Composable
private fun LearnScreen(course: Course, progress: List<Progress>, reviews: List<Review>, attempts: Int, onLesson:(String)->Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    val lessonProgress=progress.filter {it.lessonId in course.lessonById}
    val completed=lessonProgress.filter {it.completed}.map {it.lessonId}.toSet()
    val last=lessonProgress.maxByOrNull {it.lastReadAt}?.lessonId ?: course.lessons.first().id
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF102F3A)),shape=RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("今天，弄懂一个为什么",color=Color.White,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
                Text("${course.lessons.size} 节讲解 · ${course.questions.size} 道练习 · 全部离线",color=Color(0xFFB8DDD6))
                LinearProgressIndicator(progress={completed.size.toFloat()/course.lessons.size},modifier=Modifier.fillMaxWidth(),color=Color(0xFF72DDD0),trackColor=Color(0xFF31525A))
                Text("已完成 ${completed.size}/${course.lessons.size} 节 · 已练习 $attempts 次",color=Color.White)
                Button(onClick={onLesson(last)},colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF72DDD0),contentColor=Color(0xFF102F3A))) {
                    Text(if(lessonProgress.isEmpty()) "从${if(course.subject=="info") "预备知识" else "第一节"}开始" else "继续：${course.lessonById[last]?.title}")
                }
            }
        } }
        item { val due=reviews.count {it.dueDay<=LocalDate.now().toEpochDay()}; Text("今日待复习 $due 道 · 错题 ${reviews.count {it.wrong}} 道",color=palette.primary) }
        item { OutlinedTextField(value=search,onValueChange={search=it},label={Text("搜索知识点或关键词")},singleLine=true,modifier=Modifier.fillMaxWidth()) }
        course.chapters.forEach { chapter ->
            val lessons=course.lessons.filter {it.chapter==chapter.id && (search.isBlank() || it.title.contains(search,true) || it.summary.contains(search,true))}
            if(lessons.isNotEmpty()) {
                item(key=chapter.id) { Column(Modifier.padding(top=12.dp)) {
                    Text(chapter.title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                    Text(chapter.subtitle,style=MaterialTheme.typography.bodySmall,color=palette.primary)
                } }
                items(lessons,key={it.id}) { lesson -> LessonCard(lesson,lesson.id in completed){onLesson(lesson.id)} }
            }
        }
        if(search.isNotBlank() && course.lessons.none {it.title.contains(search,true)||it.summary.contains(search,true)}) item { Text("没有找到对应知识点，试试“熵”“编码”或“概率”。") }
    }
}

@Composable
private fun LessonCard(lesson: Lesson, complete: Boolean, onClick:()->Unit) {
    Card(onClick=onClick,modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color.White)) {
        Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(if(complete) "✓" else "○",color=palette.primary,style=MaterialTheme.typography.titleLarge)
            Column(Modifier.weight(1f)) {
                Text(lesson.title,fontWeight=FontWeight.SemiBold)
                Text(lesson.summary,style=MaterialTheme.typography.bodySmall,color=Color(0xFF62757E))
            }
            Text("约${lesson.minutes}分",style=MaterialTheme.typography.labelSmall,color=palette.primary)
        }
    }
}

@Composable
private fun LessonScreen(lesson: Lesson, course: Course, complete: Boolean, onComplete:(Boolean)->Unit,
                         onPractice:()->Unit,onSource:(SourceRef)->Unit,onPrerequisite:(String)->Unit) {
    Column(Modifier.fillMaxSize()) {
        if(lesson.prerequisites.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            lesson.prerequisites.forEach { id -> AssistChip(onClick={onPrerequisite(id)},label={Text("先修 · ${course.lessonById[id]?.title}")}) }
        }
        if(lesson.sources.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            lesson.sources.forEach { source -> TextButton(onClick={onSource(source)}) { Text("${course.documentById[source.document]?.title ?: "资料"} · 第${source.page}页") } }
        }
        FormulaReader(lesson.html,Modifier.fillMaxWidth().weight(1f))
        Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick={onComplete(!complete)},modifier=Modifier.weight(1f)) { Text(if(complete) "已掌握 ✓" else "标记已学") }
            Button(onClick=onPractice,modifier=Modifier.weight(1f)) {Text("做配套练习")}
        }
    }
}

@Composable
private fun PracticeScreen(course: Course,onQuiz:(List<String>)->Unit) {
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Text("用练习检查理解",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold) }
        item { Text("选择题自动判分；数值题支持小数、科学计数法和分数。简答题看解析后自评。三类参数题每次生成有限变式。") }
        item { Button(onClick={onQuiz(course.questions.shuffled().take(10).map {it.id})},modifier=Modifier.fillMaxWidth()) {Text("随机练习 · 10 题")} }
        course.chapters.forEach { chapter ->
            val questions=course.questions.filter {course.lessonById[it.lessonId]?.chapter==chapter.id}
            item { ActionCard(chapter.title,"${questions.size} 道配套题 · 按章巩固"){onQuiz(questions.map {it.id})} }
        }
    }
}

@Composable
private fun QuestionScreen(question: Question,index:Int,total:Int,onAnswer:suspend(Boolean,String)->Unit,onNext:()->Unit,onLesson:()->Unit) {
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    var input by rememberSaveable { mutableStateOf("") }
    var revealed by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var correct by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope=rememberCoroutineScope()
    fun submit(value:Boolean,response:String) {
        if(busy || submitted) return
        busy=true;error=null
        scope.launch {
            runCatching {onAnswer(value,response)}.onSuccess {correct=value;revealed=true;submitted=true}
                .onFailure {error="保存失败，请重试：${it.message}"}
            busy=false
        }
    }
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("$index / $total",color=palette.primary,fontWeight=FontWeight.Bold)
            Text(when(question.kind){"choice"->"单项选择";"number"->"数值计算";else->"简答自评"},style=MaterialTheme.typography.labelLarge)
        };LinearProgressIndicator(progress={index.toFloat()/total},modifier=Modifier.fillMaxWidth().padding(top=8.dp)) }
        item { Text(question.prompt,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold) }
        if(question.kind=="choice") items(question.options.indices.toList()) { i ->
            Card(onClick={if(!submitted && !busy) selected=i},colors=CardDefaults.cardColors(containerColor=if(selected==i) Color(0xFFDDF2ED) else Color.White),modifier=Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                    RadioButton(selected=selected==i,onClick={if(!submitted && !busy)selected=i},enabled=!submitted && !busy)
                    Text(question.options[i],Modifier.padding(start=8.dp))
                }
            }
        } else item {
            OutlinedTextField(value=input,onValueChange={if(!submitted)input=it.take(4000)},readOnly=submitted,
                label={Text(if(question.kind=="number")"输入数值（不带单位）" else "先试着用自己的话回答")},modifier=Modifier.fillMaxWidth(),
                minLines=if(question.kind=="self")3 else 1,
                keyboardOptions=KeyboardOptions(keyboardType=if(question.kind=="number") KeyboardType.Text else KeyboardType.Text))
            if(question.kind=="number") Text("可输入 0.5、1/2 或 5e-1；容差 ${question.tolerance}。",style=MaterialTheme.typography.bodySmall)
        }
        if(error!=null) item {Text(error!!,color=palette.error)}
        if(!revealed) item { Button(enabled=!busy && (question.kind!="choice" || selected>=0),onClick={
            when(question.kind){
                "choice"->submit(selected.toString()==question.answer,selected.toString())
                "number"->if(StudyRules.parseNumber(input)==null)error="请输入有限数值或分数，分母不能为 0" else submit(StudyRules.numericCorrect(input,question.answer.toDouble(),question.tolerance),input)
                else->revealed=true
            }
        },modifier=Modifier.fillMaxWidth()){Text(if(busy)"正在保存…" else if(question.kind=="self")"查看参考答案" else "提交答案")} }
        if(revealed) {
            item { Card(colors=CardDefaults.cardColors(containerColor=if(submitted && !correct)Color(0xFFFFE9DD) else Color(0xFFDDF2ED)),modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text(if(question.kind=="self" && !submitted)"对照答案自评" else if(correct)"回答正确" else "再理解一次",fontWeight=FontWeight.Bold)
                    Text("参考答案："+if(question.kind=="choice")question.options[question.answer.toInt()] else question.answer)
                    Text(question.explanation)
                    if(submitted)Text(if(correct)"已更新复习计划，同一天反复作答不会连续升级。" else "已加入错题，明天会再遇见。",style=MaterialTheme.typography.bodySmall)
                }
            } }
            if(question.kind=="self" && !submitted) item { Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick={submit(false,input)},enabled=!busy,modifier=Modifier.weight(1f)){Text("还不熟悉")}
                Button(onClick={submit(true,input)},enabled=!busy,modifier=Modifier.weight(1f)){Text("已经理解")}
            } }
            item {TextButton(onClick=onLesson){Text("回到知识点讲解")}}
            if(submitted)item {Button(onClick=onNext,modifier=Modifier.fillMaxWidth()){Text(if(index==total)"完成本次练习" else "下一题")}}
        }
    }
}

@Composable
private fun QuizFinished(total:Int,onAgain:()->Unit,onDone:()->Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
        Text("一次练习，一步进展",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("已完成 $total 道题，记录与复习计划已保存在本机。",Modifier.padding(vertical=20.dp))
        Button(onClick=onDone){Text("回到自习室")};TextButton(onClick=onAgain){Text("再练一轮")}
    }
}

@Composable
private fun ReviewScreen(course:Course,reviews:List<Review>,onQuiz:(List<String>)->Unit,onLesson:(String)->Unit) {
    val today=LocalDate.now().toEpochDay()
    val due=reviews.filter {it.dueDay<=today}.sortedBy {it.dueDay}
    val wrong=reviews.filter {it.wrong}
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Text("让理解留得更久",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold) }
        item { Text("按 1、3、7、14、30 天复习。答错后回到 1 天；简答题根据自评安排。") }
        item { Button(onClick={onQuiz(due.map {it.questionId})},enabled=due.isNotEmpty(),modifier=Modifier.fillMaxWidth()){Text("开始今日复习 · ${due.size} 题")} }
        item { OutlinedButton(onClick={onQuiz(wrong.map {it.questionId})},enabled=wrong.isNotEmpty(),modifier=Modifier.fillMaxWidth()){Text("巩固错题 · ${wrong.size} 题")} }
        if(reviews.isEmpty()) item {Text("完成第一道练习后，这里会出现你的复习计划。")} else {
            item {Text("复习卡片",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
            items(reviews.sortedBy {it.dueDay},key={it.questionId}) { r ->
                val original=course.questionById.getValue(r.questionId)
                val q=r.lastVariant.substringAfterLast(':').toIntOrNull()?.let {QuestionVariants.make(original,it)} ?: original
                Card(colors=CardDefaults.cardColors(containerColor=Color.White)) {
                    Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(course.lessonById.getValue(q.lessonId).title,fontWeight=FontWeight.SemiBold)
                        Text(q.prompt)
                        Text("${if(r.wrong)"错题 · " else ""}${if(r.dueDay<=today)"已到复习时间" else "下次：${LocalDate.ofEpochDay(r.dueDay)}"}",color=palette.primary,style=MaterialTheme.typography.labelMedium)
                        Row {TextButton(onClick={onQuiz(listOf(q.id))}){Text("练这道题")};TextButton(onClick={onLesson(q.lessonId)}){Text("回顾讲解")}}
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialsScreen(course:Course,busy:Boolean,onDocument:(Document)->Unit,onCoverage:()->Unit,onAbout:()->Unit,onExport:()->Unit,onImport:()->Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {Text("资料始终在身边",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)}
        item {Text("${course.title} · ${course.documents.size} 份阅读资料，共 ${course.documents.sumOf { it.pages }} 页。页码均为 PDF 物理页码；与教材印刷页码可能不同。支持翻页、跳页和双指缩放。")}
        item { OutlinedTextField(value=query,onValueChange={query=it},label={Text("搜索资料名称或章节")},modifier=Modifier.fillMaxWidth(),singleLine=true) }
        items(course.documents.filter { query.isBlank() || it.title.contains(query,true) || it.kind.contains(query) },key={it.id}) {d->ActionCard(d.title,"${d.kind} · ${d.pages} 页 · 本地完整收录"){onDocument(d)}}
        item {ActionCard("资料覆盖清单","逐页查看课程关联，共 ${course.coverage.size} 页",onCoverage)}
        item {Text("学习数据",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
        item {Text("数据只保存在本机。卸载应用会删除记录；换机或卸载前请手动导出备份。")} 
        item {Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Button(onClick=onExport,enabled=!busy,modifier=Modifier.weight(1f)){Text("导出备份")}
            OutlinedButton(onClick=onImport,enabled=!busy,modifier=Modifier.weight(1f)){Text("恢复备份")}
        }}
        item {TextButton(onClick=onAbout){Text("关于课程与开源许可")}}
    }
}

@Composable
private fun CoverageScreen(course:Course,onDocument:(String,Int)->Unit,onLesson:(String)->Unit) {
    var selected by rememberSaveable {mutableStateOf(course.documents.first().id)}
    Column {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            course.documents.forEach {d->FilterChip(selected=selected==d.id,onClick={selected=d.id},label={Text(d.title)})}
        }
        LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {Text("各页提供章节或知识点讲解入口，不代表逐式讲解。完整推导、原题和图表请在原页对照阅读。",style=MaterialTheme.typography.bodySmall)}
            items(course.coverage.filter {it.document==selected},key={"${it.document}:${it.page}"}) {p->
                Card(colors=CardDefaults.cardColors(containerColor=Color.White)) {
                    Column(Modifier.padding(16.dp)) {
                        TextButton(onClick={onDocument(p.document,p.page)}){Text("第 ${p.page} 页 · 查看原页")}
                        Text(p.kind,style=MaterialTheme.typography.labelSmall)
                        p.lessons.forEach {id->TextButton(onClick={onLesson(id)}){Text(course.lessonById.getValue(id).title)}}
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionCard(title:String,subtitle:String,onClick:()->Unit) {
    Card(onClick=onClick,modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color.White)) {
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text(title,fontWeight=FontWeight.SemiBold)
            Text(subtitle,style=MaterialTheme.typography.bodySmall,color=palette.primary)
        }
    }
}

@Composable
private fun AboutScreen() {
    val context=androidx.compose.ui.platform.LocalContext.current
    val licenses=remember {context.assets.open("NOTICE.txt").bufferedReader().use{it.readText()}}
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {Text("三科学习室 · 0.3.0",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
        item {Text("信息论、通信原理与数字信号处理，依据本机课件、笔记、作业和教材整理。学习区提供基础讲解和例题，完整资料可在各学科资料页阅读。课件转换为阅读副本，原始文件不改写。扫描材料与公式应以原页核对。")}
        item {Text("本应用以个人离线学习为目的。公开分发前应另行核实教材和课件授权。当前课程属于初版讲解，原书的长推导请结合来源页阅读。")} 
        item {Text(licenses,style=MaterialTheme.typography.bodySmall)}
    }
}
