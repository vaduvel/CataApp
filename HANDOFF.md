# HANDOFF — fișier de execuție pentru agentul următor

> Scris pe 20 august 2026 de agentul care a construit M0–M8, înainte de a atinge limita de
> context. Dacă tu ești agentul următor: **citește acest fișier până la capăt înainte de
> a scrie o linie de cod.** Conține tot ce mai e de făcut, cum se face, și toate capcanele
> deja plătite cu timp pierdut. Nu le plăti a doua oară.

---

## 0. Ordinea de citire

1. `HANDOFF.md` (acest fișier) — starea de acum și ce mai e de făcut.
2. `AGENTS.md` — regulile de scriere a codului (stil, convenții, teste, ce nu se face niciodată).
3. `SPEC.md` — ce trebuie să facă aplicația (sursa de adevăr pentru scop).
4. Doar apoi fișierele de cod pe care le atingi, **integral**, înainte de a le modifica.

Dacă ceva din acest fișier contrazice realitatea din repo, **realitatea din repo câștigă** —
și actualizează fișierul ăsta în același commit.

---

## 1. Pentru cine e aplicația (contextul care schimbă deciziile)

Aplicația se numește **„Lucrări”** (`com.emanus.lucrari`) și e un **cadou surpriză** pentru
fratele proprietarului repo-ului: constructor în Italia, lucrează singur — e și muncitor, și
administrator. Nu e om de calculatoare. Nu poate fi întrebat nimic despre cum vrea aplicația,
pentru că nu trebuie să afle până nu e gata.

Ce înseamnă asta concret:

- **Se instalează prin cablu**, direct pe telefonul lui. Nu ajunge în Play Store. Fără cont,
  fără login, fără onboarding lung.
- **Totul e local pe telefon.** Zero rețea. Vezi §5 — asta e o regulă verificată automat.
- **Nu procesează facturi și nu ține date personale de facturare.** Facturile există în
  aplicație doar ca *evidență* („am facturat / am încasat”), pentru că omul chiar emite
  facturi și are nevoie să știe ce a rămas neîncasat. Aplicația nu emite nimic oficial.
- **Interfața e în română.** Textul pentru descrierea de factură se generează în italiană
  (dicționar RO→IT în `domain/Dictionary.kt` + `domain/Descrizione.kt`), pentru clienții lui.
- **Ergonomia bate frumusețea.** Omul deschide telefonul cu mâinile murdare, în picioare, pe
  șantier. Un ciclu „am lucrat azi aici” trebuie să se facă în **2 apăsări**. Butoanele mari
  (56 dp) și cifrele mari nu sunt decor, sunt cerință.

**Nu adăuga funcții noi din proprie inițiativă.** Aplicația e la ~95%. Cel mai mare risc de
aici încolo nu e o funcție lipsă, ci o regresie într-o funcție care merge deja.

---

## 2. Cum se lucrează la proiectul ăsta (fluxul cu doi agenți)

Sunt **doi** agenți și fiecare poate ce celălalt nu poate:

| | Agentul din chat (tu, probabil) | Agentul local (pe Mac-ul lui) |
|---|---|---|
| Scrie cod | ✅ prin API-ul GitHub, direct pe `main` | ✅ |
| Rulează Gradle / teste | ❌ | ✅ |
| Instalează pe emulator / telefon | ❌ | ✅ |
| Citește imagini / screenshot-uri | ❌ (nici el) | ❌ |
| Comite fișiere binare (PNG etc.) | ❌ *(vezi §7)* | ✅ |

Deci ciclul e: **tu scrii și împingi → utilizatorul dă promptul agentului local → agentul
local rulează, testează pe emulator + telefon și raportează text → tu repari.**

Pentru că niciunul dintre agenți nu vede imagini, **tot ce se verifică se dovedește prin
text**: `uiautomator dump`, `dumpsys notification`, `logcat`, `sqlite3` pe baza de date,
`md5`. Dacă o dovadă nu se poate scrie ca text, cere-i **utilizatorului** să se uite el.

### Regula de aur a push-ului (încălcată de 2 ori, ambele scump)

Un commit trebuie să compileze **singur**. Greșeala repetată a fost: plănuiesc 3–4 fișiere,
trimit 2.

