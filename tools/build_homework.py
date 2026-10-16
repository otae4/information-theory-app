"""Maintain the reviewed homework separately from the original practice bank.

Source documents are read-only inputs kept outside the repository. Question 4's
transition probabilities were checked against the embedded channel diagram.
"""
import hashlib
import json
import math
from pathlib import Path

APP = Path(__file__).resolve().parents[1]


def entropy(ps):
    assert all(p >= 0 for p in ps) and abs(sum(ps) - 1) < 1e-10
    return -sum(p * math.log2(p) for p in ps if p)


def table(headers, rows):
    return '<div class="table-scroll"><table><thead><tr>' + ''.join(f'<th>{x}</th>' for x in headers) + '</tr></thead><tbody>' + ''.join('<tr>' + ''.join(f'<td>{x}</td>' for x in row) + '</tr>' for row in rows) + '</tbody></table></div>'


def section(title, body):
    return f'<section><h2>{title}</h2>{body}</section>'


counts = [1, 2, 3, 4, 5, 6, 5, 4, 3, 2, 1]
dice = [n / 36 for n in counts]
h_dice = entropy(dice)
h_white, h_black = entropy([25/33, 8/33]), entropy([35/67, 32/67])
h_coin_cond = .33*h_white + .67*h_black
joint = [[1/4, 1/12, 0], [1/12, 5/24, 1/24], [0, 5/18, 1/18]]
ys = [sum(row[j] for row in joint) for j in range(3)]
h_joint, h_y = entropy(sum(joint, [])), entropy(ys)
h_channel_cond = .4*entropy([3/4, 1/4]) + .6*entropy([1/3, 2/3])
h_x = entropy([.4, .6])
results = {
    'dice_self_4': math.log2(12), 'dice_self_8': math.log2(36/5), 'dice_entropy': h_dice,
    'coin_given_white': h_white, 'coin_given_black': h_black, 'coin_conditional': h_coin_cond,
    'joint_entropy': h_joint, 'marginal_y': h_y, 'conditional_x_y': h_joint-h_y,
    'channel_output': 1., 'channel_y_x': h_channel_cond, 'channel_x_y': h_x+h_channel_cond-1,
    'channel_joint': h_x+h_channel_cond, 'point_information': math.log2(2/3),
    'given_output_information': .6*math.log2(1.5)+.4*math.log2(2/3),
    'mutual_information': 1-h_channel_cond,
}
expected = [3.5850, 2.8480, 3.2744, .7990, .9986, .9327, 2.5050, 1.3178, 1.1872,
            1., .8755, .8464, 1.8464, -.5850, .1170, .1245]
assert all(abs(a-b) < .00005 for a, b in zip(results.values(), expected))

problems = []


def problem(number, title, lessons, prompt, solution, related=None):
    problems.append(dict(id=f'hw1-q{number}', number=number, title=title, chapter='c2',
                         relatedChapters=related or [], points=25, lessons=lessons,
                         promptHtml=prompt, solutionHtml=solution))


problem(1, '两枚骰子的自信息与熵', ['entropy-self', 'entropy-mean'],
    section('第 1 题 · 自信息和信源熵 · 25 分', r'''<p>同时投掷两枚正常的骰子（即每个点数的概率都是 1/6），令 \(X\) 为两个点数之和，求：</p><ol><li>\(X\) 的概率空间。</li><li>消息 \(X=4\) 和 \(X=8\) 的自信息。</li><li>\(X\) 的熵（平均自信息）。</li></ol>'''),
    section('1. 概率空间', '<p>两枚骰子有 36 个等可能的有序结果。点数和为 x 的概率等于对应组合数除以 36；点数和本身并不等概率。</p>' +
        table(['点数和 x', '组合数', 'P(X=x)'], [[str(x), str(n), f'{n}/36'] for x, n in zip(range(2, 13), counts)]) + r'<p>\(\sum_{x=2}^{12}p(x)=1\)。</p>') +
    section('2. 消息的自信息', r'''<p>使用 \(I(x)=-\log_2p(x)\)。</p><p>\(p(4)=3/36=1/12\)，所以 \(I(4)=\log_2 12=3.5850\) bit。</p><p>\(p(8)=5/36\)，所以 \(I(8)=\log_2(36/5)=2.8480\) bit。</p><p>4 比 8 更少见，因此自信息更大。</p>''') +
    section('3. 平均自信息', r'''<p>\(H(X)=-\sum_{x=2}^{12}p(x)\log_2p(x)\)。</p><p>利用分布的对称性：</p><p>\(H(X)=-2\sum_{k=1}^{5}\frac{k}{36}\log_2\frac{k}{36}-\frac6{36}\log_2\frac6{36}=3.2744\) bit。</p><p>自检：\(H(X)\le\log_2 11\)。不能用 log₂36 代替，因为本题只观察两个骰子的和，而非有序点数对。</p>'''))

