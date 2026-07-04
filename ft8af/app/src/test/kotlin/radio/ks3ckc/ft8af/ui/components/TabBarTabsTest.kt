package radio.ks3ckc.ft8af.ui.components

import com.google.common.truth.Truth.assertThat
import com.k1af.ft8af.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Guards the tab set after the Map and POTA features were stripped (SSTVAF
 * transformation, PR 2): the bottom bar must offer exactly the four remaining
 * screens, in this order, and each tab's label resource must stay wired to the
 * matching string. Robolectric because the labels are Android string resources.
 */
@RunWith(RobolectricTestRunner::class)
class TabBarTabsTest {

    @Test
    fun `tab set is exactly the four remaining screens in order`() {
        assertThat(FT8AFTab.entries.map { it.name })
            .containsExactly("DECODE", "WATERFALL", "LOG", "SETTINGS")
            .inOrder()
    }

    @Test
    fun `each tab is wired to its own label resource`() {
        assertThat(FT8AFTab.DECODE.labelRes).isEqualTo(R.string.tab_decode)
        assertThat(FT8AFTab.WATERFALL.labelRes).isEqualTo(R.string.tab_waterfall)
        assertThat(FT8AFTab.LOG.labelRes).isEqualTo(R.string.tab_logbook)
        assertThat(FT8AFTab.SETTINGS.labelRes).isEqualTo(R.string.tab_settings)
    }
}
