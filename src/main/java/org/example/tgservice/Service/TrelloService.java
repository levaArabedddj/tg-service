package org.example.tgservice.Service;


import lombok.extern.slf4j.Slf4j;
import org.example.tgservice.BD.Feedback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class TrelloService {

    private static final String TRELLO_KEY = System.getenv("TRELLO_KEY");
    private static final String TRELLO_TOKEN = System.getenv("TRELLO_TOKEN");
    private static final String LIST_ID = System.getenv("LIST_ID"); // список на доске


    private final RestTemplate restTemplate;

    @Autowired
    public TrelloService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void createCard(Feedback feedback) {
        if (feedback.getCriticality() < 4) return;

        String url = String.format(
                "https://api.trello.com/1/cards?name=%s&desc=%s&pos=top&idList=%s&key=%s&token=%s",
                encode(feedback.getMessage()),
                encode("Sentiment: " + feedback.getSentiment() + "\nРішення: " + feedback.getSuggestion()),
                LIST_ID,
                TRELLO_KEY,
                TRELLO_TOKEN
        );

        try {
            restTemplate.postForObject(url, null, String.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Сервер Trello/GSheets вернул ошибку
            log.error("Вилетіла помилка HTTP в створенні картки в Trello", e);
        } catch (ResourceAccessException e) {
            // Таймаут или недоступность сервиса
            log.error("Сервис недоступен", e);
        }
    }

    private String encode(String s) {
        return s.replaceAll("\n", "%0A");
    }
}

