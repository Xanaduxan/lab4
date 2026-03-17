package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final GenreStorage genreStorage;
    private final MpaStorage mpaStorage;

    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       @Qualifier("genreDbStorage") GenreStorage genreStorage,
                       MpaStorage mpaStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.genreStorage = genreStorage;
        this.mpaStorage = mpaStorage;
    }

    public Collection<Film> findAll() {
        log.info("Запрос вывода всех фильмов");
        Collection<Film> films = filmStorage.findAll();
        enrichFilms(films);
        return films;
    }

    public Film create(Film film) {
        log.info("Запрос на создание фильма");
        validateFilm(film);
        validateMpa(film);
        prepareGenres(film);

        return filmStorage.create(film);
    }

    public Film update(Film film) {
        log.info("Запрос на обновление фильма с id={}", film.getId());
        getFilmOrThrow(film.getId());
        validateFilm(film);
        validateMpa(film);
        prepareGenres(film);

        return filmStorage.update(film);
    }

    public Film findById(Long id) {
        Film film = getFilmOrThrow(id);
        enrichFilms(java.util.List.of(film));
        return film;
    }

    public void addLike(Long filmId, Long userId) {
        log.info("Добавление лайка пользователем с id={} к фильму с id={}", userId, filmId);

        checkUserExists(userId);
        getFilmOrThrow(filmId);

        filmStorage.addLike(filmId, userId);

        log.info("Лайк к фильму с id={} от пользователя с id={} добавлен", filmId, userId);
    }

    public void removeLike(Long filmId, Long userId) {
        log.info("Удаление лайка пользователя с id={} к фильму с id={}", userId, filmId);

        checkUserExists(userId);
        getFilmOrThrow(filmId);

        filmStorage.removeLike(filmId, userId);

        log.info("Лайк к фильму с id={} от пользователя с id={} удален", filmId, userId);
    }

    public Collection<Film> getPopular(int count) {
        log.info("Запрос популярных фильмов: count={}", count);

        if (count <= 0) {
            throw new ValidationException("Параметр count должен быть положительным");
        }

        Collection<Film> films = filmStorage.getPopular(count);
        enrichFilms(films);
        return films;
    }

    private void enrichFilms(Collection<Film> films) {
        if (films == null || films.isEmpty()) {
            return;
        }

        Set<Long> filmIds = films.stream()
                .map(Film::getId)
                .collect(Collectors.toSet());

        Map<Long, Set<Genre>> genresByFilmIds = genreStorage.getGenresByFilmIds(filmIds);
        Map<Long, Set<Long>> likesByFilmIds = filmStorage.getLikesByFilmIds(filmIds);

        for (Film film : films) {
            film.setGenres(new LinkedHashSet<>(
                    genresByFilmIds.getOrDefault(film.getId(), Set.of())
            ));
            film.setLikes(new HashSet<>(
                    likesByFilmIds.getOrDefault(film.getId(), Set.of())
            ));
        }
    }

    private void validateFilm(Film film) {
        String name = film.getName();

        if (name == null || name.isBlank()) {
            throw new ValidationException("Название не может быть пустым");
        }

        String description = film.getDescription();

        if (description != null && description.length() > 200) {
            throw new ValidationException("Максимальная длина описания — 200 символов");
        }

        LocalDate releaseDate = film.getReleaseDate();

        if (releaseDate == null || releaseDate.isBefore(LocalDate.of(1895, 12, 28))) {
            throw new ValidationException("Дата релиза — не раньше 28 декабря 1895 года");
        }

        if (releaseDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Дата релиза не может быть в будущем");
        }

        if (film.getDuration() <= 0) {
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }

    private void validateMpa(Film film) {
        if (film.getMpa() == null || film.getMpa().getId() == null) {
            throw new ValidationException("У фильма должен быть указан рейтинг");
        }

        mpaStorage.findById(film.getMpa().getId())
                .orElseThrow(() -> new NotFoundException("Рейтинг с id=" + film.getMpa().getId() + " не найден"));
    }

    private void prepareGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }

        Set<Genre> uniqueGenres = new LinkedHashSet<>();

        for (Genre genre : film.getGenres()) {
            if (genre == null || genre.getId() == null) {
                throw new ValidationException("У жанра должен быть указан id");
            }

            Genre existingGenre = genreStorage.findById(genre.getId())
                    .orElseThrow(() -> new NotFoundException("Жанр с id=" + genre.getId() + " не найден"));

            uniqueGenres.add(existingGenre);
        }

        film.setGenres(uniqueGenres);
    }

    private Film getFilmOrThrow(Long filmId) {
        return filmStorage.findById(filmId)
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + filmId + " не найден"));
    }

    private void checkUserExists(Long userId) {
        userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }
}