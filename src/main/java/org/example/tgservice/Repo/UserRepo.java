package org.example.tgservice.Repo;

import org.example.tgservice.BD.TelegramUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<TelegramUser, Long> {

    Optional<TelegramUser> findByChatId(Long chatId);
}
