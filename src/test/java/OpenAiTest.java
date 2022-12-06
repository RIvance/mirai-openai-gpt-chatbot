import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public class OpenAiTest {

    @Test
    void openAiTest() throws IOException {

        String token = new String(
            Objects.requireNonNull(getClass().getResourceAsStream("openai-token")
        ).readAllBytes(), StandardCharsets.UTF_8);

        OpenAiService service = new OpenAiService(token, 15);
        System.out.println(service.listModels());

        var completionRequest = CompletionRequest.builder()
            .model("text-davinci-003").echo(true)
            .prompt("What is GPT-3?")
            .temperature(0.9)
            .maxTokens(200)
            .topP(1.0)
            .frequencyPenalty(0.0)
            .presencePenalty(0.6)
            .stop(List.of(" Human:", " AI:"))
            .build();
        service.createCompletion(completionRequest).getChoices().forEach(completionChoice -> {
            System.out.println(completionChoice.getText());
        });
    }
}
