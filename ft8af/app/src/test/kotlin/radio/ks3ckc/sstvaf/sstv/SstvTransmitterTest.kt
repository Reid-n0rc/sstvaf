package radio.ks3ckc.sstvaf.sstv

import android.os.Looper
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Exercises [SstvTransmitter] with a fake codec/keyer/player and a
 * synchronous worker runner, so the whole keyDown → play → keyUp sequence
 * runs deterministically on the test thread. Robolectric only for LiveData.
 */
@RunWith(RobolectricTestRunner::class)
class SstvTransmitterTest {

    private val codec = FakeSstvCodec()
    private val events = mutableListOf<String>()
    private val logs = mutableListOf<String>()

    private val keyer = object : SstvTransmitter.Keyer {
        override fun keyDown() {
            events += "keyDown"
        }

        override fun keyUp() {
            events += "keyUp"
        }
    }

    private class FakePlayer(
        private val events: MutableList<String>,
        var playResult: Boolean = true,
        var onPlay: (() -> Unit)? = null,
    ) : SstvTransmitter.Player {
        var cancelCalls = 0

        override fun play(buffer: FloatArray, sampleRate: Int): Boolean {
            events += "play(samples=${buffer.size}, rate=$sampleRate)"
            onPlay?.invoke()
            return playResult
        }

        override fun cancel() {
            cancelCalls++
            events += "cancel"
        }
    }

    private fun newTransmitter(
        player: FakePlayer,
        tuneActive: Boolean = false,
    ) = SstvTransmitter(
        codec,
        keyer,
        player,
        { tuneActive },
        { 12000 },
        { 0L }, // no PTT settle sleep in tests
        { logs += it },
        { 42L },
        { body -> body.run() }, // synchronous worker
    )

    private val pixels = IntArray(SstvMode.ROBOT_36.width * SstvMode.ROBOT_36.height)

    private fun transmitRobot36(tx: SstvTransmitter): Boolean =
        tx.transmit(pixels, SstvMode.ROBOT_36.width, SstvMode.ROBOT_36.height, SstvMode.ROBOT_36)

    @Test
    fun ordersKeyDownPlayKeyUp() {
        codec.encodeSampleCount = 24000
        val player = FakePlayer(events)
        val tx = newTransmitter(player)

        assertThat(transmitRobot36(tx)).isTrue()

        assertThat(events)
            .containsExactly("keyDown", "play(samples=24000, rate=12000)", "keyUp")
            .inOrder()
        assertThat(codec.encodeCalls).containsExactly(
            FakeSstvCodec.EncodeCall(320, 240, SstvMode.ROBOT_36, 12000),
        )
        assertThat(tx.isTransmittingNow()).isFalse()
        assertThat(logs.any { it.contains("SSTV TX: start") }).isTrue()
        assertThat(logs.any { it.contains("SSTV TX: end") && it.contains("completed=true") })
            .isTrue()
    }

    @Test
    fun progressReachesOneOnCompletion() {
        val player = FakePlayer(events)
        val tx = newTransmitter(player)
        transmitRobot36(tx)

        shadowOf(Looper.getMainLooper()).idle()
        assertThat(tx.txProgress.value).isEqualTo(1f)
        assertThat(tx.isTransmitting.value).isFalse()
    }

    @Test
    fun cancelMidPlayStillUnkeys() {
        lateinit var tx: SstvTransmitter
        val player = FakePlayer(events, playResult = false)
        player.onPlay = { tx.cancel() } // operator hits stop while audio is playing
        tx = newTransmitter(player)

        transmitRobot36(tx)

        assertThat(player.cancelCalls).isEqualTo(1)
        assertThat(events.first()).isEqualTo("keyDown")
        assertThat(events.last()).isEqualTo("keyUp")
        assertThat(tx.isTransmittingNow()).isFalse()
        shadowOf(Looper.getMainLooper()).idle()
        // A cancelled TX never reports full progress.
        assertThat(tx.txProgress.value).isEqualTo(0f)
    }

    @Test
    fun rejectsConcurrentTransmit() {
        lateinit var tx: SstvTransmitter
        var nestedResult: Boolean? = null
        val player = FakePlayer(events)
        player.onPlay = { nestedResult = transmitRobot36(tx) } // TX while TX active
        tx = newTransmitter(player)

        assertThat(transmitRobot36(tx)).isTrue()

        assertThat(nestedResult).isFalse()
        // Exactly one keying cycle happened.
        assertThat(events.count { it == "keyDown" }).isEqualTo(1)
        assertThat(events.count { it == "keyUp" }).isEqualTo(1)
        assertThat(logs.any { it.contains("already transmitting") }).isTrue()
    }

    @Test
    fun rejectsTransmitWhileTuneActive() {
        val player = FakePlayer(events)
        val tx = newTransmitter(player, tuneActive = true)

        assertThat(transmitRobot36(tx)).isFalse()

        assertThat(events).isEmpty()
        assertThat(codec.encodeCalls).isEmpty()
        assertThat(logs.any { it.contains("tune carrier active") }).isTrue()
    }

    @Test
    fun encodeFailureNeverKeysTheRig() {
        codec.encodeFailure = IllegalStateException("bad image")
        val player = FakePlayer(events)
        val tx = newTransmitter(player)

        transmitRobot36(tx)

        assertThat(events).isEmpty() // no keyDown, no play, no keyUp needed
        assertThat(tx.isTransmittingNow()).isFalse()
        assertThat(logs.any { it.contains("SSTV TX: failed") }).isTrue()
    }

    @Test
    fun playFailureStillUnkeysAndClearsState() {
        val player = FakePlayer(events, playResult = false)
        val tx = newTransmitter(player)

        transmitRobot36(tx)

        assertThat(events.last()).isEqualTo("keyUp")
        assertThat(tx.isTransmittingNow()).isFalse()
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(tx.txProgress.value).isEqualTo(0f)
    }

    @Test
    fun cancelWithoutActiveTransmissionIsANoOp() {
        val player = FakePlayer(events)
        val tx = newTransmitter(player)
        tx.cancel()
        assertThat(player.cancelCalls).isEqualTo(0)
    }

    @Test
    fun progressFractionIsClampedAndMonotonicInputs() {
        assertThat(SstvTransmitter.txProgressFraction(0, 1000)).isEqualTo(0f)
        assertThat(SstvTransmitter.txProgressFraction(500, 1000)).isEqualTo(0.5f)
        assertThat(SstvTransmitter.txProgressFraction(999, 1000)).isWithin(1e-4f).of(0.99f)
        // The ticker never reports full completion — only a finished play does.
        assertThat(SstvTransmitter.txProgressFraction(2000, 1000))
            .isEqualTo(SstvTransmitter.MAX_TICKER_PROGRESS)
        assertThat(SstvTransmitter.txProgressFraction(100, 0)).isEqualTo(0f)
        assertThat(SstvTransmitter.txProgressFraction(-5, 1000)).isEqualTo(0f)
    }
}
