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

### M4 — Core UI
`core:ui` — дизайн-система, анімації, базові компоненти.

**Accept:**
- Кожен публічний компонент має `@Preview` у `commonMain`
- Юніт-тести через `ComposeTestRule` на інтерактивні компоненти
  (клік, свайп, стан — не тільки статичний рендер)
- Screenshot-тести (Roborazzi + ComposablePreviewScanner) на кожен
  `@Preview` — генеруються автоматично, без ручного дублювання тесту
  на кожен компонент
- Анімаційні хелпери (count-up/down, typewriter, stagger-reveal)
  мають юніт-тест на кінцевий стан (наприклад: typewriter після
  завершення показує повний текст, а не проміжний)
- Playground (`:playground`) запускається і показує всі компоненти
  дизайн-системи — вручну перевірено хоч раз

---

### M5 — Login (Google Sign-In)
`feature:auth`.

**Accept:**
- Бізнес-логіка (Orbit `AuthContainer`: `State`/`Intent`/`SideEffect`)
  юніт-тестується повністю через **фейкову** реалізацію
  `GoogleAuthProvider`-інтерфейсу — успіх, відмова користувача, мережевий
  збій, невалідний токен. Використовується `orbit-test` для перевірки
  послідовності станів.
- **Чесне обмеження, яке варто прийняти, а не вдавати, що його нема:**
  сам виклик нативного SDK (Credential Manager на Android, GIDSignIn на
  iOS) — це системний UI-флоу з реальним Google-акаунтом, його не можна
  надійно автоматизувати в CI. Це НЕ юніт-теститься напряму — теститься
  тільки код по обидва боки від нього (наш `Container` + обробка
  результату).
- Інтеграційний тест — на етап **після** отримання ID-токена: обмін
  токена на сесію через Firebase Auth REST API, протестований через
  Ktor `MockEngine` (симулюємо відповідь Firebase Auth на валідний і
  невалідний токен)
- Мануальний чекліст (не автоматизований, документується як такий):
  реальний Sign-In на фізичному Android-пристрої й iOS-пристрої/симуляторі
  хоча б раз перед мержем

---

### M6 — Android Onboarding
`feature:onboarding`, Android-специфічна частина.

**Accept:**
- Послідовність кроків (photo permission → geolocation-для-камери →
  share-setup → notification permission) перевіряється Compose UI-тестом
  навігації — правильний наступний крок при кожному можливому вихідному
  стані (дозвіл вже наданий раніше → крок пропускається)
- Screenshot-тести на кожен крок окремо (включно зі станом "дозвіл
  відхилено — показуємо пояснення")
- Дозволи запитуються just-in-time (не всі одразу на старті), з
  поясненням-рационале перед системним діалогом там, де це доречно
- Відмова в дозволі не блокує застосунок повністю — показує шлях назад
  (посилання в системні налаштування), це окремо перевірено тестом

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

### M8 — Pairing
`feature:pairing` — створення/приєднання до поїздки.

**Accept:**
- Юніт-тести: генерація інвайт-коду (унікальність, формат), логіка
  приєднання (валідний код / невалідний / вже використаний власний код)
- Repository-логіка тестується на фейкових дата сорсах (без реальної
  мережі/БД — той самий підхід, що в M1-M3)
- UI-флоу create/join — screenshot-тести + Compose UI-тест навігації
  (create → показ коду → очікування партнера; join → введення коду →
  успіх/помилка)

---

### M9 — Data layer & offline sync
`data` — Repository-реалізації, що зводять `core:database` +
`core:network`, sync-queue, conflict-resolution.

**Accept:**
- Інтеграційний тест повного циклу: write офлайн (тільки в Room) →
  імітація появи мережі → sync-queue обробляє чергу → дані з'являються
  віддалено (перевіряється через фейковий мережевий клієнт, що фіксує
  виклики)
- Тест конфлікт-резолюшну: два "конкурентні" запити на один запис —
  результат детерміністичний (last-write-wins за timestamp), перевірено
  явним тестом з контрольованими часовими мітками
- Тест часткового збою черги: N операцій в черзі, K з них падають —
  решта обробляються, збійні лишаються в черзі з позначкою retry

---

### M10 — Diary: capture & processing pipeline
`feature:diary` (частина 1) — EXIF, кластеризація епізодів, Google
Places геокодинг, Gemini vision-опис.

**Accept:**
- Юніт-тести кластеризації: синтетичні набори фото з відомими
  timestamp/координатами → перевірка, що епізоди групуються правильно
  (межові випадки: фото рівно на межі 500м/2год порогу)
- Places геокодинг і Gemini виклики — тестуються через фейкові клієнти
  (інтерфейс, не HTTP напряму), окремо від `core:network`, який вже
  покритий в M3
- Тест вибору репрезентативних фото з епізоду (2-4 з N, за критерієм
  найбільшої різниці в часі)
- Тест ліміту перегенерації тексту (лічильник спроб на епізод, поведінка
  при вичерпанні)

---

### M11 — Diary: symmetric unlock & sync integration
`feature:diary` (частина 2) — логіка розблокування дня, інтеграція з
data-layer.

**Accept:**
- Юніт-тести `DayUnlockState`: усі комбінації станів обох учасників
  (жоден не готовий / один готовий / обидва готові), включно з
  проміжним станом "дані є, але ще не засинкані"
- Тест умови тригера пуша (обидва DiaryEntry цієї дати мають
  `syncStatus == Synced`) — саме умова, без реального виклику FCM
  (той — в M17)
- Інтеграційний тест: DiaryEntry проходить повний шлях capture →
  processing (M10) → sync (M9) → unlock-стан оновлюється коректно

---

### M12 — Timeline: UI & animations
`feature:diary` (частина 3) — карусельний екран Таймлайн (замінює
окремі List+Detail: лічильник → День 1 → День 2 → ... в одному
пейджері, поточний день по центру, сусідні виглядають по боках).

**Accept:**
- Screenshot-тести: заблокований день у каруселі, кожен з варіантів
  "стан очікування" (партнер ще знімає / чекає мережі / генерує
  текст), розблокований день
- Compose UI-тест: свайп вперед/назад коректно змінює поточний день,
  сусідні дні відображаються прев'ю по боках (навігація як по
  двозв'язному списку), розблокування впливає тільки на конкретний
  індекс дня, не на всю карусель
- Юніт-тест на послідовність стагер-анімації фото (порядок появи
  відповідає порядку фото в епізоді)
- Count-down до зустрічі (перший елемент каруселі, перед Днем 1) —
  юніт-тест на коректність числа, окремо від анімації відображення

---

### M13 — Places: import via Google Maps share-link
`feature:places` (частина 1) — імпорт через Google Maps share-лінк.

**Accept:**
- Юніт-тести парсингу URL (короткий лінк, повний лінк, лінк без
  координат — тільки назва) на фейкових прикладах
  реальних форматів Google Maps лінків
- Тест resolve-редиректу через Ktor `MockEngine` (симуляція HTTP
  redirect-ланцюжка)
- Інтеграційний тест: URL → Places lookup (MockEngine) → готовий
  `PlaceCandidate`
- Android: тест, що intent-filter приймає `ACTION_SEND` з очікуваним
  MIME-типом (інструментальний тест або Robolectric)
- iOS: мануальна перевірка Share Extension (автоматизувати важко,
  фіксується як мануальний чекліст, як і в M7)

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
