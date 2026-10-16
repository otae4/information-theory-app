package cn.selfstudy.information

import kotlin.math.ln
import kotlin.random.Random

/** Only these three explicitly supported templates generate variations. */
object QuestionVariants {
    fun make(question: Question, seed: Int): Question {
        val random = Random(seed xor question.id.hashCode())
        return when (question.variant) {
            "self_information" -> {
                val k = random.nextInt(2, 9)
                val denominator = 1 shl k
                question.copy(prompt = "某事件的概率为 1/$denominator，自信息是多少 bit？", answer = k.toString(),
                    explanation = "i=−log₂p=−log₂(1/$denominator)=$k bit。")
            }
            "bsc_capacity" -> {
                val e = listOf(0.0, 0.05, 0.1, 0.2, 0.25, 0.5, 1.0).random(random)
                val capacity = 1.0 - StudyRules.entropy(listOf(e,1-e))
                question.copy(prompt = "二元对称信道翻转概率 ε=$e，容量是多少 bit/次使用？保留三位小数。",
                    answer = capacity.toString(), explanation = "C=1−h₂(ε)=1−h₂($e)≈${"%.4f".format(capacity)} bit/次使用。", tolerance = .001)
            }
            "bandwidth_capacity" -> {
                val bandwidth = listOf(1000,2000,3000,4000).random(random)
                val ratio = listOf(3,7,15,31).random(random)
                val c = bandwidth * ln(1.0+ratio) / ln(2.0)
                question.copy(prompt = "理想带限高斯信道带宽 $bandwidth Hz，线性功率信噪比 $ratio，容量是多少 bit/s？",
                    answer = c.toString(), explanation = "C=B log₂(1+S/N)=$bandwidth×log₂(${ratio+1})=${c.toInt()} bit/s。")
            }
            else -> question
        }
    }
}
