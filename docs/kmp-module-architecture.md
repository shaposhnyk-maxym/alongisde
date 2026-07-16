# KMP — модульна архітектура (аргументовані рішення)

## Граф модулів (пропозиція)

```
core:model         — сутності (Trip, DiaryEntry, Episode, PlaceCandidate
                      з match-статусом...), без платформних залежностей
core:domain        — інтерфейси репозиторіїв/дата сорсів + use-case'и,
                      залежить тільки від core:model
core:database      — SQLDelight/Room-реалізація локальних дата сорсів,
                      імплементує інтерфейси з core:domain
core:network       — Ktor-клієнт + sync-queue процесор, імплементує
                      remote дата сорси з core:domain
core:ui            — дизайн-система: тема, типографіка, базові компоненти,
                      анімації (count-up/down, typewriter, stagger-reveal,
                      shared-element helpers)
data               — конкретні Repository-реалізації, що зв'язують
                      core:database + core:network (тут живе
                      offline-first merge/conflict-логіка)
feature:auth
feature:onboarding
feature:pairing
feature:diary
feature:matcher
feature:places
feature:recap
feature:settings
app (androidApp)   — Navigation 3 граф, DI (Koin), Android application
                      entry point
playground         — Compose Desktop застосунок, залежить тільки від
                      core:ui
iosApp             — Xcode-проєкт (не Gradle-модуль), SwiftUI-обгортка
                      над експортованим framework з app
```

## Обґрунтування по кожному модулю

### core:model / core:domain
Класичний Clean Architecture domain-шар. Тільки Kotlin data-класи +
інтерфейси, нуль платформних залежностей (жодного expect/actual тут не
повинно бути). Feature-модулі залежать тільки вниз на цей шар, ніколи
одне на одного напряму — це тримає граф залежностей ациклічним і
дозволяє паралельну компіляцію.

**Прагматична нотатка:** для соло-проєкту можна об'єднати model+domain
в один `core`-модуль без втрати архітектурної чистоти — розділяти варто,
коли відчуєш реальний біль (наприклад, коли domain стане достатньо
великим, щоб компілюватись довго при кожній зміні моделі).

### core:database
Окремий модуль виправданий: implementation detail (яка саме БД) не
повинен просочуватись у feature-код, і це дозволяє юніт-тестити фічі з
фейковими репозиторіями без залежності на SQLDelight/Room у тестовому
classpath.

**Вибір технології:** Room отримав повну multiplatform-підтримку
(Room 3.0, працює на Android/iOS/Desktop/JS-WasmJS). Якщо ти вже знаєш
Room з Android — поріг входу мінімальний, той самий API. SQLDelight
довше на ринку як multiplatform-рішення і вважається трохи
"перевіренішим", але вимагає SQL-first підходу. Для твого випадку
(обмежений час, паралельно готуєшся до співбесід) — Room виглядає
доречніше, бо менше нового вчити.

### core:network
Окремий модуль тому, що sync-механіка (черга, retry,
conflict-resolution) — це окрема відповідальність від "як підключитись
до бекенду".

**Firebase SDK напряму vs Ktor + власний бекенд:** раджу Ktor + власний
легкий бекенд (або звертання до Firestore REST API через Ktor, якщо
хочеш лишити Firestore як сховище). Причини:
- GitLive'івський firebase-kotlin-sdk (стандартна multiplatform-обгортка
  Firebase) має реальні проблеми з лінкуванням через CocoaPods на iOS і
  конфлікти версій lifecycle-бібліотек — це видно навіть у відкритих
  issues 2026 року
- У вас і так буде власна sync-queue логіка для offline-first, тож
  вбудований офлайн-кеш Firestore дає менше переваг, ніж у простому
  CRUD-застосунку — конфлікт-резолюшн все одно свій
- Ktor вже буде в стеку для Google Places і Gemini — один HTTP-клієнт
  на все, а не два паралельні стеки

FCM для пушів лишається окремо — це тонкий шар токен-реєстрації/прийому
пушів, не пов'язаний з основним синком даних. Можна тримати всередині
core:network або винести в мінімальний окремий модуль.

### core:ui + playground
Дизайн-система окремим модулем — стандартна практика, дає змогу
ітерувати над UI/анімаціями без збірки всього застосунку.

**Свіжий аргумент за playground саме як Compose Desktop-застосунок:**
Compose Hot Reload стабілізувався і тепер вбудований у Compose
Multiplatform Gradle plugin за замовчуванням для desktop-таргетів. Це
означає миттєвий hot reload змін в UI/анімаціях без емулятора чи
симулятора в циклі — ідеально для тюнінгу count-up, typewriter,
stagger-reveal ефектів.

### data (repository implementations)
Тут живе конкретна реалізація репозиторіїв, що зводить докупи
core:database + core:network + sync-логіку. Feature-модулі не знають
про базу даних чи мережу взагалі — тільки викликають методи репозиторію
й отримують Flow<State>. Це відповідає підходу, який використовує і
офіційний Now in Android sample.

### feature:*
По одному модулю на bounded context (auth, onboarding, pairing, diary,
matcher, places, recap, settings). Кожен залежить від core:domain
(інтерфейси) + core:ui (дизайн-система) + Orbit MVI. Кожен має власний
State/Intent/SideEffect контейнер + Compose-екрани. Фічі ніколи не
залежать одна від одної напряму.

### app / androidApp / iosApp
З новою дефолтною структурою KMP і AGP 9.0 більше не можна поєднувати
KMP multiplatform plugin з com.android.application в одному модулі.
Тому: агрегуючий KMP-модуль (умовно `app`), який збирає DI (Koin) +
Navigation 3 граф + експортує iOS framework, і окремий тонкий
`androidApp`, що застосовує com.android.application і просто
бутстрапить Activity. iOS-сторона — окремий Xcode-проєкт (не
Gradle-модуль), SwiftUI-shell навколо ComposeUIViewController з
експортованого framework.

## Головні рішення, що варто зафіксувати
- Room KMP замість SQLDelight (менше нового вчити, той самий API з
  Android)
- Ktor + власний легкий бекенд замість Firebase multiplatform SDK
  напряму (менше ризику на iOS, і так буде свій sync-queue)
- FCM лишається окремо для пушів (не залежить від вибору вище)
- Playground — Compose Desktop-застосунок, не Android/iOS build
- Domain-шар можна почати як один `core`-модуль, розділяти на
  model/domain пізніше за потреби