**Procedura obligatorie înainte de fiecare push:**

1. Scrie pe hârtie (în răspuns) lista completă de fișiere pe care o cere schimbarea —
   inclusiv testele, inclusiv `strings_*.xml`.
2. Trimite-le pe **toate într-un singur apel** `push_files`.
3. După push, **recitește lista de fișiere a commit-ului** și compar-o cu lista de la pasul 1.
4. Dacă ai schimbat o semnătură de funcție, caută **toți** apelanții înainte de push
   (căutarea de cod GitHub nu merge — vezi §7 — deci mergi pe căi exacte).

**Regula simetrică pentru agentul local:** împinge imediat după fiecare commit și confirmă că
`origin/main` indică același SHA ca `HEAD`. Altfel, agentul din chat construiește peste o bază
veche și istoriile diverg — exact ce s-a întâmplat când commit-ul iconiței a rămas numai local.

---

## 3. Starea exactă la predare

- **Branch:** `main`. **HEAD de cod la actualizarea acestui fișier:** `6bd2b8d605cfd8c5388e22f60ef9b4d8c55586bb`
- **Versiune:** `versionName = "0.10.0-m8"`, `versionCode = 10`
- **Teste:** **88 unitare** + **8 instrumentate**, verzi pe emulator și pe telefonul fizic
- **Schema Room:** v1, `app/schemas/com.emanus.lucrari.data.AppDb/1.json`, hash `9a2846d7ed22222385ac20bef74419e7` — **neschimbată din M1** și așa trebuie să rămână (§5)
- **Instalat:** `0.10.0-m8` pe emulator (API 36) și pe telefonul fizic (`R5GL52XSGQP`, Samsung SM-A165F)

### Ce e verificat pe dispozitiv real

M0–M8 sunt verificate integral, pe emulator **și** pe telefon: schelet, Room + demo, clienți,
lucrări, etape și zile lucrate, rest de făcut, materiale, măsurători și extra, bani și facturi,
descrierea pentru factură (test golden byte cu byte), poze, backup automat + export/import,
memento-uri, programare cu dată de început și calendar lunar.

### P0 verificat pe 20 august 2026

Runda restantă, iconița și regresia găsită manual au fost verificate cap-coadă:

| Commit | Ce face | Stare |
|---|---|---|
| `89f1dd4c32df75e9f6fafb2bbb642b79107b6aaf` | „Calendar de lucru” are rând propriu în ecranul „Mai mult” | verificat manual pe emulator |
| `45a4df6` + `7ed1f85` | lucrarea programată apare pe „Azi” din ziua începerii | verificate împreună; primul commit rămâne inert luat separat |
| `7d846ec75fcff87792ef446ae2bd02e865819653` | testul instrumentat pentru regula de mai sus | 8/8 verzi pe emulator și 8/8 pe telefon |
| `de94e9bc69d3212e5cf7eeffc5841156068f48fe` | iconița aplicației | build + lint verzi; 5 densități și adaptive icon corecte; aspectul din sertarul Samsung rămâne de confirmat de utilizator |
| `6bd2b8d605cfd8c5388e22f60ef9b4d8c55586bb` | păstrează lucrarea restantă în `PROGRAMAT` până la prima zi trecută | regresie găsită și reparată în P0; test unitar + scenariu manual verzi |

Următoarea schimbare de produs este P1.

### Regula implementată în ultima rundă (ca s-o poți verifica)

O lucrare cu status `PROGRAMAT` intră pe ecranul **Azi** din **ziua în care ar trebui să
înceapă**, nu mai devreme. Dacă ziua a trecut, rămâne acolo cu textul „Trebuia să înceapă
18/08”. Stă **prima** în listă. Apăsarea butonului mare („Am lucrat azi aici”) o trece
automat pe `IN_LUCRU` și dispare din secțiunea de programate.

Implementare: `JobDao.observeToday(date, statuses, planned)` în `data/Daos.kt` (interogare,
fără schemă nouă) + sortarea în Kotlin cu `TODAY_ORDER` în `data/repo/JobRepo.kt`.

---

## 4. Mediu și comenzi standard

JDK-ul din Android Studio e singurul care merge. `JAVA_HOME` **trebuie** să se termine în
`/Contents/Home`, altfel apare „Unable to locate a Java Runtime”.

