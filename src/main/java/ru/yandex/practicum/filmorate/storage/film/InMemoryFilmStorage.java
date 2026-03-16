package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@Qualifier("inMemoryFilmStorage")
public class InMemoryFilmStorage implements FilmStorage {

    private final Map<Long, Film> films = new HashMap<>();

    @Override
    public Collection<Film> findAll() {

        log.info("Запрос вывода всех фильмов");

        return films.values();
    }

    @Override
    public Film create(Film film) {

        film.setId(getNextId());

        films.put(film.getId(), film);

        log.info("Создан фильм с id={}", film.getId());

        return film;
    }

    @Override
    public Film update(Film film) {

        films.put(film.getId(), film);

        log.info("Обновлён фильм с id={}", film.getId());

        return film;
    }

    @Override
    public Optional<Film> findById(Long id) {

        log.info("Поиск фильма с id={}", id);

        return Optional.ofNullable(films.get(id));
    }

    private long getNextId() {

        return films.keySet().stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0) + 1;
    }
}