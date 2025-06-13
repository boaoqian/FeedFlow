package com.qba.feedflow

import android.util.Log
import androidx.room.util.query
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.qba.feedflow.network.ProxyData
import com.qba.feedflow.network.buildClient
import com.qba.feedflow.network.fetchAndParseRss
import kotlinx.coroutines.runBlocking
import  com.qba.feedflow.data.RssDatabaseManager
import com.qba.feedflow.data.RssDatabaseManager.updateDB
import org.junit.Assert

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun testDB() {
        runBlocking {
            // Context of the app under test.
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            assertEquals("com.qba.feedflow", appContext.packageName)
            val db = RssDatabaseManager.initDb(appContext)
//        val proxy_data = ProxyData("http://127.0.0.1", 7890)
            val client = buildClient()
            val result = fetchAndParseRss("https://sspai.com/feed", client = client)
            result.fold(
                onSuccess = { items ->
                    items.forEach { item ->
                        Log.d("RSS", "Title: ${item.title}, Link: ${item.link}")
                    }
                },
                onFailure = { error ->
                    Log.e("RSS", "Error: ${error.message}")
                    Assert.fail("Failed to fetch RSS: ${error.message}")
                }
            )
            val newslist = result.getOrNull() ?: emptyList()
            updateDB(newslist)
            val items = RssDatabaseManager.loadData(10)
            items.forEach { item ->
                Log.d("RSS", "db:Title: ${item.title}, Link: ${item.link}")
            }
        }
    }
}