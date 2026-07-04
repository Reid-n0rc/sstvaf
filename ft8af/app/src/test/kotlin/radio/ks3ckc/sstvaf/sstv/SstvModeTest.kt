package radio.ks3ckc.sstvaf.sstv

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pins the Kotlin mode table against the C codec's (cpp/sstv_lib/sstv.h +
 * sstv_modes.c). The mode ids cross JNI verbatim, so any drift between this
 * enum and the C `SSTV_MODE_*` order silently decodes/encodes the WRONG mode
 * — this test turns that into a build failure. The expected values below are
 * hardcoded on purpose (do not derive them from SstvMode).
 */
class SstvModeTest {

    @Test
    fun modeIdsPinnedToNativeEnumOrder() {
        // Mirror of the SSTV_MODE_* enum in cpp/sstv_lib/sstv.h.
        val expected = mapOf(
            SstvMode.ROBOT_36 to 0,
            SstvMode.ROBOT_72 to 1,
            SstvMode.MARTIN_1 to 2,
            SstvMode.MARTIN_2 to 3,
            SstvMode.SCOTTIE_1 to 4,
            SstvMode.SCOTTIE_2 to 5,
            SstvMode.PD_50 to 6,
            SstvMode.PD_90 to 7,
            SstvMode.PD_120 to 8,
        )
        assertThat(expected).hasSize(SstvMode.entries.size)
        for ((mode, id) in expected) {
            assertThat(mode.modeId).isEqualTo(id)
        }
    }

    @Test
    fun decodeStatusValuesPinnedToNativeEnum() {
        // Mirror of SSTV_STATUS_* in cpp/sstv_lib/sstv.h.
        assertThat(DecodeStatus.IDLE.nativeValue).isEqualTo(0)
        assertThat(DecodeStatus.LEADER.nativeValue).isEqualTo(1)
        assertThat(DecodeStatus.VIS.nativeValue).isEqualTo(2)
        assertThat(DecodeStatus.IMAGE.nativeValue).isEqualTo(3)
        assertThat(DecodeStatus.DONE.nativeValue).isEqualTo(4)
        assertThat(DecodeStatus.ABORTED.nativeValue).isEqualTo(5)
        assertThat(DecodeStatus.fromNative(3)).isEqualTo(DecodeStatus.IMAGE)
        assertThat(DecodeStatus.fromNative(99)).isEqualTo(DecodeStatus.IDLE)
    }

    @Test
    fun dimensionsMatchModeTable() {
        assertThat(SstvMode.ROBOT_36.width to SstvMode.ROBOT_36.height).isEqualTo(320 to 240)
        assertThat(SstvMode.ROBOT_72.width to SstvMode.ROBOT_72.height).isEqualTo(320 to 240)
        assertThat(SstvMode.MARTIN_1.width to SstvMode.MARTIN_1.height).isEqualTo(320 to 256)
        assertThat(SstvMode.MARTIN_2.width to SstvMode.MARTIN_2.height).isEqualTo(320 to 256)
        assertThat(SstvMode.SCOTTIE_1.width to SstvMode.SCOTTIE_1.height).isEqualTo(320 to 256)
        assertThat(SstvMode.SCOTTIE_2.width to SstvMode.SCOTTIE_2.height).isEqualTo(320 to 256)
        assertThat(SstvMode.PD_50.width to SstvMode.PD_50.height).isEqualTo(320 to 256)
        assertThat(SstvMode.PD_90.width to SstvMode.PD_90.height).isEqualTo(320 to 256)
        assertThat(SstvMode.PD_120.width to SstvMode.PD_120.height).isEqualTo(640 to 496)
    }

    @Test
    fun visCodesMatchModeTable() {
        assertThat(SstvMode.ROBOT_36.visCode).isEqualTo(8)
        assertThat(SstvMode.ROBOT_72.visCode).isEqualTo(12)
        assertThat(SstvMode.MARTIN_1.visCode).isEqualTo(44)
        assertThat(SstvMode.MARTIN_2.visCode).isEqualTo(40)
        assertThat(SstvMode.SCOTTIE_1.visCode).isEqualTo(60)
        assertThat(SstvMode.SCOTTIE_2.visCode).isEqualTo(56)
        assertThat(SstvMode.PD_50.visCode).isEqualTo(93)
        assertThat(SstvMode.PD_90.visCode).isEqualTo(99)
        assertThat(SstvMode.PD_120.visCode).isEqualTo(95)
    }

    @Test
    fun visCodesAreUnique() {
        assertThat(SstvMode.entries.map { it.visCode }.toSet())
            .hasSize(SstvMode.entries.size)
    }

    @Test
    fun shortCodesMatchAndAreUnique() {
        val expected = mapOf(
            SstvMode.ROBOT_36 to "R36",
            SstvMode.ROBOT_72 to "R72",
            SstvMode.MARTIN_1 to "M1",
            SstvMode.MARTIN_2 to "M2",
            SstvMode.SCOTTIE_1 to "S1",
            SstvMode.SCOTTIE_2 to "S2",
            SstvMode.PD_50 to "PD50",
            SstvMode.PD_90 to "PD90",
            SstvMode.PD_120 to "PD120",
        )
        for ((mode, code) in expected) {
            assertThat(mode.shortCode).isEqualTo(code)
        }
        assertThat(SstvMode.entries.map { it.shortCode }.toSet())
            .hasSize(SstvMode.entries.size)
    }

    @Test
    fun txDurationsMatchModeTable() {
        // 0.91 s calibration header + the image duration from the C mode
        // table (sstv_mode_image_us). Tolerance is one scan line.
        val expectedSeconds = mapOf(
            SstvMode.ROBOT_36 to 36.91,
            SstvMode.ROBOT_72 to 72.91,
            SstvMode.MARTIN_1 to 115.200176,
            SstvMode.MARTIN_2 to 58.970288,
            SstvMode.SCOTTIE_1 to 110.54332,
            SstvMode.SCOTTIE_2 to 72.008152,
            SstvMode.PD_50 to 50.59448,
            SstvMode.PD_90 to 90.89912,
            SstvMode.PD_120 to 127.01304,
        )
        for ((mode, seconds) in expectedSeconds) {
            assertThat(mode.txDurationSeconds).isWithin(0.001).of(seconds)
        }
    }

    @Test
    fun totalRowsEqualsHeight() {
        for (mode in SstvMode.entries) {
            assertThat(mode.totalRows).isEqualTo(mode.height)
        }
    }

    @Test
    fun fromModeIdRoundTripsAndRejectsUnknown() {
        for (mode in SstvMode.entries) {
            assertThat(SstvMode.fromModeId(mode.modeId)).isEqualTo(mode)
        }
        assertThat(SstvMode.fromModeId(-1)).isNull()
        assertThat(SstvMode.fromModeId(9)).isNull()
    }
}
