package org.ivance.chatbot

import com.theokanning.openai.OpenAiService
import com.theokanning.openai.completion.CompletionRequest
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.content
import org.ivance.chatbot.ChatBotPluginConfig.frequencyPenalty
import org.ivance.chatbot.ChatBotPluginConfig.maxTokens
import org.ivance.chatbot.ChatBotPluginConfig.model
import org.ivance.chatbot.ChatBotPluginConfig.presencePenalty
import org.ivance.chatbot.ChatBotPluginConfig.temperature
import org.ivance.chatbot.ChatBotPluginConfig.topProb
import org.ivance.chatbot.ChatBotPluginConfig.triggerPrefixes
import org.ivance.chatbot.ChatBotPluginConfig.triggerWords
import retrofit2.HttpException
import java.io.IOException
import java.util.*

internal object ChatBotPluginListener {

    private lateinit var service: OpenAiService
    private lateinit var context: ChatBotPluginMain

    fun init(context: ChatBotPluginMain) {
        this.context = context
        context.logger.info("Initializing OpenAI service")
        this.service = OpenAiService(ChatBotPluginConfig.token)
        try {
            context.logger.info("Available models: ${this.service.listModels()}")
        } catch (exception: HttpException) {
            context.logger.error("Unable to fetch models, please check your API token")
            throw exception
        }
        subscribeEvents()
    }

    private fun subscribeEvents() {
        GlobalEventChannel.subscribeAlways<MessageEvent> {
            triggerPrefixes.firstOrNull {
                message.content.startsWith(it)
            } ?.lowercase(Locale.getDefault()) ?.let {
                message.content.replace(it, "").trim()
            } ?: if (triggerWords.any { message.content.contains(it) }) {
                message.content
            } else { null } ?.let { prompt ->
                try {
                    chat(prompt, sender.nick) ?.let {
                        subject.sendMessage(At(subject.id) + it)
                    } ?: {
                        context.logger.warning("Unable to fetch response of prompt \"$prompt\" ")
                    }
                } catch (exception: IOException) {
                    subject.sendMessage(At(subject.id) + exception.message.toString())
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
