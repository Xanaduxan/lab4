package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dal.mappers.MpaRowMapper;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import({MpaDbStorage.class, MpaRowMapper.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class MpaDbStorageTest {

    private final MpaDbStorage mpaDbStorage;

    @Test
    void shouldFindAllMpa() {
        Collection<Mpa> mpaList = mpaDbStorage.findAll();

        assertThat(mpaList).hasSize(5);
    }

    @Test
    void shouldFindMpaById() {
        Optional<Mpa> mpaOptional = mpaDbStorage.findById(1);

        assertThat(mpaOptional)
                .isPresent()
                .hasValueSatisfying(mpa -> {
                    assertThat(mpa.getId()).isEqualTo(1);
                    assertThat(mpa.getName()).isEqualTo("G");
                });
    }

    @Test
    void shouldReturnEmptyOptionalWhenMpaNotFound() {
        Optional<Mpa> mpaOptional = mpaDbStorage.findById(999);

        assertThat(mpaOptional).isEmpty();
    }
}