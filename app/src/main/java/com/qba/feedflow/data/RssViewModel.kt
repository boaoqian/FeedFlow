package com.qba.feedflow.data

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Database
import com.qba.feedflow.data.RssDatabaseManager.loadData
import com.qba.feedflow.network.fetchAndParseRss
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.qba.feedflow.data.RssDatabaseManager.addChannel
import com.qba.feedflow.data.RssDatabaseManager.getChannels
import com.qba.feedflow.data.RssDatabaseManager.likeItem
import com.qba.feedflow.data.RssDatabaseManager.loadLikedItems
import com.qba.feedflow.data.RssDatabaseManager.unlikeItem
import com.qba.feedflow.data.RssDatabaseManager.updateDB
import com.qba.feedflow.data.RssDatabaseManager.deleteChannel as deleteChannel_db
import com.qba.feedflow.network.ProxyData
import com.qba.feedflow.network.buildClient
import okhttp3.OkHttpClient
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

val ALL_CHANNELS = "全部文章"
val Liked_ITEMS = "收藏"

data class uiState(
    val nowItems: List<RssItem> = emptyList(),
    val selectedChannel: String = ALL_CHANNELS,
    val selectedItem: Long = -1L,
    val selectedTab: Boolean = false,
    val newChannel: String = "",
    val channels: List<RssChannel> = emptyList(),
    val addChannelFailed: Boolean = false
)

class RssViewModel: ViewModel() {
    private var _uiState = MutableStateFlow(uiState())
    val uiState: StateFlow<uiState> = _uiState.asStateFlow()
    private var client: OkHttpClient
    private var count: Int=30

    init {
        val p = getSystemProxy()
        if (p != null) {
            client = buildClient(proxy_data = p)
        }else{
            client = buildClient()
        }
        initData()
    }

    fun updateNewChannel(channel: String) {
        _uiState.update{currentState -> currentState.copy(newChannel = channel, addChannelFailed = false) }
    }

    fun read(id: Long){
        //todo
    }

    fun like(id: Long,rev: Boolean = false){
        if (rev){
            viewModelScope.launch {
                unlikeItem(id)
                _uiState.update { state -> state.copy(nowItems = state.nowItems.map {if (id==it.id) it.copy(is_like = false) else it})}
            }
        }else{
            viewModelScope.launch {
                likeItem(id)
                _uiState.update { state -> state.copy(nowItems = state.nowItems.map {if (id==it.id) it.copy(is_like = true) else it})}
            }
        }

    }

    fun loadItems(i: Int = 30) {
        viewModelScope.launch {
            val selected = uiState.value.selectedChannel
            val items = when (selected) {
                ALL_CHANNELS -> loadData(i)
                Liked_ITEMS -> loadLikedItems()
                else -> loadData(i, selected)
            }

            _uiState.update { state ->
                state.copy(nowItems = items)
            }
        }
    }


    fun submitNewChannel() {
        val newChannel = uiState.value.newChannel
        viewModelScope.launch {
            try {
                val items = fetchAndParseRss(newChannel,client=client).getOrNull()
                if (items == null || items.isEmpty()){
                    _uiState.update { currentState ->
                        currentState.copy(addChannelFailed = true)
                    }
                    Log.d("submitNewChannel", "items is null"+items.toString())
                }
                else{
                    val channel_name = items[0].channel
                    addChannel(channel_name = channel_name,newChannel)
                    updateDB(items)
                    _uiState.update { currentState ->
                        currentState.copy(
                            channels = getChannels(),
                            nowItems = loadData(30),
                            newChannel = "",
                            )
                    }

                }
            }catch (e: Exception){
                _uiState.update { currentState ->
                    currentState.copy(addChannelFailed = true)
                }
                Log.e("submitNewChannel", e.toString())
            }

        }
    }

    fun deleteChannel(channel: String){
        viewModelScope.launch {
            try {
                deleteChannel_db(channel)
                _uiState.update { currentState ->
                    currentState.copy(
                        channels = getChannels())
                }
                loadItems()
            }catch (e: Exception){
                Log.e("deleteChannel", e.toString())
            }
        }
    }

    fun getSystemProxy(): ProxyData? {
        val host = System.getProperty("https.proxyHost")
        val port = System.getProperty("https.proxyPort")?.toIntOrNull()
        return if (host != null && port != null) {
            ProxyData(host, port)
        } else {
            null
        }
    }

    fun resetUistate(){
        _uiState.update { now -> now.copy(
            addChannelFailed = false,
            newChannel = "",
            selectedItem = -1L,
            selectedChannel = ALL_CHANNELS,
            selectedTab = false
        ) }
    }

    fun setProxy(host: String, port: Int){
        client = buildClient(proxy_data = ProxyData(host, port))
    }

    fun selectChannel(channel: String) {
        _uiState.update { currentState ->
            currentState.copy(selectedChannel = channel, selectedTab = false, selectedItem = -1L)
        }
        loadItems()
        count = 30
    }

