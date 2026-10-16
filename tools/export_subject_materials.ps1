$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
$workspace = Split-Path $project -Parent
$desktop = Split-Path $workspace -Parent
$assets = Join-Path $project 'app\src\main\assets'
$output = Join-Path $workspace 'build-content\subjects'
New-Item -ItemType Directory -Force -Path $output | Out-Null
$manifest = [System.Collections.Generic.List[object]]::new()
$ppt = $null
$word = $null
try {
    foreach ($subject in @(@{id='comm';name='通信原理'},@{id='dsp';name='数字信号处理'})) {
        $root = Join-Path $desktop $subject.name
        $folder = Join-Path $assets ('documents\'+$subject.id)
        New-Item -ItemType Directory -Force -Path $folder | Out-Null
        $files = @(Get-ChildItem -LiteralPath $root -Recurse -File | Where-Object {
            $_.Extension -in '.pdf','.pptx','.docx' -and $_.FullName.Substring($root.Length) -notmatch '\\[.]'
        } | Sort-Object FullName)
        $counter = 0
        foreach ($file in $files) {
            # Prefer an existing PDF counterpart over converting the same Word document twice.
            if ($file.Extension -eq '.docx' -and (Test-Path -LiteralPath ([IO.Path]::ChangeExtension($file.FullName,'.pdf')))) { continue }
            $counter++
            $id = $subject.id+'-doc-'+$counter.ToString('000')
            $target = Join-Path $folder ($id+'.pdf')
            $pages = 0
            if ($file.Extension -eq '.pdf') {
                if (!(Test-Path -LiteralPath $target) -or (Get-Item -LiteralPath $target).Length -ne $file.Length) { Copy-Item -LiteralPath $file.FullName -Destination $target }
            } elseif ($file.Extension -eq '.pptx') {
                if (!(Test-Path -LiteralPath $target)) {
                    if ($null -eq $ppt) { $ppt = New-Object -ComObject PowerPoint.Application }
                    $deck = $ppt.Presentations.Open($file.FullName,-1,0,0)
                    try { $pages=$deck.Slides.Count; $deck.SaveCopyAs($target,32) } finally { $deck.Close() }
                }
            } else {
                if (!(Test-Path -LiteralPath $target)) {
                    if ($null -eq $word) { $word=New-Object -ComObject Word.Application; $word.Visible=$false; $word.DisplayAlerts=0 }
                    $document=$word.Documents.Open($file.FullName,$false,$true,$false)
                    try { $document.ExportAsFixedFormat($target,17); $pages=$document.ComputeStatistics(2) } finally { $document.Close(0) }
                }
            }
            $relative=$file.FullName.Substring($root.Length+1)
            $manifest.Add([pscustomobject]@{subject=$subject.id;id=$id;source=$relative;sourceBytes=$file.Length;asset=('documents/'+$subject.id+'/'+$id+'.pdf');pages=$pages})
            Write-Output ($subject.name+' '+$counter+': '+$relative+' -> '+[math]::Round((Get-Item -LiteralPath $target).Length/1MB,2)+' MiB')
        }
    }
    $manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $output 'materials-manifest.json') -Encoding UTF8
} finally {
    if ($null -ne $ppt) { $ppt.Quit() }
    if ($null -ne $word) { $word.Quit() }
}
