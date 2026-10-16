"""Build the two additional subjects; original files stay outside Git.

Run export_subject_materials.ps1 and verify the actual PDF page counts first.
The learning units below explain core methods; complete derivations, figures,
notes and assignments remain readable in the linked local documents.
"""
from pathlib import Path, PureWindowsPath
import json
import math
import re
import sys

APP = Path(__file__).resolve().parents[1]
ASSETS = APP/'app/src/main/assets'
MANIFEST = json.loads((APP.parent/'build-content/subjects/materials-manifest.json').read_text(encoding='utf-8-sig'))
if any(entry['pages']<=0 for entry in MANIFEST):
    sys.path.insert(0,str(APP.parent/'.tools/python-libs'))
    import fitz
    for entry in MANIFEST:
        with fitz.open(ASSETS/entry['asset']) as pdf:
            if entry['pages']>0:
                assert entry['pages']==len(pdf),entry['source']
            entry['pages']=len(pdf)
    (APP.parent/'build-content/subjects/materials-manifest.json').write_text(json.dumps(MANIFEST,ensure_ascii=False,indent=2),encoding='utf8')

COMM = [
('c1','第1章 绪论','消息、信息、信号与系统模型'),
('c3','第3章 信道','信道特性、衰落、噪声与容量'),
]
DSP = [(f'c{i}', title, subtitle) for i,title,subtitle in [
(0,'课程导读与先修','时间、频率、复数与学习路线'),
(1,'第1章 离散时间信号与系统','序列、卷积、系统性质和抽样'),
(2,'第2章 Z变换与DTFT','收敛域、频率响应与复指数'),
(3,'第3章 离散傅里叶变换','DFT、频率抽样与应用'),
(4,'第4章 快速傅里叶变换','FFT算法、线性卷积与相关'),
(5,'第5章 数字滤波器的基本结构','IIR、FIR与格型实现'),
(6,'第6章 IIR及特殊滤波器','指标、原型、映射和频带变换'),
(7,'第7章 FIR数字滤波器设计','线性相位、窗与最优化'),
(8,'第8章 多抽样率信号处理','抽取、插值与有理变换'),
(9,'第9章 有限字长效应','量化、溢出与极限环'),
]]


def sec(title, text): return f'<section><h2>{title}</h2><p>{text}</p></section>'


