package com.signlanguage.translator.domain.services

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.signlanguage.translator.utils.Constants
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TFLiteServiceInstrumentedTest {
    @Test
    fun loadsBundledModelAndLabels() {
        val service = TFLiteService(ApplicationProvider.getApplicationContext())
        service.close()
    }

    @Test
    fun bundledLabels_matchModelOutputContract() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val labels = context.assets.open(Constants.LABELS_FILE).bufferedReader().useLines { lines ->
            lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
        }

        assertEquals(Constants.MODEL_OUTPUT_CLASSES, labels.size)
        assertEquals("TV", labels.first())
        assertEquals("zipper", labels.last())
    }
}
