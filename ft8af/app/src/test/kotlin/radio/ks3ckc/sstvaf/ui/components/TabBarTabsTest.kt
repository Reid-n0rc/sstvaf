package radio.ks3ckc.sstvaf.ui.components

import com.google.common.truth.Truth.assertThat
import com.k1af.ft8af.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Guards the tab set after the FT8 engine was stripped (SSTVAF transformation,
 * PR 3): the bottom bar must offer exactly the three remaining screens —
 * WATERFALL (the default) first — and each tab's label resource must stay
 * wired to the matching string. Robolectric because the labels are Android
 * string resources.
 */
@RunWith(RobolectricTestRunner::class)
class TabBarTabsTest {

    @Test
    fun `tab set is exactly the three remaining screens in order`() {
        assertThat(SstvTab.entries.map { it.name })
            .containsExactly("WATERFALL", "LOG", "SETTINGS")
            .inOrder()
    }

    @Test
    fun `waterfall is the first (default) tab`() {
        assertThat(SstvTab.entries.first()).isEqualTo(SstvTab.WATERFALL)
    }

    @Test
    fun `each tab is wired to its own label resource`() {
        assertThat(SstvTab.WATERFALL.labelRes).isEqualTo(R.string.tab_waterfall)
        assertThat(SstvTab.LOG.labelRes).isEqualTo(R.string.tab_logbook)
        assertThat(SstvTab.SETTINGS.labelRes).isEqualTo(R.string.tab_settings)
    }
}
