package com.signlanguage.translator.ui.activities

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.signlanguage.translator.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @Test
    fun demoPipeline_displaysTranslationControls() {
        launchDemoActivity().use {
            onView(withText(R.string.recognized_words_title)).check(matches(isDisplayed()))
            onView(withId(R.id.playTtsButton)).check(matches(isDisplayed()))
            onView(withId(R.id.copyTranslationButton)).check(matches(isDisplayed()))
            onView(withId(R.id.refreshTranslationButton)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun settingsButton_opensSettingsScreen() {
        launchDemoActivity().use {
            onView(withId(R.id.settingsButton)).perform(click())

            onView(withText(R.string.camera_selection)).check(matches(isDisplayed()))
            onView(withId(R.id.confidenceSeekBar)).check(matches(isDisplayed()))
        }
    }

    private fun launchDemoActivity(): ActivityScenario<MainActivity> {
        val intent = Intent(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext,
            MainActivity::class.java
        ).putExtra(MainActivity.EXTRA_DEMO_PIPELINE, true)
        return ActivityScenario.launch(intent)
    }
}
