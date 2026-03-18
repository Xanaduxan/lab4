INSERT INTO mpa (id, name)
SELECT 1, 'G'
    WHERE NOT EXISTS (SELECT 1 FROM mpa WHERE id = 1);

INSERT INTO mpa (id, name)
SELECT 2, 'PG'
    WHERE NOT EXISTS (SELECT 1 FROM mpa WHERE id = 2);

INSERT INTO mpa (id, name)
SELECT 3, 'PG-13'
    WHERE NOT EXISTS (SELECT 1 FROM mpa WHERE id = 3);

INSERT INTO mpa (id, name)
SELECT 4, 'R'
    WHERE NOT EXISTS (SELECT 1 FROM mpa WHERE id = 4);

INSERT INTO mpa (id, name)
SELECT 5, 'NC-17'
    WHERE NOT EXISTS (SELECT 1 FROM mpa WHERE id = 5);

INSERT INTO genres (id, name)
SELECT 1, 'Комедия'
    WHERE NOT EXISTS (SELECT 1 FROM genres WHERE id = 1);

INSERT INTO genres (id, name)
SELECT 2, 'Драма'
    WHERE NOT EXISTS (SELECT 1 FROM genres WHERE id = 2);

INSERT INTO genres (id, name)
SELECT 3, 'Мультфильм'
    WHERE NOT EXISTS (SELECT 1 FROM genres WHERE id = 3);

INSERT INTO genres (id, name)
SELECT 4, 'Триллер'
    WHERE NOT EXISTS (SELECT 1 FROM genres WHERE id = 4);

INSERT INTO genres (id, name)
SELECT 5, 'Документальный'
    WHERE NOT EXISTS (SELECT 1 FROM genres WHERE id = 5);

INSERT INTO genres (id, name)
SELECT 6, 'Боевик'
    WHERE NOT EXISTS (SELECT 1 FROM genres WHERE id = 6);