package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import ru.yandex.practicum.filmorate.model.User;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@Qualifier("inMemoryUserStorage")
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();

    @Override
    public Collection<User> findAll() {
        log.info("Запрос вывода всех пользователей");
        return users.values();
    }

    @Override
    public User create(User user) {
        log.info("Запрос на создание пользователя");


        user.setId(getNextId());


        users.put(user.getId(), user);
        log.info("Создан пользователь с id={}", user.getId());
        return user;
    }

    @Override
    public User update(User user) {
        Long id = user.getId();
        log.info("Запрос на обновление пользователя с id={}", id);

        users.put(id, user);
        log.info("Обновлён пользователь с id={}", id);

        return user;
    }

    @Override
    public Optional<User> findById(Long id) {


        return Optional.ofNullable(users.get(id));

    }


    private long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return currentMaxId + 1;
    }


}
