package radio.ks3ckc.sstvaf.sstv

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.k1af.ft8af.GeneralVariables
import java.util.concurrent.atomic.AtomicBoolean

/**
 * SSTV image transmitter: encode → key PTT → play → unkey, on a worker
 * thread, composing the PR-3 extracted transmit plumbing the same way
 * TuneOperator does (PttController keyDown/keyUp around a blocking
 * TransmitAudioSink play, un-key in a finally — a stuck carrier is never
 * acceptable).
 *
 * The controller/sink are injected behind the minimal [Keyer]/[Player]
 * interfaces so JVM tests can fake them; MainViewModel adapts the real
 * PttController + TransmitAudioSink.
 *
 * The sink has no playback-progress callback, so [txProgress] is computed
 * from elapsed time vs the waveform duration by a small ticker thread, and
 * snapped to 1.0 when playback completes.
 */
class SstvTransmitter @JvmOverloads constructor(
    private val codec: SstvCodec,
    private val keyer: Keyer,
    private val player: Player,
    private val tuneActive: TuneActiveCheck = TuneActiveCheck { false },
    private val sampleRateSource: () -> Int = { GeneralVariables.audioSampleRate },
    private val settleDelayMsSource: () -> Long = { GeneralVariables.pttDelay.toLong() },
    private val log: (String) -> Unit = { GeneralVariables.fileLog(it) },
    private val clock: () -> Long = { System.currentTimeMillis() },
    /** Runs the TX worker; tests substitute a synchronous runner. */
    private val workerRunner: (Runnable) -> Unit = { body ->
        Thread(body, "SstvTransmit").start()
    },
) {

    /** Rig keying surface (MainViewModel adapts PttController). */
    interface Keyer {
        fun keyDown()

        fun keyUp()
    }

    /** Audio playback surface (MainViewModel adapts TransmitAudioSink). */
    interface Player {
        /** Blocking whole-buffer play. True when it played to completion. */
        fun play(buffer: FloatArray, sampleRate: Int): Boolean

        /** Abort an in-progress play from any thread (unblocks [play]). */
        fun cancel()
    }

    /** Whether the Tune carrier currently owns the rig. */
    fun interface TuneActiveCheck {
        fun isActive(): Boolean
    }

    private val transmitting = AtomicBoolean(false)
    private val cancelled = AtomicBoolean(false)

    private val mutableIsTransmitting = MutableLiveData(false)
    val isTransmitting: LiveData<Boolean> get() = mutableIsTransmitting

    private val mutableTxProgress = MutableLiveData(0f)
    val txProgress: LiveData<Float> get() = mutableTxProgress

    /**
     * Start transmitting [pixels] ([width] x [height], 0xAARRGGBB) in [mode].
     * Returns false without keying when a transmission is already running or
     * the Tune carrier is active.
     */
    fun transmit(pixels: IntArray, width: Int, height: Int, mode: SstvMode): Boolean {
        if (tuneActive.isActive()) {
            log("SSTV TX: rejected — tune carrier active")
            return false
        }
        if (!transmitting.compareAndSet(false, true)) {
            log("SSTV TX: rejected — already transmitting")
            return false
        }
        cancelled.set(false)
        mutableIsTransmitting.postValue(true)
        mutableTxProgress.postValue(0f)
        workerRunner(Runnable { runTransmission(pixels, width, height, mode) })
        return true
    }

    /** Abort an in-flight transmission; the worker's finally un-keys PTT. */
    fun cancel() {
        if (!transmitting.get()) return
        cancelled.set(true)
        player.cancel()
    }

    fun isTransmittingNow(): Boolean = transmitting.get()

    // ------------------------------------------------------------------

    private fun runTransmission(pixels: IntArray, width: Int, height: Int, mode: SstvMode) {
        var keyed = false
        var completed = false
        val startedAt = clock()
        try {
            val sampleRate = sampleRateSource()
            // Encode BEFORE keying: a bad image/mode must never key the rig.
            val audio = codec.encode(pixels, width, height, mode, sampleRate)
            val durationMs = audio.size * 1000L / sampleRate
            log(
                "SSTV TX: start — mode=${mode.displayName} ${width}x$height" +
                    " samples=${audio.size} rate=$sampleRate durationMs=$durationMs",
            )

            keyer.keyDown()
            keyed = true
            val settleMs = settleDelayMsSource()
            if (settleMs > 0) Thread.sleep(settleMs)

            val ticker = startProgressTicker(durationMs)
            try {
                completed = player.play(audio, sampleRate)
            } finally {
                // Stop-and-join so a late ticker post can never overwrite the
                // final progress value published below.
                ticker.stop()
            }
        } catch (ie: InterruptedException) {
            Thread.currentThread().interrupt()
            log("SSTV TX: interrupted")
        } catch (t: Throwable) {
            log("SSTV TX: failed — ${t.javaClass.simpleName}: ${t.message}")
        } finally {
            // Single point of teardown: whatever happened above, PTT drops.
            if (keyed) {
                try {
                    keyer.keyUp()
                } catch (t: Throwable) {
                    log("SSTV TX: key-up failed — $t")
                }
            }
            mutableTxProgress.postValue(if (completed) 1f else 0f)
            transmitting.set(false)
            mutableIsTransmitting.postValue(false)
            log(
                "SSTV TX: end — completed=$completed cancelled=${cancelled.get()}" +
                    " elapsedMs=${clock() - startedAt}",
            )
        }
    }

    /** Elapsed-time progress publisher. */
    private inner class ProgressTicker(durationMs: Long) {
        private val keepRunning = AtomicBoolean(true)
        private val startedAt = clock()
        private val thread = Thread(
            {
                while (keepRunning.get()) {
                    mutableTxProgress.postValue(
                        txProgressFraction(clock() - startedAt, durationMs),
                    )
                    try {
                        Thread.sleep(PROGRESS_TICK_MS)
                    } catch (ie: InterruptedException) {
                        return@Thread
                    }
                }
            },
            "SstvTxProgress",
        ).apply {
            isDaemon = true
            start()
        }

        /** Deterministic stop: no posts can land after this returns. */
        fun stop() {
            keepRunning.set(false)
            thread.interrupt()
            try {
                thread.join(TICKER_JOIN_MS)
            } catch (ie: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    private fun startProgressTicker(durationMs: Long): ProgressTicker =
        ProgressTicker(durationMs)

    companion object {
        internal const val PROGRESS_TICK_MS = 200L
        internal const val TICKER_JOIN_MS = 1000L

        /**
         * Elapsed/duration as a 0..1 fraction, capped just below 1 — only a
         * completed play posts exactly 1.0. Pure; unit-tested directly.
         */
        @JvmStatic
        internal fun txProgressFraction(elapsedMs: Long, durationMs: Long): Float {
            if (durationMs <= 0) return 0f
            val fraction = elapsedMs.toFloat() / durationMs.toFloat()
            return fraction.coerceIn(0f, MAX_TICKER_PROGRESS)
        }

        internal const val MAX_TICKER_PROGRESS = 0.99f
    }
}
