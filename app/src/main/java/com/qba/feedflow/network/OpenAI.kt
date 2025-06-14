package com.qba.feedflow.network

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.chat.completions.ChatCompletion
import com.openai.models.chat.completions.ChatCompletionCreateParams
import kotlin.jvm.optionals.getOrNull

class DashScopeAPI {

    private val apiKey: String = "sk-7382bcc5cabf4523bfad7f6904567428"
    private val baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    private val client: OpenAIClient

    init {
        client = OpenAIOkHttpClient.builder()
            .apiKey(apiKey.toString())
            .baseUrl(baseUrl)
            .build()
    }

    fun chat(userMessage: String, model: String): String {
        val params = ChatCompletionCreateParams.builder()
            .addUserMessage(userMessage)
            .model(model)
            .build()

        return try {
            val completion: ChatCompletion = client.chat().completions().create(params)
            completion.choices()[0].message().content().getOrNull().toString()
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.message}"
        }
    }
}

fun main(){
    val api = DashScopeAPI()
    val reply = api.chat("你是谁？", "qwen-plus")
    println("AI回复：$reply")
}