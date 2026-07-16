# CLAUDE.md

## Огляд проєкту

KMP-застосунок (Android + iOS) для двох людей на відстані: автоматичний
щоденник подорожі на основі фото/EXIF, ігровий вибір спільних місць
(тіндер-свайп для матчингу місць), лічильник до зустрічі, фінальний
рекап у форматі
Stories. Повна концепція — в `docs/concept.md`.

## Технологічний стек

| Шар | Технологія | Версія |
|---|---|---|
| Мова / платформа | Kotlin Multiplatform | 2.4.0 |
| UI | Compose Multiplatform | 1.11.0 |
| Навігація | Navigation 3 (multiplatform) | 1.1.0-alpha01 |
| Адаптивний UI | Material 3 Adaptive | 1.1.0 |
| Архітектура | MVI — Orbit MVI | 10.0.0 |
| Локальна БД | Room 3.0 (KSP-only) | 3.0.0-rc01 |
| Мережа | Ktor client | 3.5.0 |
| DI | Koin | 4.2.2 |
| Серіалізація | kotlinx.serialization | 1.10.0 |
| Async | kotlinx.coroutines | 1.11.0 |
| Бекенд | Firebase (Firestore + Auth + FCM), доступ через REST з Ktor, не через multiplatform SDK | — |
| Cloud-логіка | Firebase Cloud Functions (TypeScript), тільки для push-тригера | — |
| Зовнішні API | Google Places (геокодинг/імпорт), Gemini API (vision-опис щоденника) | free tier |

Повний версійний каталог — `gradle/libs.versions.toml`.

## Архітектура модулів

```
core:model      — сутності, без платформних залежностей
core:domain     — інтерфейси репозиторіїв/дата сорсів, use-case'и
core:database   — Room-реалізація локальних дата сорсів
core:network    — Ktor-клієнт + sync-queue процесор
core:ui         — дизайн-система: тема, анімації, базові компоненти
data            — Repository-реалізації (core:database + core:network),
                  тут живе offline-first merge/conflict-логіка
feature:auth
feature:onboarding
feature:pairing
feature:diary
feature:matcher
feature:places
feature:recap
feature:settings
app / androidApp — Navigation 3 граф, DI-збірка, Android entry point
playground        — Compose Desktop, залежить тільки від core:ui,
                     для Hot Reload UI-ітерації
iosApp             — Xcode-проєкт (не Gradle-модуль)
```

Feature-модулі залежать тільки вниз на core:domain + core:ui, ніколи
одне на одного напряму. Детальне обґрунтування кожного модуля —
`docs/kmp-module-architecture.md`.

## Конвенції коду

- Стиль коду — Google/Android Kotlin Style Guide
  (https://developer.android.com/kotlin/style-guide), перевіряється
  ktlint у CI (форматування) + detekt (код-смели, складність,
  Compose-специфічні антипатерни через `ru.kode:detekt-rules-compose`)
- Detekt-конфіг — `config/detekt.yml`, застосовується до всіх модулів
  через кореневий `build.gradle.kts` (`subprojects {}`); звіти всіх
  модулів мерджаться в один SARIF і вантажаться в GitHub Code Scanning
  — знахідки показуються прямо на діффі PR
- Без wildcard-імпортів, явні модифікатори видимості
- MVI-найменування: `<Feature>State`, `<Feature>Intent`,
  `<Feature>SideEffect`, контейнер — `<Feature>Container`
  (Orbit `ContainerHost`)
- Кожен feature-модуль: `presentation/` (Compose-екрани + Container),
  без прямого доступу до `core:database`/`core:network` — тільки через
  інтерфейси `core:domain`

## Тестова стратегія (TDD)

Тест пишеться до реалізації — мінімум для domain-логіки та
repository-логіки.

- **Юніт-тести** — `kotlin.test` + `kotlinx-coroutines-test`, живуть у
  `commonTest` кожного модуля. Orbit-контейнери тестуються через
  `orbit-test`. Domain use-case'и та repository-логіка — обов'язково
  до merge.
- **Інтеграційні тести** — `core:database` проти реального in-memory
  Room (`Room.inMemoryDatabaseBuilder`, без Android Context);
  `core:network` проти Ktor `MockEngine`; sync-queue логіка в `data` —
  повний цикл write-offline → sync-online на фейкових дата сорсах.
- **Скріншот-тести** — Roborazzi + ComposablePreviewScanner, на основі
  уніфікованої `@Preview` анотації в `commonMain` (доступна з
  Compose Multiplatform 1.10+). Рендер через Android/Robolectric-таргет
  на JVM, без емулятора. Golden-зображення — в репозиторії, поряд з
  модулем (`core:ui`, кожен `feature:*`). Обов'язкові для будь-якого
  нового/зміненого Composable в core:ui чи feature-екранах.

## Git-флоу

- Гілки: `feat/<назва>`, `fix/<назва>`, `refactor/<назва>`
- Коміти — Conventional Commits, узгоджені з префіксом гілки:
  `feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`
- Усі зміни — тільки через Pull Request, без прямих push у `main`
- PR мерджиться тільки при зеленому CI

## CI (GitHub Actions)

Тригер: PR → `main`, push у `main`.

1. **Lint** — ktlint проти Google Kotlin Style Guide
2. **Detekt** — код-смели, складність, Compose-антипатерни; SARIF
   мерджиться по всіх модулях і вантажиться в GitHub Code Scanning
3. **Unit tests** — усі модулі, `commonTest`
4. **Integration tests** — `core:database`, `core:network`, `data`
5. **Screenshot tests** — Roborazzi verify, при розбіжності — fail +
   diff-артефакти в звіті PR
6. **Build check** — Android debug-збірка (iOS-framework — окремо,
   вручну, через `ios-check.yml`, щоб не спалювати macOS-хвилини)

CD (публікація в сторах) — поки НЕ налаштовується, свідомо поза
скоупом.

## Ключові архітектурні рішення (міні-ADR лог)

1. **Orbit MVI** замість MVIKotlin/FlowMVI/Ballast — найменше
   boilerplate, нативна підтримка Compose Multiplatform з v10
2. **Room 3.0 (RC)** замість SQLDelight — знайомий API з Android,
   менше нового вчити паралельно з interview-препом
3. **Ktor + Firebase REST API** замість multiplatform Firebase SDK —
   уникає CocoaPods-проблем на iOS; власна sync-queue логіка все одно
   потрібна, тож вбудований офлайн-кеш Firestore не критичний
4. **Firebase (Firestore+Auth+FCM+мінімум Cloud Functions)** замість
   власного бекенд-сервера — мінімізує backend-поверхню для розробника
   без бекенд-досвіду
5. **Симетричне розблокування щоденника** — чесність важливіша за
   швидкість/зручність
6. **Matcher вимагає взаємного "так"** (тіндер-свайп, не турнір-на-виліт):
   обоє свайпнули "так" на ту саму картку → у Match-лист; обоє "ні" →
   картка прибирається назавжди; розійшлись → картка повертається в
   колоду й показується знову
7. **Recap генерується автоматично** по завершенню поїздки, без
   ручного тригера
8. **Лічильник = countdown до зустрічі**, не live-відстань між
   поточними локаціями — приватність, без геотрекінгу одне одного
9. **Імпорт місць через Google Maps share-лінк**, не через Takeout —
   природніший флоу під час самої поїздки

## Поза скоупом (свідомо)

- CD/публікація в App Store чи Google Play
- Власний бекенд-сервер (Ktor server + хостована БД)
- Live-геолокаційний трекінг одне одного
