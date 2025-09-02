package org.example.tgservice.Bot;

import lombok.extern.slf4j.Slf4j;
import org.example.tgservice.BD.Feedback;
import org.example.tgservice.Enum.Role;
import org.example.tgservice.Repo.FeedbackRepo;
import org.example.tgservice.BD.TelegramUser;
import org.example.tgservice.Repo.UserRepo;
import org.example.tgservice.Service.GoogleSheetsService;
import org.example.tgservice.Service.OpenAIService;
import org.example.tgservice.Service.TrelloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import java.time.LocalDateTime;
import java.util.*;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig botConfig;
    private final UserRepo userRepository;
    private final FeedbackRepo feedbackRepo;
    private final OpenAIService openAIService;
    private final GoogleSheetsService googleSheetsService;
    private final TrelloService trelloService;
    private final Map<Long, Integer> loginAttempts = new HashMap<>();
    private final Map<Long, LocalDateTime> blockedUntil = new HashMap<>();
    private final int MAX_ATTEMPTS = 3;
    private final int BLOCK_TIME_MINUTES = 10;
    private final String ADMIN_PASSWROD = System.getenv("ADMIN_PASSWROD");
    private final Set<Long> adminChatIds = new HashSet<>();
    private final Map<Long, Boolean> waitingForPassword = new HashMap<>();


    @Autowired
    public TelegramBot(BotConfig botConfig, UserRepo userRepository, FeedbackRepo feedbackRepo,
                       OpenAIService openAIService, GoogleSheetsService googleSheetsService,
                       TrelloService trelloService) {
        this.botConfig = botConfig;
        this.userRepository = userRepository;
        this.feedbackRepo = feedbackRepo;
        this.openAIService = openAIService;
        this.googleSheetsService = googleSheetsService;
        this.trelloService = trelloService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();


        // рефакторінг завдяки горячим клавішам в Інтелідж, обережно через клавіши переводим у приватні методи
        if (handleAuth(chatId, text)) return; // авторізація адміна

        if (adminChatIds.contains(chatId)) {
            if (handleAdminCommands(chatId, text)) return; // команди авторізованого адміна
        }

        Optional<TelegramUser> userOpt = userRepository.findByChatId(chatId);
        TelegramUser user;
        if (userOpt.isEmpty()) {
            user = TelegramUser.builder().chatId(chatId).registered(false)
                    .waitingForFeedback(false)
                    .build();
            userRepository.save(user);
            sendMessage(chatId, "Вітаю! Оберіть вашу посаду: Автомеханік, Автоелектрик, Діагност, Моторист, " +
                    "Трансмісіонщик, Ходовик, Слюсар з ремонту авто, Шиномонтажник, " +
                    "Газовщик (ГБО), Маляр, Кузовщик, Жестянщик, Сварщик, Поліровщик, " +
                    "Майстер-приймальник, Сервіс-менеджер, Керівник СТО, Адміністратор, Мийник, " +
                    "Оцінювач збитків, Запчастинник.");
            return;
        } else {
            user = userOpt.get();
        }

        if (handleRegisterUserAnonim(user, text, chatId)) return; // регістрація юзера

        handleFeedBackUserAnonim(chatId, text, user);// передача відклику від юзера
    }

    private boolean handleRegisterUserAnonim(TelegramUser user, String text, Long chatId) {
        if (!user.isRegistered()) {
            if (user.getRole() == null) {
                Role role = Role.fromString(text);
                if (role != null) {
                    user.setRole(role.name());
                    userRepository.save(user);
                    sendMessage(chatId, "Добре 👍 Тепер введіть назву вашої філії (відділення СТО).");
                }
                return true;
            }
            else if (user.getBranch() == null) {
                user.setBranch(text);
                user.setRegistered(true);
                user.setWaitingForFeedback(true);
                userRepository.save(user);
                sendMessage(chatId, "Дякуємо! Ви зареєстровані ✅ Можете залишати відгуки за командою /feedback.");
                return true;
            }
        }
        return false;
    }

    private void handleFeedBackUserAnonim(Long chatId, String text, TelegramUser user) {
        if (!adminChatIds.contains(chatId)) {
            if (text.toLowerCase().startsWith("/feedback")) {
                String feedbackText = null;
                if(text.length()>9){
                    feedbackText = text.substring(9).trim();
                }
                if (feedbackText == null || feedbackText.isEmpty()) {
                    user.setWaitingForFeedback(true);
                    userRepository.save(user);
                    sendMessage(chatId, "Будь ласка, введіть ваш відгук:");
                    return;
                }


                Feedback feedback = null;
                try {
                    feedback = openAIService.analyzeFeedback(chatId, feedbackText);
                } catch (HttpClientErrorException | HttpServerErrorException e) {
                    sendMessage(chatId, "Помилка при аналізі відгуку: Сервер недоступний або запит неправильний. Спробуйте пізніше");
                    log.error("OpenAI API returned error", e);
                    return;
                } catch (ResourceAccessException e) {
                    sendMessage(chatId, "Помилка підключення к OpenAI. Спробуйте пізніше.");
                    log.error("OpenAI API not reachable", e);
                    return;
                } catch (Exception e) {
                    sendMessage(chatId, "Відбулася непередбачена помилка під час аналізу відгуку.");
                    log.error("Unknown error while analyzing feedback", e);
                    return;
                }

                feedback.setBranch(user.getBranch());
                feedback.setRole(user.getRole());
                feedbackRepo.save(feedback);

                googleSheetsService.appendFeedback(feedback);
                trelloService.createCard(feedback);

                sendMessage(chatId,
                        "Ваш відгук отримано 🙌\n" +
                                "Тональність: " + feedback.getSentiment() + "\n" +
                                "Критичність: " + feedback.getCriticality() + "/5\n" +
                                "Рішення: " + feedback.getSuggestion());

                user.setWaitingForFeedback(false);
                userRepository.save(user);
                return;
            }
        }
    }

    private boolean handleAuth(Long chatId, String text) {
        if (waitingForPassword.getOrDefault(chatId, false)) {
            waitingForPassword.remove(chatId);

            int attempts = loginAttempts.getOrDefault(chatId, 0);

            if (blockedUntil.containsKey(chatId)) {
                if (blockedUntil.get(chatId).isAfter(LocalDateTime.now())) {
                    sendMessage(chatId, "Слишком много попыток. Попробуйте позже.");
                    return true;
                } else {
                    blockedUntil.remove(chatId);
                    loginAttempts.put(chatId, 0);
                }
            }

            if (text.equals(ADMIN_PASSWROD)) {
                loginAttempts.remove(chatId);
                adminChatIds.add(chatId);
                sendMessage(chatId, "Пароль верный ✅. Вы теперь админ. Используйте команды...");
            } else {
                attempts++;
                loginAttempts.put(chatId, attempts);
                if (attempts >= MAX_ATTEMPTS) {
                    blockedUntil.put(chatId, LocalDateTime.now().plusMinutes(BLOCK_TIME_MINUTES));
                    sendMessage(chatId, "Забагато  спроб. Спробуйте через " + BLOCK_TIME_MINUTES + " минут.");
                } else {
                    sendMessage(chatId, "Невірний пароль ❌. Залишилось спроб: " + (MAX_ATTEMPTS - attempts));
                }
            }
            return true;
        }

        if (text.equalsIgnoreCase("/admin")) {
            waitingForPassword.put(chatId, true);
            sendMessage(chatId, "Введіть пароль администратора:");
            return true;
        }
        return false;
    }

    private void sendMessage(Long chatId, String text) {
        try {
            execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String formatFeedbackList(List<Feedback> feedbacks) {
        if (feedbacks.isEmpty()) return "Немає відгуків 😔";
        StringBuilder sb = new StringBuilder();
        for (Feedback f : feedbacks) {
            sb.append("ID: ").append(f.getId()).append("\n")
                    .append("Відгук: ").append(f.getMessage()).append("\n")
                    .append("Тональність: ").append(f.getSentiment()).append("\n")
                    .append("Критичність: ").append(f.getCriticality()).append("\n")
                    .append("Роль/Філія: ").append(f.getRole()).append(", ").append(f.getBranch())
                    .append("\n\n");
        }
        return sb.toString();
    }

    private boolean handleAdminCommands(Long chatId, String text) {
        if (!adminChatIds.contains(chatId)) return false;

        if (text.equalsIgnoreCase("/all")) {
            sendMessage(chatId, formatFeedbackList(feedbackRepo.findAll()));
            return true;
        }
        if (text.toLowerCase().startsWith("/branch ")) {
            String branch = text.substring(8).trim();
            sendMessage(chatId, formatFeedbackList(
                    feedbackRepo.findAll().stream()
                            .filter(f -> f.getBranch().equalsIgnoreCase(branch))
                            .toList()
            ));
            return true;
        }
        if (text.toLowerCase().startsWith("/role ")) {
            String role = text.substring(6).trim();
            sendMessage(chatId, formatFeedbackList(
                    feedbackRepo.findAll().stream()
                            .filter(f -> f.getRole().equalsIgnoreCase(role))
                            .toList()
            ));
            return true;
        }
        if (text.toLowerCase().startsWith("/critical ")) {
            try {
                int lvl = Integer.parseInt(text.substring(10).trim());
                sendMessage(chatId, formatFeedbackList(
                        feedbackRepo.findAll().stream()
                                .filter(f -> f.getCriticality() >= lvl)
                                .toList()
                ));
            } catch (NumberFormatException e) {
                sendMessage(chatId, "Неверный уровень критичности");
            }
            return true;
        }
        return false;
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }
}
