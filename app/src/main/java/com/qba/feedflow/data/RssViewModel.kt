package com.qba.feedflow.data

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.DeleteTable
import com.qba.feedflow.data.RssDatabaseManager.loadData
import com.qba.feedflow.network.fetchAndParseRss
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
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
import com.qba.feedflow.data.RssDatabaseManager.updateDB
import com.qba.feedflow.network.ProxyData
import com.qba.feedflow.network.buildClient
import okhttp3.OkHttpClient
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.TimeZone

val ALL_CHANNELS = "全部文章"

data class uiState(
    val nowItems: List<RssItem> = emptyList(),
    val selectedChannel: String = "",
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

    init {
        client = buildClient()
        initData()
    }

    fun updateNewChannel(channel: String) {
        _uiState.update{currentState -> currentState.copy(newChannel = channel, addChannelFailed = false) }
    }

    fun readItem(id: Long){
        //TODO:
    }

    fun likeItem(id: Long){

    }

     fun loadItems(i: Int = 30){
        viewModelScope.launch {
            var items: List<RssItem> = emptyList()
            if (uiState.value.selectedChannel == "" || uiState.value.selectedChannel == ALL_CHANNELS){
                items = loadData(i)
            }
            else{
                items = loadData(i, uiState.value.selectedChannel)
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
                }
            }catch (e: Exception){
                _uiState.update { currentState ->
                    currentState.copy(addChannelFailed = true)
                }
                Log.e("submitNewChannel", e.toString())
            }

        }
    }

    fun resetUistate(){
        _uiState.update { now -> now.copy(
            addChannelFailed = false,
            newChannel = "",
            selectedItem = -1L,
            selectedChannel = "",
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
    }

    fun selectItem(itemId: Long) {
        _uiState.update { currentState ->
            currentState.copy(selectedItem = itemId)
        }
    }

    fun initData(){
        loadItems()
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(channels = getChannels())
            }
            fetchNews()
        }
        loadItems()
    }

    suspend fun fetchNews(){
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

fun stringToLong(dateString: String): Long? {
    return try {
        val formatter = DateTimeFormatter.RFC_1123_DATE_TIME // 直接支持 RFC 1123 格式
        val zonedDateTime = ZonedDateTime.parse(dateString, formatter)
        zonedDateTime.toInstant().toEpochMilli()
    } catch (e: Exception) {
        e.printStackTrace()
        0
    }
}

fun makeRssItem(
    channel: String,
    title: String,
    link: String,
    description: String,
    pubDate: String?
): RssItem{
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
    if (dateMillis == 0L) return ""

    val now = System.currentTimeMillis()
    val diffMillis = now - dateMillis
    val days = TimeUnit.MILLISECONDS.toDays(diffMillis)

    val timeZone = TimeZone.getDefault()
    val locale = Locale.getDefault()

    val sdf = when {
        days == 0L -> SimpleDateFormat("'今天' HH:mm", locale)
        days in 1..6 -> SimpleDateFormat("'$days 天前' HH:mm", locale)
        else -> SimpleDateFormat("yyyy年MM月dd日 HH:mm", locale)
    }

    sdf.timeZone = timeZone
    return sdf.format(Date(dateMillis))
}


