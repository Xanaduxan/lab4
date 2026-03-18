package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.dal.mappers.GenreRowMapper;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@Qualifier("genreDbStorage")
@RequiredArgsConstructor
public class GenreDbStorage implements GenreStorage {

    private static final String FIND_ALL_QUERY = """
            SELECT *
            FROM genres
            ORDER BY id
            """;

    private static final String FIND_BY_ID_QUERY = """
            SELECT *
            FROM genres
            WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final GenreRowMapper genreRowMapper;

    @Override
    public Collection<Genre> findAll() {
        return jdbcTemplate.query(FIND_ALL_QUERY, genreRowMapper);
    }

    @Override
    public Optional<Genre> findById(Integer id) {
        return jdbcTemplate.query(FIND_BY_ID_QUERY, genreRowMapper, id)
                .stream()
                .findFirst();
    }

    @Override
    public Map<Long, Set<Genre>> getGenresByFilmIds(Collection<Long> filmIds) {
        if (filmIds == null || filmIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String placeholders = String.join(",", Collections.nCopies(filmIds.size(), "?"));
        String query = """
                SELECT fg.film_id, g.id, g.name
                FROM film_genres fg
                JOIN genres g ON fg.genre_id = g.id
                WHERE fg.film_id IN (%s)
                ORDER BY fg.film_id, g.id
                """.formatted(placeholders);

        Map<Long, Set<Genre>> genresByFilmId = new HashMap<>();

        jdbcTemplate.query(query, rs -> {
            Long filmId = rs.getLong("film_id");
            Genre genre = new Genre(
                    rs.getInt("id"),
                    rs.getString("name")
            );

            genresByFilmId
                    .computeIfAbsent(filmId, id -> new LinkedHashSet<>())
                    .add(genre);
        }, filmIds.toArray());

        return genresByFilmId;
    }
}