```bash
git pull
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# build + unitare + lint
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug

# instrumentate (cere emulator pornit)
./gradlew :app:connectedDebugAndroidTest

# aplicația nu are voie să atingă rețeaua
bash tools/check-no-internet.sh

# schema Room nu are voie să se schimbe (referință: commit-ul M6)
git diff --exit-code eaafada9c88a5ba205b41b17ad8688532a33bf37 -- app/schemas/com.emanus.lucrari.data.AppDb/1.json

# instalare
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell dumpsys package com.emanus.lucrari | grep -e versionName -e versionCode

# unelte de verificare fără ochi
adb shell uiautomator dump          # DOAR pe emulator, vezi §7
adb shell dumpsys notification --noredact
adb shell pm grant com.emanus.lucrari android.permission.POST_NOTIFICATIONS
```

Stive: AGP 8.7.3, Kotlin 2.0.21, Gradle 8.9, JDK 17, compileSdk/targetSdk 35, minSdk 26,
Compose BOM 2024.10.01 (`material-icons-extended`), Room + KSP, `work-runtime-ktx`,
`kotlinx-serialization-json`, `coil-compose`.

### Promptul standard de verificare (dă-i-l utilizatorului după fiecare rundă)

> `git pull` (HEAD trebuie să fie `<sha>`) → `export JAVA_HOME=...` →
> `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` (88 unitare) →
> `./gradlew :app:connectedDebugAndroidTest` (8 instrumentate) →
> `bash tools/check-no-internet.sh` → `git diff --exit-code eaafada9... -- app/schemas/...1.json` →
> `adb install -r ...` pe ambele dispozitive → apoi scenariul manual: *(pașii concreți, numerotați,
> cu ce trebuie să apară pe ecran la fiecare pas)*.

Scenariul manual nu e opțional. Testele verzi nu au prins niciunul dintre bug-urile reale de
UI din M8 — toate au ieșit la mână, pe emulator.

---

## 5. Invarianți — lucruri care nu se încalcă niciodată

1. **Zero rețea.** Fără permisiuni de internet, nici măcar tranzitiv din biblioteci. Se verifică
   automat cu `tools/check-no-internet.sh`, care se uită în manifestele **fuzionate**. Dacă o
   bibliotecă adaugă `ACCESS_NETWORK_STATE`, se scoate cu `tools:node="remove"` în manifest.
2. **Schema Room rămâne v1.** Tot ce s-a cerut până acum a încăput în coloanele existente.
   O migrare pe telefonul lui, cu date reale, fără să poți depana la fața locului, e un risc
   pe care nu-l luăm pentru un finisaj. Dacă chiar e nevoie de o coloană nouă: migrare scrisă
   de mână + test de migrare + backup înainte, și **întreabă utilizatorul întâi**.
3. **Fără biblioteci noi** fără motiv serios și acordul utilizatorului.
4. **Textele de interfață stau în `strings*.xml`**, în română, cu diacritice. Fără text
   hardcodat în Compose.
5. **Fiecare regulă de logică are test unitar**, în `domain/`, fără Android și fără bază de date.
   Convenția: JUnit4, `org.junit.Assert.*`, nume de test în română cu underscore, tabulatori, KDoc.
6. **Mesajele de commit** sunt în română **fără diacritice**, în stil `feat(scope): ...`,
   `fix(scope): ...`, `test(scope): ...`, `docs(scope): ...`.
7. **Nu se rescriu fișiere pe care nu le-ai citit integral** în sesiunea curentă.

---

## 6. Ce mai e de făcut (în ordinea asta)

### P0 — Verificat pe emulator și telefon

Build + 88 unitare + 8 instrumentate pe fiecare dispozitiv + lint + offline + schemă + install
au trecut. Scenariul de mai jos a fost verificat pe emulator la data de 20/08/2026 și rămâne
checklist de regresie:

1. Lucrare nouă cu început **azi** → apare **prima** pe „Azi”, cu chip `Programat` și „Începe azi”.
2. Lucrare nouă cu început **mâine** → **nu** apare pe „Azi”, dar apare în calendar.
3. Apasă butonul mare pe (1) → trece pe `În lucru`, rândul de programat dispare.
4. Lucrare cu început 18/08 → „Trebuia să înceapă 18/08”.
5. „Mai mult” → există rândul **„Calendar de lucru”** și deschide calendarul (`89f1dd4`).