problem(2, '贝叶斯公式与条件熵', ['p-bayes', 'entropy-joint'],
    section('第 2 题 · 条件熵 · 25 分', r'''<p>瓮 1 中有 5 个白球、7 个黑球；瓮 2 中有 3 个白球、12 个黑球。抛一枚偏硬币，正面概率为 0.6，反面概率为 0.4。正面从瓮 1 取球，反面从瓮 2 取球。</p><p>令 \(X\in\{H,T\}\) 表示硬币结果，\(Y\in\{W,B\}\) 表示球的颜色。</p><ol><li>已知取到白球时，抛硬币结果的平均不确定度是多少？提示：使用贝叶斯公式。</li><li>已知取到黑球时，该不确定度是多少？</li><li>已知球颜色情况下，求硬币结果的平均不确定度（条件熵）。</li></ol>'''),
    section('1. 先算边缘概率', r'''<p>\(p(W)=0.6\times5/12+0.4\times3/15=0.33\)。</p><p>\(p(B)=0.6\times7/12+0.4\times12/15=0.67\)。</p><p>两种颜色概率之和为 1。</p>''') +
    section('2. 已知白球', r'''<p>先用贝叶斯公式更新正反面的概率：</p><p>\(p(H\mid W)=\frac{0.6\times5/12}{0.33}=25/33\)，\(p(T\mid W)=8/33\)。</p><p>\(H(X\mid W)=-\frac{25}{33}\log_2\frac{25}{33}-\frac8{33}\log_2\frac8{33}=0.7990\) bit。</p>''') +
    section('3. 已知黑球', r'''<p>\(p(H\mid B)=\frac{0.6\times7/12}{0.67}=35/67\)，\(p(T\mid B)=32/67\)。</p><p>\(H(X\mid B)=-\frac{35}{67}\log_2\frac{35}{67}-\frac{32}{67}\log_2\frac{32}{67}=0.9986\) bit。</p><p>后验分布接近等概率，故熵接近 1 bit。</p>''') +
    section('4. 条件熵', r'''<p>\(H(X\mid Y)=p(W)H(X\mid W)+p(B)H(X\mid B)\)。</p><p>\(H(X\mid Y)=0.33H(X\mid W)+0.67H(X\mid B)=0.9327\) bit。</p><p>计算时保留中间精度，再对最终值四舍五入。不要对两种颜色的后验熵直接取算术平均。</p>'''))

joint_table = table(['X / Y', 'y₁', 'y₂', 'y₃'], [
    ['x₁', '1/4', '1/12', '0'], ['x₂', '1/12', '5/24', '1/24'], ['x₃', '0', '5/18', '1/18']])
problem(3, '联合熵、边缘熵与条件熵', ['entropy-joint', 'entropy-table'],
    section('第 3 题 · 联合熵 · 25 分', r'<p>设两个试验 \(X\in\{x_1,x_2,x_3\}\)、\(Y\in\{y_1,y_2,y_3\}\)，联合概率 \(p_{ij}=p(x_i,y_j)\) 如下（行对应 X，列对应 Y）：</p>' + joint_table +
        r'<ol><li>得知 X 和 Y 的结果，得到的平均信息量是多少？</li><li>只得知 Y 的结果，得到的平均信息量是多少？</li><li>已经得知 Y 后，再告诉你 X 的结果，得到的平均信息量是多少？</li></ol>'),
    section('1. 检查与边缘化', joint_table + r'<p>每一行和均为 1/3；各列之和为 \(p(Y)=(1/3,41/72,7/72)\)，总和为 1。</p>') +
    section('2. 联合熵', r'<p>\(H(X,Y)=-\sum_{i,j}p_{ij}\log_2p_{ij}=2.5050\) bit。</p><p>遍历所有非零格子，零概率项按 \(0\log_2 0=0\) 的极限约定处理。</p>') +
    section('3. 边缘熵', r'<p>\(H(Y)=-\frac13\log_2\frac13-\frac{41}{72}\log_2\frac{41}{72}-\frac7{72}\log_2\frac7{72}=1.3178\) bit。</p>') +
    section('4. 条件熵', r'<p>应用链式法则：</p><p>\(H(X\mid Y)=H(X,Y)-H(Y)=1.1872\) bit。</p><p>自检：\(H(X\mid Y)\le H(X)=\log_2 3\)，且 \(H(X,Y)=H(Y)+H(X\mid Y)\)。</p>'))

