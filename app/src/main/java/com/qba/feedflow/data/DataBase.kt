// RssDatabaseManager.kt
package com.qba.feedflow.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 1. Entity
@Entity(tableName = "rss_items")
data class RssItem(
    @PrimaryKey val id: Long,
    val channel: String,
    val title: String,
    val link: String,
    val description: String,
    val pubDate: Long = 0,
    val read: Boolean = false,
    val is_like: Boolean = false
)
@Entity(tableName = "rss_channels")
data class RssChannel(
    @PrimaryKey val name: String,
    val url: String
)
@Entity(tableName = "apikey")
data class ApiKey(
    @PrimaryKey val key: String = "",
)

@Dao
interface ApiKeyDao {
    @Query("SELECT * FROM apikey LIMIT 1")
    suspend fun getApiKey(): ApiKey?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiKey(apiKey: ApiKey)

    @Query("DELETE FROM apikey")
    suspend fun deleteApiKey()
}


// 2. DAO
@Dao
interface RssItemDao {
    @Query("UPDATE rss_items SET read = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("SELECT * FROM rss_items WHERE is_like = 1 ORDER BY pubDate DESC")
    suspend fun loadLikedItems(): List<RssItem>

    @Query("SELECT * FROM rss_items ORDER BY pubDate DESC LIMIT :limit")
    suspend fun getRecentItems(limit: Int): List<RssItem>

    @Query("SELECT * FROM rss_items WHERE channel = :channel ORDER BY pubDate DESC LIMIT :limit")
    suspend fun getRecentItemsByChannel(limit: Int, channel: String): List<RssItem>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItems(items: List<RssItem>)

    @Query("DELETE FROM rss_items WHERE channel = :channel")
    suspend fun clearChannel(channel: String)

    @Query("UPDATE rss_items SET is_like = 1 WHERE id = :id")
    suspend fun likeItem(id: Long)

    @Query("UPDATE rss_items SET is_like = 0 WHERE id = :id")
    suspend fun unlikeItem(id: Long)

}

@Dao
interface RssChannelDao {
    @Query("SELECT * FROM rss_channels ORDER BY name ASC")
    suspend fun getAllChannels(): List<RssChannel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: RssChannel)

    @Query("DELETE FROM rss_channels WHERE name = :name")
    suspend fun deleteChannel(name: String)
}


// 3. Database
@Database(entities = [RssItem::class, RssChannel::class, ApiKey::class], version = 1)
abstract class RssDatabase : RoomDatabase() {
    abstract fun rssItemDao(): RssItemDao
    abstract fun rssChannelDao(): RssChannelDao
    abstract fun apiKeyDao(): ApiKeyDao
}

// 4. Singleton Manager
object RssDatabaseManager {
    private var db: RssDatabase? = null

    fun initDb(context: Context) {
        if (db == null) {
            db = Room.databaseBuilder(
                context.applicationContext,
                RssDatabase::class.java,
                "rss_database"
            ).build()
        }
    }

    private fun getRssDao(): RssItemDao {
        return db?.rssItemDao()
            ?: throw IllegalStateException("Database not initialized. Call initDb() first.")
    }

    private fun getChannelDao(): RssChannelDao {
        return db?.rssChannelDao()
            ?: throw IllegalStateException("Database not initialized. Call initDb() first.")
    }

    private fun getApiKeyDao(): ApiKeyDao {
        return db?.apiKeyDao()
            ?: throw IllegalStateException("Database not initialized. Call initDb() first.")
    }

    suspend fun loadData(i: Int): List<RssItem> = withContext(Dispatchers.IO) {
        getRssDao().getRecentItems(i)
    }

    suspend fun likeItem(id: Long) = withContext(Dispatchers.IO) {
        getRssDao().likeItem(id)
    }

    suspend fun unlikeItem(id: Long) = withContext(Dispatchers.IO) {
        getRssDao().unlikeItem(id)
    }

    suspend fun loadLikedItems(): List<RssItem> = withContext(Dispatchers.IO) {
        getRssDao().loadLikedItems()
    }

    suspend fun loadData(i: Int, channel: String): List<RssItem> = withContext(Dispatchers.IO) {
        getRssDao().getRecentItemsByChannel(i, channel)
    }

    suspend fun updateDB(items: List<RssItem>) = withContext(Dispatchers.IO) {
        getRssDao().insertItems(items)
    }

    suspend fun addChannel(channel_name: String,channel_url: String) = withContext(Dispatchers.IO) {
        val item = RssChannel(name = channel_name, url = channel_url)
        getChannelDao().insertChannel(item)
    }
    suspend fun getChannels(): List<RssChannel> = withContext(Dispatchers.IO) {
        getChannelDao().getAllChannels()
    }

    suspend fun deleteChannel(name: String) = withContext(Dispatchers.IO) {
        getChannelDao().deleteChannel(name)
        getRssDao().clearChannel(name)
    }

    suspend fun getApiKey(): String? = withContext(Dispatchers.IO) {
        getApiKeyDao().getApiKey()?.key
    }

    suspend fun saveApiKey(key: String) = withContext(Dispatchers.IO) {
        val apiKey = ApiKey(key = key)
        getApiKeyDao().insertApiKey(apiKey)
    }

    suspend fun deleteApiKey() = withContext(Dispatchers.IO) {
        getApiKeyDao().deleteApiKey()
    }

}
