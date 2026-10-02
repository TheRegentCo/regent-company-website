package uk.co.regentcompany.mile.nativebeta

import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class LaunchTest {
    @Test
    fun appLaunchesAndLoadsComplianceUi() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val latch = CountDownLatch(1)
            scenario.onActivity { activity ->
                val web = activity.findViewById<WebView>(R.id.webview)
                assertNotNull(web)
                web.postDelayed({
                    web.evaluateJavascript("document.getElementById('compliance-card') !== null") { value ->
                        assertEquals("true", value)
                        latch.countDown()
                    }
                }, 2500)
            }
            check(latch.await(12, TimeUnit.SECONDS)) { "Web UI did not load" }
        }
    }
}
