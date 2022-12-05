package org.ivance.chatbot

import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import java.lang.RuntimeException

object ChatBotPluginMain : KotlinPlugin(
    JvmPluginDescription(
        id = "org.ivance.chatbot",
        name = "OpenAiGPT3Chatbot",
        version = "0.1"
    ) {
        author("RIvance")
        info("A Chatbot based on OpenAI GPT3 API")
    }
) {
    private val tokenPattern = "sk-(?:[A-Za-z0-9+]{4})*(?:[A-Za-z0-9+]{2}==|[A-Za-z0-9+]{3}=)?".toRegex()

    override fun onEnable() {
        try {
            ChatBotPluginConfig.reload()
            if (ChatBotPluginConfig.token == "YOUR_OPENAI_API_KEY_HERE") {
                logger.error("You have not set your OpenAI API key yet")
                logger.error("Please set your API key in `$configFolder/ChatBotPluginConfig.yml`")
                throw RuntimeException("Invalid token: ${ChatBotPluginConfig.token}")
            } else if (!ChatBotPluginConfig.token.matches(tokenPattern)) {
                logger.error("Invalid API key format, please check your key in `$configFolder/ChatBotPluginConfig.yml`")
                logger.error("A valid key is a base64 string with `sk-` prefix")
                throw RuntimeException("Invalid token: ${ChatBotPluginConfig.token}")
            }
            ChatBotPluginListener.init(this)
            logger.info("OpenAI GPT3 chatbot plugin loaded")
        } catch (exception: Exception) {
            logger.error("Unable to load OpenAI GPT3 chatbot plugin")
            throw exception
        }
    }
}
