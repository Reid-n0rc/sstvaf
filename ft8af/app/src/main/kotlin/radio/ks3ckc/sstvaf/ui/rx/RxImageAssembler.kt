package radio.ks3ckc.sstvaf.ui.rx

import android.graphics.Bitmap

/**
 * Assembles the in-progress RX image: owns a mutable ARGB_8888 [Bitmap] sized
 * on VIS lock from the mode dimensions and paints decoded rows into it as they
 * arrive. NOT a composable — [RxScreen] feeds it from `rxState` transitions and
 * renders [snapshotBitmap] copies, so Compose never observes the mutable
 * bitmap directly.
 *
 * Not thread-safe by design: all calls happen on the UI thread (LiveData
 * delivery).
 */
class RxImageAssembler {

    var width: Int = 0
        private set
    var height: Int = 0
        private set

    private var bitmap: Bitmap? = null

    /**
     * Size (or re-size) the canvas for a newly locked mode. Returns true when
     * a fresh bitmap was created (caller must restart its row bookkeeping);
     * false when the existing canvas already matches.
     */
    fun ensureSize(width: Int, height: Int): Boolean {
        require(width > 0 && height > 0) { "bad image size ${width}x$height" }
        if (bitmap != null && this.width == width && this.height == height) return false
        this.width = width
        this.height = height
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(0xFF000000.toInt()) // undecoded rows render black
        }
        return true
    }

    /**
     * Paint [nRows] decoded rows starting at [firstRow]. [pixels] is row-major
     * 0xAARRGGBB, at least `nRows * width` long. Rows outside the canvas are
     * clipped; a call before [ensureSize] is a no-op.
     */
    fun applyRows(firstRow: Int, nRows: Int, pixels: IntArray) {
        val b = bitmap ?: return
        if (firstRow < 0 || firstRow >= height) return
        val rows = nRows.coerceAtMost(height - firstRow).coerceAtMost(pixels.size / width)
        if (rows <= 0) return
        b.setPixels(pixels, 0, width, 0, firstRow, width, rows)
    }

    /** Immutable copy for Compose; later [applyRows] calls don't affect it. */
    fun snapshotBitmap(): Bitmap? = bitmap?.copy(Bitmap.Config.ARGB_8888, false)

    /** Drop the canvas (back to hunting). */
    fun reset() {
        bitmap = null
        width = 0
        height = 0
    }
}
