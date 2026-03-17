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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
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

    private static final String DELETE_FILM_LIKE_QUERY = """
            DELETE FROM film_likes
            WHERE film_id = ? AND user_id = ?
            """;

    private static final String INSERT_FILM_LIKE_QUERY = """
            INSERT INTO film_likes (film_id, user_id)
            VALUES (?, ?)
            """;

    private static final String FIND_POPULAR_QUERY = """
            SELECT f.*, m.id AS mpa_id, m.name AS mpa_name,
                   COUNT(fl.user_id) AS likes_count
            FROM films f
            LEFT JOIN mpa m ON f.mpa_id = m.id
            LEFT JOIN film_likes fl ON f.id = fl.film_id
            GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id, m.id, m.name
            ORDER BY likes_count DESC, f.id
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final FilmRowMapper filmRowMapper;

    @Override
    public Collection<Film> findAll() {
        return jdbcTemplate.query(FIND_ALL_QUERY, filmRowMapper);
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
        film.setLikes(new HashSet<>());

        return film;
    }

    @Override
    public Film update(Film film) {
        jdbcTemplate.update(UPDATE_QUERY, film.getName(), film.getDescription(), Date.valueOf(film.getReleaseDate()), film.getDuration(), film.getMpa() != null ? film.getMpa().getId() : null, film.getId());

        jdbcTemplate.update(DELETE_FILM_GENRES_QUERY, film.getId());
        saveGenres(film);

        return film;
    }

    @Override
    public Optional<Film> findById(Long id) {
        try {
            Film film = jdbcTemplate.queryForObject(FIND_BY_ID_QUERY, filmRowMapper, id);
            return Optional.ofNullable(film);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        jdbcTemplate.update(INSERT_FILM_LIKE_QUERY, filmId, userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        jdbcTemplate.update(DELETE_FILM_LIKE_QUERY, filmId, userId);
    }

    @Override
    public Collection<Film> getPopular(int count) {
        return jdbcTemplate.query(FIND_POPULAR_QUERY, filmRowMapper, count);
    }

    @Override
    public Map<Long, Set<Long>> getLikesByFilmIds(Collection<Long> filmIds) {
        if (filmIds == null || filmIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String placeholders = String.join(",", Collections.nCopies(filmIds.size(), "?"));
        String query = """
                SELECT film_id, user_id
                FROM film_likes
                WHERE film_id IN (%s)
                ORDER BY film_id, user_id
                """.formatted(placeholders);

        Map<Long, Set<Long>> likesByFilmId = new HashMap<>();

        jdbcTemplate.query(query, rs -> {
            Long filmId = rs.getLong("film_id");
            Long userId = rs.getLong("user_id");
            likesByFilmId.computeIfAbsent(filmId, id -> new HashSet<>()).add(userId);
        }, filmIds.toArray());

        return likesByFilmId;
    }

    private void saveGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }

        Set<Integer> uniqueGenreIds = new LinkedHashSet<>();
        for (Genre genre : film.getGenres()) {
            if (genre != null && genre.getId() != null) {
                uniqueGenreIds.add(genre.getId());
            }
        }

        for (Integer genreId : uniqueGenreIds) {
            jdbcTemplate.update(INSERT_FILM_GENRE_QUERY, film.getId(), genreId);
        }
    }
}