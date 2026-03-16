package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@Qualifier("filmDbStorage")
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {

    private static final String FIND_ALL_QUERY = """
            SELECT f.*, m.id AS mpa_id, m.name AS mpa_name
            FROM films f
            LEFT JOIN mpa m ON f.mpa_id = m.id
            ORDER BY f.id
            """;

    private static final String FIND_BY_ID_QUERY = """
            SELECT f.*, m.id AS mpa_id, m.name AS mpa_name
            FROM films f
            LEFT JOIN mpa m ON f.mpa_id = m.id
            WHERE f.id = ?
            """;

    private static final String INSERT_QUERY = """
            INSERT INTO films (name, description, release_date, duration, mpa_id)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_QUERY = """
            UPDATE films
            SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ?
            WHERE id = ?
            """;

    private static final String DELETE_FILM_GENRES_QUERY = """
            DELETE FROM film_genres
            WHERE film_id = ?
            """;

    private static final String INSERT_FILM_GENRE_QUERY = """
            INSERT INTO film_genres (film_id, genre_id)
            VALUES (?, ?)
            """;

    private static final String FIND_GENRES_BY_FILM_ID_QUERY = """
            SELECT g.id, g.name
            FROM film_genres fg
            JOIN genres g ON fg.genre_id = g.id
            WHERE fg.film_id = ?
            ORDER BY g.id
            """;

    private static final String DELETE_FILM_LIKES_QUERY = """
            DELETE FROM film_likes
            WHERE film_id = ?
            """;

    private static final String INSERT_FILM_LIKE_QUERY = """
            INSERT INTO film_likes (film_id, user_id)
            VALUES (?, ?)
            """;

    private static final String FIND_LIKES_BY_FILM_ID_QUERY = """
            SELECT user_id
            FROM film_likes
            WHERE film_id = ?
            ORDER BY user_id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final FilmRowMapper filmRowMapper;

    @Override
    public Collection<Film> findAll() {
        List<Film> films = jdbcTemplate.query(FIND_ALL_QUERY, filmRowMapper);
        films.forEach(film -> {
            loadGenres(film);
            loadLikes(film);
        });
        return films;
    }

    @Override
    public Film create(Film film) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, film.getName());
            statement.setString(2, film.getDescription());
            statement.setDate(3, Date.valueOf(film.getReleaseDate()));
            statement.setInt(4, film.getDuration());

            if (film.getMpa() != null && film.getMpa().getId() != null) {
                statement.setInt(5, film.getMpa().getId());
            } else {
                statement.setNull(5, Types.INTEGER);
            }

            return statement;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            film.setId(keyHolder.getKey().longValue());
        }

        saveGenres(film);
        saveLikes(film);
        loadGenres(film);
        loadLikes(film);

        return film;
    }

    @Override
    public Film update(Film film) {
        jdbcTemplate.update(
                UPDATE_QUERY,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                film.getMpa() != null ? film.getMpa().getId() : null,
                film.getId()
        );

        jdbcTemplate.update(DELETE_FILM_GENRES_QUERY, film.getId());
        jdbcTemplate.update(DELETE_FILM_LIKES_QUERY, film.getId());

        saveGenres(film);
        saveLikes(film);
        loadGenres(film);
        loadLikes(film);

        return film;
    }

    @Override
    public Optional<Film> findById(Long id) {
        try {
            Film film = jdbcTemplate.queryForObject(FIND_BY_ID_QUERY, filmRowMapper, id);
            if (film != null) {
                loadGenres(film);
                loadLikes(film);
            }
            return Optional.ofNullable(film);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private void saveGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }

        Set<Integer> uniqueGenreIds = new HashSet<>();
        for (Genre genre : film.getGenres()) {
            if (genre != null && genre.getId() != null) {
                uniqueGenreIds.add(genre.getId());
            }
        }

        for (Integer genreId : uniqueGenreIds) {
            jdbcTemplate.update(INSERT_FILM_GENRE_QUERY, film.getId(), genreId);
        }
    }

    private void loadGenres(Film film) {
        List<Genre> genres = jdbcTemplate.query(
                FIND_GENRES_BY_FILM_ID_QUERY,
                (rs, rowNum) -> new Genre(rs.getInt("id"), rs.getString("name")),
                film.getId()
        );

        film.setGenres(new HashSet<>(genres));
    }

    private void saveLikes(Film film) {
        if (film.getLikes() == null || film.getLikes().isEmpty()) {
            return;
        }

        Set<Long> uniqueUserIds = new HashSet<>(film.getLikes());

        for (Long userId : uniqueUserIds) {
            jdbcTemplate.update(INSERT_FILM_LIKE_QUERY, film.getId(), userId);
        }
    }

    private void loadLikes(Film film) {
        List<Long> userIds = jdbcTemplate.query(
                FIND_LIKES_BY_FILM_ID_QUERY,
                (rs, rowNum) -> rs.getLong("user_id"),
                film.getId()
        );

        film.setLikes(new HashSet<>(userIds));
    }
}