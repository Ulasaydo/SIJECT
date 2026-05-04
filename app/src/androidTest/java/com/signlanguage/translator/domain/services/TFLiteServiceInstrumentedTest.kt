package com.signlanguage.translator.domain.services

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TFLiteServiceInstrumentedTest {
    @Test
    fun loadsBundledModelAndLabels() {
        val service = TFLiteService(ApplicationProvider.getApplicationContext())
        service.close()
    }
}
