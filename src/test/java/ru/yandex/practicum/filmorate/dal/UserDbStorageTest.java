package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dal.mappers.UserRowMapper;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import({UserDbStorage.class, UserRowMapper.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class UserDbStorageTest {

    private final UserDbStorage userDbStorage;

    @Test
    void shouldCreateUser() {
        User user = new User();
        user.setEmail("test@mail.com");
        user.setLogin("test-login");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User createdUser = userDbStorage.create(user);

        assertThat(createdUser.getId()).isNotNull();

        Optional<User> userFromDb = userDbStorage.findById(createdUser.getId());

        assertThat(userFromDb)
                .isPresent()
                .hasValueSatisfying(foundUser -> {
                    assertThat(foundUser.getEmail()).isEqualTo("test@mail.com");
                    assertThat(foundUser.getLogin()).isEqualTo("test-login");
                    assertThat(foundUser.getName()).isEqualTo("Test User");
                    assertThat(foundUser.getBirthday()).isEqualTo(LocalDate.of(2000, 1, 1));
                });
    }

    @Test
    void shouldFindUserById() {
        User user = new User();
        user.setEmail("find@mail.com");
        user.setLogin("find-login");
        user.setName("Find User");
        user.setBirthday(LocalDate.of(1999, 2, 2));

        User createdUser = userDbStorage.create(user);

        Optional<User> userOptional = userDbStorage.findById(createdUser.getId());

        assertThat(userOptional)
                .isPresent()
                .hasValueSatisfying(foundUser -> {
                    assertThat(foundUser.getId()).isEqualTo(createdUser.getId());
                    assertThat(foundUser.getEmail()).isEqualTo("find@mail.com");
                    assertThat(foundUser.getLogin()).isEqualTo("find-login");
                    assertThat(foundUser.getName()).isEqualTo("Find User");
                    assertThat(foundUser.getBirthday()).isEqualTo(LocalDate.of(1999, 2, 2));
                });
    }

    @Test
    void shouldReturnEmptyOptionalWhenUserNotFound() {
        Optional<User> userOptional = userDbStorage.findById(999L);
        assertThat(userOptional).isEmpty();
    }

    @Test
    void shouldUpdateUser() {
        User user = new User();
        user.setEmail("old@mail.com");
        user.setLogin("old-login");
        user.setName("Old Name");
        user.setBirthday(LocalDate.of(1990, 5, 5));

        User createdUser = userDbStorage.create(user);

        createdUser.setEmail("new@mail.com");
        createdUser.setLogin("new-login");
        createdUser.setName("New Name");
        createdUser.setBirthday(LocalDate.of(2001, 6, 6));

        User updatedUser = userDbStorage.update(createdUser);

        assertThat(updatedUser.getId()).isEqualTo(createdUser.getId());

        Optional<User> userFromDb = userDbStorage.findById(createdUser.getId());

        assertThat(userFromDb)
                .isPresent()
                .hasValueSatisfying(foundUser -> {
                    assertThat(foundUser.getEmail()).isEqualTo("new@mail.com");
                    assertThat(foundUser.getLogin()).isEqualTo("new-login");
                    assertThat(foundUser.getName()).isEqualTo("New Name");
                    assertThat(foundUser.getBirthday()).isEqualTo(LocalDate.of(2001, 6, 6));
                });
    }

    @Test
    void shouldFindAllUsers() {
        User firstUser = new User();
        firstUser.setEmail("first@mail.com");
        firstUser.setLogin("first-login");
        firstUser.setName("First User");
        firstUser.setBirthday(LocalDate.of(1991, 1, 1));

        User secondUser = new User();
        secondUser.setEmail("second@mail.com");
        secondUser.setLogin("second-login");
        secondUser.setName("Second User");
        secondUser.setBirthday(LocalDate.of(1993, 3, 3));

        userDbStorage.create(firstUser);
        userDbStorage.create(secondUser);

        Collection<User> users = userDbStorage.findAll();

        assertThat(users).hasSize(2);
        assertThat(users)
                .extracting(User::getEmail)
                .contains("first@mail.com", "second@mail.com");
    }

    @Test
    void shouldAddFriendOneWay() {
        User firstUser = new User();
        firstUser.setEmail("first@mail.com");
        firstUser.setLogin("first-login");
        firstUser.setName("First User");
        firstUser.setBirthday(LocalDate.of(1991, 1, 1));
        User createdFirstUser = userDbStorage.create(firstUser);

        User secondUser = new User();
        secondUser.setEmail("second@mail.com");
        secondUser.setLogin("second-login");
        secondUser.setName("Second User");
        secondUser.setBirthday(LocalDate.of(1992, 2, 2));
        User createdSecondUser = userDbStorage.create(secondUser);

        userDbStorage.addFriend(createdFirstUser.getId(), createdSecondUser.getId());

        Collection<User> firstUserFriends = userDbStorage.getFriends(createdFirstUser.getId());
        Collection<User> secondUserFriends = userDbStorage.getFriends(createdSecondUser.getId());

        assertThat(firstUserFriends)
                .extracting(User::getId)
                .containsExactly(createdSecondUser.getId());

        assertThat(secondUserFriends).isEmpty();
    }

    @Test
    void shouldRemoveFriend() {
        User firstUser = new User();
        firstUser.setEmail("first@mail.com");
        firstUser.setLogin("first-login");
        firstUser.setName("First User");
        firstUser.setBirthday(LocalDate.of(1991, 1, 1));
        User createdFirstUser = userDbStorage.create(firstUser);

        User secondUser = new User();
        secondUser.setEmail("second@mail.com");
        secondUser.setLogin("second-login");
        secondUser.setName("Second User");
        secondUser.setBirthday(LocalDate.of(1992, 2, 2));
        User createdSecondUser = userDbStorage.create(secondUser);

        userDbStorage.addFriend(createdFirstUser.getId(), createdSecondUser.getId());
        userDbStorage.removeFriend(createdFirstUser.getId(), createdSecondUser.getId());

        Collection<User> friends = userDbStorage.getFriends(createdFirstUser.getId());

        assertThat(friends).isEmpty();
    }

    @Test
    void shouldGetCommonFriends() {
        User firstUser = new User();
        firstUser.setEmail("first@mail.com");
        firstUser.setLogin("first-login");
        firstUser.setName("First User");
        firstUser.setBirthday(LocalDate.of(1991, 1, 1));
        User createdFirstUser = userDbStorage.create(firstUser);

        User secondUser = new User();
        secondUser.setEmail("second@mail.com");
        secondUser.setLogin("second-login");
        secondUser.setName("Second User");
        secondUser.setBirthday(LocalDate.of(1992, 2, 2));
        User createdSecondUser = userDbStorage.create(secondUser);

        User commonFriend = new User();
        commonFriend.setEmail("common@mail.com");
        commonFriend.setLogin("common-login");
        commonFriend.setName("Common Friend");
        commonFriend.setBirthday(LocalDate.of(1993, 3, 3));
        User createdCommonFriend = userDbStorage.create(commonFriend);

        userDbStorage.addFriend(createdFirstUser.getId(), createdCommonFriend.getId());
        userDbStorage.addFriend(createdSecondUser.getId(), createdCommonFriend.getId());

        Collection<User> commonFriends =
                userDbStorage.getCommonFriends(createdFirstUser.getId(), createdSecondUser.getId());

        assertThat(commonFriends)
                .extracting(User::getId)
                .containsExactly(createdCommonFriend.getId());
    }
}