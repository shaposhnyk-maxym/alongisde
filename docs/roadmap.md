# Roadmap

Кожен мілстоун — окрема гілка/PR (`feat/mX-назва`), мерджиться тільки
при зеленому CI. Порядок нижче — з урахуванням залежностей між
модулями (наприклад, diary не може початись раніше за data-layer).

---

### M0 — Project structure ✅ done
Скелет за `settings.gradle.kts` + `libs.versions.toml`, усі модулі
підключені й білдяться (навіть пустими).

**Accept:**
- [x] `./gradlew build` проходить без помилок на всіх модулях
- [x] Структура відповідає `docs/kmp-module-architecture.md`
- [x] CI (`ci.yml`) зелений на пустому скелеті (перевірено локально
      відтворенням точних тасок з `ci.yml`; сам PR на GitHub ще не
      мерджився)

**Відхилення від початкового плану (виявлено реальністю Maven/AGP,
не суб'єктивний вибір):**
- **Room**: `CLAUDE.md` пінить `3.0.0-rc01`, якої не існує на Maven.
  Використано `2.8.4` (останній стабільний, повна multiplatform-
  підтримка). Повернутись до `3.0.x`, коли JetBrains реально випустить.
- **AGP 9 + KMP**: `com.android.library`/`com.android.application`
  разом з `org.jetbrains.kotlin.multiplatform` в одному модулі більше
  **не підтримується взагалі** (не лише для app/androidApp, як
  малося на увазі в `kmp-module-architecture.md`, а для кожного
  KMP-бібліотечного модуля). Усі `core:*`/`feature:*`/`data`/`app`
  тепер використовують `com.android.kotlin.multiplatform.library` —
  новий уніфікований плагін з іншою DSL (`kotlin { android { ... } }`
  замість окремого `android {}` блоку) і без debug/release-варіантів.
  Це також змінило назву Roborazzi-таски: `verifyRoborazziDebug` →
  `verifyRoborazzi` (виправлено в `ci.yml`).
- **iOS-таргети**: `iosX64` (Intel-симулятор) прибрано — Compose
  Multiplatform не публікує під нього артефакти після
  `1.11.0-alpha`. Залишились лише `iosArm64`/`iosSimulatorArm64`
  (актуально й для M1+ — не додавати `iosX64` назад).
  Release-фреймворки (`linkRelease...`) не збираються — лінкування
  падало з `OutOfMemoryError`, а публікація в App Store свідомо поза
  скоупом. Аналогічно вимкнено `lint.checkReleaseBuilds` для
  `androidApp` (release-lint по всьому графу з 17 модулів впирався в
  `StackOverflowError`).
- **ktlint/detekt і згенерований код**: Compose Multiplatform генерує
  ресурс-акцесори в `build/generated/...`, які потрапляють у
  `commonMain` як звичайний source root. `KtlintExtension.filter{}`
  на це не впливає (джерело ktlint-задачі — плаский
  `ConfigurableFileCollection`, а не дерево з коренем, тому ant-glob
  патерни не матчаться) — довелось фільтрувати вже резолвлений
  `source` файлів напряму в `build.gradle.kts` (root, `subprojects{}`
  блок). Якщо додаватимеш нові ktlint/detekt-конфігурації — дивись
  туди, а не в `KtlintExtension`/`DetektExtension` filter-и.

---

### M1 — Core entities, data sources, repo interfaces ✅ done
`core:model` (Trip, DiaryEntry, Episode, PlaceCandidate з
match-статусом...) +
`core:domain` (інтерфейси репозиторіїв/дата сорсів, use-case'и).

**Accept:**
- [x] Усі сутності з `docs/concept.md` мають відповідні data-класи
      (`docs/concept.md` не існує як файл — стале посилання, актуальний
      документ `docs/trip-app-concept.md`; усі сутності звідти покриті:
      Trip, DiaryEntry, Episode, Photo, PlaceCandidate)
- [x] Кожна сутність має репозиторій-інтерфейс з мінімум CRUD +
      реактивним читанням (`Flow<T>`) — TripRepository,
      DiaryEntryRepository, EpisodeRepository, PlaceCandidateRepository
      (Photo — value-об'єкт всередині Episode, без окремого репозиторія)
- [x] `core:model`/`core:domain` компілюються без жодної платформної
      залежності (нуль expect/actual) — перевірено `grep` по обох
      модулях і повним `build` на всіх таргетах (android, jvm,
      iosArm64, iosSimulatorArm64)
- [x] Юніт-тести на бізнес-правила, які вже можна виразити на рівні
      моделі (наприклад: `DayReadiness` — правило симетричного
      розблокування, як чиста функція від двох станів) —
      `isDayUnlocked` (4 тести) + `resolveMatchStatus` для
      PlaceCandidate-матчингу (7 тестів), TDD (тест спочатку, red
      → green)

**Відхилення від початкового плану:**
- **kotlinx-datetime додано в `libs.versions.toml`** (`0.8.0`) —
  `CLAUDE.md`'s стек-таблиця не згадувала дати/час взагалі. Чистий
  multiplatform, без expect/actual у нашому коді, тож не порушує
  M1's accept-критерій. `LocalDate` — для календарних дат
  (Trip.startDate/endDate, DiaryEntry.date), `kotlin.time.Instant`
  (стандартна бібліотека Kotlin, не `kotlinx.datetime.Instant` —
  останній задеклеровано deprecated у 0.8.0 на користь
  stdlib-варіанту) — для точних міток часу.
- **`libs.<library>` type-safe accessors не резолвляться** в
  regular-скриптах модулів цього білда (`core/model/build.gradle.kts`,
  `core/domain/build.gradle.kts`) — незрозуміла особливість роздачі
  version-catalog accessors саме для основного білда (тоді як
  `libs.plugins.*` в `plugins {}` блоці й `libs.<x>` в
  `build-logic/convention/build.gradle.kts` резолвляться нормально).
  Обхід — `libs.findLibrary("alias").get()`, той самий патерн, що вже
  використовується в `KmpLibraryPlugin.kt` з інших причин (precompiled
  plugin-класи взагалі не отримують codegen). Якщо додаватимеш нову
  library-залежність у модуль — використовуй `findLibrary`, не
  `libs.xxx.yyy`.
- **`iosSimulatorArm64Test` не проганяється локально** — на цій машині
  не встановлено жодного iOS Simulator runtime (`xcrun simctl list
  devices` — порожньо). Не блокер: CI (`ubuntu-latest`) взагалі не
  виконує ці таски (Kotlin/Native не компілює під Apple-таргети з
  Linux-хоста). `jvmTest`/`testAndroidHostTest` — обидва зелені,
  11 тестів (4 DayReadiness + 7 PlaceMatchResolver) на кожному.

---

### M2 — DB ✅ done
`core:database` — Room реалізація дата сорсів з M1 (Trip, DiaryEntry,
Episode+Photo, PlaceCandidate).

**Accept:**
- [x] Усі DAO покриті інтеграційними тестами на `Room.inMemoryDatabaseBuilder`
      (без Android Context, без реального файлу БД) — `TripDaoTest`,
      `DiaryEntryDaoTest`, `PlaceCandidateDaoTest`, `EpisodeDaoTest`, усі в
      `core:database/src/jvmTest`
- [x] Тести покривають: insert/update/delete, реактивні `Flow`-запити
      (емітять оновлення при зміні даних), конфлікти первинних ключів
      (`upsert with existing id replaces existing row` у кожному DAO-тесті)
- [x] Міграції (навіть якщо схема поки одна) — `DatabaseCreationTest`
      створює БД з нуля й проганяє round-trip по кожній з 5 таблиць;
      `exportSchema = true` комітить schema-1 JSON у `core/database/schemas/`
- Додатково (не в буквальних Accept-критеріях, але з testing-strategy
  CLAUDE.md "repository-логіка — обов'язково до merge"): кожен
  `*RepositoryImpl` (домен-мапінг над DAO) також вкритий `*RepositoryImplTest`

**Відхилення від початкового плану:**
- **Room/KSP wiring через новий `convention.room`-плагін** — Room 2.8.4 KMP
  не автовайрить per-target KSP (на відміну від класичної Android-only
  Room+KSP звʼязки), тож `room-compiler` додається вручну через
  `dependencies.add("ksp<Target>", ...)` для `kspAndroid`/`kspJvm`/
  `kspIosArm64`/`kspIosSimulatorArm64`. Плагіни `com.google.devtools.ksp`
  та `androidx.room` довелось додатково задекларувати з `apply false` в
  корневому `build.gradle.kts` (за тим самим патерном, що й `roborazzi`) —
  інакше `pluginManager.apply(...)` всередині convention-плагіна не міг їх
  зарезолвити без версії.
- **`Dispatchers.IO` недоступний на Kotlin/Native** (`internal` у
  kotlinx.coroutines 1.11.0 для Native-таргетів) — спільна
  `getRoomDatabase(builder)`-функція використовує `Dispatchers.Default`
  замість `Dispatchers.IO` для query-coroutine-контексту, єдиний вибір,
  доступний одразу на android/jvm/iosArm64/iosSimulatorArm64.
- **`Episode.photos: List<Photo>`** — `PhotoEntity` отримав окрему таблицю
  з FK+CASCADE на `EpisodeEntity` (єдиний enforced FK у схемі — Photo не
  має власного репозиторія/lifecycle в домені), читання назад через
  `@Relation`-проєкцію `EpisodeWithPhotos`. Немає FK/cascade між Trip і
  його дочірніми сутностями (DiaryEntry/Episode/PlaceCandidate) — поведінка
  при видаленні поїздки не визначена ні в M1, ні в concept-документі,
  тож не вигадувалась зараз; переглянути, коли delete-trip буде реально
  реалізовано.
- **Детект-правило `coroutines.InjectDispatcher`** (дефолтний детект-рулсет,
  не кастомний) спрацьовував на прямі `Dispatchers.IO`-виклики в
  test-сетапах (побудова in-memory Room БД) — додано `excludes` для
  test-джерел у `config/detekt.yml`, оскільки для тестової інфраструктури
  пряма побудова реального диспатчера є стандартним, а не хибним,
  патерном.
- **Продакшн (не-тестове) провіжинування БД** — надано мінімально:
  `getDatabaseBuilder(...)` per-platform (`androidMain`/`iosMain`/`jvmMain`)
  + спільна `getRoomDatabase(builder)`. Без Koin-реєстрації — DI-вайринг
  свідомо відкладено на мілстоун, що збирає `data`/`app` (M9+).

---

### M3 — Network ✅ done
`core:network` — Ktor-клієнт до Firestore REST API + sync-queue логіка.

**Accept:**
- [x] Усі мережеві виклики тестуються через Ktor `MockEngine` — жодного
      реального HTTP-запиту в тестах, CI не потребує інтернету і не залежить
      від живого Firebase-проєкту
  - [x] MockEngine симулює: успішну відповідь, 4xx (401 + 404 окремо),
        5xx, timeout, malformed JSON — кожен кейс окремим тестом у
        `FirestoreApiReadTest`/`FirestoreApiWriteTest` (`core:network/src/jvmTest`)
- [x] Sync-queue тестується окремо від HTTP-шару: чергу можна наповнити й
      обробити з фейковим "мережевим клієнтом" (інтерфейс, не Ktor напряму),
      щоб перевірити порядок обробки, retry-логіку, поведінку при частковому
      збої (два з трьох записів синкнулись, третій — ні) —
      `SyncQueueProcessorTest` + `FakeSyncNetworkClient` (`core:network/src/commonTest`)
- [x] Контрактні тести: форма запиту/відповіді відповідає задокументованій
      схемі Firestore REST API (щоб зміни в цій схемі ловились одразу) —
      `FirestoreValueSerializationTest`, `FirestoreDocumentContractTest`,
      `FirestoreErrorResponseContractTest` (typed-value wrapper, Document,
      Google's standard error envelope) + request-body structural
      assertion в `FirestoreApiWriteTest`

**Відхилення від початкового плану:**
- **`core:network` не імплементує M1 repository-інтерфейси напряму** —
  та реконсиляція local (Room) + remote (Firestore) з offline
  merge/conflict-логікою свідомо належить M9 (`data`), не M3. Тут — лише
  дві перевикористовувані частини: `FirestoreApi` (Ktor-клієнт) і
  entity-agnostic `SyncQueueProcessor` (без жодної залежності на
  core:model/core:domain).
- **`SyncOperationType` — `UPSERT`/`DELETE`**, не `CREATE`/`UPDATE`, бо
  M1 repository-інтерфейси мають лише `upsert`/`delete`.
  `FirestoreApi.upsertDocument` шле `PATCH` без `updateMask` — Firestore
  REST документує це як create-if-absent + full overwrite, тобто точно
  upsert-семантика.
- **`SyncQueue` — інтерфейс** (+ `InMemorySyncQueue` за замовчуванням) —
  `core:network` не може залежати на `core:database`, тож персистентна
  черга — задача `data`-модуля (M9), яка зможе підмінити реалізацію без
  зміни `SyncQueueProcessor`.
- **`FirestoreSyncNetworkClient`** (адаптер `SyncNetworkClient` →
  `FirestoreApi`) додано в M3, а не залишено на M9 — інакше
  "Ktor-клієнт + sync-queue логіка" лишались би двома незв'язаними
  половинками, які ніколи не говорять одна з одною поза тестами/фейками.
- **Ретраї без backoff/затримки** — `SyncQueueProcessor` ретраїть
  одразу до `maxAttempts`, без штучної паузи; тести лишаються швидкими
  й детермінованими, реальний scheduling — питання пізніших мілстоунів,
  якщо взагалі знадобиться.
- **`kotlinx.coroutines.test.runTest` несумісний із Ktor `HttpTimeout`
  проти `MockEngine`** — під virtual-time-скедулером `runTest` навіть
  тривіальні (без жодного `delay`) запити падали з
  `HttpRequestTimeoutException` ще до першої відповіді MockEngine.
  Усі HTTP-рівневі тести (`FirestoreApiReadTest`, `FirestoreApiWriteTest`,
  `FirestoreSyncNetworkClientTest`) використовують `runBlocking` замість
  `runTest`; тест на реальний timeout чекає ~200мс реального часу
  (`requestTimeoutMillis = 200`, `delay(2_000)` у MockEngine-хендлері) —
  прийнятно швидко, і уникає цього конфлікту.

---

### M4 — Core UI ✅ done
`core:ui` — дизайн-система, анімації, базові компоненти.

**Accept:**
- [x] Кожен публічний компонент має `@Preview` у `commonMain` —
      `AlongsideButton` (Primary/Secondary), `PaperCard`, `CountUpText`,
      `TypewriterText`, `StaggerRevealColumn` (6 preview-функцій)
- [x] Юніт-тести через `ComposeTestRule` на інтерактивні компоненти
      (клік, свайп, стан — не тільки статичний рендер) —
      `AlongsideButtonTest` (клік для обох варіантів + disabled-кейс),
      `core:ui/src/androidHostTest`
- [x] Screenshot-тести (Roborazzi + ComposablePreviewScanner) на кожен
      `@Preview` — генеруються автоматично, без ручного дублювання тесту
      на кожен компонент — `generateComposePreviewRobolectricTests` у
      `RoborazziConventionPlugin.kt`, 0 рядків ручного тест-коду;
      golden-зображення в `core/ui/screenshots/`
- [x] Анімаційні хелпери (count-up/down, typewriter, stagger-reveal)
      мають юніт-тест на кінцевий стан (наприклад: typewriter після
      завершення показує повний текст, а не проміжний) —
      `CountUpTextTest`, `TypewriterTextTest` (явно перевіряє і
      проміжний, і фінальний стан), `StaggerRevealColumnTest`
- [x] Playground (`:playground`) запускається і показує всі компоненти
      дизайн-системи — вручну перевірено (`./gradlew :playground:run`
      + скріншот вікна): тема, обидві кнопки, картка, усі три анімації
      рендеряться коректно

**Відхилення від початкового плану:**
- **`generateComposePreviewRobolectricTests` вимагає `includePrivatePreviews.set(true)`** —
  без цього сканер мовчки ігнорує всі `@Preview`-функції (вони
  ідіоматично `private`), генеруючи 0 тестів замість помилки. Явно
  перевірено кількість згенерованих тестів (не просто "BUILD SUCCESSFUL"),
  щоб зловити це.
- **`androidx.compose.ui.tooling.preview.Preview` напряму, не
  `org.jetbrains.compose.ui.tooling.preview.Preview`** — CLAUDE.md
  описував останній як "уніфіковану" анотацію, але в Compose
  Multiplatform 1.11 (реальна версія в проєкті) вона deprecated на
  користь прямого використання androidx-анотації, яка сама стала
  multiplatform-ready.
- **Golden-зображення — в `<module>/screenshots/`, не в `build/`** —
  `RoborazziExtension.outputDir` явно виставлено в convention-плагіні,
  інакше скріншоти живуть в gitignored build-директорії й губляться
  при clean build (суперечить CLAUDE.md "Golden-зображення — в
  репозиторії").
- **`separateOutputDirs` + `robolectric.pixelCopyRenderMode=hardware`** —
  два додаткові фікси в `RoborazziConventionPlugin.kt`, знайдені по
  ходу (race condition на спільній intermediates-директорії; Roborazzi
  сам рекомендує другий для якості зображень).
- **`FunctionNaming.ignoreAnnotated` розширено на `"Test"`** —
  backtick-named `org.junit.Test`-функції (androidHostTest,
  Robolectric+Compose) не покривались вже наявним виключенням для
  `kotlin.test.Test`.
- **`TypewriterText`/`StaggerRevealColumn` preview з фіксованим
  розміром** — у першому кадрі анімації (порожній текст / нуль
  показаних елементів) композиція має нульовий розмір, через що
  Espresso не може знайти root-компонент для скріншоту; прев'ю
  обгорнуті в `Modifier.size(...)`, сам компонент не змінено.
- **Golden-скріншоти записані на macOS (arm64) не проходили verify в
  CI (ubuntu x86_64)** — `AlongsideButtonPrimaryPreview`/
  `SecondaryPreview`/`PaperCardPreview` падали з `AssertionError`;
  diff виявився суто антиаліасингом на заокругленому куті (1-2
  пікселі, не реальна відмінність — підтверджено візуально через
  `_compare.png` з артефакту невдалого CI-рану). Rounded-corner
  рендеринг відрізняється між Skia на arm64/macOS і x86_64/Linux.
  Не знайдено документованого Gradle-рівня способу виставити
  tolerance для авто-згенерованих `generateComposePreviewRobolectricTests`
  тестів (RoborazziOptions.CompareOptions приймає tolerance-подібний
  `Float`-параметр, але auto-generated test завжди використовує
  дефолтний `RoborazziOptions` без способу його перевизначити з
  Gradle-конфігу). Практичний фікс: замінено ці 3 golden-файли на
  реально відрендерені CI зображення (`_actual.png` з артефакту
  `screenshot-diffs` невдалого рану), а не перезаписано локально.
  **Наслідок на майбутнє:** якщо надалі локально записувати нові
  golden'и (`recordRoborazzi` на macOS) для компонентів із
  заокругленими кутами, вони можуть знову розійтися з CI — надійніше
  або записувати голдени безпосередньо в CI (напр. workflow_dispatch
  таска, що комітить результат), або оцінити реальний Gradle-механізм
  tolerance, коли він знадобиться частіше.

---

### M5 — Login (Google Sign-In) ✅ done
`feature:auth`.

**Accept:**
- [x] Бізнес-логіка (Orbit `AuthContainer`: `State`/`Intent`/`SideEffect`)
      юніт-тестується повністю через **фейкову** реалізацію
      `GoogleAuthProvider`-інтерфейсу — успіх, відмова користувача, мережевий
      збій, невалідний токен. Використовується `orbit-test` для перевірки
      послідовності станів — `AuthContainerTest` (11 тестів, incl. dismiss-error,
      провайдер-фейлюр як `SIGN_IN_FAILED`, і сесія-кеш сценарії нижче)
- [x] **Чесне обмеження, яке варто прийняти, а не вдавати, що його нема:**
      сам виклик нативного SDK (Credential Manager на Android, GIDSignIn на
      iOS) — це системний UI-флоу з реальним Google-акаунтом, його не можна
      надійно автоматизувати в CI. Це НЕ юніт-теститься напряму — теститься
      тільки код по обидва боки від нього (наш `Container` + обробка
      результату) — `CredentialManagerGoogleAuthProvider`, KDoc фіксує це явно
- [x] Інтеграційний тест — на етап **після** отримання ID-токена: обмін
      токена на сесію через Firebase Auth REST API, протестований через
      Ktor `MockEngine` (симулюємо відповідь Firebase Auth на валідний і
      невалідний токен) — `FirebaseAuthApiTest` + `FirebaseAuthSessionRepositoryTest`
      (200/400 INVALID_IDP_RESPONSE/INVALID_ID_TOKEN/інший 400/500/malformed
      JSON/timeout)
- [x] Мануальний чекліст (не автоматизований, документується як такий):
      реальний Sign-In на фізичному Android-пристрої й iOS-пристрої/симуляторі
      хоча б раз перед мержем — `docs/manual-checklists.md`

**Відхилення від початкового плану:**
- **Локальний кеш сесії + silent re-auth — додано поза буквальними
  Accept-критеріями M5, за прямим запитом користувача під час мануальної
  перевірки.** Виявлено на реальному пристрої: відповідь Firebase
  `accounts:signInWithIdp` для цього проєкту не містить `refreshToken`
  (і часом `expiresIn`) попри `returnSecureToken: true` — задокументована
  поведінка per Google's REST API docs, але без нього штатний Firebase
  silent-refresh неможливий. Замість "sign in щоразу при запуску
  застосунку" (що й так було б поведінкою M5 без цього, оскільки сесія
  жила тільки в `AuthState`) — додано:
  - `AuthSession.issuedAt` + `core:domain`'s `isExpired()` (чиста функція,
    5-хвилинний safety margin, own unit-тести — `AuthSessionExpiryTest`)
  - `core:domain.AuthSessionCache` seam, `core:database`-реалізація на
    Room (single-row таблиця `auth_session`, schema bumped 2→3, DAO +
    repository тести + `DatabaseCreationTest`-кейс, той самий підхід, що
    й `PushToken`)
  - `GoogleAuthProvider.signInSilently` (Android:
    `filterByAuthorizedAccounts(true)` — без UI, якщо акаунт уже
    авторизований)
  - `AuthContainer`'s `container(AuthState()) { restoreSession() }`
    onCreate-хук: не протермінована кешована сесія відновлюється без
    жодного мережевого виклику; протермінована — silent re-auth через
    Credential Manager, потім той самий REST-обмін, що й явний sign-in;
    будь-яка невдача цього фонового флоу тихо чистить кеш і лишає idle
    (без банера помилки — користувач цей флоу не запускав)
  - Обмін `androidApp` DI: `androidAppModule` тепер приймає `Context` (для
    Room) поряд з `firebaseApiKey`
- **Скоуп розширено мінімальним `AuthScreen`** — Accept-критерії M5 явно
  говорять тільки про логіку, але CLAUDE.md вимагає screenshot-тести для
  будь-якого нового/зміненого Composable в `feature:*`. Додано мінімальний
  екран (заголовок, кнопка "Continue with Google", pulsing-dot під час
  завантаження, `DotBanner` для помилки) з трьома preview (idle/loading/error)
  і auto-generated Roborazzi-тестами.
- **iOS-частина мануального чекліста — BLOCKED, не пройдена** — `iosApp`
  (Xcode-проєкт) ще не існує, Apple Developer акаунт заблокований (окрема
  відома проблема). `GoogleAuthProvider` спроєктований під майбутню
  Swift-реалізацію (callback-based, без `suspend`, single-method interface),
  але сам GIDSignIn-код і його мануальна перевірка відкладені до моменту,
  коли iOS-таргет реально з'явиться в проєкті — зафіксовано в
  `docs/manual-checklists.md`, а не приховано.
- **`core:network` тепер залежить від `core:domain`** (раніше — незалежний
  шар) — `FirebaseAuthSessionRepository` імплементує
  `core.domain.auth.AuthSessionRepository`; дозволено доком
  (`core:network` — "імплементує remote дата сорси з `core:domain`"), тому
  не порушує архітектурні межі, лише вперше їх реально задіює.
- **`core:domain`'s залежність на `core:model` — `implementation` → `api`** —
  публічні інтерфейси (`TripRepository` тощо) вже експонували model-типи;
  зроблено явним, щоб `feature:auth` бачив `AuthSession`/`AuthUser` через
  `core:domain`, не додаючи прямої залежності на `core:model` (яка порушила б
  правило "feature-модулі залежать тільки на core:domain + core:ui").
- **`androidHostTest` не успадковує `commonMain`'ові `implementation`-залежності**
  (задокументовано раніше в `RoborazziConventionPlugin.kt` для Compose-залежностей,
  M5 — перший випадок, де це вдарило по project-залежностях: `core:domain`/
  `core:model`) — довелось явно передекларувати `implementation(projects.core.domain)`
  для `androidHostTest`-сорссету в `feature/auth/build.gradle.kts`. Якщо
  наступні мілстоуни пишуть `androidHostTest`-тести, що імпортують типи з
  іншого модуля (а не лише свого) — дивись сюди першою чергою.
- **DI-вайринг, що читає `google-services.json`-ресурси (`google_api_key`,
  `default_web_client_id`), живе в `androidApp`, не в `app`** — ці рядки
  генеруються тільки в модулі, де застосований `com.google.gms.google-services`
  (`androidApp`), і не видимі з `app`'s окремого namespace. `androidApp` тепер
  має власний `AlongsideApplication`/`AndroidAppModule` (замість тільки
  `MainActivity`), і тимчасово показує `AuthScreen` напряму — `AlongsideApp()`
  (Navigation 3 граф у `app`) підключиться в M6+.

---

### M6 — Android Onboarding ✅ done
`feature:onboarding`, Android-специфічна частина.

**Accept:**
- [x] Послідовність кроків (photo permission → geolocation-для-камери →
      share-setup → notification permission) перевіряється Compose UI-тестом
      навігації — правильний наступний крок при кожному можливому вихідному
      стані (дозвіл вже наданий раніше → крок пропускається) —
      `OnboardingScreenNavigationTest` (4 сценарії: нічого не надано →
      повний прохід усіх 4 кроків; photo вже надано → одразу
      CAMERA_GEOLOCATION; обидва дозволи вже надано → пропускає обидва
      permission-кроки; DENIED все одно показує крок, не пропускає) +
      `OnboardingStepTest` (11 тестів чистої `nextOnboardingStep`)
- [x] Screenshot-тести на кожен крок окремо (включно зі станом "дозвіл
      відхилено — показуємо пояснення") — 8 `@Preview` (photo/notification
      × {not-determined, denied, denied-permanently}, camera-geolocation,
      share-setup), auto-generated Roborazzi-голдени в
      `feature/onboarding/screenshots/androidHostTest/`
- [x] Дозволи запитуються just-in-time (не всі одразу на старті), з
      поясненням-рационале перед системним діалогом там, де це доречно —
      `OnboardingContainer`'s onCreate лише читає `PermissionController.status()`,
      ніколи не викликає `request()`; системний діалог з'являється тільки
      по явному тапу на кроці; `DENIED`-стан показує `DotBanner`-пояснення
      перед кнопкою повторної спроби
- [x] Відмова в дозволі не блокує застосунок повністю — показує шлях назад
      (посилання в системні налаштування), це окремо перевірено тестом —
      `OnboardingPermissionRecoveryTest` + мануально на emulator (Pixel
      8 Pro, API 36): DENIED_PERMANENTLY → "Open Settings" реально відкриває
      `com.android.settings`' App Info екран

**Відхилення від початкового плану:**
- **`PermissionStatus` — 4 стани, не 2** (`GRANTED`/`NOT_DETERMINED`/
  `DENIED`/`DENIED_PERMANENTLY`) — Android API сам не розрізняє
  "ніколи не питали" від "назавжди відхилено"
  (`shouldShowRequestPermissionRationale()` повертає `false` для обох
  випадків), тож `AndroidPermissionController` тримає власний
  `SharedPreferences`-прапорець "чи питали раніше" per permission,
  записаний в момент виклику `request()` (до результату — переживає
  process death посеред діалогу).
- **`OnboardingIntent.RefreshPermissions` + `ON_RESUME`-хук у
  `OnboardingScreen`** — не в буквальних Accept-критеріях M6, але
  необхідний, щоб критерій "показує шлях назад" реально працював: без
  цього користувач, що надав дозвіл через системні Налаштування і
  повернувся в застосунок, назавжди застряг би на
  `DENIED_PERMANENTLY`-екрані, бо ніщо не перепитало б ОС. Мануально
  підтверджено на emulator: DENIED_PERMANENTLY → Open Settings → дозвіл
  надано в Налаштуваннях → повернення в застосунок → онбординг сам
  завершується екраном "You're all set" без перезапуску.
- **Навігація між кроками — похідний `currentStep` у `OnboardingState`**
  (чиста функція від permission-статусів + acknowledgement-прапорців),
  не Navigation 3 — `androidx.navigation3` лишається невикористаним і в
  M6 (свідоме рішення, узгоджене з користувачем перед реалізацією; лінійний
  4-кроковий wizard без back-навігації не потребує повного back-stack).
- **Camera-geolocation і share-setup — завжди показуються, ніколи не
  пропускаються** (узгоджене рішення) — на відміну від photo/notification,
  це суто інструктивні кроки без реального OS-дозволу для перевірки;
  немає persisted "вже бачив" прапорця в M6.
- **`MainActivity` тимчасово показує `OnboardingScreen` замість
  `AuthScreen`** — так само, як M5 тимчасово показував `AuthScreen`
  напряму (коментар "Navigation 3 lands in M6+" виявився занадто
  оптимістичним - реальний нав-граф, що з'єднає auth → onboarding →
  решту застосунку, відкладено на майбутній мілстоун, коли з'явиться
  реальна потреба в cross-feature навігації).
  *Оновлення (пост-M6, 2026-07-18):* хребет Navigation 3 таки заведено —
  `AlongsideApp` у `:app` тримає повний граф з
  `docs/navigation-flow.mermaid` (auth-гейт через side effects
  `SignedIn`/`Completed`, 5 табів + Settings/Recap як
  placeholder-екрани до своїх мілстоунів); `MainActivity` тепер показує
  `AlongsideApp`. `navigation3-ui` (NavDisplay) не публікує iOS-артефактів
  у 1.1.0-alpha01, тож рендер стека — expect/actual
  (`AlongsideNavDisplay`): Android — справжній `NavDisplay`, iOS/jvm —
  плаский рендер верхнього entry без анімацій.
- **`feature:onboarding`'s `commonMain` не залежить від `core:domain`**
  (стаб-модуль мав цю залежність, прибрано) — уся логіка кроку/дозволів
  самодостатня всередині фічі, нічого не торкається domain-шару.

---

### M7 — iOS Onboarding
`feature:onboarding`, iOS-специфічна частина.

**Accept:**
- Той самий набір критеріїв, що й M6, адаптований під iOS:
  Photos permission, Share Extension (окремий крок з поясненням "як
  додати вручну, якщо не з'явилось одразу"), notification permission
- Screenshot-тести на iOS-специфічні кроки (Share Extension
  інструкція — це унікальний для iOS екран, якого нема в M6)
- Мануальна перевірка на реальному iOS-пристрої: Share Extension
  дійсно з'являється в списку "Поділитися" після проходження кроку

---

### M8 — Pairing ✅ done
`feature:pairing` — створення/приєднання до поїздки.

**Accept:**
- [x] Юніт-тести: генерація інвайт-коду (унікальність, формат), логіка
      приєднання (валідний код / невалідний / вже використаний власний
      код) — `InviteCodeGeneratorTest` (7: довжина/алфавіт, 1000
      seeded-кодів без колізій, retry повз зайняті коди,
      `IllegalStateException` після вичерпання спроб, формат-валідація),
      `ResolveJoinOutcomeTest` (6: Joined / InvalidCode для битого
      формату й невідомого коду / OwnCode / AlreadyUsed / ідемпотентний
      re-join), TDD red → green
- [x] Repository-логіка тестується на фейкових дата сорсах (без
      реальної мережі/БД — той самий підхід, що в M1-M3) —
      `DefaultPairingRepositoryTest` (6) на
      `RecordingPairingTripDataSource`; Orbit-контейнер —
      `PairingContainerTest` (12, orbit-test) на `FakePairingRepository`
      + `FakeAuthSessionCache`
- [x] UI-флоу create/join — screenshot-тести + Compose UI-тест
      навігації — 9 `@Preview` → auto-generated Roborazzi-голдени
      (choice, code+waiting, join порожній/заповнений/submitting, всі
      3 error-стани) + `PairingScreenNavigationTest` (5 сценаріїв:
      create → показ коду → waiting → партнер приєднався → paired;
      join → валідний код → paired; невідомий код → error і лишаємось
      на кроці; використаний код → error; back → choice)

**Відхилення від початкового плану:**
- **Формат інвайт-коду** (концепт-документ його не фіксував): 6
  символів, A–Z + 2–9 без амбіговних 0/O/1/I (32 символи, ~1.07 млрд
  комбінацій) — узгоджено з користувачем перед реалізацією.
- **Fakes-only скоуп** (узгоджено з користувачем): реальна
  Firestore-реалізація свідомо НЕ входить у M8 — це задача M9 (`data`).
  У `core:domain` додано вузьку seam `PairingTripDataSource`
  (`findByInviteCode`/`observeByUserId`/`save`) замість розширення
  id-keyed `TripRepository` (це потягло б core:database у скоуп M8).
  Рантайм тимчасово на `InMemoryPairingTripDataSource` (прецедент —
  `InMemorySyncQueue`): пейрінг не переживає рестарт застосунку й не
  видимий крос-девайс, доки M9 не підмінить біндінг.
- **Дати поїздки**: create бере сьогодні..сьогодні+14
  (`DEFAULT_TRIP_LENGTH_DAYS`); date-picker UI свідомо відкладено — не
  в Accept-критеріях, а API `createTrip(ownerId, startDate, endDate)`
  вже приймає дати, тож M9+ нічого не змінює.
- **Єдиний шлях до `Paired`**: side effect постить спостереження
  `observeActiveTrip` (owner, що чекає, і joiner, що ввів код,
  проходять один детермінований шлях). Нескінченний onCreate-колектор
  у orbit-test закривається `cancelAndIgnoreRemainingItems()` — інакше
  таймаут "waiting for remaining intents".
- **Код-інпут join-кроку не в `FadeUpReveal`** — `AnimatedVisibility`
  не компонує unrevealed контент, тож поле було б недоступне до кінця
  анімації (той самий принцип, що в onboarding: інтерактив поза
  reveal). `OutlinedTextField` потребував явних on-paper кольорів —
  дефолтні colorScheme-токени дають крем-на-кремі на `PaperCard`.
- **Мануальний smoke на emulator** (Pixel 8 Pro, API 36): login →
  onboarding → pairing → Create a Trip → реальний згенерований код
  показано з waiting-станом.

---

### M9 — Data layer & offline sync ✅ done
`data` — Repository-реалізації, що зводять `core:database` +
`core:network`, sync-queue, conflict-resolution.

**Accept:**
- [x] Інтеграційний тест повного циклу: write офлайн (тільки в Room) →
  імітація появи мережі → sync-queue обробляє чергу → дані з'являються
  віддалено (перевіряється через фейковий мережевий клієнт, що фіксує
  виклики) — `OfflineSyncIntegrationTest` (`data/src/jvmTest`, 2):
  реальні in-memory Room + `SyncOperationStore` + `SyncingTripRepository`
  + `SyncCoordinator`/`SyncQueueProcessor`, мережа —
  `RecordingSyncNetworkClient`; окремий сценарій sync-під-час-офлайну
  (операція лишається RETRY, після появи мережі — SYNCED)
- [x] Тест конфлікт-резолюшну: два "конкурентні" запити на один запис —
  результат детерміністичний (last-write-wins за timestamp), перевірено
  явним тестом з контрольованими часовими мітками —
  `ResolveConflictTest` (4: local newer / remote newer / tie→LOCAL /
  no-remote→LOCAL) + `SyncCoordinatorTest` (LWW в обидва боки на
  контрольованому `FixedClock`, remote-переміг → застосовується в Room
  як SYNCED без push, local-переміг → push і remote не застосовується)
- [x] Тест часткового збою черги: N операцій в черзі, K з них падають —
  решта обробляються, збійні лишаються в черзі з позначкою retry —
  `SyncCoordinatorTest`: 3 ops / середня падає → 2 синкнулись і
  прибрані зі store, збійна лишилась зі `status = RETRY` та
  персистентним лічильником attempts, її рядок — `FAILED`; наступний
  `sync()` ретраїть і відновлює до SYNCED

**Відхилення від початкового плану:**
- **`SyncQueue`/`SyncQueueProcessor` (M3) не змінювались** — персистентність
  живе в новій таблиці `sync_operations` (schema v4) за suspend-інтерфейсом
  `SyncOperationStore` (core:database); `SyncCoordinator` (data) на кожен
  `sync()` матеріалізує store у тимчасову `InMemorySyncQueue`, ганяє
  незмінений процесор і застосовує результат назад. core:database не бачить
  `FirestoreValue` — поля операції зберігаються як `fieldsJson`
  (`SyncOperationCodec` у data).
- **`updatedAt: Instant` додано в Trip/DiaryEntry/PlaceCandidate**
  (узгоджено з користувачем) — LWW-мітка модифікації; міграція v3→v4
  (перша реальна `Migration` проєкту) бекфілить її з `createdAt`.
  LWW — клієнтські timestamps (`TimestampValue` у полях документа), не
  серверний `updateTime`; tie → LOCAL.
- **Скоуп репозиторіїв: тільки Trip** (`SyncingTripRepository` +
  Firestore-пейрінг) — Accept-критерії повністю доводяться на Trip, це
  єдина сутність з живим споживачем. Інфраструктура
  (coordinator/codec/binding) entity-agnostic; DiaryEntry → M11,
  PlaceCandidate → matcher-мілстоуни, PushToken → M17.
- **Pairing став реальним** (узгоджено з користувачем, виконує обіцянку
  M8): `FirestoreApi.runQuery` (structured query: EQUAL, OR-composite,
  limit) + `FirestorePairingTripDataSource` — Room як source of truth,
  invite-код шукається в remote з фолбеком на локальний кеш (офлайн
  createTrip працює), `observeByUserId` полить remote (~5s, поки є
  підписник) — власник бачить приєднання партнера; realtime listen
  свідомо відкладено. Koin-біндінг `InMemoryPairingTripDataSource`
  замінено на `dataModule`.
- **`FirestoreTokenProvider` — закритий follow-up-гілкою**
  `feat/firestore-auth-token` (виявлено мануальним крос-девайс smoke:
  create/join падав з 403, бо всі запити йшли неавтентифікованими):
  `SessionFirestoreTokenProvider` віддає кешований `idToken` і рефрешить
  прострочений через securetoken.googleapis.com
  (`FirebaseAuthApi.refreshIdToken`); `trips` read у
  `firebase/firestore.rules` відкрито для будь-якого залогіненого
  (інакше joiner не може виконати runQuery по inviteCode; посилення
  через inviteCodes-lookup-колекцію — свідомий борг, TODO в rules).
- **Poison ops**: non-retryable збої теж лишаються RETRY (обмежені
  `MaxAttemptsRetryPolicy` per-run); паркування/дроп — відкладено.
  Без op-коалесингу і без LWW для DELETE (DELETE пушиться без
  preflight).
- **Без connectivity-listener** — `sync()` викликається явно і
  best-effort при `save()` пейрінгу; авто-тригер за появою мережі /
  foreground — пізніший мілстоун.

---

### M10 — Diary: capture & processing pipeline ✅ done
`feature:diary` (частина 1) — EXIF, кластеризація епізодів, Google
Places геокодинг, Gemini vision-опис.

**Accept:**
- [x] Юніт-тести кластеризації: синтетичні набори фото з відомими
      timestamp/координатами → перевірка, що епізоди групуються правильно
      (межові випадки: фото рівно на межі 500м/2год порогу) —
      `EpisodeClusteringTest` (`core:domain`, 12 тестів: порожній список,
      одне фото, злиття/розрив по часу й по відстані окремо, обидва
      межові випадки в обох напрямках, sliding-chain vs anchor-based
      поведінка, порядок вхідних фото не має значення)
- [x] Places геокодинг і Gemini виклики — тестуються через фейкові клієнти
      (інтерфейс, не HTTP напряму), окремо від `core:network`, який вже
      покритий в M3 — `EpisodeProcessingPipelineTest` (`core:domain`, 6
      тестів на фейкових `PlaceGeocodingClient`/`EpisodeVisionDescriptionClient`)
- [x] Тест вибору репрезентативних фото з епізоду (2-4 з N, за критерієм
      найбільшої різниці в часі) — `RepresentativePhotoSelectorTest`
      (`core:domain`, 7 тестів)
- [x] Тест ліміту перегенерації тексту (лічильник спроб на епізод, поведінка
      при вичерпанні) — `EpisodeDescriptionAttemptsTest` (`core:domain`,
      3 тести) + `Episode.descriptionAttempts` персиститься (schema v4→v5)

**Відхилення від початкового плану (усі узгоджені з користувачем перед
реалізацією):**
- **Реальні HTTP-клієнти для Places і Gemini, не лише фейки** —
  розширення поза буквальним Accept-критерієм, за прямим запитом
  користувача. `core:network`'s `GooglePlacesGeocodingApi`/`GeminiVisionApi`
  повторюють `FirebaseAuthApi`'s скелет (config → rawRequest/throwIfError/
  parseBody → sealed exception → MockEngine `jvmTest`), плюс адаптери
  (`GooglePlacesGeocodingClient`/`GeminiVisionDescriptionClient`), що
  імплементують `core:domain`'s `PlaceGeocodingClient`/
  `EpisodeVisionDescriptionClient` seam-інтерфейси (той самий патерн, що
  `PairingTripDataSource` з M8). Google Geocoding API (не "Places API"
  буквально) — правильний Google-продукт для reverse-геокодингу за
  координатами; назва пакета/класів лишена "places" відповідно до
  CLAUDE.md-термінології.
- **Ключі API — нова територія, без готового механізму** (на відміну від
  Firebase, де `google-services.json` авто-генерує ресурси) —
  `GOOGLE_PLACES_API_KEY`/`GEMINI_API_KEY` читаються з `local.properties`
  (вже в `.gitignore`) в `androidApp/build.gradle.kts` через
  `buildConfigField`; задокументовано в `docs/local-setup.md`.
- **Правило кластеризації** (не зафіксоване в жодному доці до цього):
  sliding-chain, не anchor-based — фото лишається в поточному епізоді,
  тільки якщо воно в межах 2год **І** 500м від **попереднього** фото (не
  від першого фото епізоду); перевищення будь-якого з порогів починає
  новий епізод. Межовий випадок (рівно 500м/2год) — лишається в тому ж
  епізоді (inclusive).
- **Ліміт перегенерації = 3 спроби** — конкретне число не було
  зафіксоване ніде. При вичерпанні — регенерація вимикається, але
  останній згенерований текст лишається (користувач ніколи не
  залишається зовсім без тексту).
- **EXIF-читання (`ExifPhotoReader`/`PhotoByteReader`) — тільки Android**,
  через `androidx.exifinterface`; iOS-реалізація відкладена (Apple dev
  акаунт заблокований, `iosApp` ще не існує — той самий статус, що й M7).
  Чесне обмеження: сам виклик `ContentResolver`/`ExifInterface` не
  юніт-тестується напряму (немає дешевого фейка для `ContentResolver`,
  той самий клас проблеми, що нативний SDK у M5) — але пайплайн, який він
  живить (`clusterPhotosIntoEpisodes` і решта), повністю покритий тестами
  на синтетичних `Photo`-списках, незалежно від способу захоплення.
  **Формат фото не обмежений JPEG**: `androidx.exifinterface` (не
  платформний `android.media.ExifInterface`) читає EXIF з JPEG, PNG,
  WebP, HEIF/HEIC, AVIF і купи RAW-форматів — жодного JPEG-хардкоду в
  коді, `ExifInterface(stream)` формат-агностичний. Перевірено емпірично
  (2026-07-19): реальний JPEG сконвертовано в HEIC (`sips`), GPS +
  `DateTimeOriginal` пережили конвертацію 1:1, файл запушено на пристрій
  — MediaStore проіндексував як `image/heic` з коректним `datetaken`.
  Компоненти (бібліотека + MediaStore) сумісні з HEIC/HEIF (Samsung
  опційно, iPhone за замовчуванням) — але наскрізний виклик
  `AndroidExifPhotoReader.readOne()` проти цього файлу ще не запущений
  (той самий блокер: немає entry point/UI).
- **`GeocodeResult.preferredPlaceName()` пріоритет розширено** — реальний
  виклик Google Geocoding API (2026-07-19, ключ з `local.properties`,
  координати обох тестових епізодів) показав: `point_of_interest`/
  `premise` майже ніколи не зустрічаються як тип окремого
  `address_component` (навіть коли результат В ЦІЛОМУ — заклад), тож
  алгоритм падав аж до `locality` — обидва тестові епізоди (різні
  вулиці, ~4.2км одна від одної, те саме місто) отримували однакову
  назву "Arezzo". Додано `route`/`neighborhood` у пріоритет (між
  `sublocality_level_1` і `locality`) — тепер відрізняються за назвою
  вулиці/району. `GeocodeResultTest.kt` — 6 тестів на всю пріоритетну
  логіку (раніше перевірялась лише побіжно, одним фікстур-кейсом у
  `GooglePlacesGeocodingClientTest`).
- **Gemini vision-опис протестовано наскрізно реальним викликом**
  (2026-07-19): 4 представницьких фото епізоду 1 (відібрані вручну за
  тим самим алгоритмом, що `selectRepresentativePhotos`) + промпт у
  точному форматі `GeminiVisionDescriptionClient` → реальний
  `gemini-flash-latest`. Результат — саме "емоційний абзац, не сухий
  опис", як вимагає концепт-документ, і природно підхопив
  `placeName`-підказку з промпту. Дорогою знайдено й виправлено 2 реальні
  проблеми (окремі коміти): (1) дефолтна модель `gemini-2.0-flash` мала
  нульову free-tier квоту на реальному ключі — замінено на
  `gemini-flash-latest`; (2) `GeminiVisionApi`/`GooglePlacesGeocodingApi`
  не парсили JSON error envelope на HTTP 4xx/5xx, губили реальне
  повідомлення (напр. "Your prepayment credits are depleted..." замість
  generic "Too Many Requests") — виправлено за тим самим патерном, що
  `FirebaseAuthApi` вже використовував.
  **Білінг-нюанс, не пов'язаний з кодом**: проєкт, спільний з Firebase
  (`alongside-b05f2`), автоматично стає Gemini API "Tier 1" (вимагає
  prepay-баланс) сам факт наявності білінг-акаунта на проєкті — Free
  Tier практично недоступний, поки білінг лишається підключеним.
  Робочий ключ узято з окремого, не пов'язаного з Firebase проєкту.
- **Промпт переписано після живої перевірки якості тексту, не лише
  факту виклику** (2026-07-19) — перший варіант промпту генерував
  правильний за змістом, але типовий "AI-травелблог" текст ("wrapped in
  the warmth... a dream we never want to wake up from"). Новий промпт
  додає: (1) **локалізацію** — `EpisodeVisionDescriptionClient.describeEpisode`
  отримав третій параметр `languageTag: String` (BCP-47, напр. "uk"),
  протягнутий через `EpisodeProcessingPipeline.process` — Gemini пишуть
  рідною мовою локалі застосунку, не перекладом; (2) явну заборону
  кліше ("magical", "breathtaking", "wrapped up in" тощо), вимогу
  прив'язки до однієї конкретної видимої деталі (об'єкт/колір/текстура),
  формат "підпис" (1-2 речення), а не розлогий абзац. Перевірено вживу
  тим самим набором фото у двох мовах — англійською й українською:
  укр. "Втекли від сонця на цю алею і тепер просто блукаємо вздовж
  старого кам'яного муру, згрібаючи ногами сухе руде листя на гравії" —
  природна, не перекладена українська, прив'язана до реального
  кам'яного муру й листя на фото. **Хто ще не отримав `languageTag`**:
  реального джерела локалі застосунку (Android `Locale.getDefault()`
  чи подібне) ще не підключено — жоден виклик з реального коду поки не
  існує (той самий "немає entry point" блокер), тож параметр наразі
  тільки протестований, не звʼязаний з реальною системною локаллю.
- **Немає UI/Orbit Container у M10** — Accept-критерії цього мілстоуна не
  мають жодної UI-вимоги (Timeline UI — M12); `feature:diary` отримав
  лише capture/processing-логіку.
- **`Episode.descriptionAttempts: Int`** — нове поле, schema v4→v5,
  `MIGRATION_4_5` (backfill 0 для існуючих рядків), окремий
  migration-тест (`core:database`, hand-rolled v4-фікстура, той самий
  підхід, що v3→v4 в M9).

**iOS TODO (накопичено в M10, перевірити коли `iosApp` реально стартує):**
- [ ] `ExifPhotoReader` — немає `iosMain`-реалізації взагалі (тільки
      commonMain-інтерфейс). На Android — `ContentResolver` +
      `androidx.exifinterface`; на iOS еквівалент — швидше за все
      `PHAsset`/`PHImageManager` (доступ до фото) + `CGImageSource`/
      `CGImageSourceCopyPropertiesAtIndex` (читання EXIF GPS+
      DateTimeOriginal з `kCGImagePropertyExifDictionary`/
      `kCGImagePropertyGPSDictionary`) — інший API, інша структура даних,
      писати з нуля, не портувати Android-код.
- [ ] `PhotoByteReader` — та сама історія: немає `iosMain`, на Android —
      `ContentResolver.openInputStream`, на iOS — читання байтів з
      `PHAsset` через `PHImageManager.requestImageDataAndOrientation`
      (async callback-based API, не suspend напряму — знадобиться
      обгортка, як `GoogleAuthProvider` в M5).
- [ ] `androidx.exifinterface` — Android-only бібліотека в
      `libs.versions.toml`; для iOS жодної бібліотеки ще не обрано
      (ImageIO — системний фреймворк, не Gradle-залежність, тож питання
      radше в `iosApp`/cinterop налаштуванні, коли Xcode-проєкт з'явиться).
- [ ] **Google Places/Gemini Ktor-клієнти (`core:network`) технічно
      мультиплатформні** (`ktor-client-darwin` вже підключений для iOS в
      `core:network/build.gradle.kts`), **але жодного разу не перевірені
      на Darwin-таргеті** — самі HTTP-виклики можуть просто запрацювати,
      коли з'явиться iOS-виклик, а можуть і ні (напр. `kotlin.io.encoding.Base64`
      у `GeminiVisionDescriptionClient` теоретично мультиплатформний, але
      не тестований на Kotlin/Native). Перевірити першим ділом, це
      найдешевше з усього списку.
- [ ] **Ключі API (`GOOGLE_PLACES_API_KEY`/`GEMINI_API_KEY`) — механізм
      Android/Gradle-специфічний** (`local.properties` → `BuildConfig`
      через `buildConfigField`). Для iOS еквівалента ще не існує (варіанти:
      `.xcconfig` + `Info.plist`, або окремий `Secrets.swift`, не
      закритий у Git — треба спроєктувати, коли `iosApp` з'явиться, не
      раніше).
- [ ] `iosApp` (Xcode-проєкт) все ще не існує — жодна з вищенаведених
      точок не тестована на реальному пристрої/симуляторі. Заблоковано
      тим самим Apple dev account issue, що й M7.

**Знайдено при підготовці реальних тестових фото (не iOS, Android-специфічно):**
`AndroidExifPhotoReader` спершу читав GPS через звичайний
`ContentResolver.openInputStream()` — на API 29+ MediaStore редагує
(вирізає) GPS EXIF-теги з цього виклику за замовчуванням (privacy
scoped storage), тож `ExifInterface.latLong` мовчки повертав би `null`
для КОЖНОГО фото на будь-якому сучасному пристрої (перевірено на
реальному пристрої, API 37). Виправлено:
`MediaStore.setRequireOriginal(uri)` перед відкриттям стріму + fallback
на редаговану версію, якщо permission не надано. Додано
`ACCESS_MEDIA_LOCATION` в `AndroidManifest.xml`. **Сам runtime-запит
цього permission ще не заведений** (немає permission-флоу для diary
capture, як є в M6 для photo/notification) — тобто GPS все одно буде
`null`, поки якийсь майбутній мілстоун не додасть реальний запит
дозволу. Задокументовано в kdoc `AndroidExifPhotoReader`, не приховано.

---

### M11 — Diary: symmetric unlock & sync integration ✅ done
`feature:diary` (частина 2) — логіка розблокування дня, інтеграція з
data-layer.

**Accept:**
- [x] Юніт-тести `DayUnlockState`: усі комбінації станів обох учасників
      (жоден не готовий / один готовий / обидва готові), включно з
      проміжним станом "дані є, але ще не засинкані" —
      `DayUnlockStateTest` (9: повна 3×3 матриця `DiaryDayStatus` —
      NOT_READY/PENDING_SYNC/READY — через `resolveDayUnlockState`) +
      `DiaryDayStatusTest` (5: мапінг `DiaryEntry?` → `DiaryDayStatus`
      по всіх 4 значеннях `SyncStatus` і `null`)
- [x] Тест умови тригера пуша (обидва DiaryEntry цієї дати мають
      `syncStatus == Synced`) — саме умова, без реального виклику FCM
      (той — в M17) — `PartnerReadyPushTriggerTest` (7),
      `shouldTriggerPartnerReadyPush(own, partner)` в `core:domain`
- [x] Інтеграційний тест: DiaryEntry проходить повний шлях capture →
      processing (M10) → sync (M9) → unlock-стан оновлюється коректно —
      `DiaryOfflineSyncIntegrationTest` (`data/src/jvmTest`): реальні
      in-memory Room + `EpisodeProcessingPipeline` (фейкові
      geocoding/vision клієнти) → `SyncingDiaryEntryRepository`/
      `SyncingEpisodeRepository` → `SyncCoordinator` з обома
      bindings (Diary + Episode) → `resolveDayUnlockState` LOCKED до
      сінку, UNLOCKED і `shouldTriggerPartnerReadyPush == true` після

**Відхилення від початкового плану (узгоджено з користувачем перед
реалізацією):**
- **Скоуп розширено на `Episode`-сінк, не лише `DiaryEntry`** —
  буквальні Accept-критерії перевіряють умову розблокування/пуша лише
  на `DiaryEntry.syncStatus`, але без синку `Episode` розблокований
  день не мав би реального контенту (фото/назва місця/Gemini-опис —
  усе на `Episode`, не на `DiaryEntry`) на пристрої партнера. M9
  залишив `SyncCoordinator`/`SyncEntityBinding` entity-agnostic саме
  для такого розширення (пошук binding'а по `collectionPath`), тож це
  механічне повторення патерну Trip, не новий дизайн. Сама умова
  розблокування/пуша лишається прив'язаною тільки до
  `DiaryEntry.syncStatus`, як і вимагають Accept-критерії — `Episode`-
  синк тільки робить контент фізично доступним.
- **`Episode` отримав `syncStatus`/`updatedAt`** (яких не було взагалі,
  на відміну від Trip/DiaryEntry, що мали `syncStatus` з M1/M2 і лише
  `updatedAt` з M9) — нова міграція `MIGRATION_5_6` (schema v5→v6):
  `syncStatus` бекфілиться як `PENDING` (чесно — локальні епізоди до
  M11 ще не пушились), `updatedAt` бекфілиться з `endTime` (немає
  `createdAt` на Episode, на відміну від Trip/DiaryEntry, звідки M9
  бекфілив). `EpisodeProcessingPipeline` отримав `clock`-параметр
  (дефолт `Clock.System`, той самий патерн, що `SyncingTripRepository`)
  для проставлення цих полів при створенні епізоду.
- **`Episode.photos` синкається як embedded-масив, не окрема
  колекція** — на відміну від Room, де `Photo` живе в окремій
  FK+cascade таблиці (M2), у Firestore немає `photos`-колекції
  (задокументовано коментарем у `firebase/firestore.rules` ще з M9) —
  `EpisodeFirestoreMapper` кодує `photos` як `ArrayValue` з `MapValue`
  на кожне фото.
- **`DiaryDayStatus` — 3 стани, не 2** (`NOT_READY`/`PENDING_SYNC`/
  `READY`, було `NOT_READY`/`READY` з M1) — `PENDING_SYNC` покриває
  одразу `PENDING`/`SYNCING`/`FAILED` з `SyncStatus`: з погляду правила
  розблокування "ще не з'явилось на пристрої партнера" — це один і той
  самий стан, деталізація тут не потрібна.
- **`isDayUnlocked` (M1) перейменовано/замінено на
  `resolveDayUnlockState`**, що повертає новий `DayUnlockState`
  (`LOCKED`/`UNLOCKED`) замість `Boolean` — жодних викликів поза його
  власними тестами не існувало (Timeline UI, який його реально
  споживатиме, — M12), тож заміна на місці без backward-compat шва.
- **Немає нового UI/Orbit Container** (той самий підхід, що в M10) —
  Accept-критерії M11 суто про domain-логіку й data-layer; `feature:diary`
  так і не отримав `presentation/`-пакету — це M12.
- **`firestore.rules` не змінювались** — правила для `diaryEntries` й
  `episodes` вже існували з M9 (закладені саме під цей мілстоун,
  включно з коментарем "Revisit when M11 ... lands"), включно з
  усвідомленим боргом (симетричний unlock не форситься на
  Firestore-рівні, лишається client/domain-layer concern).

---

### M12 — Timeline: UI & animations ✅ done
`feature:diary` (частина 3) — карусельний екран Таймлайн (замінює
окремі List+Detail: лічильник → День 1 → День 2 → ... в одному
пейджері, поточний день по центру, сусідні виглядають по боках).

**Accept:**
- [x] Screenshot-тести: заблокований день у каруселі, кожен з
      варіантів "стан очікування" (партнер ще знімає / чекає мережі /
      генерує текст), розблокований день — 5 `@Preview` →
      auto-generated Roborazzi-голдени (`DiaryTimelinePreviews.kt`),
      кожна карусель-картка через спільний `animateEntrance = false`
      (settled end-state, той самий патерн, що `PairingPreviews`)
- [x] Compose UI-тест: свайп вперед/назад коректно змінює поточний
      день, сусідні дні відображаються прев'ю по боках, розблокування
      впливає тільки на конкретний індекс дня, не на всю карусель —
      `DiaryTimelineNavigationTest` (4 тести), керує `HorizontalPager`
      напряму фіксованим списком `items`, без реального Container
- [x] Юніт-тест на послідовність стагер-анімації фото (порядок появи
      відповідає порядку фото в епізоді) — `EpisodePhotoGalleryTest`
      (2 тести, androidHostTest/Robolectric — той самий термін
      "юніт-тест", що М4 вже використовував для ComposeTestRule-тестів)
- [x] Count-down до зустрічі — юніт-тест на коректність числа, окремо
      від анімації відображення — `ReunionCountdownTest` (`core:domain`,
      4 тести на чистій `daysUntilReunion`), незалежно від `CountUpText`
      (яка вже мала власні тести з M4)

**Відхилення від початкового плану (усі узгоджені з користувачем перед
реалізацією, де відзначено):**
- **Гілка застакана на `feat/m11-diary-unlock-sync`, не на `main`** —
  M11 (DayUnlockState/DiaryDayStatus/sync-репозиторії) ще не
  змерджений, а M12 напряму залежить від цього коду; той самий
  прецедент, що `feat/firestore-auth-token` (стак на #11).
- **"Генерує текст" (третій варіант очікування) реально підключений
  через Container, не лишений тільки прев'ю-стабом** — узгоджено з
  користувачем окремим питанням перед реалізацією (варіант "Wire it
  live via Container" з трьох запропонованих). `DiaryTimelineContainer`
  реально залежить від `EpisodeProcessingPipeline` (той самий клас з
  M10) через новий `DiaryCaptureCoordinator` (виділений окремим
  класом, щоб конструктор Container не впирався в detekt's
  `LongParameterList`), і `Intent.ProcessCapturedPhotos(uris)` реально
  ганяє EXIF-читання → пайплайн → persistence, виставляючи
  `processingOwnDate` на час виконання. **Чесне обмеження**: сам
  вхідний тригер (photo picker/дозвіл) все ще не існує — той самий
  "немає entry point" розрив, що M10/M11 задокументували; Container
  готовий прийняти URI, коли picker з'явиться.
- **`DiaryDayLockReason` (`core:domain`) — 2 варіанти, не 3** —
  `PARTNER_CAPTURING`/`WAITING_FOR_SYNC`, похідні тільки від
  partner-статусу (own-параметр прибрано після detekt's
  `UnusedParameter`, а не засупресений — YAGNI). Третій варіант,
  `DiaryDayWaitingState.GENERATING_TEXT`, існує тільки в
  `feature:diary`'s presentation-шарі, як живий Container-прапорець
  поверх домен-логіки — немає persisted-сигналу для "генерації в
  процесі" (пайплайн атомарний, немає проміжного стану в БД).
- **`DiaryTimelineDay`/`buildDiaryTimelineDays` (`core:domain`,
  нове)** — по одному дню на кожен календарний день
  `trip.startDate..trip.endDate`, кожен незалежно зіставлений зі своїм
  own/partner `DiaryEntry` за датою — саме це гарантує "розблокування
  одного дня не впливає на інший" на рівні чистої функції, а не тільки
  UI-шару.
- **Зустріч = `trip.startDate`** (`daysUntilReunion(today, meetingDate)`,
  `core:domain`) — не задокументовано explicitly ніде раніше.
  Початкове припущення в M12 (`trip.endDate` — "мандрують окремо всю
  поїздку, зустрічаються в останній день") виявилось хибним при
  мануальному тестуванні (2026-07-20, реальний акаунт, старт поїздки =
  сьогодні, лічильник помилково показував "3 дні" замість "0"):
  поїздка й є періодом, коли вони разом, тож лічильник рахує до
  **початку** поїздки, не до кінця. Виправлено в `DiaryTimelineState`
  (`daysUntilReunion(today, trip.startDate)`) і в KDoc самої
  `daysUntilReunion`; `DiaryTimelineContainerTest` перерахований під
  новий кейс. **Другий раунд фідбеку того ж дня**: лічильник "0 днів"
  після настання дня зустрічі — просто шум, картку-лічильник тепер
  повністю ховає з каруселі (`items` не додає `Countdown`, коли
  `daysUntilReunion == 0`), карусель одразу відкривається на Дні 1.
  Новий `DiaryTimelineStateTest` (3 тести: сьогодні = зустріч, дата
  зустрічі вже в минулому, дата зустрічі ще попереду) ізольовано
  покриває цю похідну логіку `items`, без Container/фейків.
- **`StaggerRevealColumn`/`TypewriterText` (`core:ui`) отримали
  `initiallyRevealed`** — той самий патерн, що `FadeUpReveal` вже мав
  з M6/M8: без нього auto-generated Roborazzi-тест каптурить перший
  (порожній/нерозкритий) кадр аніmації, а не завершений вигляд. Обидва
  компоненти мають новий юніт-тест на цю поведінку, дефолт лишився
  `false` — існуючі голдени не змінились.
- **`swipeLeft()`/`swipeRight()` (default) пропускають одразу дві
  сторінки під Robolectric** — стандартний edge-to-edge свайп (весь
  вузол, 200мс) на дефолтному 320dp Robolectric-вікні дає забагато
  velocity відносно ширини сторінки (з урахуванням `contentPadding`
  peek), pager долітає на 2 сторінки замість 1.
  `DiaryTimelineNavigationTest` використовує коротший явний
  `swipe(start, end, durationMillis=300)` (~160dp) для детермінованого
  переходу на одну сторінку за раз; задокументовано в коді як
  Robolectric-специфічний нюанс.
- **Фото рендеряться як прямокутні плейсхолдери, не реальні
  зображення** — у проєкті ще немає image-loading бібліотеки (Coil чи
  подібне) в `libs.versions.toml`; той самий рівень чесного
  спрощення, що плейсхолдер-екрани (`PlaceholderScreen`) для
  ще-не-реалізованих фіч. `EpisodePhotoGallery` (index → photo)
  повністю готовий прийняти реальний рендер, коли бібліотека
  з'явиться.
- **Прапор країни (з `docs/screens.md`) не реалізований** — жодна
  сутність домену (`Episode`, `GeocodeResult`) не має поля країни,
  тільки `placeName`; вигадувати нове поле поза Accept-критеріями
  цього мілстоуна — за межами скоупу.
- **Day-картка показує обидва боки** — секції "You"/"Your partner",
  кожна з власним набором епізодів (назва місця + typewriter-опис +
  фото-галерея), одна під одною в скрольованій колонці. `screens.md`
  описує картку однини ("колода фото + typewriter-опис"), не
  уточнюючи, чий саме бік показується — обраний найпростіший
  однозначний варіант (обидва боки, симетрично до симетричного
  розблокування), а не merge в один опис.
- **Koin: `ExifPhotoReader`/`PhotoByteReader` заведені в
  `androidAppModule`** (Android-only, `ContentResolver`-based) — той
  самий патерн, що Room-database Context-injection; iOS-реалізація
  залишається нездійсненою (той самий блокер, що M7/M10).
- **Мануальна перевірка на реальному пристрої/емуляторі НЕ пройдена**
  — у цьому середовищі розробки немає доступного `adb`/емулятора;
  `./gradlew :androidApp:assembleDebug` збирається успішно, усі
  автотести (jvmTest/testAndroidHostTest/ktlintCheck/detekt) зелені,
  але живий смоук-тест каруселі на пристрої — відкритий пункт,
  перенесений у мануальний чекліст перед мержем.

---

### M12.5 — Diary follow-ups: дати поїздки, автоімпорт фото, синк фото між пристроями
Три знахідки з мануального тестування M8-M12: (1) `PairingContainer`
досі хардкодить `today..today+14`, вибору дат поїздки нема; (2)
capture→processing пайплайн (M10-M12) не має реального тригера — фото
треба підхопити самостійно; (3) `Photo.uri` — локальний `content://`
URI, синкається в Firestore як є, тож партнер отримує нерозв'язний
рядок замість фото. Три незалежні фічі в межах одного мілстоуна;
послідовність між ними важлива (нижче).

**Послідовність, критично важлива**: фіча "синк фото" має йти ПЕРЕД
"автоімпортом" — обидва шляхи захоплення (ручний з M12 і фоновий новий)
проходять через один і той самий `EpisodeProcessingPipeline`, тож якщо
автоімпорт зробити першим, кожне автопідхоплене фото відтворить той
самий `content://`-баг, який має виправити фіча синку. "Дати поїздки"
незалежна від обох, іде в будь-якому порядку. Виконується частинами, з
чисткою контексту між ними — кожна фіча своя окрема гілка/сесія.

**Accept — Дати поїздки (`feature:pairing`) ✅ done:**
- [x] Юніт-тест: конвертація epoch-millis (Material3 DatePicker) ↔
      `LocalDate` (kotlinx-datetime) коректно проходить round-trip —
      `DatePickerConversionsTest` (4 тести, включно з межею року й
      високосним днем)
- [x] `PairingContainerTest`: підтвердження обраних дат викликає
      `PairingRepository.createTrip` саме з цими датами, не
      `today+14`
- [x] `PairingContainerTest`: крок вибору дат відкривається
      попередньо заповненим `today..today+DEFAULT_TRIP_LENGTH_DAYS`
- [x] `PairingScreenNavigationTest`: повний create-флоу проходить
      `CHOICE → CREATE_PICK_DATES → CREATE_SHOW_CODE → PAIRED`
- [x] Screenshot-тести: `@Preview` кроку вибору дат → Roborazzi-голдени

  Відхилення: Material3 `DateRangePicker` (узгоджено з користувачем —
  стандартна поведінка/валідність діапазону "з коробки" важливіша за
  візуальну відповідність ink/paper-дизайну решти Pairing-кроків, тож
  крок навмисно виглядає інакше, ніж `PaperCard`-картки навколо).
  Домен (`PairingRepository.createTrip`) уже приймав довільні дати —
  змін поза `feature:pairing` не знадобилось. Кнопка "Confirm Dates"
  вимкнена при `endDate < startDate`, хоча на практиці
  `DateRangePickerState.setSelection` сам не дає обрати таку пару.

**Accept — Синк фото через Firebase Storage ✅ done:**
- [x] Тест Room-міграції v6→v7: `Photo.remoteUrl` (nullable) додається,
      старі рядки бекфіляться `NULL`, новий запис round-trip'иться
- [x] Юніт-тести `FirebaseStorageApi` проти Ktor `MockEngine`: успіх
      (включно з побудовою download-URL з `downloadTokens`), 4xx, 5xx,
      timeout, malformed body
- [x] Юніт-тест `FirebaseStorageUploadClient`: мапінг
      exception → `PhotoUploadResult.Failure`
- [x] `EpisodeProcessingPipelineTest`: кожне фото кластера (не лише
      репрезентативна підмножина для Gemini) отримує спробу
      завантаження; збій одного фото не валить увесь епізод; `remoteUrl`
      кожного фото відповідає тому, що повернув фейковий клієнт
- [x] `EpisodeFirestoreMapperTest`: `Photo.remoteUrl` round-trip'иться
      (присутній і `null` кейси)
- [x] Інтеграційний тест (`data/src/jvmTest`): запушені в Firestore
      поля синканого епізоду містять `remoteUrl`, не `content://...`
- [x] **Чесне обмеження**: реальний виклик
      `BitmapFactory`/`Bitmap.compress` (`AndroidPhotoCompressor`) не
      юніт-теститься напряму (немає фейкового `BitmapFactory`, той
      самий клас проблеми, що `ContentResolver`-код); реальний рендер
      фото на пристрої партнера (image-loading бібліотека) лишається
      задокументованим наступним кроком — `EpisodePhotoGallery`
      лишає плейсхолдер-тайли, `remoteUrl` просто протікає крізь них
      невикористаним
- [x] Мануальний чекліст: реальний крос-пристрій смоук пройдено на
      реальному Android-пристрої (Pixel 8 Pro) + емуляторі — фото,
      зняте на пристрої A, реально завантажилось за `remoteUrl` після
      синку (перевірено `curl` — `HTTP 200 image/jpeg`), і синканий
      епізод дотягнувся до пристрою B через новий `DiaryContentPuller`
      (див. відхилення нижче)

**Відхилення від початкового плану, усі знайдені й виправлені під час
реального крос-пристрій смоуку (2026-07-20):**
- **Критичний pre-existing баг у `firestore.rules`, не пов'язаний з
  M12.5 напряму, але який блокував саму можливість це перевірити:**
  read-правила для `diaryEntries`/`episodes`/`placeCandidates`
  розіменовували `resource.data` без урахування випадку, коли
  документ ще не існує (`resource == null`). `SyncCoordinator`'s
  preflight-читання (перед КОЖНИМ першим пушем нового документа) через
  це отримувало `403 PERMISSION_DENIED` замість чистого "not found",
  і трактувало це як "unreachable, retry пізніше" — тобто жоден
  DiaryEntry/Episode ніколи не досягав Firestore насправді, хоча
  локально виглядало, що все синкається. Виправлено додаванням
  `resource == null ||` перед усіма трьома правилами; задеплоєно в
  продакшн (`firebase deploy --only firestore:rules`). Це, найімовірніше,
  та сама причина, чому M9/M11 мануальні смоук-чеклісти закривались
  "видима поведінка ок", а фактичні дані ніколи не перевірялись у
  Firestore-консолі напряму.
- **`Photo.id` — реальний `content://` URI, не UUID:** дизайн
  `FirebaseStorageConfig.objectPath` спирався на припущення "photo id
  вже безпечний рядок" (з юніт-тестів, де fixture-і завжди мали прості
  id на кшталт `"p1"`). Реальний Android capture-шлях (`Photo.id = uri`,
  рішення з M10) підставляє `content://...` рядок з `://`, що порушує
  заборону Firebase Storage на послідовні `/` в імені об'єкта —
  кожен реальний аплоуд падав з `ClientError` "path may not end with
  '/' or contain two consecutive '/'s". Виправлено санітизацією
  photoId (усе поза `[A-Za-z0-9_.-]` → `_`) перед побудовою
  object-шляху; регресійний тест — `FirebaseStorageConfigTest`.
- **Android Photo Picker (`PickVisualMedia`) видаляє GPS EXIF
  незалежно від дозволів** — підтверджено емпірично: фото з реальними
  GPS-координатами (перевірено `exiftool` на оригінальному файлі)
  читалось назад через Picker-URI з `latLong = null`, навіть з
  наданим `ACCESS_MEDIA_LOCATION`. Це задокументована поведінка
  уніфікованого Photo Picker (окремий, привacy-мотивований шлях
  доступу, відмінний від класичного `MediaStore`). Виправлено заміною
  `ActivityResultContracts.PickMultipleVisualMedia` на
  `OpenMultipleDocuments` (SAF, системний Files-пікер) у тимчасовому
  `PhotoPickerLauncher` — GPS читається коректно через цей шлях.
- **Додано `DiaryContentPuller`/`FirestoreDiaryContentPuller`,
  узгоджено з користувачем як розширення скоупу поза M12.5** — до
  цього в кодовій базі взагалі не існувало шляху "затягнути з
  Firestore діарі-контент партнера в локальний Room": `observeByTrip`/
  `observeByDiaryEntry` — суто локальні Room-запити, а
  `SyncCoordinator` пуляла тільки ВЛАСНІ операції пристрою. Реалізовано
  запитом `diaryEntries` по `tripId` + `episodes` по `diaryEntryId`
  (виключно для записів ІНШОГО userId — власні дані пристрій ніколи
  не переписує цим шляхом, бо Firestore-правила відхилили б `update`
  чужого документа), періодичний пул (5с) під час перегляду Timeline,
  зі власним `DiaryTimelineDataSource` (виділено з
  `DiaryTimelineContainer` через ту саму причину, що й
  `DiaryCaptureCoordinator` — detekt's `LongParameterList`).
  **"Add Photos"-пікер лишено як реальну фічу** (узгоджено з
  користувачем) — `app/.../capture/PhotoPickerLauncher` (SAF,
  `OpenMultipleDocuments`) + кнопка в `DiaryTimelineScreen` тепер
  постійна ручна точка входу для захоплення, не лише смоук-тест-аід:
  користувач сам обирає фото з Files, замість очікування на майбутній
  автоімпорт (WorkManager, окрема частина цього ж мілестоуна).
- **Product-фідбек від користувача під час смоук-тесту, зафіксовано як
  TODO для майбутнього мілестоуна, свідомо НЕ реалізовано зараз:**
  поточна умова розблокування дня (`resolveDayUnlockState`) відкриває
  день щойно ОБИДВА `DiaryEntry` мають `syncStatus == SYNCED` —
  буквально одне синхронізоване фото з кожного боку вже "закриває"
  день. Користувач вважає це занадто слабким: потрібна явна дія
  "закрити день" (а не автоматичне закриття по першому фото), і UI
  копірайтинг типу "У щоденнику вже є записи! Продовжуй або подивись,
  що було цього дня" замість поточного нейтрального стану очікування.
  Потребує окремого дизайн-обговорення (нового `DiaryTimelineIntent`,
  зміни моделі стану `DiaryDayStatus`/`DayUnlockState`, UI) — не
  зачіпати мимохідь у майбутніх мілестоунах, це самостійна задача.

**Автоімпорт фото (WorkManager) — винесено в окремий майбутній
мілстоун (номер TBD).** Консультація під час планування M12.11 (2026-07-21)
знайшла нерозв'язаний продуктовий ризик: автоімпорт без курації мовчки
завантажив би повний Gemini+Storage-пайплайн на будь-яке фото з
діапазону дат поїздки (чеки, випадкові кадри, приватне) без вибору
користувача, плюс реальний Android 14 partial-photo-access підводний
камінь. Чернетка Accept-критеріїв (не змінена, тільки перенесена) —
див. кінець секції M12.11, "Занотовано для майбутнього мілстоуна".

---

### M12.6 — Ручне закриття дня (Close Day) ✅ done

Продуктовий фідбек з M12.5's смоук-тесту: поточне розблокування дня
(`resolveDayUnlockState`) відкривало день щойно ОБИДВА `DiaryEntry`
мали `syncStatus == SYNCED` — буквально одне синхронізоване фото з
кожного боку вже "закривало" день. Узгоджено з користувачем: кожна
сторона закриває свій день явною дією ("Close Day"); додатково будь-
який день, чия дата вже минула, автоматично вважається закритим без
окремого таймера/фонового джоба — просто порівнянням `entry.date` з
`today`. Повторний "Add Photos" для дня, що вже має записи, показує
проміжне повідомлення (без можливості переглянути, що вже знято —
це має лишатись сюрпризом до обопільного закриття); закриття
остаточне — фото на цій стороні для цього дня більше не додати.

**Accept:**
- [x] `core:model`: `DiaryEntry.closedAt: Instant? = null`
- [x] `core:database`: `MIGRATION_7_8` (`diary_entries.closedAt`,
      nullable INTEGER, без backfill), версія БД 7→8, міграційний тест
      (стара строка → `closedAt == null`, новий запис round-trips
      non-null значення)
- [x] `core:domain`: `DiaryDayStatus` отримує новий стан `OPEN`
      (синхронізовано, але ще не закрито і дата не минула);
      `diaryDayStatus(entry, today)` — READY тільки якщо
      `closedAt != null || entry.date < today`; `resolveDayUnlockState`
      не змінювався — це й була вся суть
- [x] `data`: `DiaryEntryFirestoreMapper` round-trips `closedAt`
      (present/null), партнерський `closedAt` підтягується існуючим
      `FirestoreDiaryContentPuller` без змін коду там
- [x] `feature:diary`: `DiaryTimelineIntent.ProcessCapturedPhotos`/
      `CloseDay` беруть явний `date` — це день, що зараз в центрі
      карусельного пейджера (`pagerState.currentPage`), НЕ обов'язково
      `today`; `DiaryCaptureCoordinator.closeDay()`, кнопка "Close Day"
      (з'являється поруч з "Add Photos", тільки коли обраний день вже
      має ВЛАСНІ епізоди й ще не закритий), діалог-підтвердження при
      повторному "Add Photos" на дні з власними епізодами ("You
      already have entries for this day! Keep adding." — єдина дія
      "Continue", без перегляду вже знятого), і лейбл "This day's
      entry is closed" замість обох кнопок, коли закрито
- [x] Юніт/контейнер-тести на кожному шарі (включно з кейсами для
      дня, відмінного від `today`) + 3 нові screenshot-голдени для
      трьох станів кнопкової зони (порожньо / відкрито з записами
      / закрито), тепер поверх реальної day-картки, не Countdown

**Відхилення:**
- **Мід-імплементаційний піврот: з "тільки today" на "day, обраний в
  карусельці"** — під час мануального тестування (два пристрої,
  owner+member) з'ясувалось, що прив'язка виключно до `today` робить
  дебаг незручним (не можеш протестувати довільний день поїздки без
  синхронізації системного часу обох пристроїв). Узгоджено з
  користувачем: `Add Photos`/`Close Day` тепер діють на день, що
  зараз показаний в пейджері (будь-який, без обмеження діапазоном
  поїздки чи `today`) — свідомо тимчасова, найширша поведінка для
  зручності тестування. **Явно занотовано як майбутнє обмеження, яке
  ще належить додати** (не зараз, щоб не ламати поточну логіку):
  сховати "Add Photos" для дня, що не є `today` і/або поза діапазоном
  дат поїздки (тобто до початку поїздки додати фото не можна) —
  окрема невелика задача поверх вже готової day-scoped архітектури.
- **`resolveDayLockReason` лишився без змін навмисно** — `OPEN`
  потрапляє в ту саму гілку "не NOT_READY", що й раніше
  `PENDING_SYNC`/`READY`, тож копірайтинг далі каже "waiting for the
  network to confirm", навіть коли партнер зняв фото, але просто ще
  не натиснув "Close Day". Ця функція вже мала окрему, існуючу до
  цього мілестоуна проблему — вона ніколи не враховує *свій* статус
  (якщо саме ВЛАСНА сторона ще не зняла/не закрила, а партнер вже
  готовий, копірайтинг все одно звинувачує мережу). Виправлення цього
  повністю вимагає нових варіантів тексту "ти сам ще не готовий" проти
  "партнер не готовий" — свідомо не займався цим тут, це окрема задача.
- **Копірайтинг англійською**, не тим українським плейсхолдером, що
  проговорювався в чаті — узгоджено з користувачем, що це тимчасовий
  текст і буде замінений пізніше; англійська узгоджується з рештою
  екрана ("Add Photos", "Your partner is still out there" тощо).
- **Діалог-підтвердження лишився локальним `feature:diary`-компонентом**,
  не виніс у `core:ui` — жодного `AlertDialog`/модального компонента в
  дизайн-системі ще не було, і це єдина точка виклику; виносити зараз
  було б передчасною абстракцією.
- **Знайдено і виправлено реальний visual-баг через записаний
  screenshot-голден, не тільки код-рев'ю**: перша версія кнопки "Close
  Day" використовувала `AlongsideSecondaryButton` (світлий "paper"
  текст, розрахований на темне inkGradient-тло) — але кнопкова зона
  фактично спливає НАД кремовою `PaperCard` дня/кантдауна (яка займає
  майже весь екран), а не над темним тлом, тож текст ставав
  невидимим (кремовий на кремовому). Побачено на записаному
  Roborazzi-скріншоті, не здогадкою. Виправлено на `AlongsideTextButton`
  (приглушений сірий `labelMuted`), який читається на обох тлах;
  той самий фікс застосовано до лейбла закритого дня.
- **Знайдено під час дводеврайсного мануального тесту (owner=емулятор,
  member=телефон), некритично для цього мілестоуна, але задокументовано
  й заплановано нижче як M12.7**: `member`-пристрій показував порожній
  `ownEntries`, хоча відповідний `DiaryEntry` реально існує в Firestore
  (перевірено прямим REST-запитом) — власні дані, синхронізовані ДО
  того, як на пристрої очистили локальне сховище (частина протоколу
  мануального смоук-тесту M12.5), не підтягуються назад, бо
  `FirestoreDiaryContentPuller` навмисно фільтрує тільки партнерські
  записи (`userId != ownUserId`), щоб не переписувати власні документи
  повторно.

---

### M12.7 — Відновлення власних даних після втрати локального сховища ✅ done

**Проблема (знайдено 2026-07-20 під час мануального тесту M12.6,
користувач наполіг зафіксувати як обов'язкове до виправлення):** якщо
локальне сховище пристрою втрачається (очищення app data/cache,
перевстановлення, новий пристрій) вже ПІСЛЯ того, як `DiaryEntry`/
`Episode` користувача були успішно синхронізовані у Firestore,
застосунок не мав шляху підтягнути ці власні дані назад. `Room`
порожній, хоча документи реально існують у хмарі — користувач бачить
"порожній" щоденник, хоча насправді дані просто локально недоступні.
`FirestoreDiaryContentPuller` (M12.5) навмисно тягнув ТІЛЬКИ партнерські
записи (`entry.userId != ownUserId`) — свідоме рішення M12.5, щоб
уникнути повторного запису/конфлікту з власними документами через
sync-queue, але як побічний ефект це закривало єдиний існуючий шлях
відновлення власних даних.

**Рішення:** `DiaryContentPuller.pullPartnerContent` перейменовано на
`pullTripContent` і розширено — партнерські записи як і раніше
безумовно перезаписуються з хмари на кожен виклик (локальне сховище
ніколи незалежно не редагує партнерські документи, тож перезаписувати
нема ризику); власні записи тепер теж тягнуться, але **тільки якщо
локально відсутні взагалі** (`localDiaryEntryRepository.getById(id) ==
null`) — жодного порівняння `updatedAt`/конфлікт-резолюції не
знадобилось: якщо локальний рядок існує в БУДЬ-ЯКОМУ стані, він
лишається недоторканим, і вся ця "self-heal"-гілка просто ніколи не
спрацьовує, поки локальний рядок є. Це прибирає ризик, що періодичний
пул (кожні 5с) перезатре щойно закритий локально (ще `PENDING`) день
застарілою хмарною версією.

**Accept:**
- [x] `core/domain/.../DiaryContentPuller.kt`: перейменовано метод +
      оновлено KDoc, що описує асиметрію partner-always-overwrite /
      own-only-if-missing
- [x] `data/.../FirestoreDiaryContentPuller.kt`: `pullTripContent` — для
      `entry.userId == ownUserId` перевіряє `getById` перед upsert;
      відповідно episodes для такого entry теж тягнуться лише ті, яких
      ще нема локально (`pullEpisodes(..., onlyIfMissingLocally = true)`)
- [x] `feature:diary`'s `DiaryTimelineDataSource`: перейменовано
      `pollPartnerContent` → `pollTripContent`, `PARTNER_CONTENT_POLL_INTERVAL`
      → `TRIP_CONTENT_POLL_INTERVAL`, той самий 5-секундний періодичний
      пул тепер відновлює й власні дані, не тільки партнерські
- [x] Тести: `FirestoreDiaryContentPullerTest` — власний entry відсутній
      локально → підтягується разом з епізодами; власний entry вже є
      локально → НІКОЛИ не перезаписується (і episodes-запит для нього
      взагалі не робиться); існуючі партнерські тести незмінні по суті,
      лише перейменований виклик

**Бонусний фікс, знайдено в тому ж мануальному тесті (обидва пристрої
закрили день, обидва `closedAt` реально дійшли до Firestore —
перевірено напряму, а день все одно лишався LOCKED з "Almost there"):**
`SyncCoordinator.applyBatchResult` видаляв успішну операцію з durable
черги ДО того, як позначити локальний рядок `SYNCED`
(`store.remove(...)` перед циклом `markStatus(...)`). Якщо корутина
скасовується саме між цими двома кроками (наприклад, екран, що тримає
sync-loop живим, покидає композицію одразу після успішного пушу) —
push вже відбувся, операція назавжди зникає з черги, а локальний
рядок лишається застряглим у старому статусі (`PENDING`) НАЗАВЖДИ, бо
retry вже нема що. Саме це трапилось з `closedAt` на обох пристроях:
`sync_operations` — порожня таблиця на обох, `diary_entries.syncStatus`
— досі `PENDING` на власному записі кожного, хоча `closedAt` вже
коректно в Firestore. `diaryDayStatus` вимагає `syncStatus == SYNCED`
ще до перевірки `closedAt`, тож день лишався LOCKED назавжди, без
жодного механізму самовиправлення.
- [x] Виправлено порядок в `SyncCoordinator.applyBatchResult`:
      `markStatus` тепер завжди ДО `store.remove()`/`store.markRetry()`
      — найгірший наслідок того самого скасування тепер лише зайвий
      (нешкідливий, ідемпотентний) повторний push наступного циклу,
      а не назавжди застрягла локальна невідповідність
- [x] Новий тест в `SyncCoordinatorTest`: підмінені store/binding
      фіксують порядок викликів, підтверджують `markStatus` перед
      `remove`
- [x] Два вже застряглих локальних записи (по одному на кожному
      тестовому пристрої) виправлено вручну через прямий SQL-патч
      (`UPDATE diary_entries SET syncStatus='SYNCED'`) — разовий
      ручний фікс тестових даних, не частина застосунку; сам код-фікс
      вище запобігає повторенню наперед

**Справжня першопричина (той самий бонусний фікс вище лише зменшував
шкоду, не усував джерело) — знайдено додаванням `bindingFor(...) ==
null`-логування, коли той самий баг ВІДТВОРИВСЯ ЗНОВУ на новому дні
навіть з уже виправленим порядком `markStatus`/`store.remove()`:**
жодного разу за весь мануальний тест не спрацював
`DiaryEntrySyncEntityBinding.markStatus`, хоча `SyncCoordinator` явно
доходив до `applyBatchResult` й завершував його щоразу. `DataModule.kt`
оголошував `single<SyncEntityBinding> { ... }` тричі (Trip/DiaryEntry/
Episode) БЕЗ кваліфаєрів — підтверджено окремим Koin-тестом
(`KoinSameTypeMultiBindTest`), що це призводить до тихого
перезаписування: кожен наступний `single<T>` з тим самим типом і без
кваліфаєра замінює попередній запис у реєстрі Koin, тож
`getAll<SyncEntityBinding>()` реально повертав ЛИШЕ ОСТАННЄ оголошення
(`EpisodeSyncEntityBinding`) — `TripSyncEntityBinding` і
`DiaryEntrySyncEntityBinding` взагалі ніколи не потрапляли в
`SyncCoordinator.bindings`. Це існувало ще з M9/M11, повністю невидиме
для юніт-тестів (усі конструюють `SyncCoordinator` з явним списком
`bindings`, ніколи не проганяючи через реальний Koin `getAll`).
- [x] Виправлено на `single { Concrete() } bind Interface::class` для
      всіх трьох `SyncEntityBinding`-реєстрацій в `DataModule.kt` —
      кожна лишається адресована за власним конкретним типом, але й
      далі коректно потрапляє в `getAll<SyncEntityBinding>()`
- [x] Новий permanent-регресійний тест `KoinSameTypeMultiBindTest`
      (`data/src/jvmTest/.../sync/`) — фіксує сам патерн (не залежить
      від конкретних Trip/DiaryEntry/Episode класів), щоб цей клас
      Koin-помилки не міг непомітно повернутися
- [x] Усі застряглі локальні записи на обох тестових пристроях (три
      дні поспіль — 20, 21, 22 липня — відтворювало баг тричі,
      підтверджуючи, що це не одноразовий збіг) виправлено тим самим
      разовим SQL-патчем після встановлення реального фіксу

**Ще один бонусний фікс, знайдено в тому ж тесті:** на другому й
третьому днях в епізодів взагалі не було `description` — в консолі
Gemini показував `503 UNAVAILABLE` ("model overloaded", типово для
free tier). Це вже коректно оброблялось (епізод все одно зберігався,
просто без опису — `EpisodeProcessingPipeline` саме так і
спроєктований), але ретраїв не було взагалі — жодна спроба, жодного
backoff. `Episode.descriptionAttempts` (з M10) виявився чистим
скафолдингом: завжди хардкодиться в `1`, ніде не використовується для
жодної реальної retry-логіки.
- [x] `GeminiVisionApi.generateContent` тепер ретраїть
      `GeminiException.ServerError` (5xx) і `NetworkTimeout` з
      експоненційним backoff + jitter (1с, 2с, 4с бази + випадкова
      добавка до 30% — запобігає "thundering herd", коли всі клієнти
      б'ють у сервер, що саме відновлюється, одночасно), до 4 спроб
      загалом за замовчуванням. `ClientError` (4xx — напр. вичерпана
      квота з реального прикладу в тестах цього ж файлу) і
      `MalformedResponse` навмисно НЕ ретраяться — повтор їх не
      виправить
- [x] Нові тести в `GeminiVisionApiTest`: 503 → успіх з другої спроби;
      persistent 503 → вичерпує спроби й кидає `ServerError`; backoff
      між спробами зростає; 4xx і malformed response — жодного ретраю

**Критичний фікс, знайдено тим самим мануальним тестом — реальний
краш, не просто відсутність опису:** `OutOfMemoryError` під час
JSON/base64-кодування фото для Gemini-запиту (`Failed to allocate a
257873392 byte allocation` — майже чверть гігабайта в одну
алокацію). `AndroidPhotoCompressor`'s власний KDoc-коментар прямо
стверджував: "deliberately not applied to the bytes fed to Gemini
vision (that path already works uncompressed)" — це трималось лише на
маленьких, вручну підготовлених тестових фото; реальне фото з камери
Pixel 8 Pro (50МП) важить 10-20МБ нестиснутим, і саме таке фото
розвалило застосунок вщент (`FATAL EXCEPTION`, процес гине до того,
як Gemini взагалі встигає відповісти — тому опис показувало як
"null", хоча насправді причина зовсім не в Gemini).
- [x] `AndroidPhotoCompressor` тепер СПОЧАТКУ маштабує найдовшу сторону
      до 1600px (`Bitmap.createScaledBitmap`), потім вже JPEG quality
      80 — раніше маштабування не було взагалі, тільки якість
- [x] `imageBytesLoader` в `DiaryModule.kt` (Gemini vision шлях) тепер
      теж проходить через `PhotoCompressor.compress(...)` — раніше
      використовувались сирі байти напряму з `PhotoByteReader`, без
      жодного стиснення; шлях завантаження в Storage й раніше вже
      використовував той самий компресор

---

### M12.8 — Реальна фотогалерея епізоду (Coil + shimmer) ✅ done

Досі `EpisodePhotoGallery` рендерив просто кольорові плейсхолдер-бокси
(`PhotoPlaceholderTile`) — жодного реального завантаження фото не
було взагалі, ще з M12. Узгоджено з користувачем: лейзі horizontal
grid (не вертикальна колонка) + справжнє завантаження зображень через
Coil + shimmer-анімація на місці ще не завантаженого фото.

**Accept:**
- [x] Coil 3.5.0 доданий у version catalog
      (`coil-compose`/`coil-network-ktor3` — останній перевикористовує
      вже наявний у проєкті Ktor-стек замість тягнути OkHttp окремо
      для картинок)
- [x] `core:ui`: `Modifier.shimmer()` (`core/ui/animation/Shimmer.kt`)
      — біжучий градієнт, той самий патерн що `PulsingDot`
      (`rememberInfiniteTransition`); `AsyncPhotoTile`
      (`core/ui/component/`) — `SubcomposeAsyncImage` з shimmer на
      `Loading`, приглушеним тайлом на `Error`; `installAlongsideImageLoader`
      встановлює `SingletonImageLoader` з `KtorNetworkFetcherFactory`,
      викликається раз з `AlongsideApplication.onCreate()`
- [x] `EpisodePhotoGallery` (`feature:diary`) переписаний на
      `LazyHorizontalGrid(rows = GridCells.Fixed(2))` — фото тепер
      течуть горизонтально, скролиться при переповненні, замість
      вертикальної колонки `StaggerRevealColumn`; stagger-reveal
      таймінг (80ms/фото, той самий accept-критерій з M12) відтворено
      напряму (той самий `LaunchedEffect`-патерн, `StaggerRevealColumn`
      сам лишився недоторканим — він і далі vertical-only, generic
      компонент для інших use-case'ів)
- [x] Кожен тайл вантажить `photo.remoteUrl`, якщо є, інакше локальний
      `photo.uri` (`content://` — Coil приймає обидва напряму)
- [x] Існуючий `EpisodePhotoGalleryTest` (порядок stagger-reveal)
      пройшов без змін логіки тесту; оновлено 1 screenshot-голден
      (`UnlockedDayPreview` — фото тепер у гріді, не в колонці)

---

### M12.9 — Fullscreen photo viewer з pinch-to-zoom ✅ done

Продовження M12.8: нормального розміру мініатюри (64px → 96px),
клік по мініатюрі відкриває fullscreen-переглядач з pinch/double-tap
zoom і свайпом між фото епізоду. Розроблено спочатку в `playground`
(простими кольоровими плейсхолдерами замість реальних фото — Coil там
не налаштований, і не мав бути, оскільки мета була суто в жестах/лейауті).

**Accept:**
- [x] `core:ui`: `ZoomableState`/`Modifier.zoomable()`
      (`component/Zoomable.kt`) — pinch-to-zoom + drag-pan + double-tap
      toggle, затиснуто в межах контенту (не дає запанити за краї).
      Свідомо НЕ centroid-anchored (зум завжди від центру контенту, не
      від точки пальців) — простіше, все одно природньо відчувається,
      повноцінна фокус-точкова математика окремої zoom-бібліотеки була
      б overkill для цього обсягу
- [x] `FullscreenPhotoViewer` (`component/FullscreenPhotoViewer.kt`) —
      `HorizontalPager` (по фото на сторінку) + zoomable на поточній
      сторінці (скидається при переході на іншу — не лишається
      заzoomленим назавжди), темний скрім, кнопка закриття,
      `PagerDots` знизу якщо фото більше одного; окрема "Couldn't load
      this photo" помилка замість тихого порожнього кадру при
      fullscreen-розмірі
- [x] `AsyncPhotoTile` отримав `onClick` + дефолтний розмір збільшено
      64.dp → 96.dp
- [x] `EpisodePhotoGallery` (`feature:diary`) — клік по тайлу відкриває
      `FullscreenPhotoViewer` через `Dialog(usePlatformDefaultWidth =
      false)` (єдиний спосіб коректно "втекти" з `PaperCard`/`Column`
      ієрархії картки на весь екран, а не лише в межах картки)
- [x] `playground`: секція "Photo gallery" — той самий
      `Modifier.zoomable`/`rememberZoomableState` з `core:ui`, кольорові
      градієнти замість фото, окреме Desktop `Window` як fullscreen-вьювер
      для живої ітерації жестів через hot reload
- [x] Оновлено screenshot-голден (`UnlockedDayPreview` — більші тайли)

**Бонусний фікс #1, знайдено в мануальному тесті на обох пристроях
(2026-07-20) — фулскрін-вьювер завжди показував те саме фото, хоч
би на яку мініатюру не тапнули:** `EpisodeSection`'s `episodes.forEach`
не мав `key(episode.id)` — коли реактивно опитуваний список епізодів
переставлявся/доповнювався (живий пул кожні 5с), Compose тихо
переносив запам'ятований стан (`fullscreenIndex`, stagger-reveal
прогрес) одного епізоду на інший епізод, що займав ту саму позицію в
списку. Підтверджено прямим SQL-запитом до обох пристроїв, що жоден
епізод не мав більше 1 фото — тобто дублікатів даних не було, проблема
була суто в Compose-стані.
- [x] `episodes.forEach` обгорнуто в `key(episode.id) { ... }`
- [x] `EpisodePhotoGallery`'s `LazyHorizontalGrid`'s `items(...)` тепер
      з явним `key = { index -> photos[index].id }`

**Бонусний фікс #2, знайдено відразу після фіксу #1 в тому ж
мануальному тесті — картка галереї безперервно перемальовувалась на
телефоні:** `FirestoreDiaryContentPuller.pullTripContent`'s
партнерська гілка (з M12.5/M12.7) безумовно викликала `.upsert(...)`
на КОЖЕН 5-секундний тік пулу, навіть якщо дані не змінились. Room's
Flow invalidation спрацьовує на будь-який запис у спостережувану
таблицю, незалежно від того, чи реально змінився вміст рядка — тобто
кожні 5с відбувався повний recomposition Timeline-екрану без жодної
причини.
- [x] Новий приватний `upsertEntryIfChanged`: порівнює
      `localDiaryEntryRepository.getById(entry.id) == entry` перед
      upsert, пропускає запис якщо ідентично
- [x] `pullEpisodes`'s партнерська гілка (`onlyIfMissingLocally =
      false`) так само тепер порівнює `existing != episode` перед
      upsert
- [x] Нові тести в `FirestoreDiaryContentPullerTest`: незмінний
      партнерський entry/episode при повторному пулі — НЕ
      перезаписується; змінений — все ще перезаписується

**Справжня першопричина нестабільності картки галереї (бонусний фікс
#2 вище лише зменшував частоту записів, не усував джерело) — знайдено
2026-07-20 через тимчасове діагностичне логування прямо в
`pullEpisodes`, коли користувач помітив, що партнерське фото на
Day 1 то з'являється на долю секунди, то знову зникає:** `photos`
мав PRIMARY KEY лише на `id` (сам `Photo.id` — це `content://` URI
фото, унікальний в межах ОДНОГО фізичного файлу, але не в межах
епізоду). Тестові дані на емуляторі повторно використали той самий
фізичний файл (`test_photo2.jpg`) для епізодів у двох різних днів —
обидва отримали ІДЕНТИЧНИЙ `Photo.id`, лише з різним `episodeId`.
`EpisodeDao.upsert`'s `INSERT OR REPLACE` на такому ключі означає:
кожен `pullEpisodes`-тік (кожні ~2.5с) "краде" єдиний фізичний рядок
фото в базі назад до того епізоду, який щойно себе апсертнув,
забираючи його в іншого. Обидва епізоди від цього постійно бачили
`existing.photos = []` (бо в цей момент рядок належав "іншому"), що й
викликало як видиме мигтіння фото, так і безкінечний, самопідтримний
цикл зайвих Room-записів/recomposition — цей коренний баг existed
незалежно від фіксу #2 вище, і саме тому той фікс сам по собі
"нічого не змінив" з точки зору користувача.
- [x] `PhotoEntity`: PRIMARY KEY розширено з `(id)` на `(id,
      episodeId)` (`primaryKeys` замість `@PrimaryKey` на полі) —
      кожен епізод тепер власник свого рядка незалежно від того, який
      `id` в нього збігається з чужим
- [x] `MIGRATION_8_9` (v8 -> v9): SQLite не може розширити PRIMARY KEY
      на місці — таблиця `photos` перестворюється з новою схемою й
      переливається; `AlongsideDatabase` version 8 -> 9
- [x] Новий тест `EpisodeDaoTest`: два різні епізоди з ОДНАКОВИМ
      `Photo.id` — кожен зберігає свій рядок незалежно, повторний
      upsert одного не витісняє інший
- [x] Новий тест `MigrationTest` (8 -> 9): існуючий рядок фото
      зберігається після міграції; другий епізод з тим самим `id`
      фото, вставлений через новий (post-migration) DAO, не витісняє
      перший

---

### M12.10 — Стабільність capture-пайплайну (дублі записів + втрата епізодів) ✅ done

Мануальне тестування виявило нову партію багів: після закриття дня
ніколи не показувалось більше одного епізоду з кожної сторони, і було
незрозуміло, за яким принципом обирається саме цей епізод; фото
надійно доходили до Firebase Storage, але Episode-сутність не завжди
створювалась; і окремо — вибір фото, потім одразу ще раз вибір нового
фото до завершення попередньої обробки, іноді призводило до того, що
в Storage потрапляло лише останнє фото, а сутності взагалі не
доходили до Firestore. Дослідження (пряме читання коду + три
паралельні Explore-агенти: семантика Orbit MVI intent-диспетчеризації,
реальні реалізації upload/geocode/vision-клієнтів, виклик
photo-пікера та наявний sync-queue патерн) підтвердило чотири окремі,
конкретні першопричини.

**Причина 1 — дублікати `DiaryEntry` на один день, непередбачувано
який епізод показується:** `DiaryTimelineContainer.processCapturedPhotos`
резолвив `existingEntryId` з реактивного, потенційно застарілого
`state.ownEntries`. Це реальний рейс з двох незалежних причин: (a)
orbit-core 10.0.0's `intent { }`-диспетчер запускає кожен intent як
справді конкурентну корутину без `.join()` на попередню
(`Dispatchers.Unconfined` за замовчуванням, жодного cancel-on-new-intent)
— два швидкі виклики `processCapturedPhotos` реально виконуються
паралельно; (b) незалежно від цього, `observeTimeline()` — окремий,
завжди активний Flow-колектор, і ніщо не синхронізує "Room upsert
завершився" з "state.ownEntries це відображає". Два швидкі захоплення
на той самий день могли згенерувати два різні випадкові `entryId`
через `Uuid.random()`; `buildDiaryTimelineDays`'s
`ownEntries.associateBy { it.date }` тихо лишав лише один запис на
дату, губивши інший разом з його епізодом(ами) з відображення —
"переможець" залежав від довільного порядку списку/емісії.
- [x] `DiaryCaptureCoordinator.capture()`: замість `existingEntryId ?:
      Uuid.random()` — `existingEntryId ?: deterministicNewEntryId(tripId,
      userId, date)` (стабільний композитний рядок). Два конкурентні
      захоплення на день БЕЗ існуючого запису тепер завжди сходяться на
      той самий id незалежно від таймінгу; існуючий запис (знайдений
      через `state.ownEntries`) і далі має пріоритет — легасі-записи зі
      старими випадковими id не дублюються
- [x] Новий `DiaryCaptureCoordinatorTest`: два послідовні `capture()`
      виклики для того самого (tripId, userId, date) без існуючого
      entry дають РІВНО один `DiaryEntry`-рядок, з епізодами обох
      викликів присутніми

**Причина 2 — все-або-нічого пакетне збереження з реальними
необробленими винятками:** `EpisodeProcessingPipeline.process()` не
мав жодного try/catch; `DiaryCaptureCoordinator.capture()` теж. Два
реальні необроблені шляхи: `imageBytesLoader` (EXIF-читання +
стиснення, `DiaryModule.kt`) не мав жодного catch на всьому шляху
(може кинути `SecurityException`/`FileNotFoundException`/`IOException`/
`OutOfMemoryError`); і `FirebaseStorageUploadClient`'s шлях отримання
токена дозволяв винятку auth-кешу (`SessionFirestoreTokenProvider
.currentToken()`) прослизнути повз вужчий catch клієнта. Оскільки
нічого не зберігалось, доки НЕ повертався ВЕСЬ список епізодів, один
невдалий кластер (через будь-яку з цих прогалин) обнуляв весь батч —
губивши епізоди попередніх кластерів, чиї фото вже досягли Firebase
Storage.
- [x] `EpisodeProcessingPipeline.process()` отримав `onEpisodeReady:
      suspend (Episode) -> Unit` callback — кожен кластер персистується
      одразу після побудови, а не пакетом в кінці; `processClusterOrNull`
      ловить (`CancellationException` виключено) і пропускає лише
      невдалий кластер, логуючи, а не кидаючи далі — інші кластери й
      далі обробляються
- [x] `loadImageBytesOrNull` — новий приватний хелпер, обгортає кожен
      виклик `imageBytesLoader` (і для vision-репрезентативних фото, і
      для upload-циклу); фото, чиї байти не вдалось прочитати,
      деградує до "без зображення" замість падіння всього кластера
- [x] `SessionFirestoreTokenProvider.cachedSessionOrNull()` — новий
      приватний хелпер, ловить будь-який виняток з `cache.get()` і
      повертає null (== "неавтентифікований запит", вже існуючий
      оброблюваний кейс), а не дає йому просочитись повз
      `FirebaseStorageApi`/`FirestoreApi`'s вужчі catch-и
- [x] `DiaryCaptureCoordinator.capture()`: `DiaryEntry` тепер
      персистується ПЕРШИМ, до будь-якої обробки фото — навіть якщо
      EXIF-читання чи весь пайплайн впаде, день не втрачає свій запис
- [x] Нові тести: `EpisodeProcessingPipelineTest` (кластер 2 з 3 кидає
      необроблений виняток — кластери 1 і 3 все одно персистуються через
      `onEpisodeReady`; одне фото з невдалим читанням деградує саме
      воно, не весь кластер); `SessionFirestoreTokenProviderTest` (збій
      локального читання кешу дає null, а не кидає)

**Причина 3 — UI-рейс `captureDate`:** цільова дата фото-пікера
йшла через єдину спільну мутабельну `captureDate`-змінну в
`AlongsideApp.kt`, читану ВСЕРЕДИНІ callback'а пікера, а не захоплену
в момент запуску. Практично малоймовірно (системний пікер модальний,
блокує подальші тапи), але дешево виправити:
- [x] `captureDate` тепер скидається в `null` одразу після використання
      всередині callback'а — не лишається зі старим значенням
      назавжди

**Причина 4 — НЕ баг:** кластеризація (`EpisodeClustering.kt`,
2h/500m proximity) з великою ймовірністю працює як спроєктовано для
тісно розташованих тестових фото — легко сплутати з "завжди лише
один епізод" у поєднанні з причиною 1.

**Свідомо поза скоупом (узгоджено з користувачем):** один прогон
пікера сьогодні практично дає один епізод (фото одного батчу зазвичай
близькі за часом/місцем, тож кластеризуються разом) — це прийнято як
поточна модель, не змінюється цим фіксом. Підтримка пікером свідомого
вибору фото, що мають розбитись на кілька окремих епізодів в межах
одного прогону — окрема майбутня фіча (потребує і UX пікера, і
переробки групування для виклику Gemini vision). Так само — повноцінна
довговічна persistent-черга захоплення, що переживає вбивство
застосунку СЕРЕД обробки кластера (mid-upload/mid-Gemini-call) —
свідомо НЕ реалізовується: інкрементальне збереження вище вже дає
основну довговічність (раз епізод збережено локально, наявна
`SyncCoordinator`-черга довговічно ретраїть push у Firestore); лишається
лише вузький кейс "вбито рівно посеред обробки одного кластера" —
прийнятне обмеження, а не мовчазна прогалина.

**Бонусний фікс, знайдено відразу під час мануальної перевірки причини
2 вище (airplane-mode-посеред-захоплення):** entry+episode коректно
збереглись без втрати даних, як і задумано, але епізод лишився без
Gemini-опису — і, окремо, фото не завантажилось у Storage (на
пристрої партнера показувало "Couldn't load this photo", бо
`remoteUrl` лишився null, а локальний `content://` URI існує тільки
на пристрої, де фото було зроблено). Обидва — той самий клас
проблеми: щось не вдалось через тимчасову мережеву проблему, і НІЩО
ніколи не повертається до вже збереженого епізоду, щоб повторити
спробу.
- [x] `EpisodeProcessingPipeline.retryIncomplete(episode, languageTag)`
      — новий публічний метод: фото без `remoteUrl` отримує повторну
      спробу завантаження; відсутній `description` отримує повторний
      виклик vision-клієнта; використовує `episode.photos` як є, без
      пере-кластеризації чи пере-геокодингу. `descriptionAttempts`
      інкрементується ЛИШЕ якщо опис реально намагались згенерувати
      (тобто був null) — спроба довантажити лише фото не рахується
- [x] `DiaryCaptureCoordinator.retryIncompleteEpisodes(episodes)` —
      новий публічний метод: фільтрує епізоди, яким справді потрібна
      повторна спроба (`photos.any { remoteUrl == null }` АБО
      `description == null && descriptionAttempts < MAX_DESCRIPTION_ATTEMPTS`
      (5)), викликає pipeline, апсертить лише якщо щось змінилось.
      Ліміт стосується лише опису (Gemini-квота/стійка помилка —
      є сенс здатись); повторна спроба завантаження фото не лімітована
      цим лічильником — дешева й безпечна, окремого лічильника спроб
      на фото немає й не варто його заводити зараз
- [x] `DiaryTimelineDataSource`: новий фоновий поллінг-цикл (кожні
      30с — свідомо рідше за 5-секундний trip-content-пул, бо це
      реальні виклики Gemini/Storage, не дешевий локальний запит),
      бере власні епізоди з останньої відомої реактивної emisії й
      викликає `captureCoordinator.retryIncompleteEpisodes(...)`;
      конструктор отримав новий параметр `captureCoordinator`
      (DI-wiring в `DiaryModule.kt` оновлено)
- [x] Нові тести: `EpisodeProcessingPipelineTest` (`retryIncomplete` —
      довантажує фото, регенерує опис і рахує спробу, вже повний
      епізод лишає незмінним, невдала повторна спроба фото не кидає);
      `DiaryCaptureCoordinatorTest` (`retryIncompleteEpisodes` —
      лікує обидва дефекти разом, пропускає вже повний епізод, здається
      на описі після ліміту спроб, але й далі довантажує фото понад
      цей ліміт)

**Відома межа поточного рішення (заплановано на майбутнє, НЕ
реалізовано зараз):** `retryIncompleteEpisodes`'s поллінг-цикл живе
всередині `DiaryTimelineDataSource`, чий scope прив'язаний до
`DiaryTimelineContainer` (ViewModel) — тобто працює лише поки
Timeline-екран/застосунок живий в пам'яті. Якщо застосунок прибрали з
форграунда (свайп із recent apps, System kill під тиском пам'яті,
Doze) саме в вікні, коли епізод лишився неповним, retry просто ніколи
не відбудеться, поки користувач сам не відкриє Timeline знову. Причин
для незавершеності може бути декілька, не лише втрата мережі — сам
факт, що застосунок прибрали з форграунда посеред обробки, вже
достатній.

### M12.11 — Генералізована надійність фонової роботи: WorkManager/BGTaskScheduler-оркестратор ✅ done (iOS — no-op стаб); мануальна перевірка на реальному Pixel 8 Pro пройдена 2026-07-21/22, три знайдені баги виправлено (див. нижче)

Початкова чернетка описувала вузьку задачу: перенести лише
`retryIncompleteEpisodes` (episode-капчур ретрай) з in-memory поллінгу
на WorkManager. За прямим проханням користувача мілстоун розширено —
дослідження показало, що це один із **чотирьох** незалежно
реалізованих in-memory поллінг-луупів з однією й тією ж проблемою:
живуть лише поки живий Compose Container/ViewModel, гинуть при
закритті застосунку/kill процесу/перезавантаженні пристрою:

1. `DiaryTimelineDataSource.pollIncompleteEpisodes` (30с) — episode retry
2. `PlaceRetryDataSource.pollTrip` (30с) — place retry, структурно
   ідентичний до (1)
3. `FirestorePairingTripDataSource`'s поллер (5с) — **push**-половина
   (`pushPendingSync()` → `SyncCoordinator.sync()`, єдине, що сьогодні
   реально викликає `sync()` — жоден з чотирьох `Syncing*Repository` не
   робить цього сам) і **pull**-половина (`refreshFromRemote(userId)`
   — partner-join детекція на екрані очікування) в одному лупі
4. `DiaryTimelineDataSource.pollTripContent` (5с) — партнерський
   контент для UI; це НЕ проблема надійності (нема чого "довантажувати"
   коли застосунок закритий), лишається без змін

Дизайн пройшов три окремі консультації (Opus, як третя думка на
кожну частину): на seam+Android-інфраструктуру, на episode/place retry
міграції, і окремо — жорстко — на ідею автоімпорту фото.

**Ключове рішення, знайдене дослідженням і узгоджене з користувачем:
автоімпорт фото (`DiaryImportWorker`/`DiaryImportScanner`, чернетка
з M12.5) свідомо винесено в окремий майбутній мілстоун, номер TBD, НЕ
входить сюди.** Консультація виявила реальний нерозв'язаний
продуктовий ризик: автоімпорт без курації мовчки завантажив би повний
Gemini+Storage-пайплайн і показав партнеру БУДЬ-ЯКЕ фото з діапазону
дат поїздки (чеки, випадкові кадри, приватне) — без вибору
користувача. Наявний ручний SAF-пікер (`OpenMultipleDocuments`) — це
навмисна точка курації, вже задокументована в M12.5 як реальна фіча,
не заглушка до автоімпорту. Додається реальний Android 14
partial-photo-access підводний камінь
(`READ_MEDIA_VISUAL_USER_SELECTED` може мовчки зламати "бачити
майбутні фото" геть, якщо користувач обере "Select photos" замість
"Allow all"). М12.5's секція "Accept — Автоімпорт фото (WorkManager)"
(нижче) лишена як посилання на цей майбутній мілстоун, без дублювання
чеклиста.

**Спільний seam (`core:domain`, без платформних залежностей):**

```kotlin
public enum class BackgroundJobKind { EPISODE_RETRY, PLACE_RETRY, SYNC_QUEUE_FLUSH }

public interface BackgroundWorkScheduler {
    public fun scheduleOneOff(kind: BackgroundJobKind)
    public fun ensurePeriodicSweepScheduled()
}
```

Плейн-інтерфейс, не `expect`/`actual` — свідоме відхилення від
початкового формулювання чернетки. Той самий патерн, що
`EpisodeRepository`/`PlaceGeocodingClient`: контракт в `core:domain`,
платформні реалізації injected через Koin у композиційному корені.
`expect`/`actual` у цьому проєкті — для top-level функцій без
природної класової поліморфності (`PhotoPickerLauncher`,
`AlongsideNavDisplay`); тут повноцінний інтерфейс з класовою
реалізацією — інтерфейс+Koin ідіоматичніший.

**Periodic sweep — один на всі kinds, не по одному.** Усі три
(episode retry, place retry, sync flush) поділяють однаковий дорогий
префікс (uid → активна поїздка → per-repo читання) — окремі
`PeriodicWorkRequest` на кожен kind означали б зайві періодичні
побудки й повторний вивід того самого стану. `scheduleOneOff(kind)`
лишається per-kind (швидкий, event-driven шлях, викликається одразу
після дії, що лишила щось незавершеним); `ensurePeriodicSweepScheduled()`
ставить ОДИН `PeriodicWorkRequest` (15 хв — задокументований мінімум
WorkManager, свідомо прийнятий як бекстоп, не первинний шлях
надійності — не намагатись обійти цей мінімум ланцюжком one-off задач
із затримкою, це той самий антипатерн, що in-memory поллінг, тільки
всередині WorkManager), який на sweep-прогоні виконує всі три задачі
послідовно.

**Критичний інваріант**: `BackgroundSyncWorker` НІКОЛИ не отримує
конкретні episode/place ID в `inputData` — завжди перечитує повний
набір "потребує ретраю" з Room під час виконання. Це робить
`ExistingWorkPolicy.KEEP` безпечним: повторний `scheduleOneOff` того
самого kind, поки попередній ще в черзі, безкоштовно но-опиться, бо
результат перечислюється заново — нічого не втрачається.

**`CoroutineWorker : KoinComponent`, `by inject()`, НЕ кастомний
`WorkerFactory`/`Configuration.Provider`** — цей підхід ламається на
порядку ініціалізації: `androidx.startup`'s `ContentProvider.onCreate()`
виконується ДО `Application.onCreate()`, де живе `startKoin`; лінивий
`by inject()` резолвиться вже всередині `doWork()`, після старту
Koin — проблеми порядку немає. Заразом уникає єдиної реальної
маніфест-пастки WorkManager (дефолтна ініціалізація не потребує
маніфест-змін).

**Worker — best-effort, не per-item retry-семантика**: повертає
`Result.success()` навіть якщо частина елементів лишилась
незавершеною (фото-аплоуди свідомо без ліміту спроб — periodic sweep і
майбутні event-enqueue"и підхоплять залишок). `Result.retry()` —
тільки на "твердий" збій усього проходу (кинутий виняток при резолві
uid/trip чи Room read), НЕ per-item — інакше WorkManager-бекоф і
`descriptionAttempts`-лічильник (окремий, доменний, вже існує)
конфліктували б, і на фото-аплоудах (без ліміту за дизайном)
`MAX_RUN_ATTEMPTS` рано чи пізно назавжди застряг би на "ще
довантажуваному" фото.

**Sync-queue — backstop, не заміна**: 4 `Syncing*Repository` отримують
`scheduleOneOff(SYNC_QUEUE_FLUSH)` одразу після кожного
`store.append(...)`, ДОДАТКОВО до наявного 5с pairing-поллера, не
замість нього. M9's свідоме рішення "без connectivity-listener, sync()
викликається явно" лишається чинним для in-app-швидкості.
Pull-половина того ж поллера (`refreshFromRemote` — partner-join
детекція) лишається геть без змін — їй потрібна сабсекундна швидкість,
якої 15-хвилинний sweep дати не може, і це не проблема надійності
(нема чого гоїти, якщо застосунок закритий — партнер просто не
приєднається, поки хтось не відкриє застосунок знову). На відміну
від sync-queue, in-memory лупи episode/place retry **видаляються
повністю** — WorkManager їх повністю замінює, не доповнює.

**iOS — мінімальний, чесний стаб.** `iosApp` (Xcode-проєкт) ще не
існує в репозиторії взагалі — той самий блокер, що й M7 (Apple dev
account, розблокування ~2026-07-23). Повна `BGTaskScheduler`-реєстрація
(`Info.plist`, `AppDelegate`) фізично неможлива без цього шелу. Замість
передчасно будувати робочий `BGTaskScheduler`-клас, який ніколи не
зможе реально зареєструватись — `NoOpBackgroundWorkScheduler` (обидва
методи — порожнє тіло), забайндений в iOS Koin-модулі. Справжня
`BGTaskScheduler`-реалізація — задокументований follow-up після M7.

**Структура — за зразком M12.5**: одна назва мілстоуна, чотири
незалежні Accept-блоки нижче, кожен — окрема гілка/сесія. Частина 1
(seam) приземляється першою; Частини 2/3/4 незалежні одна від одної
після цього.

**Accept — Частина 1: Seam + Android-інфраструктура:**
- [x] `core:domain`: `BackgroundWorkScheduler`/`BackgroundJobKind` (3 значення)
- [x] `androidApp`: `BackgroundSyncWorker`, `AndroidWorkManagerScheduler`,
      Koin-біндинг, виклик `ensurePeriodicSweepScheduled()` в `onCreate()`
- [x] iOS: `NoOpBackgroundWorkScheduler`, Koin-біндинг
- [x] `gradle/libs.versions.toml` + `androidApp/build.gradle.kts`: WorkManager
      (тільки `androidApp`, не `commonMain` — плейн Kotlin-інтерфейс не
      тягне платформну залежність)
- [x] `BackgroundSyncWorkerTest` (Robolectric, `TestListenableWorkerBuilder`
      + `startKoin`/`stopKoin` з фейками — `WorkManagerTestInitHelper` тут
      не потрібен): one-off гілка per kind; sweep-гілка викликає всі три
      послідовно; `Result.success()` при частковій незавершеності;
      `Result.retry()` тільки на кинутий виняток усього проходу
- [x] `AndroidWorkManagerSchedulerTest` (`WorkManagerTestInitHelper` +
      `SynchronousExecutor`): unique work з `NetworkType.CONNECTED`;
      periodic sweep — один unique request, ідемпотентно

**Accept — Частина 2: Episode retry на WorkManager (залежить від Частини 1):**
- [x] `DiaryCaptureCoordinator` отримує нову залежність `PairingRepository`
      (`observeActiveTrip(uid)`, вже наявний ідіом); `retryAllIncompleteEpisodes(ownUserId)`
      — gather-ланцюжок → наявний `retryIncompleteEpisodes(list)` без змін.
      `AuthSessionCache` лишається поза координатором (Worker сам резолвить uid)
- [x] `retryIncompleteEpisodes(list)` лишається публічним — одноразовий
      "nudge" у `DiaryTimelineContainer`/`DiaryTimelineDataSource` (не луп,
      разово при першому непорожньому емішені стану, з уже наявним `ownEpisodes`)
- [x] `capture()` → `scheduleOneOff(EPISODE_RETRY)`, якщо після обробки
      лишився `needsRetry`-епізод
- [x] Видалити `DiaryTimelineDataSource.pollIncompleteEpisodes` +
      `INCOMPLETE_EPISODE_RETRY_POLL_INTERVAL` + відповідний `launch{}`
      (лише ПІСЛЯ/разом з тим, що Частина 1's periodic sweep реально
      підключено — інакше вікно без жодного гоєння); `pollTripContent`
      лишається без змін
- [x] Тести: `retryAllIncompleteEpisodes` фільтрує власні/чужі/завершені/
      `descriptionAttempts==5`; `capture()` → `scheduleOneOff` лише коли
      потрібно (наявні фейки: `FakeDiaryEntryRepository`,
      `FakeEpisodeRepository`, `FakePairingRepository`)
- [x] Мануальна перевірка: swipe from recents з неповним епізодом →
      почекати → відкрити знову → довантажено без дії користувача
      (пройдено на реальному Pixel 8 Pro 2026-07-22, після виправлення
      трьох багів нижче)

**Accept — Частина 3: Place retry на WorkManager (залежить від Частини 1,
незалежна від Частини 2):**
- [x] Новий `PlaceRetryCoordinator` (`feature:places` presentation,
      дзеркалить `DiaryCaptureCoordinator`, лишається в feature-модулі,
      не переїжджає в `core:domain`) — `retryIncompletePlaces(list)` +
      `retryAllIncompletePlaces(ownUserId)`. `PlaceCandidate.needsRetry()`
      — публічний, єдине джерело правди
- [x] `PlaceRetryDataSource.kt` видаляється цілком; `PlacesListContainer`
      — прибрати виклик `observeAndRetry`
- [x] `PlaceImportContainer.accept()` (не `runImport()` — той лише
      будує прев'ю) → `scheduleOneOff(PLACE_RETRY)`, якщо `needsRetry()`
- [x] Без foreground-nudge для places (слабший кейс, ніж diary)
- [x] Тести: `PlaceRetryCoordinatorTest` (мігрує з видаленого
      `PlaceRetryDataSourceTest`); `PlaceImportContainerTest` — новий
      кейс на `scheduleOneOff(PLACE_RETRY)`
- [ ] Мануальна перевірка: те саме для незавершеного place-імпорту

**Accept — Частина 4: Sync-queue flush backstop (залежить від Частини 1,
незалежна від Частин 2/3):**
- [x] Усі 4 `Syncing*Repository` отримують `BackgroundWorkScheduler`,
      викликають `scheduleOneOff(SYNC_QUEUE_FLUSH)` після `store.append(...)`
- [x] `FirestorePairingTripDataSource`'s 5с поллер — без змін (ні push-,
      ні pull-половина)
- [x] Тести: кожен `Syncing*RepositoryTest` — новий кейс на `scheduleOneOff`

**Спільна мануальна перевірка**: episode-retry шлях (Частина 2)
пройдено на реальному Pixel 8 Pro — airplane mode під час capture →
kill застосунку → мережа назад → `BackgroundSyncWorker` реально
стартує сам (підтверджено живим logcat, `WM-WorkerWrapper ... Result
SUCCESS`), фото й опис довантажились у Firebase без відкриття
застосунку. Place-retry (Частина 3) і `adb shell dumpsys
jobscheduler`-перевірка periodic sweep окремо НЕ виконувались — той
самий gather-ланцюжок і той самий Worker, тож ризик низький, але
чекбокс Частини 3 лишається відкритим, доки не пройдено буквально.

**Виявлено й виправлено під час мануальної перевірки на реальному
пристрої (2026-07-21/22)** — жодне з трьох НЕ покривалось юніт-тестами
до цього, оскільки жодне не було передбачено дизайном:

1. **Race condition в `retryAllIncompleteEpisodes`/`retryAllIncompletePlaces`**:
   обидва викликали `pairingRepository.observeActiveTrip(ownUserId).first()`
   — одноразовий забір першого значення з `channelFlow`, що вже мала
   власний "поллер" (фетчить Firestore, пише в Room) і паралельну
   "гілку локального кешу" (`launch { localLookup.observeByUserId(...).collect { send(it) } }`).
   Одразу після скидання локальних даних (саме сценарій ручного
   тесту) гілка локального кешу віддає `null` ще до того, як поллер
   встиг зробити перший фетч — Worker тихо ніц не робив і все одно
   повертав `Result.success()`. Виправлено новим одноразовим
   `PairingRepository.getActiveTrip(userId)`/`PairingTripDataSource.getActiveTrip(userId)`
   (local-first, remote-fallback лише коли локальний кеш порожній) —
   і саме цей метод тепер викликають обидва координатори замість
   `.first()`.
2. **`OpenMultipleDocuments`-пікер не викликав `takePersistableUriPermission`**:
   SAF-грант на URI без цього виклику — транзитний, прив'язаний до
   поточного task; після kill застосунку (точно сценарій ретраю
   з мертвого процесу) читання фото падало з `SecurityException`.
   Виправлено в `PhotoPickerLauncher.android.kt` — грант береться
   одразу, синхронно, щойно пікер повертає URI.
3. **Geocoding ніколи не ретраївся**: `EpisodeProcessingPipeline.retryIncomplete`
   свідомо (задокументовано в тодішньому kdoc) ретраяв лише
   photo-аплоуд і опис, не геокодинг — capture в Airplane Mode
   назавжди лишав `placeName`/`city`/`cityPlaceId`/`countryCode` null.
   Виправлено новим `Episode.geocodeAttempts` (той самий патерн, що
   `descriptionAttempts`, капується так само на 5 спроб) — Room-міграція
   v13→v14, Firestore-поле читається лениво (`?: 0`, не `requireInt`,
   щоб не зламати документи, вже засинхронені старою версією
   застосунку без цього поля).

**Занотовано для майбутнього мілстоуна "Автоімпорт фото"** (номер TBD,
чернетка Accept-критеріїв з M12.5, перенесена сюди без змін, щоб не
губитись — реалізовувати тільки після окремого дизайн-проходу на
курацію/приватність/дозволи, описаного вище):
- Спайк-перевірка WorkManager-АAR-мерджу під AGP 9 — НЕ актуально для
  цього (M12.11) seam-у (`androidApp` — плейн `com.android.application`,
  не KMP-library модуль), але може знову стати релевантною, якщо
  автоімпорт колись переїде в інший модуль
- Юніт-тести `DiaryImportScannerTest`: немає активної поїздки → no-op;
  фільтрація за діапазоном дат поїздки; дедуп URI проти вже
  персистентних епізодів; групування нових фото по днях з коректним
  числом/аргументами викликів `capture()`; повторне використання
  наявного `DiaryEntry.id` для дня, що вже має запис
- Юніт-тести дозволів онбордингу: об'єднаний запит
  Photos+`ACCESS_MEDIA_LOCATION` покриває всі 4 статуси дозволу
  (GRANTED/NOT_DETERMINED/DENIED/DENIED_PERMANENTLY) — і Android 14
  partial-access (`READ_MEDIA_VISUAL_USER_SELECTED`) кейс, знайдений
  консультацією, якого чернетка спершу не враховувала
- Screenshot-тест: оновлений `@Preview` кроку Photos з об'єднаним
  дозволом → Roborazzi-голден
- **Чесне обмеження**: `DiaryImportWorker.doWork()`/`MediaPhotoLister`'s
  `ContentResolver`-запит не юніт-теститься напряму (той самий клас
  проблеми, що Credential Manager в M5, `ContentResolver` в M10)
- Продуктове рішення, ще не прийняте: silent auto-publish проти
  "stage, не publish" (легкий pending-набір, Gemini+Storage-пайплайн
  запускається тільки після підтвердження користувача) — консультація
  рекомендує друге, якщо/коли цей мілстоун реалізовуватиметься
- Мануальний чекліст: періодичний імпорт реально відпрацьовує на
  пристрої/емуляторі, фото з'являються без ручного пікера; перевірити
  на реальному пристрої, що `ACCESS_MEDIA_LOCATION` + `MediaStore`-шлях
  дійсно зберігає GPS EXIF (на відміну від Photo Picker), і що вибір
  "Select photos" (Android 14+) не ламає видимість майбутніх фото

---

### M12.12 — Пропущений день (Missed Day): дата минула без записів — назавжди (заплановано, не реалізовано)

Продуктовий фідбек від користувача (2026-07-21): поточний
auto-close-by-date-lapse (`entry.date < today` → `READY`, M12.6) не
перевіряє, чи сторона взагалі щось зняла того дня — `DiaryEntry`
створюється лениво, ще в `DiaryCaptureCoordinator.capture()` (до
запуску `EpisodeProcessingPipeline`), і якщо кожен кластер фото
провалився (чи фотосет був порожнім), запис лишається з нулем
епізодів, але все одно стає `READY`, щойно дата минає — день
"успішно закрився", хоча жодного фото фактично не додано. Мета
цього мілстоуна: день, чия дата минула без хоча б одного епізоду з
боку, більше НІКОЛИ не може стати `READY` для цієї сторони — потяг
пішов, ретроактивно "виправити" пропущений день не можна. Логіка
симетричного розблокування (`resolveDayUnlockState`, ADR #5)
лишається без змін: день просто ніколи не розблокується, якщо хоч
одна сторона пропустила — обом партнерам треба було працювати над
результатом (завантажувати фото) саме того дня.

**Accept (чернетка, уточнити перед реалізацією):**
- `core:domain`: новий стан `DiaryDayStatus.MISSED` (окремий від
  `NOT_READY`/`READY` — не "ще може статись", а "вже не станеться").
  `diaryDayStatus(entry, today)` отримує додатковий параметр (напр.
  `hasEpisodes: Boolean`) — сама функція лишається чистою, дані
  постачає викликач (нижче). Пропоноване рішення:
  - `entry == null && date < today` (тут `date` — дата самого дня в
    поїздці, не `entry.date`, якого нема) → `MISSED` замість вічного
    `NOT_READY` без фіналу
  - `entry != null && closedAt != null` → `READY` без змін (явне
    закриття завжди перемагає, дата не має значення)
  - `entry != null && entry.date < today`: якщо `hasEpisodes` →
    `READY` (як зараз, auto-close-by-date-lapse не чіпаємо для
    дня, де реально щось знято); якщо не `hasEpisodes` → `MISSED`
  - інакше (сьогодні чи майбутнє, ще не закрито) → `OPEN`, без змін
  - `resolveDayUnlockState` **не змінюється** — `MISSED != READY`,
    тож день з пропущеною стороною просто ніколи не розблоковується;
    вся суть мілстоуна в цьому, без нової умови в unlock-функції
- **Дані про епізоди вже зібрані на рівень вище, нового Room/Firestore
  запиту не потрібно** — `DiaryTimelineState.episodesByDiaryEntryId`
  (`feature/diary/.../DiaryTimelineState.kt:58`, зібрано в
  `DiaryTimelineDataSource.observeEntriesAndEpisodes`) вже містить
  повний список епізодів на entry; `hasEpisodes` = перевірка
  `.isEmpty()` на цій вже наявній мапі. `DiaryTimelineDay`/
  `buildDiaryTimelineDays` (`core/domain/.../DiaryTimelineDay.kt`)
  отримують `ownHasEpisodes`/`partnerHasEpisodes` як новий вхідний
  параметр
- `resolveDayLockReason` (`core/domain/.../DiaryDayLockReason.kt`) —
  зараз дивиться тільки на `partner` (свідомо, за коментарем: "чи сама
  сторона теж не готова, не міняє того, на що чекає глядач"). Це
  припущення ламається для `MISSED` — якщо ВЛАСНА сторона пропустила
  день, копірайтинг має сказати "ти пропустив(-ла)", не "партнер ще
  знімає". Ймовірно потребує сигнатури `resolveDayLockReason(own,
  partner)` і нового варіанту (`DiaryDayLockReason.MISSED` чи
  окремо `OWN_MISSED`/`PARTNER_MISSED`) — уточнити при реалізації
- **Закриває явно занотоване в M12.6 "майбутнє обмеження"**: "Add
  Photos" ховається для дня, чия дата вже минула (не тільки не
  `today`, як тоді залишили для тестової зручності) — без цього
  користувач міг би задньою датою довантажити фото в уже `MISSED`
  день і статус перерахувався б назад у `READY` (`MISSED` — обчислюваний,
  не збережений стан). `DiaryCaptureCoordinator.capture()` додатково
  захисно відхиляє capture у минулий день (belt-and-suspenders,
  на випадок інших entry-точок у capture, не тільки кнопки)
- Новий візуальний стан day-картки для `MISSED` (окремо для "я
  пропустив" і "партнер пропустив") — тимчасовий copy, як і M12.6's
  англійський плейсхолдер, узгодити остаточний текст при реалізації;
  нові screenshot-голдени
- Тести: `diaryDayStatus` — новий кейс (дата минула, `hasEpisodes =
  false` → `MISSED`; дата минула, `hasEpisodes = true` → `READY` без
  змін); `buildDiaryTimelineDays` прокидає `hasEpisodes` правильно;
  `resolveDayUnlockState`/`resolveDayLockReason` — `MISSED` ніколи не
  розблоковує день, копірайтинг відрізняє "я"/"партнер"; `DiaryCaptureCoordinator`
  — capture у минулий день відхиляється
- Мануальна перевірка (два пристрої): одна сторона знімає фото за
  "сьогодні", інша — ні; перемотати системну дату вперед на
  пристроях (чи почекати) → пропущена сторона показує `MISSED`,
  день ніколи не розблоковується навіть після синку; спроба
  довантажити фото в минулий день заблокована на UI

**Відкриті питання (уточнити перед реалізацією):**
- Чи виключати `MISSED`-дні з майбутнього Recap (окремий, ще не
  запланований мілстоун) — поза скоупом тут, занотувати на майбутнє
- Чи `MISSED` застосовується і коли `entry == null` (взагалі нічого не
  знято тим днем), чи тільки коли `entry` існує з нульовими епізодами
  — запропоновано вище "обидва випадки", але не узгоджено остаточно
- Точний copy/дизайн `MISSED`-картки

---

### M13.1 — Places: модель і share-link парсинг (без інтеграції в застосунок) ✅ done
`core:model`/`core:domain`/`core:database`/`core:network` — розширення
`PlaceCandidate` (фото-посилання, рейтинг, категорія місця) + пайплайн
"share-лінк → готовий `PlaceCandidate`". Свідомо **не пов'язано з
застосунком напряму** — жодного Android intent-filter, iOS Share
Extension чи Compose UI, це M13.2.

**Accept:**
- [x] `core:model`: `PlaceCandidate` отримує `photoUrls: List<String>`,
      `rating: Double?`, `category: String?`
- [x] `core:database`: `MIGRATION_9_10` (schema v9→v10) — нові колонки
      `photo_urls`/`rating`/`category` на `place_candidates`, новий
      `List<String>` `@TypeConverter`, міграційний тест (backfill
      старого рядка + round-trip нового) — `MigrationTest`, `PlaceCandidateDaoTest`,
      `PlaceCandidateRepositoryImplTest`
- [x] Юніт-тести парсингу URL (`GoogleMapsShareLinkParser`, чиста
      функція без I/O): короткий лінк, повний лінк, лінк без
      координат — тільки назва (реальний, поширений кейс — усі 4
      піддослідні лінки з завдання саме такі) на фейкових прикладах
      реальних форматів Google Maps лінків — `GoogleMapsShareLinkParserTest`
      (5 тестів, фікстури — реально розрезолвлені вручну 4 лінки із завдання)
- [x] Тест resolve-редиректу через Ktor `MockEngine` (симуляція HTTP
      redirect-ланцюжка) — `KtorShareLinkRedirectResolverTest` (3)
- [x] Юніт-тести `PlaceImportPipeline` на фейкових сідах для всіх
      сімів (happy path, лінк без координат, lookup not-found,
      частковий і повний збій завантаження фото, збій resolve-редиректу) —
      `PlaceImportPipelineTest` (8 тестів)
- [x] Інтеграційний тест (`data/src/jvmTest`): URL → redirect resolve →
      Places lookup (MockEngine) → фото довантажені й перезалиті у
      Firebase Storage → готовий `PlaceCandidate` з `photoUrls`, що
      вказують на Storage, не на сирі Google photo references —
      `PlaceImportIntegrationTest`, реальний in-memory Room + реальні
      Ktor `MockEngine`-клієнти (не fakes), той самий реально
      розрезолвлений лінк із завдання як redirect-target фікстура
- [x] Підтверджено: жодного platform-specific (expect/actual,
      Android/iOS-only) коду в цьому мілстоуні — уся логіка живе в
      commonMain `core:domain`/`core:network` (перевірено компіляцією
      під `iosSimulatorArm64` для `core:domain`/`core:database`/`core:network`),
      працює однаково на обох платформах

**Відхилення від початкового плану (узгоджено з користувачем перед
реалізацією, де відзначено):**
- **Фото — Places API (New) `places:searchText` + Photo Media endpoint**,
  не класичний `maps.api/place/*` — узгоджено з користувачем: рейтинг
  зірочками (`Double`, passthrough 0.0–5.0) і локалізована категорія
  місця (`primaryTypeDisplayName.text`, напр. "Coffee shop") доступні в
  ОДНОМУ виклику лише в New API; класичний Find-Place-from-Text повертає
  тільки сирі `types` (машинні слаги на кшталт `"point_of_interest"`),
  без готового локалізованого людського лейбла. Новий клієнт
  (`GooglePlacesDetailsApi`/`Client`, `GooglePlacesPhotoApi`/`Client`)
  лишився в тому самому пакеті `core.network.places`, що й M10's
  Geocoding-клієнт (той самий прецедент "places" — назва пакета за
  CLAUDE.md-термінологією, не за конкретним Google-продуктом), і
  перевикористовує вже наявний `GooglePlacesConfig`/API-ключ — жодного
  нового конфіг-класу.
- **Фото — реально перезаливаються у Firebase Storage** (узгоджено з
  користувачем), не зберігаються як Google-хостинг URL з API-ключем у
  query. Не через `FirebaseStorageUploadClient` (той типізований під
  діарі-специфічний `Photo`), а через новий тонкий адаптер
  `FirebasePlacePhotoUploadClient`, що перевикористовує
  `FirebaseStorageApi`/`FirebaseStorageConfig` без жодної зміни в них —
  обидва вже приймали довільний `String`-id, не типізований під `Photo`.
  Object-шлях — синтетичний `place_<placeCandidateId>_<index>`, той
  самий sanitize-механізм з M12.5 застосовується автоматично.
- **`GoogleMapsShareLinkParser` — чиста функція над уже розрезолвленим
  URL**, resolve-редирект (`ShareLinkRedirectResolver`) — окремий сім.
  Розрезолвлено вручну всі 4 піддослідні лінки із завдання: усі чотири
  зводяться до одного й того самого шаблону
  `.../maps/place/<назва[,+адреса]>[/@lat,lng,zoom]/data=!4m2!3m1!1s<hex>:<hex>!18m1!1e1?...`,
  і **жоден не містить `@lat,lng`** — підтверджує, що "лінк без
  координат, тільки назва" (formulation з початкового M13) насправді
  типовий кейс для card-шеру місця, а не рідкісний виняток; `@lat,lng`
  зустрічається тільки для pin-drop шеру. Парсер працює з обома формами.
- **`PlaceCandidate` НЕ синкається через `SyncCoordinator`** (свідомо, не
  нова знахідка) — узгоджено ще в M9's ноутатці ("PlaceCandidate →
  матчер-мілстоуни"), підтверджено кодом: жодного
  `PlaceCandidateSyncEntityBinding`/`data/place/`-пакета не існує.
  `AlongsideDatabase.placeCandidateRepository()` (нова публічна
  extension-функція, той самий патерн, що `tripRepository()`/
  `diaryEntryRepository()`/`episodeRepository()`) додана лише для того,
  щоб `PlaceCandidateRepositoryImpl` (internal у `core:database`) був
  доступний з `data`-модуля для інтеграційного тесту — сама sync-логіка
  лишається задачею M14/M15, коли Matcher реально потребуватиме
  крос-девайс видимість пулу місць.
  **Оновлення (M13.2, 2026-07-21):** цей план змінено за прямим
  запитом користувача — `PlaceCandidateSyncEntityBinding`/`data/place/`
  таки реалізовано в M13.2, раніше за M14/M15, разом з photo-retry
  poll loop-ом; деталі — в M13.2's власних нотатках.
- **`List<String>` (`photoUrls`) — newline-delimited рядок у Room, не
  JSON** — `core:database` не має залежності на kotlinx.serialization;
  кожен елемент — URL (ніколи не містить `\n`), тож простий
  join/split достатній. Новий `StringListTypeConverters` — окремий
  object від основного `AlongsideTypeConverters` (не через нього),
  єдина причина — detekt's `TooManyFunctions` (12 > 11 в об'єкті).
- **`config/detekt.yml`: новий `LargeClass`-exclude для
  `MigrationTest.kt`** — файл перевалив дефолтний поріг рядків після
  додавання v9→v10 фікстури; це репетитивний за дизайном hand-rolled
  SQL-фікстур-файл (по одному фрагменту на кожну попередню схема-версію,
  той самий патерн, що M9 задокументувала), не реальна складність
  окремої функції/тесту всередині.
- **Чесне обмеження, не приховане:** жодного реального виклику Places
  API (New) з цим ключем ще не було — `GOOGLE_PLACES_API_KEY` (M10)
  перевірений лише проти класичного Geocoding endpoint. Може знадобитись
  окремо увімкнути Places API (New) на тому самому GCP-проєкті/ключі,
  перш ніж M13.2's мануальна перевірка реально спрацює — той самий клас
  сюрпризу, що M10's Gemini-квота. Не блокер для коду/тестів цього
  мілстоуна, лише пункт мануальної перевірки перед мержем M13.2.

---

### M13.2 — Places: інтеграція в застосунок (share-link entry point) ✅ done
`feature:places` (частина 1) + `androidApp` — Android `ACTION_SEND`
intent-filter, iOS Share Extension, перший реальний Orbit Container і
екран підтвердження імпортованого місця, споживає `PlaceImportPipeline`
з M13.1.

**Accept:**
- [x] Android: тест, що intent-filter приймає `ACTION_SEND` з очікуваним
  MIME-типом (`text/plain`) (інструментальний тест або Robolectric) —
  `ShareIntentManifestTest` (`androidApp/src/test`, Robolectric,
  `queryIntentActivities` проти реального маніфесту) — перший тест
  цього модуля взагалі, до цього `androidApp` не мав жодного test
  source set
- [x] `PlaceImportContainerTest` (orbit-test, реальний `PlaceImportPipeline`
  на feature-локальних фейках його чотирьох сімів — не фейк самого
  пайплайна, той самий патерн, що `feature:diary`'s `Fake*.kt` для
  `EpisodeProcessingPipeline`): отриманий share-текст → пайплайн
  викликається → стан підтвердження; Accept — зберігає `PlaceCandidate`;
  Discard — не зберігає (7 тестів, incl. no-url-in-text і no-active-trip
  гілки)
- [x] Screenshot-тести: картка підтвердження в станах
  loading/found-з-фото-й-рейтингом/not-found/error — 4 `@Preview` →
  auto-generated Roborazzi-голдени в `feature/places/screenshots/`
- [x] iOS: мануальна перевірка Share Extension зафіксована як чекліст у
  `docs/manual-checklists.md` (той самий підхід, що M7/M10) — `iosApp`
  ще не існує, BLOCKED

**Відхилення від початкового плану (узгоджені з користувачем перед
реалізацією):**
- **Скоуп розширено поза буквальні Accept-критерії, за прямим запитом
  користувача**: `PlaceCandidate` підключено до `SyncCoordinator` ЗАРАЗ
  (не відкладено до M14/M15, як планувалось у M13.1) — durable
  Firestore-синк того самого класу, що вже мають Trip/DiaryEntry/Episode
  (`PlaceCandidateFirestoreMapper`/`SyncingPlaceCandidateRepository`/
  `PlaceCandidateSyncEntityBinding`, `data/place/`); плюс новий
  photo-retry poll loop (`PlaceRetryDataSource`, `feature:places`),
  якого в діарі-модулі як окремого durable-механізму насправді нема
  (дослідження показало: діарі-фото-аплоад не має власної черги/
  паралелізму, лише in-memory 30с poll, той самий підхід і тут
  застосовано, з тим самим чесно задокументованим обмеженням — працює,
  лише поки composed якийсь екран).
- **`PlaceCandidate.photoUrls: List<String>` → `photos: List<PlacePhoto>`**
  (нове `core:model`) — без цього retry неможливий: стара форма губила
  Google Places `photoRef` і сам факт часткового збою назавжди. Room:
  schema v10→v11 (`MIGRATION_10_11`), новий `photos` стовпець
  (`photoRef\tremoteUrlOrEmpty`-пари, `PlacePhotoListTypeConverters`),
  старий `photoUrls` дропнутий — свідомо без backfill (місце-імпорт не
  діставався до жодного реального користувача до цього мілстоуна, тож
  реальних даних на схемі v10 не існує).
- **Retry-loop розміщено на існуючій Places-tab заглушці** (не на
  повноцінному списковому екрані — той належить M16) —
  `LaunchedEffect` в `AlongsideApp.kt`'s `entry<Places>`, запускає
  `PlaceRetryDataSource.observeAndRetry(uid)` поки таб на екрані; M16,
  ймовірно, перенесе цей запуск у свій реальний Container.
- **`androidApp` отримав перший test source set** (`src/test`,
  Robolectric + JUnit4) — модуль був чисто композиційним коренем без
  жодного тесту; `testOptions.unitTests.isIncludeAndroidResources`
  додано в `androidApp/build.gradle.kts`.
- **Нова `core:network` фабрика** `createShareLinkRedirectHttpClient()`
  (по одній на android/ios/jvm, поряд з `createFirestoreHttpClient()`) —
  окремий Ktor-клієнт з `followRedirects = false`, обов'язковим для
  `KtorShareLinkRedirectResolver` (не можна перевикористати спільний
  `single<HttpClient>`, інакше колізія типів у Koin).
- **`MainActivity` — `android:launchMode="singleTask"`** — без цього
  другий `ACTION_SEND` під час роботи застосунку створював би нову
  Activity-інстанцію замість виклику `onNewIntent` на існуючій.
- **Firestore rules для `placeCandidates` не знадобилось міняти** —
  повний `match /placeCandidates/{placeId}` блок вже існував у
  `firebase/firestore.rules` (підготовлений заздалегідь у M9), просто
  підтверджено, не написано заново.

**Оновлення (пост-M13.2, 2026-07-21): реальний Places-екран замість
заглушки, за прямим запитом користувача, з дизайн-референсом
`design/main_app_design/Alongside - Main App and Recap (standalone).html`
("Screen 16").** Раніше за M16 (яке лишається відповідальним за
add/edit/delete) реалізовано read-only список:
- **`PlaceCandidate.city: String?`** (нове поле) — reverse-geocoded від
  lat/lng самого місця через уже наявний `PlaceGeocodingClient` (той
  самий сім, що `EpisodeProcessingPipeline` використовує для episode
  place names) замість розширення Places API (New) відповіді
  (`addressComponents` там — інша форма полів, ніж у класичного
  Geocoding API). `GeocodeResult.cityName()` — новий, окремий від
  `preferredPlaceName()` каскад (`locality` → `administrative_area_level_2`
  → `administrative_area_level_1`, null якщо нічого не тегнуто — на
  відміну від `preferredPlaceName()`, без fallback на formatted address).
  `PlaceImportPipeline` отримав п'ятий конструкторський параметр;
  `config/detekt.yml`'s `LongParameterList` — `constructorThreshold: 8`
  додано окремо від `functionThreshold` (детект розрізняє їх, а не
  єдиний спільний поріг, як здавалось із коментаря M12.5). Схема
  v11→v12 (`MIGRATION_11_12`), без backfill (та сама "нема реальних
  даних ще" причина, що v10→v11).
- **`MediaListRow`** (нове, `core:ui`) — генерик list-row (фото +
  заголовок з приглушено-сірим "accent" через buildAnnotatedString/
  SpanStyle, перший такий кейс у кодовій базі + опційний підзаголовок),
  свідомо не Places-специфічний — той самий shape знадобиться M15's
  Match-list. Формат "назва — рейтинг" + категорія в підзаголовку —
  пряма вимога користувача, замінює мокапову спрощену (лише назва)
  версію рядка.
- **`InkBackground`** (нове, `core:ui`) — плаский, без градієнта,
  варіант `InkGradientBackground` (перевикористовує той самий
  `gradientTop`-токен, не новий колір) — свідоме відхилення саме для
  цього екрана від теплого градієнтного фону, який мають усі інші
  реальні екрани, підтверджене з користувачем (мокап читається
  холодніше/пласкіше).
- **Групування по місту** (`PlaceCityGroup`/`groupedByCity()`,
  `feature:places`) — названі міста за алфавітом, місця без міста
  (geocoding не спрацював/нічого не знайшов) — в один хвостовий "Other"
  гурт, не губляться.
- **`PlacesListContainer`/`PlacesListDataSource`** (нове) — той самий
  reactive trip→content shape, що `DiaryTimelineContainer`/
  `DiaryTimelineDataSource`, звужений (без Intent/SideEffect — цей
  екран поки що read-only, M16 додасть їх). Photo-retry loop
  (`PlaceRetryDataSource.observeAndRetry`) переїхав сюди з тимчасового
  `LaunchedEffect` на Places-tab заглушці — саме той переїзд, який
  M13.2's нотатка вище й передбачала.
- **iOS Share Extension та add/edit/delete лишаються поза скоупом** —
  без змін відносно оригінальних M13.2/M16 меж.

**Оновлення (2026-07-21): `countryCode`/`cityPlaceId` на `Episode` та
`PlaceCandidate`, за прямим запитом користувача (аудит моделей показав
координати майже скрізь, але країну — ніде).** Закриває M12's known-gap
нотатку вище про нереалізований прапор країни.
- **Дані вже надходили, але викидались** — `GooglePlacesGeocodingClient`
  завжди отримував повний масив `results` (адреса → ... → locality →
  країна, кожен зі своїм `place_id`), але код читав лише
  `results.firstOrNull()` і тільки `formatted_address`/`address_components`
  з нього; `place_id` і топрівневий `types` кожного результату не
  парсились. Жодного нового мережевого запиту не знадобилось.
- **`GeocodeResult.countryCode()`** — новий каскад, ISO 3166-1 alpha-2
  код (`short_name` компонента з `types=["country","political"]`).
  **`List<GeocodeResult>.localityPlaceId()`** — окремо від
  `countryCode()`/`cityName()`, шукає не в `addressComponents` одного
  результату, а `place_id` того результату з усього списку, чий
  топрівневий `types` містить `"locality"` — Google-власний стабільний
  код міста, без нового зовнішнього сервісу.
- **Чому не GeoNames/UN-LOCODE для коду міста** — розглядали й
  відхилили: GeoNames вимагає нового зовнішнього API (окремий акаунт,
  rate limits, новий network-клієнт), що суперечить "мінімізує
  backend-поверхню, тільки Google Places + Gemini" з CLAUDE.md; UN-LOCODE
  — фіксований список портів/торгових локацій, пропускає малі міста
  подорожі. Google `place_id` — той самий компроміс, що вже прийнятий
  для `city` в M13.2 (перевикористати наявний виклик замість нового
  джерела даних).
- **`GeocodingResult.Found`/`Episode`/`PlaceCandidate`** отримали
  `city`/`cityPlaceId`/`countryCode` (Episode — усі три вперше, у
  PlaceCandidate `city` вже був з M13.2, додались `cityPlaceId` і
  `countryCode`). Схема v12→v13 (`MIGRATION_12_13`), без backfill (та
  сама причина, що v11→v12).
- **`countryCodeToFlagEmoji`** (нове, `core:ui`) — чиста функція,
  regional indicator symbols (ручна UTF-16 surrogate-пара, бо кодпоінти
  поза BMP, а в `kotlin.text` немає codepoint-append хелпера в
  commonMain). За уточненням користувача, прапор — emoji-символ,
  що стає частиною тексту назви (не окрема іконка): підключено в
  day-картці щоденника (`EpisodeSection`, поруч з `placeName`) і в
  заголовку групи міста на Places-екрані (`PlacesByCity`, поруч з
  `city`; `PlaceCityGroup` отримав `countryCode`, похідний від першого
  місця в групі).

---

### M14 — Matcher: swipe & match mechanic
`feature:matcher` (частина 1) — тіндер-свайп по одній картці, mutual
match логіка.

**Accept:**
- Юніт-тести правил переходу картки: обоє "так" на ту саму картку →
  переходить у Match-лист; обоє "ні" → картка прибирається з колоди
  назавжди; розійшлись (один так, один ні) → картка лишається в
  колоді й показується знову пізніше
- Orbit-контейнер тестується через `orbit-test`: послідовність
  Intent → State для повного циклу (N карток, включно з кейсом
  "розійшлись → показ повторно → нарешті збіглись")
- Межові випадки: порожня колода (усі картки або matched, або
  removed), одночасний свайп обома на останню картку в колоді

---

### M15 — Matcher: UI (swipe + Match-лист)
`feature:matcher` (частина 2) — екран свайпу і список взаємних
матчів.

**Accept:**
- Screenshot-тести: картка в колоді, порожня колода, список
  Match-лист (порожній і заповнений стани)
- Compose UI-тест: свайп коректно емітить `Intent.Swipe` з правильним
  candidateId і напрямком (like/dislike)

---

### M16 — Places: custom add & management
`feature:places` (частина 2) — ручне додавання, керування пулом.

**Accept:**
- Юніт + screenshot-тести CRUD-флоу (додати/редагувати/видалити місце
  з нотаткою)
- Тест дії "додати до пулу для Matcher" — місце коректно з'являється
  в колоді свайпу

---

### M17 — Push notifications
FCM-інтеграція (Android + iOS/APNs) + Cloud Function.

**Accept:**
- Cloud Function (тригер "обидва дні готові") — юніт-тест через
  Firebase Local Emulator Suite (Firestore + Functions емулюються
  локально, реальний Firebase не потрібен)
- Юніт-тести побудови payload пуша (правильний текст/дані для кожного
  типу: partner-day-ready, days-until-reunion)
- **Чесне обмеження:** фактична доставка пуша на реальний пристрій —
  не автоматизується надійно в CI (залежить від APNs/FCM живої
  інфраструктури). Мануальний чекліст: пуш реально приходить на
  Android і iOS пристрій хоча б раз перед мержем.

---

### M18 — Settings & trip management
`feature:settings` — Leave/Delete Trip.

**Accept:**
- Юніт-тест owner-only авторизації для Delete (member не може, owner
  може) — покриває і repository-рівень, і UI-рівень (кнопка не
  показується member'у)
- Screenshot-тести відмінності UI owner vs member

---

### M19 — Debug menu
Прихований dev-екран.

**Accept:**
- Перевірено, що екран виключений з release-збірки (build-flavor/flag),
  тестом на конфігурацію збірки, не тільки візуально
- Юніт-тести дій (delete all trips, clear cache, force re-onboarding)
  на фейкових репозиторіях

---

### M20 — Recap
`feature:recap` — Stories-формат, автогенерація по завершенню.

**Accept:**
- Юніт-тест генерації послідовності слайдів із даних поїздки (правильна
  кількість day-слайдів, взаємно підтверджені місця з Match-листа
  присутні, фінальний слайд завжди останній)
- Screenshot-тести кожного типу слайду
- Compose UI-тест тап-навігації (тап справа/зліва) і auto-advance
  таймера (з можливістю прискорити тест — не чекати реальні 5-7 секунд)
- Тест тригера автогенерації по даті останнього дня маршруту

---

### M21 — Release readiness
Наскрізна перевірка перед реальним використанням у поїздці.

**Accept:**
- Усі попередні мілстоуни змерджені, CI зелений на `main`
- Повний офлайн-флоу вручну прогнаний на двох реальних пристроях
  одночасно (Android + iOS): symmetric unlock, matcher, push — всі
  працюють у зв'язці, не тільки ізольовано
- Мануальний чекліст з M5/M7/M13/M17 (усе, що позначено як
  "не автоматизується") пройдено й зафіксовано
- Немає відкритих P0/P1 багів
