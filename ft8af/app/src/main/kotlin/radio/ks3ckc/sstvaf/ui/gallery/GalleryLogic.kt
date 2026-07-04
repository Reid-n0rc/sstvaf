package radio.ks3ckc.sstvaf.ui.gallery

import com.k1af.ft8af.R
import radio.ks3ckc.sstvaf.gallery.ImageDirection
import radio.ks3ckc.sstvaf.gallery.SavedImage
import radio.ks3ckc.sstvaf.sstv.SstvMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * Pure decision/formatting logic for [GalleryScreen] and [ImageViewerSheet],
 * extracted per project testing policy (Composables stay thin wrappers; this
 * file carries the unit tests).
 */

/** The gallery's direction filter (the All / Received / Sent chip row). */
internal enum class GalleryFilter { ALL, RX, TX }

/** Which images the active filter shows. */
internal fun filterGalleryImages(
    images: List<SavedImage>,
    filter: GalleryFilter,
): List<SavedImage> = when (filter) {
    GalleryFilter.ALL -> images
    GalleryFilter.RX -> images.filter { it.direction == ImageDirection.RX }
    GalleryFilter.TX -> images.filter { it.direction == ImageDirection.TX }
}

/**
 * Newest first, matching the store's list order (utcMillis desc, id as the
 * tiebreak for two images finishing in the same millisecond).
 */
internal fun sortGalleryImages(images: List<SavedImage>): List<SavedImage> =
    images.sortedWith(
        compareByDescending<SavedImage> { it.utcMillis }.thenByDescending { it.id },
    )

/**
 * Mode display name → short code for the tight grid-cell line ("Scottie 1" →
 * "S1"). The store persists the display name, so map back through [SstvMode];
 * an unknown name (future mode, hand-edited DB) falls through unchanged.
 */
internal fun galleryModeShort(modeName: String): String =
    SstvMode.entries.firstOrNull { it.displayName == modeName }?.shortCode ?: modeName

/** Dial frequency in Hz → bare MHz label with kHz resolution, e.g. "14.230". */
internal fun formatGalleryFreqMhz(freqHz: Long): String =
    String.format(Locale.US, "%.3f", freqHz / 1_000_000.0)

/**
 * When the image landed, relative to [nowMillis]: "just now" under a minute,
 * "37m ago" under an hour, "5h ago" under a day, then the absolute UTC date
 * ("2026-07-04"). A timestamp ahead of the clock (skew) reads "just now".
 */
internal fun formatGalleryDate(utcMillis: Long, nowMillis: Long): String {
    val age = nowMillis - utcMillis
    return when {
        age < 60_000L -> "just now" // includes negative age (clock skew)
        age < 3_600_000L -> "${age / 60_000L}m ago"
        age < 86_400_000L -> "${age / 3_600_000L}h ago"
        else -> {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            fmt.format(Date(utcMillis))
        }
    }
}

/** The one-line metadata under a grid cell: mode short code + age, "S1 · 5h ago". */
internal fun galleryCellMeta(entry: SavedImage, nowMillis: Long): String =
    "${galleryModeShort(entry.mode)} · ${formatGalleryDate(entry.utcMillis, nowMillis)}"

/**
 * Empty-state copy for the active filter. TX images only exist from PR 8, so
 * the Sent filter gets its own line; All and Received both point at receiving.
 */
internal fun galleryEmptyStateRes(filter: GalleryFilter): Int = when (filter) {
    GalleryFilter.TX -> R.string.gallery_empty_tx
    else -> R.string.gallery_empty_rx
}

// ---------------------------------------------------------------------------
// Viewer-sheet metadata formatting
// ---------------------------------------------------------------------------

/** Full UTC timestamp for the viewer, e.g. "2026-07-04 15:30:12 UTC". */
internal fun formatViewerUtc(utcMillis: Long): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return "${fmt.format(Date(utcMillis))} UTC"
}

/** Image dimensions, e.g. "320 × 256". */
internal fun formatViewerDimensions(width: Int, height: Int): String = "$width × $height"

/** Engine quality (0..1) as a whole percent, clamped, e.g. "87%". */
internal fun formatViewerQuality(quality: Float): String =
    "${(quality.coerceIn(0f, 1f) * 100f).roundToInt()}%"

/** Frequency line for the viewer, e.g. "14.230 MHz". */
internal fun formatViewerFrequency(freqHz: Long): String =
    "${formatGalleryFreqMhz(freqHz)} MHz"

/** Direction → viewer label resource (Received / Sent). */
internal fun viewerDirectionRes(direction: ImageDirection): Int = when (direction) {
    ImageDirection.RX -> R.string.gallery_meta_direction_rx
    ImageDirection.TX -> R.string.gallery_meta_direction_tx
}

/** Completeness → viewer label resource (Complete / Partial). */
internal fun viewerCompletenessRes(complete: Boolean): Int =
    if (complete) R.string.gallery_meta_complete else R.string.gallery_meta_partial
