# 三科学习室

中文、单用户、离线 Android App。首页提供信息论、通信原理、数字信号处理三个入口。Kotlin + Jetpack Compose + Room，Android 8.0（API 26）及以上。

- 项目：https://github.com/otae4/information-theory-app
- 当前源码版本：https://github.com/otae4/information-theory-app/tree/v0.3.0
- 0.3.0 完整 APK 已在本机生成，尚未公开上传；当前公开安装包仍为 0.2.0：https://github.com/otae4/information-theory-app/releases/tag/v0.2.0
- 安装包：`information-theory-0.3.0-debug.apk`，约 366 MB，包含全部阅读资料。

## 课程和资料范围

| 学科 | 学习单元 | 配套练习 | 独立作业 | 阅读文件 | 页数 |
|---|---:|---:|---:|---:|---:|
| 信息论 | 49 | 98 | 4 | 6 | 235 |
| 通信原理 | 11 | 22 | 7 | 8 | 145 |
| 数字信号处理 | 23 | 46 | 8 | 67 | 1989 |

通信原理按本机现有第 1、3 章课件、两章笔记、两套作业及详解组织，不把缺失章节标为已经收录。数字信号处理包含课程导读至第 9 章的 62 份课件、两本完整教材、两份笔记和第一章作业详解。已有 PDF 的 Word 文件不重复收录，隐藏工作目录和生成脚本属于中间文件。

学习区提供核心概念、条件、例题和易错点，日常练习与原课程作业分别管理。复杂图表、全部作业小问和长推导保留在完整资料中，可直接跳到对应题目或详解页。新增资料是章节级关联，不代表逐式讲解或全书专业审校。

## 使用和升级

首页选科后，进入各自的学习、练习、作业、复习、资料。点击左上角“学科”或在学科首页按系统返回键重新选科。资料可搜索名称、前后翻页、跳页、双指缩放和拖动。

三科的课程进度、答题、错题和作业完成记录分别保存。备份统一包含全部学科；恢复前提示替换，兼容旧信息论备份。Room 数据库仍为版本 1，原信息论 ID 保持不变，新增两科使用独立前缀。

0.3.0 的应用名为“三科学习室”，包名仍是 `cn.selfstudy.information`，使用本机原签名，可以直接覆盖安装。不要先卸载旧版；重要数据建议先导出备份。此包仍是个人测试用调试 APK，新增内容尚未完成真实手机验证。

## 构建

Android Studio 打开本目录，使用 JDK 17、Android SDK 35、Build Tools 35.0.0：

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

本机已准备工具和缓存，可运行 `tools/build_local.ps1`。脚本使用独立 Gradle 进程；遇到中文路径时建立临时英文 Junction 别名，不移动原目录。Robolectric 的锁文件位于 `app/build`，复用本机 Maven 缓存。

首次依赖解析需要网络，App 使用不依赖网络。未申请 INTERNET 或外部存储权限；备份使用系统文件选择器。公式使用本地 MathJax 3.2.2。

## 内容维护

- `tools/build_course.py`：信息论课程及题库。
- `tools/build_homework.py`：信息论独立作业。
- `tools/export_subject_materials.ps1`：只读导出两科资料，复制 PDF，生成 PPT/Word 阅读副本，不改写原件。
- `tools/build_subjects.py`：核对阅读副本页数，生成通信原理、数字信号处理数据。
- `tools/check_subjects.py`：检查全部页数、引用及 ID 隔离，核对课件页数与原始 PDF 摘要。
- `tools/check_render.mjs`：检查全部课程与作业的离线公式和手机宽度排版。

本机原始资料分别位于桌面的信息论、通信原理和数字信号处理文件夹。两科数据位于 `app/src/main/assets/subjects/comm` 和 `subjects/dsp`；阅读文件位于 `app/src/main/assets/documents`。

公开源码不单独包含 PDF/PPT/Word、缓存、签名密钥或本机 SDK 路径。克隆后仍可构建学习、练习、作业和复习功能；缺少 PDF 时阅读器会提示无法读取资料。自行构建完整阅读版需要在本机补齐本地阅读资产。发布 APK 按项目所有者要求保留内嵌资料。

本机原签名位于项目父目录 `.tools/android-user/debug.keystore`，请保留该文件，不要上传 GitHub。新电脑产生的默认签名不能覆盖当前手机上已安装的版本。

## 许可与验证边界

第三方说明见 `app/src/main/assets/NOTICE.txt` 及许可证目录。教材、课件和原始作业的版权独立于软件；仓库公开不构成对材料的再授权。核心讲解和计算仍应结合原页核对，全部公式及专业正确性尚未经教师逐式审校。

实际检查及真机验证边界见 `VERIFICATION.md`。不把 JVM/Robolectric 或 Chromium 检查描述为真实设备验证。
