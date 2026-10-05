# Deadline Dynamics: предыдущая итерация локализации

Исторический отчет за 1 октября 2026 года, до упрощения продукта и нового Data Explorer / progressive Lab. Он не описывает текущую композицию страниц. Актуальный отчет: [`PRODUCT_2026_10.md`](PRODUCT_2026_10.md), технический контракт: `../SITE_HANDOFF.md`. Проект не пересоздавался; темная дизайн-система и backend сохранялись.

## Исследовательская история

Главный вопрос: связан ли момент начала работы с тем, сдано ли одно задание вовремя? Обзор показывает источник данных, ключевое сравнение, распределение времени старта, дополнительные факторы и ограничения. Сравнение использует один выбранный срок и ответы без продления. До последнего дня и в последний день сравниваются по известным завершенным исходам; при недостатке данных вывод ограничивается или не строится.

Числа берутся из текущего набора / фильтра и `StatisticsService`. `demo` содержит 144 вымышленных наблюдения и не является исследовательским доказательством. `real` использует подходящие ответы PostgreSQL. Добровольная неслучайная выборка не представляет всех студентов. Наблюдаемая связь не доказывает причинность.

## Локализация

- `config/LocaleConfig.java`: `SessionLocaleResolver`, русский по умолчанию, `LocaleChangeInterceptor`, `?lang=ru|kk|en`, UTF-8 message source.
- `web/SiteText.java`: контекст языка и ссылки переключателя; сохранение query, включая фильтры и параметры симулятора; только URL-параметры, без POST-ответов / CSRF. Переводы вариантов по исходным кодам.
- `messages{,_ru,_kk,_en}.properties`: оболочка, controls, анкета, источник, вход, кабинет, ошибки и CSV report.
- `story{,_ru,_kk,_en}.properties`: исследовательский рассказ, обзор, частоты и методика.
- `experiment{,_ru,_kk,_en}.properties`: лаборатория, модель, интерпретации, controls и labels графиков.

Все страницы используют общие шаблоны и `#{...}`. Переключатель ҚАЗ / РУС / ENG доступен с клавиатуры. Язык сохраняется в сессии при навигации и после отправки анкеты. Каждый вопрос показывается на одном выбранном языке; `eligible`, `allottedBand`, `startBand`, `submissionStatus`, `extensionStatus`, `planning`, `difficulty`, `otherDeadlines` и значения ответов не менялись.

## Семантика чисел

```text
eligible: подходящие наблюдения исследования
known = onTime + late
pending: NOT_SUBMITTED
unknown: неизвестный исход
onTimePercent = 100 × onTime / known
p_observed = onTime / known
```

Pending и unknown остаются видимыми, но не входят в долю своевременной сдачи или наблюдаемый p. В частотных таблицах знаменатель отдельного вопроса явно показывается рядом с данными; он не должен автоматически интерпретироваться как known.

## Лаборатория вероятности

Успех означает сдачу вовремя. Одна группа состоит из n независимых испытаний с одинаковой вероятностью p. `X ~ Binomial(n,p)` — количество сдач вовремя; интересующее событие `X >= threshold`.

Наблюдаемый режим связывает модель с анкетой: известные исходы, число своевременных сдач и их отношение. При отсутствии известных исходов показывается пустое состояние. Ручной режим позволяет задать p независимо от выборки.

Controls: p в процентах со slider, размер группы со stepper, presets 1000 / 10000 / 50000 повторов, целочисленный threshold, seed в свернутых настройках. Допустимые диапазоны: p=0..100%, n=1..100, repeats=1..50000, threshold=0..n. Пустой seed генерирует случайный; фиксированный делает эксперимент воспроизводимым.

Четыре представления:

1. **Распределение:** эмпирические относительные частоты и теоретическая биномиальная PMF для X=0..n.
2. **Хвост:** выделение X>=threshold, теоретическая / эмпирическая вероятность и разница в процентных пунктах.
3. **Сходимость:** оценка той же хвостовой вероятности по мере повторов и теоретическая reference line. Checkpoints получены сервером из того же эксперимента, не из отдельной случайной последовательности.
4. **Одна группа:** n точек с outcome каждого испытания, точное число успехов, повтор группы без повторения тысяч экспериментов.

Интерпретация сопоставляет E[X]=np, полученное среднее, эмпирический хвост, теорию и абсолютную разницу. Симуляция иллюстрирует случайную вариацию и закон больших чисел, а не доказывает исследовательскую гипотезу. Раскрываемый блок объясняет одинаковый p, независимость и упрощение реального поведения.

## Серверные данные графиков

Локальный Chart.js 4.5.1 подключается только для визуализации; `chart.LICENSE.md` содержит MIT-лицензию. CDN для диаграмм не требуется. CSP `script-src 'self'` сохраняется; данные передаются инертными `script[type=application/json]`.

