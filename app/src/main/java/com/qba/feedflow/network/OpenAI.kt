package com.qba.feedflow.network

import android.annotation.SuppressLint
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.chat.completions.ChatCompletion
import com.openai.models.chat.completions.ChatCompletionCreateParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.jvm.optionals.getOrNull

class DashScopeAPI {

    private var apiKey: String = "sk-"
    private val baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    private var client: OpenAIClient

    init {
        client = OpenAIOkHttpClient.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .build()
    }

    suspend fun chat(userMessage: String): String = withContext(Dispatchers.IO) {
        val params = ChatCompletionCreateParams.builder()
            .addUserMessage(userMessage)
            .model("qwen-plus")
            .build()
        try {
            val completion: ChatCompletion = client.chat().completions().create(params)
            val choices = completion.choices()
            if (choices.isEmpty()) {
                "Error: No response choices returned from API"
            } else {
                val content = choices[0].message().content().getOrNull()
                if(content!=null){
                    content.toString()
                }else{
                    "Error: No content returned from API"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.message ?: "Unknown exception"}"
        }
    }

    suspend fun chatStream(
        userMessage: String,
        model: String = "qwen-plus",
        onDelta: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        try {
            withContext(Dispatchers.IO) {
                val params = ChatCompletionCreateParams.builder()
                    .addUserMessage(userMessage)
                    .model(model)
                    .build()

                client.chat().completions().createStreaming(params).use { streamResponse ->
                    val stream = streamResponse.stream()
                    for (chunk in stream) {
                        val delta = chunk.choices()[0].delta().content().getOrNull()
                        if (!delta.isNullOrEmpty()) {
                            // 把更新 UI 的操作切回主线程
                            withContext(Dispatchers.Main) {
                                onDelta(delta.toString())
                            }
                        }
                    }
                }

                // 流结束后回调
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError(e)
            }
        }
    }




    fun setKey(key: String) {
        apiKey = key
        client = OpenAIOkHttpClient.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .build()
    }

    fun haveKey(): Boolean {
        return apiKey.isNotEmpty()
    }
}

//fun main(){
//    val p = "计算1+232"
//    val api = DashScopeAPI()
//    api.chatStream(p, onDelta = {println(it)},
//        onError = {it},
//        onComplete = {})
//}