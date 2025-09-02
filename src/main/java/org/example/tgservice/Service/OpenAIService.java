package org.example.tgservice.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.tgservice.BD.Feedback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service

@RequiredArgsConstructor
public class OpenAIService {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Autowired
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Feedback analyzeFeedback(Long chatId, String text) {
        try {
            //  промпт
            String prompt = """
                Проаналізуй цей відгук і  поверни JSON такого формату:
                {
                  "sentiment": "позитивний/нейтральний/негативний",
                  "criticality": число від 1 до 5,
                  "suggestion": "коротка порада, як вирішити питання"
                }
                
                Відгук: "%s"
                """.formatted(text);

            String url = "https://api.openai.com/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);


            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("model", "gpt-3.5-turbo");
            bodyMap.put("messages", List.of(message));

            HttpEntity<String> request = new HttpEntity<>(
                    objectMapper.writeValueAsString(bodyMap), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);


            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();



            JsonNode jsonResult = objectMapper.readTree(content);

            return Feedback.builder()
                    .chatId(chatId)
                    .message(text)
                    .sentiment(jsonResult.get("sentiment").asText())
                    .criticality(jsonResult.get("criticality").asInt())
                    .suggestion(jsonResult.get("suggestion").asText())
                    .createdAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return Feedback.builder()
                    .chatId(chatId)
                    .message(text)
                    .sentiment("невідомо")
                    .criticality(0)
                    .suggestion("Не вдалося проаналізувати")
                    .createdAt(LocalDateTime.now())
                    .build();
        }
    }

}

