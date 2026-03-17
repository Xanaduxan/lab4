package ru.yandex.practicum.filmorate.storage.film;


import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface FilmStorage {

    Collection<Film> findAll();

    Film create(Film film);

    Film update(Film film);

    Optional<Film> findById(Long id);

    void addLike(Long filmId, Long userId);

    void removeLike(Long filmId, Long userId);

    Collection<Film> getPopular(int count);

    Map<Long, Set<Long>> getLikesByFilmIds(Collection<Long> filmIds);

}