    fun selectItem(itemId: Long) {
        _uiState.update { currentState ->
            currentState.copy(selectedItem = itemId)
        }
    }

    fun getMore(){
        count+=30
        loadItems(count)
    }

    fun initData(){
        loadItems()
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(channels = getChannels())
            }
            fetchNews()
            loadItems()
        }
    }

    suspend fun fetchNews(){
        val newp = getSystemProxy()
        client = buildClient(newp)
        for(i in uiState.value.channels){
            val items = fetchAndParseRss(i.url,client=client).getOrNull()
            if (items != null){
                updateDB(items)
            }
        }
    }

    fun selectTab() {
            viewModelScope.launch {
                _uiState.update { currentState ->
                currentState.copy(selectedTab = !currentState.selectedTab, channels = getChannels())
            }
        }
    }
}

//fun stringToLong(dateString: String): Long? {
//    return try {
//        val formatter = DateTimeFormatter.RFC_1123_DATE_TIME // 直接支持 RFC 1123 格式
//        val zonedDateTime = ZonedDateTime.parse(dateString, formatter)
//        zonedDateTime.toInstant().toEpochMilli()
//    } catch (e: Exception) {
//        e.printStackTrace()
//        0
//    }
//}

fun stringToLong(dateString: String): Long? {
    // List of common date formatters
    val formatters = listOf(
        DateTimeFormatter.RFC_1123_DATE_TIME, // Thu, 14 Jun 2025 18:00:00 GMT
        DateTimeFormatter.ISO_ZONED_DATE_TIME, // 2025-06-14T18:00:00+09:00
        DateTimeFormatter.ISO_LOCAL_DATE_TIME, // 2025-06-14T18:00:00
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"), // 2025-06-14 18:00:00
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"), // 14/06/2025 18:00:00
        DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"), // 06/14/2025 18:00:00
        DateTimeFormatter.ofPattern("yyyy-MM-dd"), // 2025-06-14
        DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH), // 14-Jun-2025 18:00:00
        DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH) // Sat Jun 14 18:00:00 JST 2025
    )

    return try {
        // Try each formatter
        for (formatter in formatters) {
            try {
                // For formats without timezone, assume system default
                return when {
                    // Handle date-only formats
                    formatter.toString().contains("HH").not() -> {
                        LocalDate.parse(dateString, formatter)
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                    }
                    // Handle ISO_LOCAL_DATE_TIME which needs a timezone
                    formatter == DateTimeFormatter.ISO_LOCAL_DATE_TIME -> {
                        LocalDateTime.parse(dateString, formatter)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                    }
                    // Handle all other formats
                    else -> {
                        ZonedDateTime.parse(dateString, formatter)
                            .toInstant()
                            .toEpochMilli()
                    }
                }
            } catch (_: DateTimeParseException) {
                continue // Try next formatter
            }
        }
        null // No format matched
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun makeRssItem(
    channel: String,
    title: String,
    link: String,
    description: String,
    pubDate: String?
): RssItem{
    val title = title.replace(Regex("\\s+"), " ") // 把多个空白字符变成一个空格
        .trim()                      // 去除首尾空格
    if (pubDate == null){
        return RssItem(
            id = title.hashCode().toLong(),
            channel = channel,
            title = title,
            link = link,
            description = description,
        )
    }
    else {
        val date = stringToLong(pubDate)
        return RssItem(
            id = title.hashCode().toLong(),
            channel = channel,
            title = title,
            link = link,
            description = description,
            pubDate = date ?: 0
        )
    }
}

fun dateToString(dateMillis: Long): String {
    if (dateMillis == 0L) return "falied"

    val nowCal = Calendar.getInstance()
    val dateCal = Calendar.getInstance().apply { timeInMillis = dateMillis }

    // 只取日期部分（清除时分秒）
    nowCal.set(Calendar.HOUR_OF_DAY, 0)
    nowCal.set(Calendar.MINUTE, 0)
    nowCal.set(Calendar.SECOND, 0)
    nowCal.set(Calendar.MILLISECOND, 0)

    dateCal.set(Calendar.HOUR_OF_DAY, 0)
    dateCal.set(Calendar.MINUTE, 0)
    dateCal.set(Calendar.SECOND, 0)
    dateCal.set(Calendar.MILLISECOND, 0)

    val diffDays = ((nowCal.timeInMillis - dateCal.timeInMillis) / TimeUnit.DAYS.toMillis(1)).toInt()

    val label = when (diffDays) {
        0 -> "今天"
        1 -> "昨天"
        2 -> "前天"
        in 3..6 -> "${diffDays}天前"
        else -> null
    }

    val pattern = if (label != null) "'$label' HH:mm" else "yyyy年MM月dd日 HH:mm"
    val sdf = SimpleDateFormat(pattern, Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }

    return sdf.format(Date(dateMillis))
}