class Builder:
    def __init__(self, subject, chapters):
        self.subject=subject
        self.chapters=[dict(id=subject+'-'+i,title=t,subtitle=s) for i,t,s in chapters]
        self.lessons=[];self.questions=[];self.homework=[];self.docs=[]
        for entry in MANIFEST:
            if entry['subject']!=subject:continue
            assert entry['pages']>0, 'Actual page counts must be verified before building.'
            name=PureWindowsPath(entry['source']).stem
            kind='作业' if entry['source'].startswith('作业') else '笔记' if entry['source'].startswith('笔记') else '课件' if entry['source'].endswith('.pptx') or name.startswith('CH') else '教材'
            if subject=='comm':
                c='c3' if 'CH3' in name or '第三章' in name or '作业二' in name else 'c1'
                title={'CH1':'第1章 绪论 · 完整课件','CH3':'第3章 信道 · 完整课件'}.get(name,name)
            else:
                match=re.match(r'([0-9])\.',name)
                c='c'+match.group(1) if match else 'c0' if '第0章' in name or '零基础' in name or kind=='教材' else 'c1'
                title='数字信号处理教程 · 第五版完整教材' if entry['id']=='dsp-doc-065' else '数字信号处理教程 · 习题分析与解答' if entry['id']=='dsp-doc-066' else name
            self.docs.append(dict(id=entry['id'],title=title,asset=entry['asset'],pages=entry['pages'],kind=kind,chapter=subject+'-'+c))
        def order(doc):
            section=re.match(r'(\d+)(?:\.(\d+))?',doc['title'])
            return (int(doc['chapter'].split('-c')[-1]), {'课件':0,'笔记':1,'作业':2,'教材':3}[doc['kind']],
                    int(section.group(1)) if section else 999, int(section.group(2) or 0) if section else 0,doc['title'])
        self.docs.sort(key=order)

    def add(self, chapter, key, title, explain, formula, example, caution, answer, prompt):
        id=self.subject+'-'+key
        cid=self.subject+'-'+chapter
        sources=[dict(document=d['id'],page=1) for d in self.docs if d['chapter']==cid and d['kind']=='课件']
        if not sources:sources=[dict(document=d['id'],page=1) for d in self.docs if d['kind']=='笔记'][:1]
        # The full chapter's documents are listed, rather than inventing exact source pages.
        body=sec('先建立直觉',explain)+sec('定义与条件',formula)+sec('完整例题',example)+sec('容易混淆的地方',caution)
        self.lessons.append(dict(id=id,chapter=cid,title=title,summary=explain,minutes=10,html=body,
                                 prerequisites=[self.lessons[-1]['id']] if self.lessons else [],sources=sources))
        self.questions.append(dict(id=id+'-q1',lessonId=id,kind='number',prompt=prompt,options=[],answer=str(answer),tolerance=.0001,explanation=example))
        self.questions.append(dict(id=id+'-q2',lessonId=id,kind='self',prompt='说明“'+title+'”的定义、适用条件和一个常见误区。',options=[],answer='对照讲解中的定义与条件、例题和易错点自评。',explanation=explain+' '+caution))

    def hw(self, number, title, chapter, lesson_keys, prompt, solution, assignment, src, page, detail, detail_page):
        self.homework.append(dict(id=self.subject+f'-hw-{len(self.homework)+1}',number=number,title=title,
            chapter=self.subject+'-'+chapter,relatedChapters=[],points=0,lessons=[self.subject+'-'+x for x in lesson_keys],
            promptHtml=sec(title,prompt),solutionHtml=sec('解题路径与核对',solution),assignment=assignment,
            source=dict(document=src,page=page),solution=dict(document=detail,page=detail_page)))

    def save(self):
        coverage=[]
        for d in self.docs:
            ids=[l['id'] for l in self.lessons if l['chapter']==d['chapter']]
            assert ids,(d['title'],d['chapter'])
            coverage.extend(dict(document=d['id'],page=p,lessons=ids,kind=d['kind']+' · 章节级关联，完整内容见原页') for p in range(1,d['pages']+1))
        folder=ASSETS/'subjects'/self.subject;folder.mkdir(parents=True,exist_ok=True)
        (folder/'course.json').write_text(json.dumps(dict(version=3,chapters=self.chapters,documents=self.docs,lessons=self.lessons,questions=self.questions,coverage=coverage),ensure_ascii=False,indent=2)+'\n',encoding='utf8')
        sources=[dict(name=d['title']) for d in self.docs if d['kind']=='作业']
        (folder/'homework.json').write_text(json.dumps(dict(courseVersion=3,title='课程作业 · 按原题顺序',chapterNote='作业与日常练习分开。按资料中已有的章节归类；原题、图表和完整详解可在题内直接打开。未标明的分值不另行编造。',sources=sources,problems=self.homework),ensure_ascii=False,indent=2)+'\n',encoding='utf8')
        print(self.subject,len(self.lessons),'lessons',len(self.questions),'questions',len(self.homework),'homework',len(self.docs),'documents',len(coverage),'pages')


