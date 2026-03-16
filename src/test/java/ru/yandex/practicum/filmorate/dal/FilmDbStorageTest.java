package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import({FilmDbStorage.class, FilmRowMapper.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class FilmDbStorageTest {

    private final FilmDbStorage filmDbStorage;

    @Test
    @DisplayName("Должен создавать фильм с рейтингом и жанрами")
    void shouldCreateFilmWithMpaAndGenres() {
        Film film = new Film();
        film.setName("Film 1");
        film.setDescription("Description 1");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(new Mpa(1, null));
        film.setGenres(Set.of(
                new Genre(1, null),
                new Genre(3, null)
        ));

        Film createdFilm = filmDbStorage.create(film);

        assertThat(createdFilm.getId()).isNotNull();

        Optional<Film> filmFromDb = filmDbStorage.findById(createdFilm.getId());

        assertThat(filmFromDb)
                .isPresent()
                .hasValueSatisfying(foundFilm -> {
                    assertThat(foundFilm.getName()).isEqualTo("Film 1");
                    assertThat(foundFilm.getDescription()).isEqualTo("Description 1");
                    assertThat(foundFilm.getReleaseDate()).isEqualTo(LocalDate.of(2000, 1, 1));
                    assertThat(foundFilm.getDuration()).isEqualTo(120);

                    assertThat(foundFilm.getMpa()).isNotNull();
                    assertThat(foundFilm.getMpa().getId()).isEqualTo(1);
                    assertThat(foundFilm.getMpa().getName()).isEqualTo("G");

                    assertThat(foundFilm.getGenres()).hasSize(2);
                    assertThat(foundFilm.getGenres())
                            .extracting(Genre::getId)
                            .containsExactlyInAnyOrder(1, 3);
                });
    }

    @Test
    void shouldFindFilmById() {
        Film film = new Film();
        film.setName("Find Film");
        film.setDescription("Film Description");
        film.setReleaseDate(LocalDate.of(2010, 10, 10));
        film.setDuration(90);
        film.setMpa(new Mpa(2, null));
        film.setGenres(Set.of(
                new Genre(2, null),
                new Genre(5, null)
        ));

        Film createdFilm = filmDbStorage.create(film);

        Optional<Film> filmOptional = filmDbStorage.findById(createdFilm.getId());

        assertThat(filmOptional)
                .isPresent()
                .hasValueSatisfying(foundFilm -> {
                    assertThat(foundFilm.getId()).isEqualTo(createdFilm.getId());
                    assertThat(foundFilm.getName()).isEqualTo("Find Film");
                    assertThat(foundFilm.getMpa()).isNotNull();
                    assertThat(foundFilm.getMpa().getId()).isEqualTo(2);
                    assertThat(foundFilm.getMpa().getName()).isEqualTo("PG");

                    assertThat(foundFilm.getGenres())
                            .extracting(Genre::getId)
                            .containsExactlyInAnyOrder(2, 5);
                });
    }

    @Test
    void shouldReturnEmptyOptionalWhenFilmNotFound() {
        Optional<Film> filmOptional = filmDbStorage.findById(999L);
        assertThat(filmOptional).isEmpty();
    }

    @Test
    void shouldUpdateFilm() {
        Film film = new Film();
        film.setName("Old Film");
        film.setDescription("Old Description");
        film.setReleaseDate(LocalDate.of(2005, 5, 5));
        film.setDuration(100);
        film.setMpa(new Mpa(1, null));
        film.setGenres(Set.of(
                new Genre(1, null),
                new Genre(3, null)
        ));

        Film createdFilm = filmDbStorage.create(film);

        createdFilm.setName("New Film");
        createdFilm.setDescription("New Description");
        createdFilm.setReleaseDate(LocalDate.of(2020, 2, 2));
        createdFilm.setDuration(150);
        createdFilm.setMpa(new Mpa(4, null));
        createdFilm.setGenres(Set.of(
                new Genre(4, null),
                new Genre(6, null)
        ));

        Film updatedFilm = filmDbStorage.update(createdFilm);

        assertThat(updatedFilm.getId()).isEqualTo(createdFilm.getId());

        Optional<Film> filmFromDb = filmDbStorage.findById(createdFilm.getId());

        assertThat(filmFromDb)
                .isPresent()
                .hasValueSatisfying(foundFilm -> {
                    assertThat(foundFilm.getName()).isEqualTo("New Film");
                    assertThat(foundFilm.getDescription()).isEqualTo("New Description");
                    assertThat(foundFilm.getReleaseDate()).isEqualTo(LocalDate.of(2020, 2, 2));
                    assertThat(foundFilm.getDuration()).isEqualTo(150);

                    assertThat(foundFilm.getMpa()).isNotNull();
                    assertThat(foundFilm.getMpa().getId()).isEqualTo(4);
                    assertThat(foundFilm.getMpa().getName()).isEqualTo("R");

                    assertThat(foundFilm.getGenres())
                            .extracting(Genre::getId)
                            .containsExactlyInAnyOrder(4, 6);
                });
    }

    @Test
    void shouldFindAllFilms() {
        Film firstFilm = new Film();
        firstFilm.setName("Film A");
        firstFilm.setDescription("Desc A");
        firstFilm.setReleaseDate(LocalDate.of(2001, 1, 1));
        firstFilm.setDuration(80);
        firstFilm.setMpa(new Mpa(1, null));
        firstFilm.setGenres(Set.of(new Genre(1, null)));

        Film secondFilm = new Film();
        secondFilm.setName("Film B");
        secondFilm.setDescription("Desc B");
        secondFilm.setReleaseDate(LocalDate.of(2002, 2, 2));
        secondFilm.setDuration(95);
        secondFilm.setMpa(new Mpa(3, null));
        secondFilm.setGenres(Set.of(new Genre(3, null), new Genre(5, null)));

        filmDbStorage.create(firstFilm);
        filmDbStorage.create(secondFilm);

        Collection<Film> films = filmDbStorage.findAll();

        assertThat(films).hasSize(2);
        assertThat(films)
                .extracting(Film::getMpa)
                .isNotEmpty();
    }
}