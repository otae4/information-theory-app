package cn.selfstudy.information

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PdfViewTest {
    @Test fun pageDrawingCannotCoverSurroundingControls() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = ZoomPdfView(context)
        view.layout(0, 0, 200, 100)
        val page = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        page.eraseColor(Color.WHITE)
        view.setPage(page)
        val screen = Bitmap.createBitmap(280, 220, Bitmap.Config.ARGB_8888)
        screen.eraseColor(Color.MAGENTA)
        val canvas = Canvas(screen)
        canvas.translate(40f, 60f)
        view.draw(canvas)

        assertEquals("Navigation above the page must remain visible", Color.MAGENTA, screen.getPixel(140, 30))
        assertEquals("Content below the page must remain visible", Color.MAGENTA, screen.getPixel(140, 190))
        assertEquals(Color.MAGENTA, screen.getPixel(20, 110))
        assertEquals(Color.MAGENTA, screen.getPixel(260, 110))
        assertEquals(Color.WHITE, screen.getPixel(140, 110))
    }
}