### P1 — Intervalul programat în ecranul de detaliu al lucrării

**Singura funcție care mai lipsește.** În lista de lucrări intervalul se vede deja
(`jobs_planned_period`), în detaliu nu.

- Fișier: `app/src/main/java/com/emanus/lucrari/ui/screen/JobDetailScreen.kt`.
  **Atenție: fișierul e mare și s-a trunchiat la citire de două ori.** Citește-l integral
  înainte de orice modificare; dacă se trunchiază iar, **nu ghici** — deleagă modificarea
  agentului local cu instrucțiuni exacte (el are fișierul pe disc).
- Ce se afișează: sub adresă, dacă `job.plannedStart != null`, un rând cu intervalul.
  Sfârșitul se calculează cu **`Schedule.endDate(start, estDays)`** din `domain/Schedule.kt`
  (o lucrare de o zi începe și se termină în aceeași zi; fără `estDays` se presupune o zi).
  Formatarea zilelor: `Dates.dayMonth` („dd/MM”) sau `Dates.full` („dd/MM/yyyy”) din `domain/Dates.kt`.
- Refolosește string-ul existent `jobs_planned_period` dacă se potrivește; altfel adaugă unul
  nou în `app/src/main/res/values/strings_m8.xml`. **Nu inventa formate noi de dată.**
- Test unitar pentru `Schedule.endDate` există deja; dacă adaugi logică nouă, adaugă și test.

### P2 — Curățenia de dinaintea livrării (obligatoriu, altfel primește un telefon plin de gunoi)

Pe telefon au rămas lucrări și clienți de test din toate rundele de depanare. **Nu dispar cu
butonul „șterge datele demo”** — acela șterge doar seed-ul.

Nume de șters (cel puțin): `Test3`–`Test7`, `FixTest`, `RotTest`, `ListaTest1`–`ListaTest3`,
`AnchorTest1`–`AnchorTest3`, `Lnc1`, `Lnc2`, `OrfanTest` (posibil scris greșit `OrfannT` —
emulatorul pierdea caractere la tastare), plus orice apare din rundele următoare.

Procedura recomandată, în ordine:

1. Rulează scenariile de verificare **pe emulator**, nu pe telefon, ca să nu mai adaugi gunoi.
2. La final: `adb -s R5GL52XSGQP shell pm clear com.emanus.lucrari` — șterge tot curat.
3. Repornește aplicația o dată (seed-ul demo reapare doar dacă nu a fost șters explicit —
   vezi `Seed.ensure` + flag-ul `demoSeeded`), apoi apasă „șterge datele demo”.
4. Confirmă cu `sqlite3` că tabelele `clients`, `jobs`, `reminders` sunt goale.
5. Abia apoi instalarea finală.

Tot aici: ridică versiunea la ceva de livrare (`versionName = "1.0.0"`, `versionCode = 11`) și
instalează varianta finală pe telefon.

*(Pe emulator au rămas 4 rânduri orfane în tabelul `reminders`, din vremea când `Reminder` nu
avea filtrare. Nu se mai văd în interfață și nu afectează telefonul. Curăță-le doar dacă vrei
emulatorul curat.)*

### P3 — Finisaje de cod și vizual (nu blochează livrarea)

- **Regula de ancoră a listelor.** Ecranele `Money`, `Photos`, `Punch` nu au primit încă
  tratamentul care a reparat săritul listei la revenire (vezi §7, capcana 4). Regula e scrisă
  în `AGENTS.md`. Aplic-o **la prima reproducere**, nu preventiv pe toate.
- **Decizie deschisă:** `JobsScreen` folosește `boardOnce` (interogare o singură dată la
  revenire) în loc de abonamentul `board(query)`. A fost soluția pragmatică pentru bug-ul de
  listă care nu se împrospăta; cauza rădăcină nu a fost niciodată identificată complet
  (teoriile „snapshot WAL” și „abonamentul rămâne cu mulțimea veche” au fost **infirmate**
  amândouă). Dacă cineva vrea înapoi abonamentul reactiv: **commit separat**, și reprodus de
  **3 ori** înainte de a-l declara reparat.
