package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {

    private final UserStorage userStorage;

    public UserService(@Qualifier("userDbStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public Collection<User> findAll() {
        log.info("Запрос вывода всех пользователей");
        return userStorage.findAll();
    }

    public User create(User user) {
        log.info("Запрос на создание пользователя");
        validateUser(user);
        prepareUserName(user);
        return userStorage.create(user);
    }

    public User update(User user) {
        log.info("Запрос на обновление пользователя с id={}", user.getId());
        getUserOrThrow(user.getId());
        validateUser(user);
        prepareUserName(user);
        return userStorage.update(user);
    }

    public User findById(Long id) {
        return getUserOrThrow(id);
    }

    public void addFriend(Long userId, Long friendId) {
        checkNotEqualsId(userId, friendId, "Нельзя добавить самого себя в друзья");
        log.info("Пользователь с id={} добавляет в друзья пользователя с id={}", userId, friendId);

        User user = getUserOrThrow(userId);
        getUserOrThrow(friendId);

        user.getFriends().add(friendId);
        userStorage.update(user);
    }

    public void removeFriend(Long userId, Long friendId) {
        checkNotEqualsId(userId, friendId, "Нельзя удалить самого себя из своих друзей");
        log.info("Пользователь с id={} удаляет из друзей пользователя с id={}", userId, friendId);

        User user = getUserOrThrow(userId);
        getUserOrThrow(friendId);

        user.getFriends().remove(friendId);
        userStorage.update(user);
    }

    public Collection<User> getFriends(Long userId) {
        User user = getUserOrThrow(userId);

        return user.getFriends().stream()
                .map(this::getUserOrThrow)
                .toList();
    }

    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        User user = getUserOrThrow(userId);
        User otherUser = getUserOrThrow(otherId);
        checkNotEqualsId(userId, otherId, "Id не должны быть одинаковыми");
        Set<Long> otherFriends = otherUser.getFriends();

        return user.getFriends().stream()
                .filter(otherFriends::contains)
                .map(this::getUserOrThrow)
                .collect(Collectors.toList());
    }

    private void validateUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            throw new ValidationException("Некорректный email");
        }

        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            throw new ValidationException("Логин не может быть пустым и содержать пробелы");
        }

        LocalDate birthday = user.getBirthday();
        if (birthday == null || birthday.isAfter(LocalDate.now())) {
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }

    private void prepareUserName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }

    private User getUserOrThrow(Long userId) {
        return userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }
    private void checkNotEqualsId(Long firstId, Long secondId, String message) {
        if (firstId == null || secondId == null) {
            throw new ValidationException("Id не должны быть null");
        }
        if (firstId.equals(secondId)) {
            throw new ValidationException(message);
        }
    }
}