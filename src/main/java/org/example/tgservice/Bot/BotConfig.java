package org.example.tgservice.Bot;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;


@Configuration
@EnableScheduling
@Data
public class BotConfig {
    @Value("${telegram.name}")
    String botName;
    @Value("${telegram.token.security}")
    String token ;
}
