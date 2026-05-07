Лабораторная 4

# Плохие практики CI/CD

## Отсутствие кэширования

Зависимости скачиваются заново при каждом запуске pipeline, из-за чего сборка работает медленнее.

```yaml
- name: Set up Java 21 without cache
  uses: actions/setup-java@v4
  with:
    distribution: temurin
    java-version: 21
```

---

## Отсутствие rollback

При неудачном деплое нельзя автоматически вернуться к предыдущей стабильной версии приложения.

```yaml
- name: Deploy simulation without rollback
  run: |
    echo "Deploying application..."
    echo "No rollback configured"
```

---

## Отсутствие мониторинга и метрик

Pipeline не собирает информацию о времени сборки, ошибках и результатах тестов.

```yaml
- name: No metrics
  run: |
    echo "Pipeline finished"
```

---

## Flaky tests

Тесты могут случайно проходить или падать без изменений в коде, из-за чего pipeline становится ненадёжным.

```yaml
- name: Flaky test simulation
  run: |
    NUMBER=$(( RANDOM % 2 ))
    if [ "$NUMBER" -eq 0 ]; then
      echo "Random failure"
      exit 1
    fi
    echo "Random success"
```

---

## Отсутствие параллелизации

Все этапы pipeline выполняются последовательно, что увеличивает время выполнения CI/CD.

```yaml
- name: Run everything sequentially
  run: |
    mvn -B compile
    mvn -B test
    mvn -B package
```

# Исправление плохих практик CI/CD

## Отсутствие кэширования

В плохом pipeline Maven зависимости скачивались заново при каждом запуске.

Исправлено:

```yaml
- name: Set up Java 21 with Maven cache
  uses: actions/setup-java@v4
  with:
    distribution: temurin
    java-version: 21
    cache: maven
```

Maven использует cache и не скачивает зависимости заново при каждом запуске pipeline.

---

## Отсутствие rollback

В плохом pipeline отсутствовала возможность восстановления предыдущей версии приложения.

Исправлено:

```yaml
- name: Upload rollback artifact
  uses: actions/upload-artifact@v4
  with:
    name: rollback-version
    path: previous-version/app.jar
```

Теперь предыдущая версия приложения сохраняется как artifact и может быть использована для rollback.

---

## Отсутствие мониторинга и метрик

В плохом pipeline отсутствовала информация о выполнении pipeline.

Исправлено:

```yaml
- name: Add build metrics
  run: |
    echo "## Build metrics" >> $GITHUB_STEP_SUMMARY
    echo "- Maven cache enabled" >> $GITHUB_STEP_SUMMARY
```

Теперь GitHub Actions отображает краткие метрики и информацию о выполнении pipeline.

---

## Flaky tests

В плохом pipeline использовался случайный тест, который мог завершаться ошибкой без изменений в коде.

Исправлено:

```yaml
- name: Run stable tests
  run: mvn -B test
```

Теперь pipeline использует только стабильные тесты проекта.

---

## Отсутствие параллелизации

В плохом pipeline все этапы выполнялись последовательно.

Исправлено:

```yaml
jobs:
  compile:
  tests:
```

Сборка запускается только после успешного завершения compile и тестов:

```yaml
needs:
  - compile
  - tests
```

Теперь jobs выполняются параллельно, что ускоряет выполнение pipeline.
![alt text](image.png)

## Почему хранение секретов в CI/CD переменных репозитория не является хорошей практикой

- Секреты привязаны к одному конкретному репозиторию.
- Ими неудобно управлять и сложно контроллировать, если проектов становится много.
- Изменения секретов возможны только вручную.
- Секрет может "утечь" из-за ошибок в pipeline
- Настроить гибкие политики доступа становится сложнее.