c=Builder('comm',COMM)
c.add('c1','concept','消息、信息和信号','消息是要表达的内容，信号是承载它的物理形式，信息是接收者得到的不确定性的消除。','数字信号看承载信息的状态是否离散，不能只看时间是否连续。','用两种电压表示 0、1，承载信息的电压状态数为 2。','未量化抽样信号的幅度仍可连续，不因时间抽样就成为数字信号。',2,'用高、低两种电平表示符号，共有几种状态？')
c.add('c1','system','通信系统与编码模块','发送端把消息变成波形，信道使其衰减或受扰，接收端恢复消息。','信源编码用于表示与压缩；信道编码加入可控冗余用于抗差错；调制把符号映射成适合传输的波形。','四个等可能符号用固定二进制字表示，每个字至少需 2 bit。','译码应按与发送处理相配的逆序恢复；压缩编码与纠错编码不能混同。',2,'4 个等可能符号用等长二进制码表示，最少每符号多少 bit？')
c.add('c1','entropy','自信息与平均信息量','概率越小的事件越意外；长期平均需要按概率加权。',r'\(I(x)=-\log_2p(x),\quad H(X)=-\sum_xp(x)\log_2p(x)\)。零概率项按极限取 0。','概率为 1/8 的消息自信息为 3 bit。等概率 M 种符号的熵为 log₂M。','信息量不评价语义重要性；非等概率时不能直接用 log₂M。',3,'概率为 1/8 的消息，自信息是多少 bit？')
c.add('c1','rate','码元速率与信息速率','每秒发出多少个波形状态，与每秒传递多少比特信息，需要分别计数。',r'\(R_s=1/T_s\)，单位 Baud；等概率 M 进制固定映射下 \(R_b=R_s\log_2M\)；独立非等概率源平均速率为 \(rH(X)\)。','四进制 1200 Baud，每码元 2 bit，编码比特率为 2400 bit/s。','B 在本课件中表示 baud，不是 Byte；一个字母用两个脉冲时字母速率是脉冲速率的一半。',2400,'等概率四进制系统为 1200 Baud，比特率是多少 bit/s？')
c.add('c1','efficiency','频带利用率与差错率','系统既要传得快，也要足够可靠；速率和带宽的计量口径必须一致。',r'\(\eta_b=R_b/B\)，单位 bit/(s·Hz)；码元差错率 \(P_e=N_{se}/N_s\)，误比特率 \(P_b=N_{be}/N_b\)。','2000 bit/s 占用 1000 Hz 时，频带利用率为 2 bit/(s·Hz)。','只知道错误码元数一般不能推出误比特率；分子与分母必须统计同一种对象。',2,'2000 bit/s、1000 Hz，频带利用率的数值是多少？')
c.add('c3','models','有线、无线与恒参、随参信道','媒质、系统边界与参数随时间变化，是三种不同的分类依据。',r'线性信道可写为 \(r(t)=s(t)*h(t)+n(t)\)。恒参模型的参数固定；随参模型中增益、时延可能随时间变化。','若仅有固定增益 0.5 和固定延迟，输入幅度为 4 时输出幅度为 2。','随参不是“输入为随机信号”的同义词；有线和恒参也不是严格等同。',2,'固定增益为 0.5，输入幅度为 4，输出幅度是多少？')
c.add('c3','distortion','线性失真与均衡','不同频率受到不同衰减或时延，会使原波形改变并形成码间串扰。',r'无失真传输在工作带内满足 \(H(\omega)=K e^{-j\omega t_d}\)：幅度恒定、相位线性、群时延恒定。','3 个样本的固定延迟只平移序列；平坦增益为 2 时，幅度 1 的信号变为 2。','相位线性不意味着相位处处为零；均衡不能把任意噪声完全消除。',2,'无失真模型固定增益 2，输入幅度 1，输出幅度多少？')
c.add('c3','multipath','双径衰落与相关带宽','直射与反射可能相长或抵消。时延差越大，频率响应起伏越密。',r'等幅双径模型 \(H(f)=1+e^{-j2\pi f\tau}\)；零点 \(f=(2k+1)/(2\tau)\)，峰值 \(f=k/\tau\)。','时差 1 ms 时，第一个正频率零点为 500 Hz，相邻零点相差 1000 Hz。','等幅且无额外相位的假设是完全抵消的条件；相关带宽只是与时延扩展相关的工程尺度。',500,'等幅双径时差为 1 ms，第一个正频率衰落零点是多少 Hz？')
c.add('c3','coherence','脉冲宽度与频率选择性衰落','码元足够长时，时延展宽相对较小；等价地，信号带宽相对相关带宽较窄。',r'课件近似 \(B_c\approx1/\Delta\tau\)，并取 \(B_s\approx(1/5\sim1/3)B_c\)。若 \(B_s\approx1/T_s\)，则 \(T_s\approx(3\sim5)\Delta\tau\)。','最大多径时差 2 μs 时，相关带宽约为 500 kHz，推荐码元宽度约 6～10 μs。','工程定义和脉冲形状会改变常数；这不是适用于一切信道的精确恒等式。',500000,'按 Bc≈1/Δτ，Δτ=2 μs 时 Bc 是多少 Hz？')
c.add('c3','noise','噪声、功率谱与分贝','噪声功率与带宽有关，分贝只是功率比的对数表示。',r'功率信噪比 \(\gamma=S/N\)，\(\gamma_{dB}=10\log_{10}\gamma\)。双边白噪声谱密度为 N₀/2 时，理想低通带内噪声为 N₀B。','30 dB 对应线性功率比 1000，而不是 30。','幅度比的 20log 与功率比的 10log 不可混用；白噪声与高斯噪声描述不同性质。',1000,'功率信噪比 30 dB 对应线性比多少？')
c.add('c3','capacity','香农容量与所需带宽','容量是在给定带宽与噪声条件下可实现可靠传输的信息速率上限。',r'带限加性高斯噪声模型下 \(C=B\log_2(1+S/N)\)。反求 \(S/N=2^{R/B}-1\)。','B=1000 Hz，线性信噪比为 3，则 C=1000log₂4=2000 bit/s。','不能把 dB 数直接代入；取等号是理想极限，实际系统需要编码与实现裕量。',2000,'带宽 1000 Hz、线性信噪比 3，香农容量是多少 bit/s？')

