package org.example.tgservice.BD;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "telegram_user")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelegramUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long chatId;
    private String role;
    private String branch;
    private boolean registered;
    private boolean waitingForFeedback = false;

}
