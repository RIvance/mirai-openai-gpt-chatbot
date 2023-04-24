package org.ivance.chatbot

import com.theokanning.openai.OpenAiApi
import com.theokanning.openai.completion.CompletionRequest
import com.theokanning.openai.service.OpenAiService
import com.theokanning.openai.service.OpenAiService.*
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.message.data.*
import org.ivance.chatbot.ChatBotPluginConfig.frequencyPenalty
import org.ivance.chatbot.ChatBotPluginConfig.maxTokens
import org.ivance.chatbot.ChatBotPluginConfig.model
import org.ivance.chatbot.ChatBotPluginConfig.numRetries
import org.ivance.chatbot.ChatBotPluginConfig.presencePenalty
import org.ivance.chatbot.ChatBotPluginConfig.quoteWhenReply
import org.ivance.chatbot.ChatBotPluginConfig.requestFailureErrorMessage
import org.ivance.chatbot.ChatBotPluginConfig.socketTimeout
import org.ivance.chatbot.ChatBotPluginConfig.temperature
import org.ivance.chatbot.ChatBotPluginConfig.token
import org.ivance.chatbot.ChatBotPluginConfig.topProb
import org.ivance.chatbot.ChatBotPluginConfig.triggerPrefixes
import org.ivance.chatbot.ChatBotPluginConfig.triggerWords
import retrofit2.HttpException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.SocketTimeoutException
import java.time.Duration

internal object ChatBotPluginListener {

    private lateinit var service: OpenAiService
    private lateinit var context: ChatBotPluginMain

    fun init(context: ChatBotPluginMain) {
        this.context = context
        context.logger.info("Initializing OpenAI service")
        val timeout = Duration.ofSeconds(socketTimeout.toLong())
        if (ChatBotPluginConfig.proxy.isNotBlank()) {
            val proxy = parseProxy(ChatBotPluginConfig.proxy)
            context.logger.info("Using proxy $proxy")
            val client = defaultClient(token, timeout).newBuilder().proxy(proxy).build()
            val retrofit = defaultRetrofit(client, defaultObjectMapper())
            this.service = OpenAiService(retrofit.create(OpenAiApi::class.java))
        } else {
            this.service = OpenAiService(token, timeout)
        }
        try {
            context.logger.info("Available models: ${this.service.listModels().map { it.root }}")
        } catch (exception: HttpException) {
            context.logger.error("Unable to fetch models, please check your API token")
            throw exception
        }
        subscribeEvents()
    }

    private fun parseProxy(address: String): Proxy {
        return if (address.startsWith("http://") || address.startsWith("https://")) {
            val (host, port) = parseProxyAddress(address.substring(7))
            Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port.toInt()))
        } else if (address.startsWith("socks://")) {
            val (host, port) = parseProxyAddress(address.substring(8))
            Proxy(Proxy.Type.SOCKS, InetSocketAddress(host, port.toInt()))
        } else {
            val (host, port) = parseProxyAddress(address)
            Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port.toInt()))
        }
    }

    private fun parseProxyAddress(address: String): Pair<String, UShort> {
        val parts = address.trim().split(':')
        if (parts.size != 2) {
            throw IllegalArgumentException("Invalid proxy address: $address")
        }
        val host = parts[0].trim()
        val port = parts[1].trim().toUShort()
        return Pair(host, port)
    }

    private fun shouldTriggerReply(message: MessageEvent): Boolean {
        return (
            triggerPrefixes.any { message.message.contentToString().startsWith(it) } ||
            triggerWords.any { message.message.contentToString().contains(it) } ||
            message.message.any { it is At && it.target == message.bot.id } ||
            message.message.any { it is QuoteReply && it.source.targetId == message.bot.id }
        )
    }

    private fun subscribeEvents() {
        GlobalEventChannel.subscribeAlways<MessageEvent> {
            if (shouldTriggerReply(this)) {
                val prompt = this.message.contentToString().trim()
                context.logger.info("Prompt: $prompt")
                for (i in 0 until numRetries) {
                    try {
                        chat(prompt, sender.nick) ?.let {
                            if (quoteWhenReply) {
                                subject.sendMessage(QuoteReply(this.source) + it)
                            } else {
                                subject.sendMessage(it)
                            }
                            return@subscribeAlways
                        } ?: run {
                            context.logger.warning("Failed to fetch response, retrying")
                        }
                    } catch (exception: RuntimeException) {
                        if (exception.cause is SocketTimeoutException) {
                            context.logger.info("Request timeout, retrying")
                        } else {
                            context.logger.warning("${exception.cause}")
                            context.logger.warning("Failed to fetch response, retrying")
                        }
                    }
                }

                context.logger.warning("Failed to fetch response after $numRetries retries")
                if (requestFailureErrorMessage.trim().isNotEmpty()) {
                    subject.sendMessage(requestFailureErrorMessage)
                }
            }
        }
    }

    private fun chat(prompt: String, user: String? = null): String? {
        return service.createCompletion (with(CompletionRequest.builder()) {
            this.echo(false)
            this.model(model)
            this.user(user)
            this.prompt(prompt)
            this.temperature(temperature)
            this.maxTokens(maxTokens)
            this.topP(topProb)
            this.frequencyPenalty(frequencyPenalty)
            this.presencePenalty(presencePenalty)
            this.stop(listOf(" Human:", " AI:"))
            this.build()
        }).choices.firstOrNull()?.text?.replace(prompt, "")?.trim()
    }
}
