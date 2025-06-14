package com.qba.feedflow.network

import android.util.Log
import com.qba.feedflow.data.RssItem
import com.qba.feedflow.data.makeRssItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.Date

// Data class to represent an RSS item


data class ProxyData(
    val url: String,
    val port: Int
)

enum class Params{
    limit, filter_time
}

fun buildClient(proxy_data: ProxyData ?= null): OkHttpClient {
    val clientBuilder = OkHttpClient.Builder()

    if (proxy_data != null) {
        val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(proxy_data.url, proxy_data.port))
        clientBuilder.proxy(proxy)
        return clientBuilder.build()
    }
    else {
        return clientBuilder.build()
    }
}

// Functional approach to parse RSS feed using OkHttp and coroutines
suspend fun fetchAndParseRss(
    url: String,
    params: Map<Params, String>? = null,
    client: OkHttpClient = buildClient()
): Result<List<RssItem>> = withContext(Dispatchers.IO) {
    try {
        val fullUrl = buildUrlWithParams(url, params)
        val request = Request.Builder().url(fullUrl).build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Unexpected response: ${response.code}")
        val xmlString = response.body?.string() ?: throw IOException("Empty body")

        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(xmlString.byteInputStream())
            .apply { normalize() }

        val root = document.documentElement
        val isAtom = root.nodeName.equals("feed", ignoreCase = true)

        val channelTitle = when {
            isAtom -> root.getElementsByTagName("title").item(0)?.textContent ?: ""
            else -> document.getElementsByTagName("title").item(0)?.textContent ?: ""
        }

        val items = when {
            isAtom -> document.getElementsByTagName("entry")
            else -> document.getElementsByTagName("item")
        }

        val rssItems = (0 until items.length).mapNotNull { i ->
            val element = items.item(i) as? Element ?: return@mapNotNull null
            if (isAtom) {
                makeRssItem(
                    title = element.getElementsByTagName("title").item(0)?.textContent ?: "",
                    link = element.getElementsByTagName("link").item(0)?.attributes?.getNamedItem("href")?.nodeValue ?: "",
                    description = element.getElementsByTagName("content")?.item(0)?.textContent
                        ?: element.getElementsByTagName("summary")?.item(0)?.textContent
                        ?: "",
                    pubDate = element.getElementsByTagName("updated")?.item(0)?.textContent,
                    channel = channelTitle
                )
            } else {
                makeRssItem(
                    title = element.getElementsByTagName("title").item(0)?.textContent ?: "",
                    link = element.getElementsByTagName("link").item(0)?.textContent ?: "",
                    description = element.getElementsByTagName("description")?.item(0)?.textContent ?: "",
                    pubDate = element.getElementsByTagName("pubDate")?.item(0)?.textContent,
                    channel = channelTitle
                )
            }
        }

        Result.success(rssItems)
    } catch (e: Exception) {
        Log.e("fetchAndParseRss", "Error parsing feed: ${e.message}")
        Result.failure(e)
    }
}

private fun buildUrlWithParams(base: String, params: Map<Params, String>?): String {
    if (params.isNullOrEmpty()) return base
    return base + "?" + params.entries.joinToString("&") { "${it.key.name}=${it.value}" }
}


// Example usage
suspend fun main() {
    val rssUrl = "https://rsshub.app/huggingface/daily-papers" // Replace with actual RSS feed URL
    val proxy_data = ProxyData("127.0.0.1", 7890)
    val c = buildClient(proxy_data)
    val result = fetchAndParseRss(url = rssUrl, params = mapOf(Params.limit to "100"), client = c)
    result.fold(
        onSuccess = { items ->
            items.forEach { item ->
                println("Channel: ${item.channel}")
                println("Title: ${item.title}")
                println("Link: ${item.link}")
                println("Description: ${item.description}")
                println("PubDate: ${Date(item.pubDate.toLong())}")
                println("---")
            }
        },
        onFailure = { error ->
            println("Error fetching RSS: ${error.message}")
        }
    )
}