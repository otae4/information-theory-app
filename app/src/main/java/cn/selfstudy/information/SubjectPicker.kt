package cn.selfstudy.information

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SubjectPicker(courses: Map<String,Course>, onSelect:(String)->Unit) {
    Surface(color=MaterialTheme.colorScheme.background,modifier=Modifier.fillMaxSize()) {
        LazyColumn(modifier=Modifier.safeDrawingPadding(),contentPadding=PaddingValues(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            item {
                Text("三科学习室",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
                Text("选择今天要学的学科",Modifier.padding(top=12.dp),style=MaterialTheme.typography.titleMedium)
                Text("课程、练习和作业按学科整理，学习记录分别保存。",Modifier.padding(top=8.dp))
            }
            courses.forEach { (id,course) ->
                item(key=id) {
                    Card(onClick={onSelect(id)},modifier=Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                            Text(course.title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
                            Text(when(id) {"info"->"信息量、熵、信源信道与编码";"comm"->"通信系统、信息速率、信道与噪声";else->"离散信号、变换、数字滤波与多采样率"})
                            Text("${course.lessons.size} 节学习 · ${course.questions.size} 道练习 · ${course.homework.size} 道作业")
                            Text("进入学习 →",color=MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
