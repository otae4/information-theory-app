package cn.selfstudy.information

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.math.min

private val documentCopyLock = Any()

@Composable
fun FormulaReader(html: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val zoom = (LocalDensity.current.fontScale * 100).toInt()
    AndroidView(modifier = modifier, factory = {
        val loader = WebViewAssetLoader.Builder().addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context)).build()
        WebView(it).apply {
            setBackgroundColor(Color.rgb(247, 248, 250))
            settings.javaScriptEnabled = true // MathJax only; no native JavaScript bridge.
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.domStorageEnabled = false
            settings.textZoom = zoom
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse =
                    loader.shouldInterceptRequest(request.url) ?: WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(byteArrayOf()))
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = true
            }
        }
    }, update = { web ->
        if (web.tag != html) {
            web.tag = html
            web.loadDataWithBaseURL("https://appassets.androidplatform.net/assets/", readerHtml(html), "text/html", "UTF-8", null)
        }
    }, onRelease = { it.destroy() })
}

internal fun readerHtml(body: String) = """
<!doctype html><html lang="zh-CN"><head><meta name="viewport" content="width=device-width, initial-scale=1">
<meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'none'">
<style>body{margin:0;padding:8px 20px 32px;background:#f7f8fa;color:#20323b;font:17px/1.9 sans-serif;overflow-wrap:break-word}h2{font-size:19px;line-height:1.5;color:#11695f;margin:0 0 12px}p{margin:0}section{background:white;border:1px solid #e3e9e9;border-radius:16px;padding:20px;margin:10px 0 16px}mjx-container{display:inline-block!important;vertical-align:middle;overflow-x:auto;overflow-y:hidden;max-width:100%;padding:4px 0}a{color:#11695f}</style>
<script>window.MathJax={tex:{inlineMath:[['\\(','\\)']],displayMath:[['\\[','\\]']]},svg:{fontCache:'local'},options:{enableMenu:false}};</script>
<script defer src="mathjax/tex-svg-full.js"></script></head><body>$body</body></html>
""".trimIndent()

@Composable
fun PdfReader(document: Document, initialPage: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var page by rememberSaveable(document.id, initialPage) { mutableIntStateOf((initialPage - 1).coerceIn(0, document.pages - 1)) }
    var jump by rememberSaveable(page) { mutableStateOf((page + 1).toString()) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(document.id, page) {
        error = null
        bitmap = null
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val file = File(context.filesDir, "documents/${document.id}.pdf")
                file.parentFile?.mkdirs()
                synchronized(documentCopyLock) { if (!file.exists()) {
                    val temp = File(file.parentFile, "${document.id}.tmp")
                    context.assets.open(document.asset).use { input -> temp.outputStream().use { input.copyTo(it) } }
                    check(temp.renameTo(file)) { "资料文件复制失败" }
                } }
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        renderer.openPage(page).use { pdf ->
                            val width = min(2200, (pdf.width * 2).coerceAtLeast(1200))
                            val height = (width.toFloat() * pdf.height / pdf.width).toInt()
                            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                                eraseColor(Color.WHITE)
                                pdf.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            }
                        }
                    }
                }
            }
        }
        result.onSuccess { bitmap = it }.onFailure { error = it.message ?: "无法读取资料" }
    }
    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = { page-- }, enabled = page > 0) { Text("上一页") }
            Text("${page+1} / ${document.pages}", Modifier.padding(top=16.dp))
            OutlinedButton(onClick = { page++ }, enabled = page < document.pages-1) { Text("下一页") }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = jump, onValueChange = { jump = it.filter(Char::isDigit).take(4) },
                modifier = Modifier.weight(1f), singleLine = true, label = { Text("页码 / ${document.pages}") })
            TextButton(onClick = { jump.toIntOrNull()?.let { page = (it-1).coerceIn(0, document.pages-1) } }) { Text("跳转") }
        }
        Text("PDF 第 ${page+1} 页 · 双指缩放，拖动查看", Modifier.padding(16.dp, 8.dp), style = MaterialTheme.typography.labelMedium)
        val image = bitmap
        if (image != null) {
            AndroidView(modifier = Modifier.fillMaxWidth().weight(1f).clipToBounds(), factory = { ZoomPdfView(it) }, update = { it.setPage(image) })
        } else {
            Box(Modifier.fillMaxSize().padding(24.dp)) {
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error) else CircularProgressIndicator()
            }
        }
    }
}

internal class ZoomPdfView(context: android.content.Context) : View(context) {
    private var bitmap: Bitmap? = null
    private var zoom = 1f
    private var dx = 0f
    private var dy = 0f
    private var lastX = 0f
    private var lastY = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val detector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val old = zoom
            zoom = (zoom * detector.scaleFactor).coerceIn(1f, 5f)
            dx = detector.focusX - (detector.focusX - dx) * zoom / old
            dy = detector.focusY - (detector.focusY - dy) * zoom / old
            clamp(); invalidate(); return true
        }
    })
    init { contentDescription = "PDF 资料页面，可双指缩放和拖动" }
    fun setPage(value: Bitmap) {
        if (value === bitmap) return
        bitmap = value; zoom = 1f; dx = 0f; dy = 0f; invalidate()
    }
    private fun scale() = bitmap?.let { min(width.toFloat()/it.width, height.toFloat()/it.height) * zoom } ?: 1f
    private fun clamp() {
        val b = bitmap ?: return
        val w = b.width * scale(); val h = b.height * scale()
        dx = if (w <= width) (width-w)/2 else dx.coerceIn(width-w, 0f)
        dy = if (h <= height) (height-h)/2 else dy.coerceIn(height-h, 0f)
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // AndroidView does not clip by default; drawColor otherwise covers the page controls.
        val checkpoint = canvas.save()
        canvas.clipRect(0, 0, width, height)
        canvas.drawColor(Color.rgb(227,232,234))
        clamp()
        bitmap?.let { b ->
            canvas.translate(dx,dy); canvas.scale(scale(),scale()); canvas.drawBitmap(b,0f,0f,paint)
        }
        canvas.restoreToCount(checkpoint)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        detector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { lastX=event.x; lastY=event.y; parent.requestDisallowInterceptTouchEvent(true) }
            MotionEvent.ACTION_MOVE -> {
                if (!detector.isInProgress && event.pointerCount==1) { dx+=event.x-lastX; dy+=event.y-lastY; clamp(); invalidate() }
                lastX=event.x; lastY=event.y
            }
            MotionEvent.ACTION_POINTER_UP -> { val remaining = if (event.actionIndex == 0) 1 else 0; lastX=event.getX(remaining); lastY=event.getY(remaining) }
            MotionEvent.ACTION_UP -> { performClick(); parent.requestDisallowInterceptTouchEvent(false) }
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
}
