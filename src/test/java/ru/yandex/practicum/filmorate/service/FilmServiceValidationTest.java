package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FilmServiceValidationTest {

    private FilmService filmService;

    @BeforeEach
    void setUp() {
        FilmStorage filmStorage = new InMemoryFilmStorage();
        UserStorage userStorage = new InMemoryUserStorage();
        GenreStorage genreStorage = mock(GenreStorage.class);
        MpaStorage mpaStorage = mock(MpaStorage.class);

        when(mpaStorage.findById(anyInt())).thenReturn(Optional.of(new Mpa(1, "G")));

        filmService = new FilmService(filmStorage, userStorage, genreStorage, mpaStorage);
    }

    @Test
    void shouldThrowValidationExceptionWhenNameIsEmpty() {
        Film film = createValidFilm();
        film.setName("");

        assertThrows(
                ValidationException.class,
                () -> filmService.create(film),
                "Ожидалось исключение при пустом name"
        );
    }

    @Test
    void shouldThrowValidationExceptionWhenDescriptionMore200Symbols() {
        Film film = createValidFilm();
        film.setDescription("a".repeat(201));

        assertThrows(
                ValidationException.class,
                () -> filmService.create(film),
                "Ожидалось исключение при description больше 200 символов"
        );
    }

    @Test
    void shouldNotThrowValidationExceptionWhenDescription200Symbols() {
        Film film = createValidFilm();
        film.setDescription("a".repeat(200));

        assertDoesNotThrow(
                () -> filmService.create(film),
                "Не должно быть исключения при description длиной 200 символов"
        );
    }

    @Test
    void shouldThrowValidationExceptionWhenReleaseDateEarlier28December1895() {
        Film film = createValidFilm();
        film.setReleaseDate(LocalDate.of(1000, 1, 1));

        assertThrows(
                ValidationException.class,
                () -> filmService.create(film),
                "Ожидалось исключение при releaseDate раньше 28 декабря 1895"
        );
    }

    @Test
    void shouldNotThrowValidationExceptionWhenReleaseDate28December1895() {
        Film film = createValidFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 28));

        assertDoesNotThrow(
                () -> filmService.create(film),
                "Дата 28 декабря 1895 должна быть допустимой"
        );
    }

    @Test
    void shouldThrowValidationExceptionWhenReleaseDateInTheFuture() {
        Film film = createValidFilm();
        film.setReleaseDate(LocalDate.of(3000, 12, 28));

        assertThrows(
                ValidationException.class,
                () -> filmService.create(film),
                "Ожидалось исключение при releaseDate в будущем"
        );
    }

    @Test
    void shouldThrowValidationExceptionWhenDurationIsNegative() {
        Film film = createValidFilm();
        film.setDuration(-1);

        assertThrows(
                ValidationException.class,
                () -> filmService.create(film),
                "Ожидалось исключение при отрицательном duration"
        );
    }

    @Test
    void shouldThrowValidationExceptionWhenDurationIsZero() {
        Film film = createValidFilm();
        film.setDuration(0);

        assertThrows(
                ValidationException.class,
                () -> filmService.create(film),
                "Ожидалось исключение при duration равном нулю"
        );
    }

    @Test
    void shouldNotThrowValidationExceptionWhenDurationIsPositive() {
        Film film = createValidFilm();
        film.setDuration(1);

        assertDoesNotThrow(
                () -> filmService.create(film),
                "Duration больше нуля допустимо"
        );
    }

    private Film createValidFilm() {
        Film film = new Film();
        film.setName("name");
        film.setDescription("description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(new Mpa(1, "G"));
        return film;
    }
}