package org.ivance.chatbot

import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object ChatBotPluginConfig : ReadOnlyPluginConfig("ChatBotPluginConfig") {

    @ValueDescription("""
        Your OpenAI API key, starts with `sk-`. 
        See https://beta.openai.com/account/api-keys
    """)
    val token: String by value("YOUR_OPENAI_API_KEY_HERE")

    @ValueDescription("""
        Prefixes used to trigger the response.
        e.g. "/chat How are you today?"
    """)
    val triggerPrefixes: List<String> by value(listOf("/chat"))

    @ValueDescription("""
        Keywords used to trigger the response.
        e.g. "What is GPT-3?"
    """)
    val triggerWords: List<String> by value(listOf("what is", "how to"))

    @ValueDescription("""
        Http request timeout in seconds, 0 means no timeout.
    """)
    val socketTimeout: Int by value(20)

    @ValueDescription("""
        Number of retries on request timeout
    """)
    val numRetries: Int by value(3)

    @ValueDescription("""
        Message sent when a request fails after `numRetries` retries.
        An empty string means do not send fail message.
    """)
    val requestFailureErrorMessage: String by value("")

    @ValueDescription("""
        Quote the orignal message when sending reply.
    """)
    val quoteWhenReply: Boolean by value(true)

    @ValueDescription("""
        The name of the model to use.
        Required if specifying a fine tuned model or if using the new v1/completions endpoint.
    """)
    val model: String by value("text-davinci-003")

    @ValueDescription("""
        The maximum number of tokens to generate.
        Requests can use up to 2048 tokens shared between prompt and completion.
        (One token is roughly 4 characters for normal English text)
        Notice that a large `maxToken` value may cause request timeout.
    """)
    val maxTokens: Int by value(300)

    @ValueDescription("""
        What sampling temperature to use. Higher values means the model will take more risks.
        Try 0.9 for more creative applications, and 0 (argmax sampling) for ones with a well-defined answer.
    """)
    val temperature: Double by value(0.9)

    @ValueDescription("""
        An alternative to sampling with temperature, called nucleus sampling, where the model considers the results of
        the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are
        considered.
    """)
    val topProb: Double by value(1.0)

    @ValueDescription("""
        Number between 0 and 1 (default 0) that penalizes new tokens based on whether they appear in the text so far.
        Increases the model's likelihood to talk about new topics.
    """)
    val frequencyPenalty: Double by value(0.0)

    @ValueDescription("""
        Number between 0 and 1 (default 0) that penalizes new tokens based on their existing frequency in the text so far.
        Decreases the model's likelihood to repeat the same line verbatim.
    """)
    val presencePenalty: Double by value(0.6)
}