- **Refactorizare vizuală:** `Dimens.listBottomSpace` în loc de spații magice, cifrele mari pe
  `displayLarge`, cardul de backup și dialogul de import aduse la aceeași grilă, eliminarea
  hex-urilor și a `dp`-urilor rămase prin ecrane.
- **Cod mort de la M7:** `PhotoStore.complete()`, `BackupRepo.readAndImport()`, shortcut-ul
  nefolosit din `PhotosScreen`.
- **`SPEC.md`:** mută în el regulile deja notate în `AGENTS.md`, ca să existe un singur loc.

### P4 — M9, opțional, doar dacă mai e timp după livrare

Statistici simple, widget „Am lucrat azi la…”, „Spațiu folosit”, stările „camera nu e
disponibilă” și „backup eșuat”, zile per etapă (**cere migrare** — vezi invariantul 2),
câmpul `work`. Nimic din astea nu blochează predarea.

---

## 7. Capcane dovedite (plătite deja, nu le mai plăti)

1. **Căutarea de cod GitHub nu funcționează pe repo-ul ăsta.** `search_code` întoarce
   `{"total_count":0,"incomplete_results":true}`. Folosește `get_file_contents` cu **cale
   exactă**. Pentru listarea unui director: calea cu `/` la final și `ref: "refs/heads/main"`.
2. **Nu poți comite fișiere binare din chat.** PNG-uri, APK-uri, orice non-text: dă-i
   instrucțiuni agentului local (el le generează cu `sips` și le comite). Așa s-a făcut iconița.
3. **`LaunchedEffect(key)` rulează și la prima compoziție**, nu doar la schimbarea cheii.
   A produs bug-ul „lista sare în cap la revenirea dintr-o lucrare”. Dacă efectul trebuie să
   ruleze doar la schimbări ulterioare, ține un `rememberSaveable` cu ultima valoare tratată.
4. **`remember` vs `rememberSaveable`.** Starea care trebuie să supraviețuiască rotirii sau
   revenirii din background merge în `rememberSaveable`. Un `remember` pus greșit a costat o rundă.
5. **Mementourile se creează DOAR de `ReminderWorker`, nu la salvarea lucrării.** Dacă vrei să
   testezi o notificare, nu căuta rândul în `reminders` imediat după ce ai salvat.
6. **`cmd jobscheduler run -f` nu expediază workerul** pe API 36. Singura metodă care a
   funcționat: rescrierea lui `last_enqueue_time` direct în baza de date WorkManager.
7. **`uiautomator` e blocat pe telefonul Samsung** (SIGKILL în
   `UiAutomationManager.registerUiTestAutomationServiceLocked`). Automatizarea UI se face
   **doar pe emulator**; pe telefon verifică utilizatorul, manual.
8. **Emulatorul API 36 dă ANR-uri și pierde caractere la tastare** („OrfanTest” → „OrfannT”).
   Verifică textul introdus cu `uiautomator dump` înainte de a trage concluzii dintr-un test.
9. **Capcane de automatizare UI:** FAB-ul nu e un nod cu text (zona clickabilă a fost
   `[603,1642][1035,1800]`); tap-urile la `y > 1500` cu tastatura deschisă lovesc taste reale.
10. **`adb install -r` repornește procesul**, deci „merge după reinstalare” nu dovedește că un
    bug de stare în proces a fost reparat. Reproduce **în același proces**.
11. **Căi care înșală:** `JobRepo.kt` e în `data/repo/`, nu în `data/`. `ReminderNotifier` e în
    `work/ReminderWorker.kt`, nu într-un fișier propriu.
12. **Testează commit-ul corect.** S-a raportat o dată un rezultat de pe `44bce39` când HEAD
    era `46f2817`. Pune mereu SHA-ul așteptat în promptul de verificare.

---

## 8. Harta codului (ca să nu cauți)

