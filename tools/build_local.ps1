param([switch]$DeviceTests)
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
$workspace = Split-Path $project -Parent
# Java 17's Windows argument-file launcher cannot reliably read a Unicode classpath.
if ($workspace -match '[^\x00-\x7F]') {
    $alias = Join-Path $env:TEMP 'information-theory-workspace'
    if (Test-Path -LiteralPath $alias) {
        $existing = Get-Item -LiteralPath $alias -Force
        if ($existing.LinkType -ne 'Junction' -or @($existing.Target)[0] -ne $workspace) {
            throw ('临时路径已被其他目录占用，请先检查：' + $alias)
        }
    } else {
        New-Item -ItemType Junction -Path $alias -Target $workspace | Out-Null
    }
    $workspace = $alias
    $project = Join-Path $workspace 'InformationTheoryApp'
}
$sdk = Join-Path $workspace '.tools\android-sdk'
$gradle = Join-Path $workspace '.tools\gradle-8.11.1\bin\gradle.bat'
if (!(Test-Path -LiteralPath $gradle) -or !(Test-Path -LiteralPath $sdk)) {
    throw '缺少工作区工具，请使用 Android Studio 或标准 gradlew.bat 构建。'
}
$env:ANDROID_HOME = $sdk
$env:ANDROID_USER_HOME = Join-Path $workspace '.tools\android-user'
$env:GRADLE_USER_HOME = Join-Path $workspace '.tools\gradle-home'
if (!$env:JAVA_HOME) {
    $java = Get-Command java -ErrorAction Stop
    $env:JAVA_HOME = Split-Path (Split-Path $java.Source -Parent) -Parent
}
$sdkSetting = 'sdk.dir=' + $sdk.Replace('\','/')
[IO.File]::WriteAllText((Join-Path $project 'local.properties'), $sdkSetting, [Text.UTF8Encoding]::new($false))
$tasks = @(':app:assembleDebug', ':app:testDebugUnitTest', ':app:lintDebug')
if ($DeviceTests) { $tasks += ':app:connectedDebugAndroidTest' }
& $gradle -p $project @tasks --console=plain
if ($LASTEXITCODE -ne 0) { throw ('Gradle failed: ' + $LASTEXITCODE) }
