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
import java.util.Set;

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
                    assertThat(foundUser.getFriends()).isEmpty();
                });
    }

    @Test
    void shouldFindUserById() {
        User friend = new User();
        friend.setEmail("friend@mail.com");
        friend.setLogin("friend-login");
        friend.setName("Friend User");
        friend.setBirthday(LocalDate.of(1998, 1, 1));
        User createdFriend = userDbStorage.create(friend);

        User user = new User();
        user.setEmail("find@mail.com");
        user.setLogin("find-login");
        user.setName("Find User");
        user.setBirthday(LocalDate.of(1999, 2, 2));
        user.setFriends(Set.of(createdFriend.getId()));

        User createdUser = userDbStorage.create(user);

        Optional<User> userOptional = userDbStorage.findById(createdUser.getId());

        assertThat(userOptional)
                .isPresent()
                .hasValueSatisfying(foundUser -> {
                    assertThat(foundUser.getId()).isEqualTo(createdUser.getId());
                    assertThat(foundUser.getEmail()).isEqualTo("find@mail.com");
                    assertThat(foundUser.getLogin()).isEqualTo("find-login");
                    assertThat(foundUser.getName()).isEqualTo("Find User");
                    assertThat(foundUser.getFriends()).containsExactly(createdFriend.getId());
                });
    }

    @Test
    void shouldReturnEmptyOptionalWhenUserNotFound() {
        Optional<User> userOptional = userDbStorage.findById(999L);
        assertThat(userOptional).isEmpty();
    }

    @Test
    void shouldUpdateUser() {
        User firstFriend = new User();
        firstFriend.setEmail("first-friend@mail.com");
        firstFriend.setLogin("first-friend-login");
        firstFriend.setName("First Friend");
        firstFriend.setBirthday(LocalDate.of(1990, 1, 1));
        User createdFirstFriend = userDbStorage.create(firstFriend);

        User secondFriend = new User();
        secondFriend.setEmail("second-friend@mail.com");
        secondFriend.setLogin("second-friend-login");
        secondFriend.setName("Second Friend");
        secondFriend.setBirthday(LocalDate.of(1991, 1, 1));
        User createdSecondFriend = userDbStorage.create(secondFriend);

        User user = new User();
        user.setEmail("old@mail.com");
        user.setLogin("old-login");
        user.setName("Old Name");
        user.setBirthday(LocalDate.of(1990, 5, 5));
        user.setFriends(Set.of(createdFirstFriend.getId()));

        User createdUser = userDbStorage.create(user);

        createdUser.setEmail("new@mail.com");
        createdUser.setLogin("new-login");
        createdUser.setName("New Name");
        createdUser.setBirthday(LocalDate.of(2001, 6, 6));
        createdUser.setFriends(Set.of(createdSecondFriend.getId()));

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
                    assertThat(foundUser.getFriends()).containsExactly(createdSecondFriend.getId());
                });
    }

    @Test
    void shouldFindAllUsers() {
        User friend = new User();
        friend.setEmail("friend@mail.com");
        friend.setLogin("friend-login");
        friend.setName("Friend User");
        friend.setBirthday(LocalDate.of(1992, 2, 2));
        User createdFriend = userDbStorage.create(friend);

        User firstUser = new User();
        firstUser.setEmail("first@mail.com");
        firstUser.setLogin("first-login");
        firstUser.setName("First User");
        firstUser.setBirthday(LocalDate.of(1991, 1, 1));
        firstUser.setFriends(Set.of(createdFriend.getId()));

        User secondUser = new User();
        secondUser.setEmail("second@mail.com");
        secondUser.setLogin("second-login");
        secondUser.setName("Second User");
        secondUser.setBirthday(LocalDate.of(1993, 3, 3));

        userDbStorage.create(firstUser);
        userDbStorage.create(secondUser);

        Collection<User> users = userDbStorage.findAll();

        assertThat(users).hasSize(3);
        assertThat(users)
                .filteredOn(user -> user.getEmail().equals("first@mail.com"))
                .singleElement()
                .satisfies(user -> assertThat(user.getFriends()).containsExactly(createdFriend.getId()));
    }

    @Test
    void shouldStoreFriendshipAsOneWay() {
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

        createdFirstUser.setFriends(Set.of(createdSecondUser.getId()));
        userDbStorage.update(createdFirstUser);

        User firstFromDb = userDbStorage.findById(createdFirstUser.getId()).orElseThrow();
        User secondFromDb = userDbStorage.findById(createdSecondUser.getId()).orElseThrow();

        assertThat(firstFromDb.getFriends()).containsExactly(createdSecondUser.getId());
        assertThat(secondFromDb.getFriends()).isEmpty();
    }
}