channel_table = table(['输入 X', 'P(Y=0 | X)', 'P(Y=1 | X)', 'P(X)'], [
    ['0', '3/4', '1/4', '0.4'], ['1', '1/3', '2/3', '0.6']])
problem(4, '二元信道的熵与互信息', ['entropy-point', 'entropy-mutual', 'channel-model'],
    section('第 4 题 · 互信息 · 25 分', r'<p>二元信道输入概率为 \(p(X=0)=0.4\)、\(p(X=1)=0.6\)。原题信道图的四条转移概率整理如下；行是输入 X，列是输出 Y：</p>' + channel_table +
        r'''<ol><li>求输出熵 \(H(Y)\)。</li><li>求 \(H(Y\mid X)\)、\(H(X\mid Y)\)、\(H(X,Y)\)。</li><li>收到 Y=0 对发送 X=1 提供的点互信息 \(i(X=1;Y=0)\)。</li><li>收到 Y=0 时，该消息提供的关于 X 的平均互信息量 \(I(X;Y=0)\)。提示：按后验概率对两个点互信息加权。</li><li>求平均互信息 \(I(X;Y)\)。</li></ol>'''),
    section('1. 从信道图到联合分布', channel_table + r'<p>用 \(p(x,y)=p(x)p(y\mid x)\) 得到：</p>' +
        table(['X / Y', 'Y=0', 'Y=1', 'P(X)'], [['X=0', '0.3', '0.1', '0.4'], ['X=1', '0.2', '0.4', '0.6'], ['P(Y)', '0.5', '0.5', '1']])) +
    section('2. 输出熵', r'<p>\(H(Y)=-0.5\log_2 0.5-0.5\log_2 0.5=1.0000\) bit。</p>') +
    section('3. 条件熵与联合熵', r'''<p>记 \(H_2(p)=-p\log_2p-(1-p)\log_2(1-p)\)。</p><p>\(H(Y\mid X)=0.4H_2(3/4)+0.6H_2(1/3)=0.8755\) bit。</p><p>\(H(X)=H_2(0.4)=0.9710\) bit。</p><p>\(H(X,Y)=H(X)+H(Y\mid X)=1.8464\) bit。</p><p>\(H(X\mid Y)=H(X,Y)-H(Y)=0.8464\) bit。</p><p>这些结果均使用未舍入的中间值计算；不能把显示的四位小数再相加作为高精度结果。</p>''') +
    section('4. 点互信息', r'''<p>\(p(X=0\mid Y=0)=0.3/0.5=0.6\)，\(p(X=1\mid Y=0)=0.2/0.5=0.4\)。</p><p>\(i(1;0)=\log_2\frac{p(1\mid0)}{p(1)}=\log_2(0.4/0.6)=-0.5850\) bit。</p><p>Y=0 使 X=1 比原来更不可能，所以此点互信息为负。不要把它与恒非负的总体平均互信息混淆。</p>''') +
    section('5. 已知单个输出的平均互信息', r'''<p>\(i(0;0)=\log_2(0.6/0.4)=0.5850\) bit。</p><p>按后验分布加权：</p><p>\(I(X;Y=0)=0.6i(0;0)+0.4i(1;0)=0.1170\) bit。</p><p>这是后验分布与先验分布之间的 KL 散度；本题不能改用先验权重 0.4、0.6。</p>''') +
    section('6. 总体平均互信息', r'''<p>\(I(X;Y)=H(Y)-H(Y\mid X)=0.1245\) bit。</p><p>另一路自检：\(I(X;Y)=H(X)-H(X\mid Y)=0.1245\) bit，结果相同。</p><p>注意：只知道 Y=0 的条件平均 0.1170 bit 与遍历全部输出的总体平均 0.1245 bit 是不同的量。</p>'''), ['c4'])

assert len(problems) == 4 and sum(p['points'] for p in problems) == 100
sources = []
for name, role in [('作业一.docx', '原始题目'), ('第一章作业详解.docx', '分步解析')]:
    path = APP.parent / '作业' / name
    sources.append(dict(name=name, role=role, sha256=hashlib.sha256(path.read_bytes()).hexdigest()))
data = dict(courseVersion=2, title='作业一 · 熵和互信息', chapterNote='原文件称“第一章作业”，按本 App 的知识点顺序归入第二章“信息量与熵”；第 4 题同时关联第四章“离散信道”。',
            sources=sources, results=results, problems=problems)
target = APP / 'app/src/main/assets/homework.json'
target.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(f'PASS: {len(problems)} homework problems, 100 points; 16 numeric results checked.')