d=Builder('dsp',DSP)
units=[
('c0','sampling','从连续时间到数字序列','采样把时间离散化，量化把幅度离散化，编码再把量化值表示成比特。',r'\(T_s=1/f_s,\ t_n=n/f_s\)，离散角频率 \(\omega=2\pi f/f_s\)，单位 rad/sample。','fs=100 Hz 时，第 3 个样本对应时刻 0.03 s。','时间索引 n 没有秒单位；只采样、不量化时幅度仍连续。',.03,'采样率 100 Hz，编号 n=3 对应多少秒？'),
('c0','complex','复数、复指数与频率单位','复数把幅度与相位放在同一个表达式中，是变换与滤波的语言。',r'\(e^{j\theta}=\cos\theta+j\sin\theta\)；\(|e^{j\theta}|=1\)。','复数 3+4j 的模为 5；频率相同而相位不同的信号不会因此变成不同频率。','j 不是时间变量；角频率、周期和 Hz 应先换为同一口径。',5,'复数 3+4j 的模是多少？'),
('c1','sequence','基本序列与移位','单位抽样只在一个索引处非零，单位阶跃从指定位置开始保持为一。',r'\(\delta[n-k]\) 在 n=k 非零；约定 u[0]=1，\(R_N[n]=u[n]-u[n-N]\)。','δ[n−2] 在 n=2 取 1，其余为零。长度 N 的矩形从 0 延续到 N−1。','x[−n] 是反折；x[n−k] 在 k>0 时向右移。',2,'δ[n−2] 在哪个整数 n 非零？'),
('c1','convolution','卷积与非零区间','卷积先反折平移，再逐点相乘求和；有限长时只需统计有重叠的索引。',r'\(y[n]=\sum_kx[k]h[n-k]\)。连续非零有限长输入长度为 L、M 时，卷积长度至多为 L+M−1。','[1,1,1] 与 [1,1,1,1] 卷积为 [1,2,3,3,2,1]，长度 6。','卷积非零范围不是两个序列支持集的交集；移位冲激使整个序列一起移动。',6,'长度 3 和 4 的全 1 序列卷积长度是多少？'),
('c1','period','周期序列与基本周期','离散索引要求重复间隔是正整数，因此连续正弦的周期性不能直接照搬。',r'对非退化复指数，\(\omega_0N=2\pi m\)。若 \(\omega_0/(2\pi)=p/q\) 为最简有理数，基本周期为 q。','cos(3πn/7) 的基本周期为 14。','相位不改变普通非退化正弦序列的基本周期；零序列、项间抵消需要单独判断。',14,'非零幅度 cos(3πn/7) 的基本周期是多少？'),
('c1','properties','线性、移不变、因果与稳定','四种性质分别检验，任何一种成立都不自动推出其余性质。',r'LSI 因果当且仅当 h[n]=0(n&lt;0)；BIBO 稳定当且仅当 \(\sum_n|h[n]|&lt;\infty\)。','h[n]=0.5ⁿu[n] 的绝对和为 2，因此因果且稳定。','h 有界不等于绝对可和；固定时变增益系统仍可线性但不移不变。',2,'h[n]=0.5ⁿu[n] 的绝对和是多少？'),
('c1','difference','差分方程与初始条件','递推方程给出相邻样本关系，初始条件决定响应是否含有零输入项。',r'\(y[n]=a y[n-1]+x[n]\)。零状态、输入 δ[n] 时，\(h[n]=a^n u[n]\)。','a=0.5、y[−1]=0、阶跃输入时，y[0]=1，y[1]=1.5。','双边复指数输入不能因为系统因果就擅自乘 u[n]；先核对输入和初始状态。',1.5,'y[n]=0.5y[n−1]+u[n]，y[−1]=0，求 y[1]。'),
('c1','alias','采样定理与混叠','采样后相隔整数倍采样率的模拟频率可能映射到同一离散频率。',r'低通信号带限到 fmax 时，理想重建通常要求 \(f_s&gt;2f_{max}\)，且采样前进行抗混叠滤波。','100 Hz 采样观察 60 Hz 余弦，会混叠到 40 Hz。','仅把 fs 恰好取两倍最高正弦频率可能受相位退化影响；不要忽略抗混叠滤波器。',40,'100 Hz 采样时，60 Hz 余弦混叠到哪个正频率 Hz？'),
('c2','z','Z变换与收敛域','Z 变换将序列映射为复变量函数；代数式必须与收敛域一起使用。',r'\(X(z)=\sum_nx[n]z^{-n}\)。对 aⁿu[n]，\(X(z)=1/(1-az^{-1}),\ |z|&gt;|a|\)。','a=0.5 的右边序列，其收敛域外边界半径为 0.5；单位圆在收敛域内。','同一个有理式配不同收敛域可对应不同序列；不能只看极点图不看 ROC。',.5,'0.5ⁿu[n] 的 Z 变换 ROC 边界半径为多少？'),
('c2','dtft','DTFT、单位圆与频率响应','DTFT 对整数时间、连续频率进行分析；其频率函数以 2π 为周期。',r'\(X(e^{j\omega})=\sum_nx[n]e^{-j\omega n}\)。ROC 包含单位圆时可由 Z 变换在 z=e^{jω} 处取得。','单位抽样的 DTFT 恒为 1；[1,1] 在 ω=0 的值为 2。','DTFT 连续频率不意味着时域连续；相位在幅度为零处需谨慎解释。',2,'x[0]=x[1]=1，其余为0，DTFT在ω=0的值是多少？'),
('c3','dft','DFT、IDFT与频率格点','DFT 把 N 个时间样本映射成 N 个频率样本，可视为有限记录的频域表示。',r'\(X[k]=\sum_{n=0}^{N-1}x[n]e^{-j2\pi kn/N}\)，\(\Delta f=f_s/N\)。','fs=800 Hz，N=800 时，频率格点间隔为 1 Hz。','补零可使谱线网格更密，但不会增加原记录的有效观测时长与真实分辨能力。',1,'采样率800 Hz，800点DFT，频率格点间隔是多少Hz？'),
('c3','circular','圆周卷积与频域抽样','DFT 中频域相乘对应周期意义下的圆周卷积，而不是任意长度的线性卷积。',r'线性卷积用 DFT 实现时应补零到 \(N\ge L+M-1\)。不足时会发生时域混叠。','长度 3 和 4 的序列做线性卷积，DFT 长度至少为 6。','圆周移位与普通移位的边界处理不同；频域抽样重建也受有限长假设约束。',6,'长度3与4的序列用DFT算线性卷积，最少N是多少？'),
('c4','fft','基2 FFT与蝶形计算','FFT 复用 DFT 的对称性，改变计算步骤而不改变 DFT 的数学定义。',r'基2 FFT 的长度 \(N=2^m\)，需要 m 级蝶形；计算量约按 Nlog₂N 增长。','1024 点基2 FFT 有 10 级蝶形。DIT 与 DIF 的分解顺序不同，需核对输入输出次序。','FFT不是另一种变换；混合基、CZT等算法有各自索引与采样条件。',10,'1024点基2FFT需要多少级蝶形？'),
('c4','blocks','用FFT处理卷积与相关','长记录可分块，用 FFT 降低卷积代价，但块边界必须正确处理。',r'重叠相加法需对各块补零，长度满足 \(N\ge L+M-1\)；相关与卷积还涉及反折和复共轭。','64点输入块与17点冲激响应卷积，长度至少为80，可用128点基2FFT。','重叠保留法应丢弃受圆周混叠污染的前 M−1 点；相关不能直接漏掉共轭。',80,'长度64的块与长度17的滤波器线性卷积，结果长度是多少？'),
('c5','structures','FIR、IIR与实现结构','同一传输函数可有不同流图，数学等价不保证有限精度误差和存储开销相同。',r'\(H(z)=B(z)/A(z)\)。FIR 无反馈的直接实现长度为 M 时，通常需 M 个系数与 M−1 个延时。','长度5的直接型FIR需要4个历史输入延时单元。','直接型、级联型、并联型与格型关注实现结构，不是新的频率响应定义。',4,'长度5的直接型FIR需几个历史输入延时单元？'),
('c6','spec','滤波器指标与特殊系统','先把通带、阻带与过渡带写成指标，再选系统类型；全通只约束幅度。',r'\(A_s=-20\log_{10}|H|\)。稳定全通在单位圆上模为1；最小相位要求零点与极点满足相应因果稳定条件。','阻带幅度为0.01，相对衰减为40 dB。','全通并不意味着相位恒定；最小相位不是“相位最小的一个频率点”。',40,'幅度比0.01对应多少dB衰减？'),
('c6','bilinear','模拟原型、双线性变换与预畸变','数字 IIR 常由模拟低通原型配频带变换得到；模拟到数字的频率映射通常不是线性的。',r'双线性变换 \(s=\frac2T\frac{1-z^{-1}}{1+z^{-1}}\)，\(\Omega=\frac2T\tan(\omega/2)\)。','T=1、ω=π/2 时，预畸变后的模拟角频率为2 rad/s。','脉冲响应不变法可能产生频谱混叠；双线性变换避免该混叠但有频率扭曲，不能省略关键频率预畸变。',2,'T=1、数字ω=π/2，经双线性预畸变得到Ω是多少？'),
('c7','linear-phase','线性相位FIR与群时延','实系数 FIR 的对称或反对称冲激响应可带来广义线性相位，适合保持波形相对位置。',r'长度 M 的对称 FIR 常见群时延为 \((M-1)/2\) 个样本，需结合零点和相位跳变解释。','长度11的对称FIR群时延为5个样本。','四类线性相位FIR在0和π处的强制零点不同；不是所有类型都能做任意通带。',5,'长度11的对称FIR群时延是多少个样本？'),
('c7','window','窗函数、频率抽样与最优设计','截断理想冲激响应会引起旁瓣与过渡带；窗设计在主瓣宽度与旁瓣高度间取舍。',r'窗法 \(h[n]=h_d[n]w[n]\)。频率抽样法由指定频率样本求 IDFT；等波纹法按加权最大误差优化。','长度7的对称窗中心位于索引3；增加长度通常缩窄过渡带，但计算量和延迟增加。','不能把低旁瓣等同于窄主瓣；最优化结果取决于权重与频带约束。',3,'长度7的对称窗若索引从0开始，其中心索引是多少？'),
('c8','decimate','整数抽取与抗混叠','抽取只保留每 D 个样本中的一个，会降低采样率，因此通常先低通滤波。',r'\(y[n]=x[Dn],\ f_{out}=f_{in}/D\)。抽取前应限制有效带宽，以免谱副本重叠。','48 kHz 采样的数据按 D=3 抽取，输出采样率为16 kHz。','抽取与直接截取一段数据不同；先丢点后滤波不能消除已经形成的混叠。',16000,'48kHz采样信号按3抽取，输出采样率是多少Hz？'),
('c8','interpolate','整数插值、有理变换与多级结构','插值先插入零，再用低通重建中间样值；有理变换组合升采样与降采样。',r'按 L/M 转换采样率，\(f_{out}=f_{in}L/M\)。多相结构可避免计算许多必为零或最终丢弃的样本。','输入48 kHz，经L=2、M=3转换后为32 kHz。','插零并不是已经完成平滑插值；滤波增益与频率标尺要按所在采样率解释。',32000,'48kHz经2/3有理采样率变换后是多少Hz？'),
('c9','quantization','定点表示与量化噪声','有限比特数意味着舍入、截断与有限动态范围；误差可能随滤波器传播。',r'均匀舍入量化、误差近似均匀且不相关时，\(\sigma_e^2=\Delta^2/12\)。这是一种统计近似。','量化步长Δ=1时，近似误差方差为1/12。','量化误差不总是独立白噪声；过载、确定性小信号可能违反模型。',1/12,'均匀舍入量化步长1，白噪声近似下误差方差是多少？'),
('c9','finite','系数量化、溢出与极限环','量化会移动极点和零点；反馈中舍入误差还可能维持无输入振荡。',r'因果有理系统稳定要求所有未消去的系统极点位于单位圆内；有限字长实现需检查量化后的极点和内部幅度。','极点半径0.9稳定；半径1.1的因果一阶系统冲激响应增长。','输入为零不保证定点IIR输出最终为零；缩放抑制溢出可能增加量化噪声，需要联合验证。',.9,'一阶系统极点为0.9，该极点的半径是多少？'),
]
for unit in units:d.add(*unit)