```
app/src/main/java/com/emanus/lucrari/
├── data/            AppDb, AppPrefs, Converters, Daos, Entities, Enums, Ids, M7Daos
│   ├── backup/
│   └── repo/        JobRepo.kt  ← aici, nu în data/
├── domain/          (14) BackupRotation, Dates, Descrizione, Dictionary, Measures, Money,
│                    MonthGrid, Progress, ReminderRules, Rules, Schedule, Seed, Templates, Totals
├── ui/
│   ├── nav/         AppNav.kt
│   └── screen/      (11) Calendar, Clients, Descrizione, JobDetail, JobMoney, Jobs, Money,
│                    More, Photos, Punch, Today
└── work/            ReminderWorker.kt (+ ReminderNotifier), WorkScheduler.kt

app/src/androidTest/java/com/emanus/lucrari/   BackupRestoreTest.kt, DbTest.kt
app/src/main/res/values/                       strings_m8.xml și restul
app/schemas/com.emanus.lucrari.data.AppDb/1.json
tools/check-no-internet.sh
```

**Entități cheie:** `Job(id, clientId, title, street?, city?, addrNote?, type?, status = OFERTAT,
plannedStart, estDays, billing = CORP, agreedPriceCents?, dayRateCents?, note?, createdAt, closedAt?)`;
`Reminder(id, jobId?, clientId?, text, dueAt, auto, done)` — **fără cheie străină**, de aceea
`ReminderDao.observeOpen()` filtrează joburile inexistente.

**Statusuri:** `OFERTAT`, `PROGRAMAT`, `IN_LUCRU`, `ASTEPTARE`, `DE_FINISAT`, `TERMINAT`, `ANULAT`.
`ACTIVE_STATUSES = [IN_LUCRU, DE_FINISAT, ASTEPTARE]`, `TODAY_ORDER = [PROGRAMAT] + ACTIVE_STATUSES`.

**Paletă** (culori de șantier, contrast mare la soare): primary `#B94708`, safety `#F47A20`,
peach `#FFDBCA`, charcoal `#1F2529`, steel blue `#305F73`, concrete `#F5F2EC`, cement `#E5E1D8`,
success `#2E7D32`, warning `#F9A825`, error `#C62828`. Iconița: fundal `#CFE3F7`.

---

## 9. Definiția de „gata” — checklist de livrare

Aplicația se predă când **toate** rândurile de mai jos sunt bifate:

- [ ] `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` — verde
- [ ] `./gradlew :app:connectedDebugAndroidTest` — verde
- [ ] `bash tools/check-no-internet.sh` — „OK: nicio permisiune de rețea”
- [ ] `git diff --exit-code eaafada9... -- app/schemas/...1.json` — fără ieșire
- [ ] Intervalul programat se vede în detaliul lucrării (P1)
- [ ] Ciclul „am lucrat azi aici” se face în 2 apăsări, pe telefonul fizic
- [ ] Rotire, background/foreground, kill și redeschidere — nu se pierde nimic
- [ ] Export → `pm clear` → import — starea revine identic, inclusiv pozele
- [ ] Datele de test șterse de pe telefon și confirmate cu `sqlite3` (P2)
- [ ] Versiunea de livrare instalată pe telefon și confirmată cu `dumpsys package`
- [ ] Iconița verificată **vizual de utilizator** în sertarul telefonului
- [ ] Pagina de spec din Notion actualizată la starea finală

---

## 10. Cum se raportează utilizatorului

- **În română.** El citește rapoartele, dă mai departe promptul agentului local și decide.
- **Spune ce nu ai verificat.** Un raport care ascunde o verificare lipsă valorează mai puțin
  decât niciun raport — și aici s-a construit totul pe dovezi text.
- **Nu declara „gata” pe baza unui build verde.** Toate bug-urile serioase din M8 au trecut de
  teste și au căzut la scenariul manual.
- **Nu putem citi imagini.** Când singura dovadă e vizuală, lasă screenshot-ul pe disc și
  cere-i lui să se uite.
- Pagina de spec (Notion, „Spec tehnic — App «Lucrări»”) are un tabel cu milestone-uri în §11
  și un paragraf „Stare la …” sub el. Se actualizează **o singură dată**, după raportul verde,
  nu la fiecare push.

**Termen:** țintă weekend 22–23 august 2026, marjă până joi 27 august. Livrare înainte de luni
24 august. Aplicația e aproape gata — dacă rămâi fără timp, sacrifică P3 și P4, niciodată P2.

---

*Succes. Omul chiar are nevoie de aplicația asta și nu știe că vine.*
