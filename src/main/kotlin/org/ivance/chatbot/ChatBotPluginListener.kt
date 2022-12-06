package org.ivance.chatbot

import com.theokanning.openai.OpenAiService
import com.theokanning.openai.completion.CompletionRequest
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
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
import org.ivance.chatbot.ChatBotPluginConfig.topProb
import org.ivance.chatbot.ChatBotPluginConfig.triggerPrefixes
import org.ivance.chatbot.ChatBotPluginConfig.triggerWords
import retrofit2.HttpException
import java.net.SocketTimeoutException

internal object ChatBotPluginListener {

    private lateinit var service: OpenAiService
    private lateinit var context: ChatBotPluginMain

    fun init(context: ChatBotPluginMain) {
        this.context = context
        context.logger.info("Initializing OpenAI service")
        this.service = OpenAiService(ChatBotPluginConfig.token, socketTimeout)
        try {
            context.logger.info("Available models: ${this.service.listModels().map { it.root }}")
        } catch (exception: HttpException) {
            context.logger.error("Unable to fetch models, please check your API token")
            throw exception
        }
        subscribeEvents()
    }

    private fun subscribeEvents() {
        GlobalEventChannel.subscribeAlways<MessageEvent> {
            (triggerPrefixes.firstOrNull { message.content.startsWith(it) } ?.let {
                message.content.replace(it, "").trim()
            } ?: if (triggerWords.any { message.content.contains(it) }) {
                message.content
            } else { null }) ?.let fetch@ { prompt ->
                context.logger.info("Prompt: $prompt")
                for (i in 0 until numRetries) {
                    try {
                        chat(prompt, sender.nick) ?.let {
                            if (quoteWhenReply) {
                                subject.sendMessage(QuoteReply(this.source) + it)
                            } else {
                                subject.sendMessage(it)
                            }
                            return@fetch
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