# Communications: preserve all seven original questions, with exact source and solution pages.
c.hw(1,'字母编码与平均信息速率','c1',['entropy','rate'],
 'A/B/C/D 分别编码为00/01/10/11，每个脉冲5ms。求等概率与概率(1/5,1/4,1/4,3/10)时的平均信息速率。',
 r'字母速率为100 symbol/s。等概率熵2 bit/字母，速率200 bit/s。非等概率熵1.985475297 bit/字母，平均速率198.5475297 bit/s。编码后的二进制位流仍为200 bit/s。','作业一','comm-doc-008',1,'comm-doc-007',1)
c.hw(2,'一小时的信息量与最大信息量','c1',['entropy','rate'],
 '五符号概率(1/4,1/8,1/8,3/16,5/16)，速率1000B，求一小时平均信息量及可达到的最大平均信息量。',
 'B指baud而非字节。H=2.227217001 bit/符号，一小时有3600000个符号，信息量约8017981.205 bit。最大熵log₂5，对应8358941.142 bit。独立符号条件不可省略。','作业一','comm-doc-008',1,'comm-doc-007',2)
c.hw(3,'四进制系统的码元差错率','c1',['rate','efficiency'],
 '四进制传信率2400bit/s，半小时内216个错误码元，求课件定义的误码率Pe。',
 r'每码元2bit，Rs=1200 Baud。总码元数1200×1800=2160000，Pe=216/2160000=10⁻⁴。此处不是误比特率，不能用错误码元数除以总比特数。','作业一','comm-doc-008',1,'comm-doc-007',3)
