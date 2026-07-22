# Known issues

Знайдені під час живого тестування на реальних пристроях, ще не
виправлені. Формат — окремий від `roadmap.md`: це не мілстоуни, а
конкретні дефекти в уже зданому коді, щоб не загубились між сесіями.
Коли виправлено — переносити нотатку в коміт-мессдж/PR і видаляти
звідси (або позначати `✅ fixed`, з посиланням на PR).

---

## Матчер/Places: свайп/картка партнера не з'являється на другому пристрої

**Знайдено:** 2026-07-22 (живе тестування, реальний пристрій)

**Симптом:** місце показалось у Matcher на одному пристрої, юзер його
задізлайкав — але на пристрої партнера це місце (і сам факт свайпу)
жодного разу не з'явилось, хоча обоє мають побачити картку, поки не
вирішать обидва.

**Корінна причина:** в застосунку взагалі немає inbound-синку
(Firestore → Room) для `placeCandidates`/`placeSwipes`. Є тільки:
- **push** (пристрій → Firestore): `SyncCoordinator`
  (`data/.../sync/SyncCoordinator.kt`) дренажить локальну чергу
  pending-writes і пушить — це працює, тому свайп із пристрою
  користувача пішов коректно.
- **pull** (Firestore → інший пристрій): існує **тільки для Diary**
  (`FirestoreDiaryContentPuller`,
  `data/.../diary/FirestoreContentPuller.kt`), і той тригериться лише
  foreground 5-секундним polling-лупом, поки відкритий Timeline-екран
  (`DiaryTimelineDataSource.pollTripContent`,
  `feature/diary/.../DiaryTimelineDataSource.kt:81-90`). Ані
  `feature:places`, ані `feature:matcher` такого пулера не мають —
  обидва лише читають локальний Room через `observeByTrip(tripId)`.
- Жодна Cloud Function не слухає `placeCandidates`/`placeSwipes` —
  єдина існуюча (`onDiaryEntryWritten`,
  `functions/src/symmetricUnlock.ts`) шле FCM тільки для
  diary "day ready".
- `BackgroundWorkScheduler`'s періодичний job (кожні 15 хв) покриває
  тільки push/retry-сторону (`EPISODE_RETRY`, `PLACE_RETRY`,
  `SYNC_QUEUE_FLUSH`) — жодного pull-виклику.

**Важливо:** це не регресія з M14/M15 — `MatcherContainer`'s логіка
(`deck`/`myTurnDeck`/`isMyTurn`) коректна й покрита тестами. Дефект
був уже прихований у M13.2 (Places), просто ніхто не тестував Places
на двох пристроях — `docs/manual-checklists.md`'s M13.2-чекліст
повністю single-device.

**Що робити:** новий Matcher/Places content puller (за зразком
`FirestoreDiaryContentPuller`) + вмикання його в
`BackgroundWorkScheduler`'s періодичний job (не тільки foreground,
на відміну від Diary) — окремий мілстоун, не швидкий фікс.

---

## Онбордінг повторюється на кожному холодному старті

**Знайдено:** 2026-07-22 (живе тестування, реальний пристрій; помічено
одразу після невдалого імпорту місця, але причина не пов'язана)

**Симптом:** застосунок постійно показує Onboarding-екран знову,
замість того щоб одразу відкрити головний екран.

**Корінна причина:** `AlongsideApp.kt`'s кореневий back stack завжди
стартує з `Login` (`rememberNavBackStack(elements = arrayOf(Login))`),
покладаючись на те, що кожен gate-екран сам "проскочить" вперед через
side effect, якщо його умова вже виконана. `Pairing`'s умова durable
(реальний trip-запис через `pairingRepository`), а от `Onboarding`'s —
ні: `OnboardingState.cameraGeolocationAcknowledged`/
`shareSetupAcknowledged` (`feature/onboarding/.../OnboardingState.kt`)
за замовчуванням `false` в кожному новому `OnboardingState()`, і ніде
не персистяться (ні Room, ні SharedPreferences/NSUserDefaults) —
дозволи (photo/notification) персистяться через `PermissionController`,
а ці два "acknowledgment"-прапори — ні. Тому кожен cold start (у т.ч.
просто те, що Android вбив процес під пам'яттю, наприклад, після
шеринга в інший застосунок) змушує пройти ці два кроки заново.

**Не пов'язано з 404-помилкою при імпорті** (`KtorShareLinkRedirectResolver.kt:28-32`
— "Expected a redirect with a Location header, got HTTP 404", коли
Google-шеринг-лінк вже недійсний/rate-limited) — та помилка
обробляється коректно, не чіпає auth/сесію, ніяк не причетна до
onboarding-багу. Збіг у часі, ймовірно, через те, що шеринг в інший
застосунок і назад міг спричинити вбивство процесу Android-ом,
зробивши наступний запуск справжнім cold start.

**Погоджена поведінка (користувач, 2026-07-22):** Onboarding має
відбутися ОДИН РАЗ — одразу після авторизації і приєднання до трипу.
Далі, на кожному наступному запуску (уже авторизований + уже в
трипі) — одразу на головний екран, без Onboarding і без Pairing.

**Статус:** у роботі, перший фікс у черзі.

---

## (не баг, для довідки) 404 при резолві Google Maps share-link

**Знайдено:** 2026-07-22, разом з онбордінг-багом, спершу здавалось
пов'язаним

**Симптом:** одноразова помилка "Expected a redirect with a Location
header, got HTTP 404" при імпорті місця через share-лінк.

**Причина:** `KtorShareLinkRedirectResolver.kt:28-32` очікує
редирект (3xx + `Location`-хедер) від `maps.app.goo.gl`
short-URL; Google повернув 404 (лінк недійсний/rate-limited/зіпсований).

**Висновок:** обробляється коректно — `PlaceImportPipeline` →
`PlaceImportContainer` → dismissable `MessageCard` в
`PlaceImportScreen`. Нічого не падає, auth/сесія не чіпаються.
Нічого виправляти не треба — залишено тут лише щоб не сплутати з
онбордінг-багом знову.
