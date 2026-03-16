package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FilmServiceLikesTest {

    private FilmService filmService;
    private UserStorage userStorage;

    @BeforeEach
    void setUp() {
        FilmStorage filmStorage = new InMemoryFilmStorage();
        userStorage = new InMemoryUserStorage();
        GenreStorage genreStorage = mock(GenreStorage.class);
        MpaStorage mpaStorage = mock(MpaStorage.class);

        when(mpaStorage.findById(anyInt())).thenReturn(Optional.of(new Mpa(1, "G")));

        filmService = new FilmService(filmStorage, userStorage, genreStorage, mpaStorage);
    }

    private User newUser(String email, String login) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName(login);
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return userStorage.create(user);
    }

    private Film newFilm(String name) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("desc");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(new Mpa(1, "G"));
        return filmService.create(film);
    }

    @Test
    void shouldAddLikeOnceWhenAddLikeTwice() {
        User user = newUser("mail1@example.com", "user1");
        Film film = newFilm("Film");

        filmService.addLike(film.getId(), user.getId());
        filmService.addLike(film.getId(), user.getId());

        Film updated = filmService.findById(film.getId());
        assertEquals(1, updated.getLikes().size());
    }

    @Test
    void shouldRemoveLikeWhenRemoveLike() {
        User user = newUser("mail1@example.com", "user1");
        Film film = newFilm("Film");

        filmService.addLike(film.getId(), user.getId());
        filmService.removeLike(film.getId(), user.getId());

        Film updated = filmService.findById(film.getId());
        assertEquals(0, updated.getLikes().size());
    }

    @Test
    void shouldReturnFilmsSortedByLikesWhenGetPopular() {
        User user1 = newUser("mail1@example.com", "user1");
        User user2 = newUser("mail2@example.com", "user2");

        Film film1 = newFilm("F1");
        Film film2 = newFilm("F2");
        Film film3 = newFilm("F3");

        filmService.addLike(film2.getId(), user1.getId());
        filmService.addLike(film2.getId(), user2.getId());
        filmService.addLike(film1.getId(), user1.getId());

        List<Film> popular = filmService.getPopular(10).stream().toList();

        assertEquals(
                List.of(film2.getId(), film1.getId(), film3.getId()),
                popular.stream().map(Film::getId).toList()
        );
    }

    @Test
    void shouldThrowValidationExceptionWhenGetPopularWithInvalidCount() {
        assertThrows(ValidationException.class, () -> filmService.getPopular(0));
        assertThrows(ValidationException.class, () -> filmService.getPopular(-1));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenAddLikeWithMissingUser() {
        Film film = newFilm("Film");
        assertThrows(NotFoundException.class, () -> filmService.addLike(film.getId(), 999L));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenAddLikeWithMissingFilm() {
        User user = newUser("mail1@example.com", "user1");
        assertThrows(NotFoundException.class, () -> filmService.addLike(999L, user.getId()));
    }

    @Test
    void shouldThrowValidationExceptionWhenCountIsNegativeInGetPopularFilms() {
        assertThrows(ValidationException.class, () -> filmService.getPopular(-1));
    }

    @Test
    void shouldThrowValidationExceptionWhenCountIsZeroInGetPopularFilms() {
        assertThrows(ValidationException.class, () -> filmService.getPopular(0));
    }

    @Test
    void shouldReturnFilmWithMostLikesWhenGetPopularFilms() {
        User user = newUser("mail1@example.com", "user1");

        Film film1 = newFilm("Film1");
        Film film2 = newFilm("Film2");

        filmService.addLike(film1.getId(), user.getId());

        Film result = filmService.getPopular(1).iterator().next();

        assertEquals(film1.getId(), result.getId());
        assertEquals(1, result.getLikes().size());
    }
}