c.hw(1,'双径衰落的零点与峰值','c3',['multipath'],
 '等幅双径信道时差τ=1ms，求最大衰耗频率及传输最有利频率。',
 r'\(H(f)=2e^{-j\pi f\tau}\cos(\pi f\tau)\)。最大衰耗在(2k+1)×500Hz，峰值在k×1000Hz；有利频段位于峰值附近，具体带宽还取决于允许的衰减。','作业二','comm-doc-006',1,'comm-doc-005',1)
c.hw(2,'时延扩展与码元宽度','c3',['coherence'],
 '最大多径时延差2μs，估算避免频率选择性衰落的码元脉冲宽度。',
 '按课件工程口径Bc≈1/Δτ=500kHz，Bs约取Bc的1/5至1/3，并取Bs≈1/Ts，则Ts约为6～10μs。不是唯一精确下限。','作业二','comm-doc-006',1,'comm-doc-005',2)
c.hw(3,'电话信道容量与所需信噪比','c3',['noise','capacity'],
 'B=3.4kHz，求30dB信噪比下容量；若传输33.6kbit/s，求理论最小接收信噪比dB。',
 r'先换成线性功率比1000。\(C=3400\log_2(1001)=33888.57\) bit/s。反求\(10\log_{10}(2^{33600/3400}-1)=29.744\)dB；工程系统需留裕量。','作业二','comm-doc-006',1,'comm-doc-005',3)
