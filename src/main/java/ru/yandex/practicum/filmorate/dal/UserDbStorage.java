package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.UserRowMapper;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.Optional;

@Repository
@Qualifier("userDbStorage")
@RequiredArgsConstructor
public class UserDbStorage implements UserStorage {

    private static final String FIND_ALL_QUERY = """
            SELECT *
            FROM users
            ORDER BY id
            """;

    private static final String FIND_BY_ID_QUERY = """
            SELECT *
            FROM users
            WHERE id = ?
            """;

    private static final String INSERT_QUERY = """
            INSERT INTO users (email, login, name, birthday)
            VALUES (?, ?, ?, ?)
            """;

    private static final String UPDATE_QUERY = """
            UPDATE users
            SET email = ?, login = ?, name = ?, birthday = ?
            WHERE id = ?
            """;

    private static final String INSERT_FRIENDSHIP_QUERY = """
            INSERT INTO friendships (user_id, friend_id)
            VALUES (?, ?)
            """;

    private static final String DELETE_FRIENDSHIP_QUERY = """
            DELETE FROM friendships
            WHERE user_id = ? AND friend_id = ?
            """;

    private static final String FIND_FRIENDS_QUERY = """
            SELECT u.*
            FROM users u
            JOIN friendships f ON u.id = f.friend_id
            WHERE f.user_id = ?
            ORDER BY u.id
            """;

    private static final String FIND_COMMON_FRIENDS_QUERY = """
            SELECT u.*
            FROM users u
            JOIN friendships f1 ON u.id = f1.friend_id
            JOIN friendships f2 ON u.id = f2.friend_id
            WHERE f1.user_id = ? AND f2.user_id = ?
            ORDER BY u.id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final UserRowMapper userRowMapper;

    @Override
    public Collection<User> findAll() {
        return jdbcTemplate.query(FIND_ALL_QUERY, userRowMapper);
    }

    @Override
    public User create(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, user.getEmail());
            statement.setString(2, user.getLogin());
            statement.setString(3, user.getName());
            statement.setDate(4, Date.valueOf(user.getBirthday()));
            return statement;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            user.setId(keyHolder.getKey().longValue());
        }

        return user;
    }

    @Override
    public User update(User user) {
        jdbcTemplate.update(
                UPDATE_QUERY,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                Date.valueOf(user.getBirthday()),
                user.getId()
        );

        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        try {
            User user = jdbcTemplate.queryForObject(FIND_BY_ID_QUERY, userRowMapper, id);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        jdbcTemplate.update(INSERT_FRIENDSHIP_QUERY, userId, friendId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        jdbcTemplate.update(DELETE_FRIENDSHIP_QUERY, userId, friendId);
    }

    @Override
    public Collection<User> getFriends(Long userId) {
        return jdbcTemplate.query(FIND_FRIENDS_QUERY, userRowMapper, userId);
    }

    @Override
    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        return jdbcTemplate.query(FIND_COMMON_FRIENDS_QUERY, userRowMapper, userId, otherId);
    }
}