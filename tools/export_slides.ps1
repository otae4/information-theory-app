$ErrorActionPreference = 'Stop'
$workspace = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$output = Join-Path $workspace 'build-content'
New-Item -ItemType Directory -Force -Path $output | Out-Null
$powerpoint = New-Object -ComObject PowerPoint.Application
try {
    $index = @()
    foreach ($file in (Get-ChildItem -LiteralPath (Join-Path $workspace '课件') -Filter '*.ppt' | Sort-Object Name)) {
        $deck = $powerpoint.Presentations.Open($file.FullName, -1, 0, 0)
        try {
            $pdf = Join-Path $output ($file.BaseName + '.pdf')
            $pptx = Join-Path $output ($file.BaseName + '.pptx')
            $deck.SaveCopyAs($pdf, 32)
            $deck.SaveCopyAs($pptx, 24)
            $slides = @()
            foreach ($slide in $deck.Slides) {
                $lines = @()
                foreach ($shape in $slide.Shapes) {
                    if ($shape.HasTextFrame -and $shape.TextFrame.HasText) {
                        $lines += $shape.TextFrame.TextRange.Text
                    }
                }
                $slides += [pscustomobject]@{page=$slide.SlideIndex; text=($lines -join "`n")}
            }
            $index += [pscustomobject]@{file=$file.Name; pages=$slides}
            Write-Output ($file.Name + ': ' + $deck.Slides.Count + ' slides exported')
        } finally { $deck.Close() }
    }
    ConvertTo-Json -InputObject $index -Depth 8 | Set-Content -LiteralPath (Join-Path $output 'slides.json') -Encoding utf8
} finally { $powerpoint.Quit() }