| Payload | Содержимое |
|---|---|
| `#study-charts` | Локализованные labels, старт: labels / counts / percentages; сравнение: labels / onTime / late |
| `.frequency-chart-data` | id canvas, field, labels, counts, percentages, total |
| `#probability-experiment` | probability, size, runs, threshold, seed; histogram с наблюдаемыми counts и двумя PMF; convergence; theoreticalTail / empiricalTail; firstGroup; labels |

`static/js/charts.js` рисует эти данные, переключает charts / tabs, обновляет controls и запрашивает отдельную группу. Статистику и биномиальные вероятности рассчитывает Java через Apache Commons Math.

`GET /simulator/group?p=<процент>&size=<n>&seed=<необязательно>` возвращает JSON `GroupExperiment(size, probability, seed, successes, outcomes)` либо HTTP 400 с `errorKey`. Этот endpoint не меняет базу. `ConvergencePoint(runs, empiricalProbability)` и `SimulationResult` используют вероятности от 0 до 1, хотя labels графиков могут показывать проценты.

## Визуализации и доступность

- Обзор переключает стартовое распределение: количество / процент. Центральное сравнение использует 100% stacked bars для своевременных / поздних исходов в обеих группах и точные значения рядом.
- Частоты переключаются между академической таблицей и столбцами; порядок вариантов сохраняется. Круговые диаграммы не добавлялись.
- Для лаборатории сохранены таблица PMF, точные результаты, checkpoints и раскрываемые числовые показатели / группировка Стерджеса.
- Empirical / observed — teal, theory — blue, threshold — amber. Цвет не является единственным носителем значения: есть labels, legend и численные эквиваленты.
- Tabs поддерживают клавиатуру; drawer сохраняет Escape, focus trap и восстановление фокуса. Поддержка reduced motion сохраняется.
- Графики имеют responsive containers; таблицы прокручиваются внутри wrapper. Переключатель языка и длинные переводы учитываются на мобильном экране.
- Категориальные графики не пропускают подписи оси. При длинных вариантах контейнер становится выше, чтобы все категории оставались читаемыми.

## Измененные файлы

Пути ниже относительно корня проекта.

| Группа | Файлы |
|---|---|
| Locale | `src/main/java/ru/deadline/lab/config/LocaleConfig.java`, `web/SiteText.java`; 12 message bundle файлов в `src/main/resources/` |
| Backend модели | `service/StatisticsService.java`, `model/HistogramBin.java`, `model/SimulationResult.java`, новые `model/ConvergencePoint.java`, `model/GroupExperiment.java` |
| Controllers | `web/PageController.java`, `web/SurveyController.java`, `web/AdminController.java` |
| Templates | Все 11 файлов `src/main/resources/templates/`: fragments, dashboard, frequencies, simulator, method, survey, thanks, connection, error, login, admin |
| Assets | `static/css/app.css`, `static/js/app.js`, новые `static/js/charts.js`, `chart.umd.min.js`, `chart.LICENSE.md`, `static/favicon.svg` |
| Тесты | `StatisticsServiceTest.java`, `WebFlowTest.java`, новый `LocaleFlowTest.java`, уточненный `PostgresIntegrationTest.java`; `scripts/verify-ui.cjs`, `src/test/resources/ui-import.csv` |
| Документация | `README.md`, `SITE_HANDOFF.md`, `docs/METHODOLOGY.md`, `DEFENSE.md`, `SURVEY.md`, `VERIFICATION.md`, этот документ; `scripts/package.ps1` включает handoff в архив |
| Превью | `previews/study-desktop.png`, `study-mobile-kk.png`, `lab-mobile-convergence.png` |

## Что сохранено

PostgreSQL persistence, сущности / репозитории / schema, demo/real separation, анкета и исходные answer codes, валидация, CSRF, Spring Security, existing routes, atomic / idempotent Google и canonical CSV import. Обновление backend ограничено моделью лаборатории, локализованными labels / уведомлениями и передачей данных интерфейсу.

## Проверки и ограничения

Полный Maven-прогон: 58 тестов без failures / errors / skipped, в том числе 5 PostgreSQL. Без opt-in PostgreSQL: 53 выполняемых и 5 пропущенных. Проверены границы p / n / threshold / repeats, seed, серверные данные графиков, denominators, три языка, query / session, формы и доступ.

Финальный browser run: PASS, 120 route × locale × width, все семь частотных графиков, четыре режима лаборатории, числовые границы интерфейса, анкета, вход и идемпотентный импорт. Ошибок консоли и page-level overflow нет. Основной сайт на 8085 проверен отдельно. Пользователь подтвердил удаление четырех прежних тестовых записей; основная база содержит 0 ответов и готова к сбору. Окончательный отчет находится в [`VERIFICATION.md`](VERIFICATION.md).

Реальные ограничения: нет автоматического Google Forms sync; добровольная выборка; модель с независимостью / одинаковым p; 120+ реальных ответов еще нужно собрать; детальные rejected-row diagnostics кабинета могут оставаться русскими; внешний hosting / Docker не проверялись. Тестовые ответы используются только в изолированной БД и не являются наблюдениями исследования.
