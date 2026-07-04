package radio.ks3ckc.sstvaf.sstv

/**
 * The nine SSTV modes implemented by the native codec (cpp/sstv_lib).
 *
 * [modeId] MUST match the `SSTV_MODE_*` enum in `cpp/sstv_lib/sstv.h` — the
 * id crosses the JNI boundary verbatim. The rest of the fields mirror the
 * mode table in `cpp/sstv_lib/sstv_modes.c` (dimensions, VIS code, total
 * transmission duration = 0.91 s calibration header + image time).
 * [SstvModeTest] pins every value against hardcoded expectations so a drift
 * between this enum and the C table fails the unit-test build.
 */
enum class SstvMode(
    val modeId: Int,
    val displayName: String,
    val shortCode: String,
    val width: Int,
    val height: Int,
    val visCode: Int,
    val txDurationSeconds: Double,
) {
    ROBOT_36(0, "Robot 36", "R36", 320, 240, 8, 36.91),
    ROBOT_72(1, "Robot 72", "R72", 320, 240, 12, 72.91),
    MARTIN_1(2, "Martin 1", "M1", 320, 256, 44, 115.200176),
    MARTIN_2(3, "Martin 2", "M2", 320, 256, 40, 58.970288),
    SCOTTIE_1(4, "Scottie 1", "S1", 320, 256, 60, 110.54332),
    SCOTTIE_2(5, "Scottie 2", "S2", 320, 256, 56, 72.008152),
    PD_50(6, "PD 50", "PD50", 320, 256, 93, 50.59448),
    PD_90(7, "PD 90", "PD90", 320, 256, 99, 90.89912),
    PD_120(8, "PD 120", "PD120", 640, 496, 95, 127.01304),
    ;

    /** Total scan lines in a full image (== [height]). */
    val totalRows: Int get() = height

    companion object {
        /** Reverse lookup from a native mode id; null for -1/unknown. */
        @JvmStatic
        fun fromModeId(modeId: Int): SstvMode? = entries.firstOrNull { it.modeId == modeId }
    }
}
