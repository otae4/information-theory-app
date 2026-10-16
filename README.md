# 信息论自习室

中文、单用户、离线 Android 应用。Kotlin + Jetpack Compose + Room，Android 8.0（API 26）及以上。

## GitHub 版本管理

本仓库公开管理应用源码、原创课程讲解与题库、本地公式组件和测试。不单独上传课件、教材 PDF/PPT，公开提交历史也不包含这些文件。安装包通过 GitHub Releases 的版本附件提供；按项目所有者要求，发布现有 APK 原文件，保留其中内嵌的阅读资料。`dist`、构建缓存、本机 SDK 路径和签名密钥不提交。

- 项目与分享链接：https://github.com/otae4/information-theory-app
- 0.2.0 安装包与校验值：https://github.com/otae4/information-theory-app/releases/tag/v0.2.0

当前版本为 `v0.2.0`。后续修改提交到 `main`，发布时增加 `app/build.gradle.kts` 中的 `versionCode` / `versionName`，完成测试后打版本标签并上传 APK。

本机用于已安装版本的调试签名保存在项目父目录 `.tools/android-user/debug.keystore`，请保留该本地文件。新电脑默认会生成不同签名，不能直接覆盖当前手机上的版本；密钥不应上传 GitHub。课件、教材 PDF、原始 PPT 和 OCR 中间文件仍保留在本机。

克隆源码后可以构建 App 并使用课程、练习和复习。若要自行构建包含资料阅读功能的完整 APK，需要先在 `app/src/main/assets/documents/` 放入本机对应的 `c1.pdf` 至 `c5.pdf` 和 `book.pdf`；缺少文件时，阅读器会提示无法读取资料。这些 PDF 已被 `.gitignore` 排除。直接下载 Releases 中的 APK 则已内嵌资料，无需另行放入文件。

## 当前安装包：0.2.0

`dist/information-theory-0.2.0-debug.apk` 新增独立“作业”入口，收录本机“作业一”及其详解中的 4 道大题（共 100 分）。题目、分步解析、公式表格、关联讲解和完成记录均可离线使用。保留 0.1.1 的资料阅读翻页修复。

通过微信文件传输助手发送此 APK 到手机，下载后直接覆盖安装。新旧版本使用相同签名，数据库结构与学习记录格式均未修改；不要先卸载旧版。重要记录可先在“资料”中导出备份。

## 已实现

- 学习：9 个单元、49 节讲解；先修知识、定义与条件、推导说明、完整例题、易错点，记录最近阅读与完成状态。
- 练习：98 道基础题，选择题和数值题自动判分，简答题对照答案自评。数值支持分数和科学计数法；3 类受控参数模板支持变式。
- 作业：独立于练习区的 4 道课程作业，每题 25 分。按内容归入第二章“信息量与熵”；第 4 题同时关联第四章“离散信道”。先自行完成，再展开分步详解与答案核对；完成状态单独记录，不加入随机练习、错题或复习排期。作业不自动评分。
- 复习：错题、到期清单；1/3/7/14/30 天排期，同一天重复作答不连续升级。复习保留原参数变式。
- 资料：5 份 PPT 的完整 PDF 阅读副本（111 页）、完整教材（124 页），双指缩放、拖动、页码跳转。235 页逐页关联到课程。
- 数据：Room 本地持久化；JSON 手动导出、校验后恢复，恢复前提示覆盖，数据库事务保证失败回滚。
- 公式：本地 MathJax 3.2.2 SVG，不依赖 CDN；应用未申请 INTERNET 权限。

## 打开和构建

使用 Android Studio 打开本目录，选择 JDK 17，安装 Android SDK Platform 35、Build Tools 35.0.0。

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

调试包位置：`app/build/outputs/apk/debug/app-debug.apk`。调试签名用于个人测试；后续版本应保留同一签名才能直接覆盖升级并保留数据。

本次工作区已准备独立的 SDK、Gradle 和缓存（位于项目父目录 `.tools`）。可直接运行：

```powershell
.\tools\build_local.ps1
```

此脚本遇到中文工作目录时，会建立指向原目录的临时英文路径别名，规避 Java 17 Windows 测试启动器的参数文件编码问题；不会重命名或移动原目录。

首次依赖解析需要网络，安装完成后的 App 学习不需要网络。`local.properties` 仅用于指定本机 SDK，不应提交。

## 内容与来源

原件仍位于父目录 `课件`，导出副本、逐页 OCR 与视觉检查位于父目录 `build-content`，应用内只打包可读 PDF、课程内容与公式资源。

- `CONTENT_COVERAGE.md`：全部物理页码、知识点与内容类型。
- `tools/build_course.py`：本项目讲解、题目及源页范围的可维护源文件，运行后生成 `app/src/main/assets/course.json`。
- `tools/build_homework.py`：维护独立作业数据，生成 `homework.json`，校验 16 项数值结果，并记录两份本机原始 Word 文档的摘要；原始文档不提交。课程数据版本提升至 2，旧学习备份仍可恢复，新增作业记录也随备份保存。
- `tools/export_slides.ps1`：使用本机 PowerPoint 导出新副本；不覆盖 PPT 原件。
- `tools/ocr_textbook.py`：本地 OCR、逐页缓存，可断点继续；需要 PyMuPDF 与 RapidOCR。
- `tools/check_content.py`：检查页数、覆盖、引用、先修图、题目格式、代表性数值与循环码例题。
- `tools/check_render.mjs`：使用 Playwright 和 Edge，在手机宽度渲染全部课程；阻止外网请求，检查 TeX 错误及页面横向溢出。

更新课程时保持知识点与题目的 ID 稳定；删除或修改 ID 会影响旧备份兼容。数据库结构变更必须增加版本并编写 Room migration，不使用破坏性迁移。

## 初版内容的边界

原资料完整收录，课程按主题提供原创解释和例题，并纠正个别原文中省略条件或容易误解的表述。覆盖清单表示主题对应关系，不意味着已经逐式复述和独立审校原书全部推导，也不意味着已解完教材每道原题。原书长推导和原始习题均可回到对应 PDF 阅读。

OCR 有识别错误，尤其是数学符号和缺损扫描处，因此 OCR 不直接充当公式原文。课程中的主要公式单独用 TeX 编写，并对代表性例题做计算校验；完整教学质量仍建议由学科教师复核。

首版不含实时 AI、账号、服务器、云同步或通知。当天到期清单按设备本地日期计算；卸载前请手动备份，系统云备份已关闭。单个备份限制 8 MiB。

## 开源许可与材料授权

MathJax 与 AndroidX/Kotlin 的许可说明见应用内“关于与开源许可”、`app/src/main/assets/NOTICE.txt` 和 `licenses`。MathJax 完整许可随包保留。

教材和课件的版权独立于软件许可证。当前包以用户已有材料的个人学习为用途，公开上传仓库、发布 APK 或商业使用前需单独核实材料授权。

## 验证记录

本次实际执行结果与尚未验证的项目记于 `VERIFICATION.md`。请勿把“提供了测试代码”理解为该测试已在设备上通过。