c.hw(4,'图像传输的最小带宽','c3',['entropy','capacity'],
 '每幅6×10⁵像素，每像素16个等概率亮度电平，每秒24幅，输出信噪比30dB，求所需理论最小带宽。',
 r'按未压缩、独立等概率像素信息与无额外开销计算，R=6×10⁵×4×24=57.6Mbit/s。\(B=R/\log_2(1001)\approx5.780\)MHz。','作业二','comm-doc-006',1,'comm-doc-005',4)

# DSP: each item opens the exact original exercise page and the complete detailed solution.
dsp_homework=[
('1.1 有限长指数与右边指数卷积','convolution',68,1,r'h[n]=αⁿR_N[n]，x[n]=β^(n−n₀)u[n−n₀]。求卷积，分别处理α=β与α≠β。',r'令r=n−n₀，r&lt;0时为0；其余取L=min(N−1,r)，求和\(\sum_{k=0}^L\alpha^k\beta^{r-k}\)。相等底数时为(L+1)a^r。完整分段公式与边界约定见详解第1页。'),
('1.2 五组卷积与输出图','convolution',68,2,'完成原题五组输入和冲激响应的卷积并画图，保留移位、双边序列和阶跃的原始定义。','结果依次涉及矩形恒等响应、[1,2,3,3,2,1]、延迟后[1,1/2,1/4]、双边衰减响应，以及n=1至3幅度0.8的矩形。原题完整数据及所有输出图见对应详解第2～6页。'),
('1.4 周期性与基本周期','period',69,7,'逐项判断9个序列是否周期，并求存在时的最小正整数周期。','非退化指数先把ω/(2π)约成有理数；无理则非周期。前两项周期14、6；第8项周期56，第9项周期7。其余需区分无理频率、衰减包络、退化零项。详解保留9个原式及逐项证明。'),
('1.6 线性与移不变性','properties',69,11,'对原题5个系统分别检验线性、移不变，特别区分幅度平方与索引平方。','累加器：线性且移不变；幅度平方：非线性但移不变；时变正弦增益、反折和平方索引：线性但移变。不能把x[n²]误读为x[n]²。详解第11～12页给出反例。'),
('1.7 四种系统性质','properties',69,13,'完成指定小问（1）、（5）、（10），分别检验线性、移不变、因果与稳定。','逐项以原题为准。第（10）项输出u[n]/n与输入无关，不是增益乘输入；n=0未定义，须先说明补定义后再讨论稳定性。完整原式、条件与证明见详解第13～14页。'),
('1.8 由h[n]判断因果与稳定','properties',69,15,'对原题8种单位抽样响应分别判断因果性与BIBO稳定性。','因果看负索引是否为零，稳定看绝对和。δ[n+4]非因果但稳定；u[4−n]幅度有界但不稳定。u[n]/n²在零点缺定义，不能只以正整数尾和收敛就直接下结论。详解第15～17页包含全部8项。'),
('1.13 框图、差分方程与输出','difference',70,18,r'由教材图P1.13列差分方程；y[n]=0(n&lt;0)，输入u[n]，求输出并画图。',r'命名内部节点：v[n]=x[n]+v[n−1]/4，y[n]=v[n]+v[n−1]。消元得y[n]−y[n−1]/4=x[n]+x[n−1]。\(y[n]=(8/3-(5/3)(1/4)^n)u[n]\)。图、首值和递推自检见详解第18～20页。'),
('1.14 因果系统与双边复指数','difference',70,21,r'y[n]−y[n−1]/2=x[n]+x[n−1]/2。求h[n]及输入e^(jωn)的零状态响应。',r'\(h[n]=\delta[n]+(1/2)^{n-1}u[n-1]\)。双边复指数输出为\(\frac{1+(1/2)e^{-j\omega}}{1-(1/2)e^{-j\omega}}e^{j\omega n}\)，不能擅自给输入加u[n]；详解第21～22页说明与参考书口径差异。'),
]
for i,(title,key,page,detail,prompt,solution) in enumerate(dsp_homework,1):
    d.hw(i,title,'c1',[key],prompt,solution,'第一章作业','dsp-doc-065',page,'dsp-doc-067',detail)
c.save();d.save()
# Independently check representative assignment answers and design conditions.
assert abs(3400*math.log2(1001)-33888.56928)<.01
assert abs(10*math.log10(2**(33600/3400)-1)-29.744)<.001
assert 216/(1200*1800)==.0001
assert [sum(1 for k in range(3) if 0<=n-k<4) for n in range(6)]==[1,2,3,3,2,1]
