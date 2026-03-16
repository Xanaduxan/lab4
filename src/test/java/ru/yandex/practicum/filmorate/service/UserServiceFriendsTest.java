package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceFriendsTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        UserStorage userStorage = new InMemoryUserStorage();
        userService = new UserService(userStorage);
    }

    private User newUser(String email, String login, String name) {
        User u = new User();
        u.setEmail(email);
        u.setLogin(login);
        u.setName(name);
        u.setBirthday(LocalDate.of(2000, 1, 1));
        return userService.create(u);
    }


    @Test
    void shouldReturnFriendsListWhenGetFriends() {
        User u1 = newUser("mail1@example.com", "user1", "User1");
        User u2 = newUser("mail2@example.com", "user2", "User2");
        User u3 = newUser("mail3@example.com", "user3", "User3");

        userService.addFriend(u1.getId(), u2.getId());
        userService.addFriend(u1.getId(), u3.getId());

        Collection<User> friends = userService.getFriends(u1.getId());
        Set<Long> ids = friends.stream().map(User::getId).collect(Collectors.toSet());

        assertEquals(Set.of(u2.getId(), u3.getId()), ids);
    }

    @Test
    void shouldReturnCommonFriendsWhenGetCommonFriends() {
        User u1 = newUser("mail1@example.com", "user1", "User1");
        User u2 = newUser("mail2@example.com", "user2", "User2");
        User u3 = newUser("mail3@example.com", "user3", "User3");
        User u4 = newUser("mail4@example.com", "user4", "User4");

        userService.addFriend(u1.getId(), u3.getId());
        userService.addFriend(u2.getId(), u3.getId());
        userService.addFriend(u1.getId(), u4.getId());

        Collection<User> common = userService.getCommonFriends(u1.getId(), u2.getId());
        Set<Long> ids = common.stream().map(User::getId).collect(Collectors.toSet());

        assertEquals(Set.of(u3.getId()), ids);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenAddFriendWithMissingUser() {
        User u1 = newUser("mail1@example.com", "user1", "User1");

        assertThrows(NotFoundException.class,
                () -> userService.addFriend(999L, u1.getId()));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenAddFriendWithMissingFriend() {
        User u1 = newUser("mail1@example.com", "user1", "User1");

        assertThrows(NotFoundException.class,
                () -> userService.addFriend(u1.getId(), 999L));
    }
}