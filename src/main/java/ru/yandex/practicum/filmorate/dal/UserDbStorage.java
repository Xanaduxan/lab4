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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@Qualifier("userDbStorage")
@RequiredArgsConstructor
public class UserDbStorage implements UserStorage {

    private static final String FIND_ALL_QUERY = "SELECT * FROM users ORDER BY id";
    private static final String FIND_BY_ID_QUERY = "SELECT * FROM users WHERE id = ?";
    private static final String INSERT_QUERY = """
            INSERT INTO users (email, login, name, birthday)
            VALUES (?, ?, ?, ?)
            """;
    private static final String UPDATE_QUERY = """
            UPDATE users
            SET email = ?, login = ?, name = ?, birthday = ?
            WHERE id = ?
            """;

    private static final String DELETE_FRIENDSHIPS_QUERY = """
            DELETE FROM friendships
            WHERE user_id = ?
            """;

    private static final String INSERT_FRIENDSHIP_QUERY = """
            INSERT INTO friendships (user_id, friend_id)
            VALUES (?, ?)
            """;

    private static final String FIND_FRIEND_IDS_QUERY = """
            SELECT friend_id
            FROM friendships
            WHERE user_id = ?
            ORDER BY friend_id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final UserRowMapper userRowMapper;

    @Override
    public Collection<User> findAll() {
        List<User> users = jdbcTemplate.query(FIND_ALL_QUERY, userRowMapper);
        users.forEach(this::loadFriends);
        return users;
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

        saveFriends(user);
        loadFriends(user);

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

        jdbcTemplate.update(DELETE_FRIENDSHIPS_QUERY, user.getId());
        saveFriends(user);
        loadFriends(user);

        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        try {
            User user = jdbcTemplate.queryForObject(FIND_BY_ID_QUERY, userRowMapper, id);
            if (user != null) {
                loadFriends(user);
            }
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private void saveFriends(User user) {
        if (user.getFriends() == null || user.getFriends().isEmpty()) {
            return;
        }

        Set<Long> uniqueFriendIds = new HashSet<>(user.getFriends());

        for (Long friendId : uniqueFriendIds) {
            jdbcTemplate.update(INSERT_FRIENDSHIP_QUERY, user.getId(), friendId);
        }
    }

    private void loadFriends(User user) {
        List<Long> friendIds = jdbcTemplate.query(
                FIND_FRIEND_IDS_QUERY,
                (rs, rowNum) -> rs.getLong("friend_id"),
                user.getId()
        );

        user.setFriends(new HashSet<>(friendIds));
    }
}