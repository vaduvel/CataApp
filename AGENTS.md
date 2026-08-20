# AGENTS.md — instrucțiuni pentru agentul care lucrează în acest repo

Sursa de adevăr pentru **ce** se construiește este [`SPEC.md`](SPEC.md). Citește-l înainte de orice modificare. Acest fișier spune **cum** se lucrează.

## Comenzi obligatorii

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
./gradlew :app:lintDebug
./gradlew :app:connectedDebugAndroidTest   # numai cu telefon/emulator conectat
bash tools/check-no-internet.sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Cerințe: JDK 17 sau 21, Android SDK 35. Wrapper-ul Gradle 8.9 este comis; folosește `./gradlew`, niciodată `gradle` direct. Bytecode-ul rămâne 17. Nu adăuga `jvmToolchain(17)`.

Dacă `./gradlew` cade cu „Unable to locate a Java Runtime”, JDK-ul de sistem lipsește. Folosește JBR-ul din Android Studio, care este tot Java 21:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
"$JAVA_HOME/bin/java" -version   # confirmă Java 21 înainte de build
```

Calea are nevoie de `/Contents/Home` la final, altfel Gradle nu găsește `bin/java`. Nu o comite în `gradle.properties`: e specifică mașinii. Semnătura debug rămâne `~/.android/debug.keystore`, deci schimbarea JDK-ului nu împiedică `adb install -r` peste o versiune deja instalată.

## Reguli care nu se încalcă

1. **Fără rețea.** Nu adăuga INTERNET, ACCESS_NETWORK_STATE sau ACCESS_WIFI_STATE. Nicio funcție nu depinde de semnal. WorkManager declară singur `ACCESS_NETWORK_STATE`; o scoatem din manifestul fuzionat cu `tools:node="remove"`. Orice bibliotecă nouă se verifică la fel, iar scriptul offline acceptă numai liniile marcate explicit cu `tools:node="remove"`.
2. **Banii sunt `Long` în cenți.** Scrierea/formatarea trece prin `domain/Money.kt`; cantitățile prin `domain/Measures.kt`; totalurile prin `domain/Totals.kt`. Fără `NumberFormat` și fără `String.format` pentru rezultatele golden.
3. **Aplicația nu emite facturi.** Produce evidența și textul pentru contabil: zero XML, SdI, TVA sau date fiscale.
4. **Facturat și încasat sunt independente.** Bifa unei facturi nu creează o încasare.
5. Un extra intră în bani numai cu `accepted && billable`.
6. Aceeași lucrare/sumă nu se repetă pe același ecran.
7. Codul și identificatorii sunt în engleză; textele UI sunt în română și stau în resurse XML; textul pentru contabil este în italiană și stă în `domain/Descrizione.kt`/`Dictionary.kt`. Nu folosi ghilimele tipografice amestecate cu ghilimele ASCII în literale Kotlin.
8. Logica de business este pură în `domain/`, nu în composable-uri.
9. Fără `!!`, `GlobalScope` sau operații Room pe main thread.
10. Dependențele se declară numai în `gradle/libs.versions.toml` și se adaugă doar cu justificare.

## Baza de date și backup

- Room are `exportSchema = true`; schemele se comit în `app/schemas/` numai după build reușit.
- Fără `fallbackToDestructiveMigration`. Orice schimbare reală de schemă primește migrare explicită.
- DAO-urile și interogările adăugate după M6 nu schimbă schema: `app/schemas/com.emanus.lucrari.data.AppDb/1.json` trebuie să rămână neschimbat față de `eaafada`.
- Enum-ul `Unit` din spec se numește `MeasureUnit` ca să nu umbrească `kotlin.Unit`.
- **`Reminder` nu are cheie străină spre `jobs`**, pentru că un memento poate fi legat de un client sau de nimic. Ștergerea unei lucrări trece prin `JobRepo.deleteJob`, care șterge întâi memento-urile ei (`ReminderDao.deleteByJob`); în plus, `observeOpen()` ignoră rândurile rămase de la lucrări care nu mai există. Orice tabel nou fără cheie străină spre `jobs` se tratează la fel.
- **Datele demo intră o singură dată.** După o instalare curată sau `pm clear`, seed-ul apare la prima pornire. Butonul „Șterge datele demo” păstrează steagul `demoSeeded`, deci demo-ul nu mai reapare la repornirile următoare.
- Importul verifică `schemaVersion` înainte să șteargă ceva.
- Modul „Adaugă ce lipsește” folosește `INSERT IGNORE` pe UUID și trebuie să fie idempotent.
- Arhiva este ZIP cu `data.json` și `photos/<photoId>.jpg`; backup-urile automate sunt `files/backup/lucrari-YYYY-MM-DD.zip`, ultimele 7.
- Export/import exclusiv prin SAF. Zero permisiuni de storage. Partajarea și camera folosesc exclusiv `FileProvider`.

## Poze și notificări

- Camera externă scrie în `files/photos/<uuid>.jpg` prin `ActivityResultContracts.TakePicture`/`FileProvider`; nu cere permisiune CAMERA sau storage.
- Imaginea salvată are latura maximă 1600 px și JPEG quality 80.
- `ReminderWorker` rulează periodic cu țintă 19:00 și fără constrângere de rețea; memento-urile de început în ziua respectivă vin de la un al doilea worker, `daily-start-reminders-0730`.
- **Memento-urile nu se creează la salvarea lucrării**, ci numai când rulează workerul. Într-un test manual trebuie forțat (vezi mai jos).
- Dedup: maxim un memento automat deschis per `(jobId, ReminderKind)`.
- `BackupWorker` rulează zilnic și rotește arhivele.
- Pe Android 13+ singura permisiune nouă este `POST_NOTIFICATIONS`; dacă e refuzată, memento-ul rămâne în aplicație.

## Interfața

- Bifa se schimbă numai din butonul de bifă; atingerea textului nu bifează/debifează.
- Butoanele principale au minimum 56 dp; `IconButton` asigură ținta de 48 dp.
- Ștergerea stă la marginea opusă bifei sau în foaia de editare.
- Textul pentru contabil se compune din datele curente și nu se salvează separat.
- Cele cinci destinații din bara de jos sunt fixe: Azi, Lucrări, Rest, Bani, Mai mult. Calendarul și memento-urile nu intră în bara de jos; calendarul se deschide din iconița de pe ecranul Lucrări și dintr-un rând din „Mai mult”.
- Cele 7 statusuri se afișează cu etichetă text și pictogramă, nu numai prin culoare.
- Culorile, mărimile de text, colțurile și distanțele vin din `ui/theme/` (`Color.kt`, `Type.kt`, `Shape.kt`, `Dimens.kt`). Nu scrie hex sau dp direct în ecrane.
- Statusul se randează numai prin `StatusChip`; culoarea plină stă în `StatusColor`, iar perechea fundal/text în `StatusTones`.
- **Inseturile de sistem se aplică o singură dată.** `Scaffold`-ul din `AppRoot` dă padding-ul lui `NavHost`, care îl consumă cu `consumeWindowInsets`, pentru că ecranele interioare au propriul `Scaffold`. Fără asta, ultimul rând al listelor lungi intră sub bara de navigare.
- **Starea formularelor și ancorele de listă folosesc `rememberSaveable`**, nu `remember`: rotația și revenirea din background nu au voie să piardă nimic.
- **Listele care primesc rânduri noi în cap se derulează la cap numai când chiar se schimbă capul.** Se ține minte id-ul primului rând într-un `rememberSaveable` și se compară înainte de `scrollToItem(0)`. Un `LaunchedEffect(cheie)` rulează și la prima compoziție, deci fără comparație lista sare în cap la fiecare revenire din detaliu, iar poziția utilizatorului se pierde.

## Verificare pe emulator și pe telefon

Dovezile se dau în text, nu în capturi de ecran: `uiautomator dump`, `dumpsys`, `logcat`, interogări SQL.

```bash
adb shell dumpsys package com.emanus.lucrari | grep -e versionName -e versionCode
adb shell uiautomator dump                      # numai pe emulator
adb shell dumpsys notification --noredact
adb shell pm grant com.emanus.lucrari android.permission.POST_NOTIFICATIONS
git diff --exit-code eaafada -- app/schemas/com.emanus.lucrari.data.AppDb/1.json
```

- **`uiautomator` e blocat pe telefonul Samsung** (SIGKILL în `UiAutomationManager`). Verificările care au nevoie de dump se fac pe emulator; pe telefon se verifică vizual și prin `dumpsys`.
- **Atinge mijlocul zonei clickabile din dump, nu coordonate fixe.** `ExtendedFloatingActionButton` nu apare ca nod text, ci numai ca zonă clickabilă.
- **Închide tastatura (BACK) înainte de orice atingere pe listă.** Cu tastatura deschisă, atingerile de jos lovesc taste reale.
- **Forțarea unui worker:** `cmd jobscheduler run -f` nu îl expediază. Rescrie `last_enqueue_time` în baza de date a WorkManager-ului; la următoarea verificare, workerul se reprogramează cu întârziere 0. Merge pe API 36 și e singurul mod de a testa un memento fără să aștepți ora reală.
- **Emulatorul devine instabil sub sarcină:** ANR-uri la ștergeri repetate și caractere pierdute la scriere („OrfanTest” → „OrfannT”). Folosește nume scurte, pauze generoase între atingeri și, dacă reapare, pornește emulatorul la rece.
- Datele de test rămân în aplicație până le ștergi manual: **nu** dispar cu „șterge datele demo”.

## Definition of done

Pentru orice milestone: unit tests + assemble + lint + scriptul offline trec; cu emulator/telefon rulează și testele instrumentate. Commit-uri Conventional Commits.

## Stadiu

- **M0–M8 — gata și verificate**, pe emulator Android API 36 și pe telefonul fizic (SM-A165F).
- **88 de teste unitare** și **9 instrumentate** verzi pe fiecare dispozitiv; `assembleDebug`, `lintDebug`, scriptul offline și verificarea schemei trec.
- **M7:** poze locale prin `FileProvider`, backup ZIP zilnic cu rotație 7, export/import SAF cu „Înlocuiește tot” / „Adaugă ce lipsește”, memento-uri la 19:00 cu deduplicare. Scenariul export → ștergerea datelor → import verificat cu modul avion activ.
- **M8:** dată de început și interval la creare, statusul `PROGRAMAT` pentru lucrările din viitor, calendarul lunar de lucru și memento-urile de început (19:00 pentru „peste 3 zile” și „mâine”, 07:30 pentru „azi”). Fără migrare de schemă.
- **Runda de finisaj:** inseturile de sistem nu se mai aplică de două ori; memento-urile pleacă odată cu lucrarea ștearsă.
- **Versiunea curentă:** `1.0.0`, versionCode 11, instalată pentru livrare pe telefon.
- **Tema din design system-ul aprobat este aplicată:** paletă M3, scara de text, colțurile, tokenii de spațiere și chip-urile de status cu pictogramă.
- **Livrare:** intervalul este afișat în detaliul lucrării; telefonul nu mai conține date de test sau demo; butonul de ștergere demo și versiunea sunt în „Mai mult”; iconița a fost verificată în sertarul Samsung. Au rămas doar refactorizarea vizuală și M9, ambele opționale.
- **M9 — opțional, nu blochează livrarea:** statistici simple, widget „Am lucrat azi la…”, „spațiu folosit”, stările „camera nu e disponibilă” și „backup eșuat”, zile lucrate per etapă (cere migrare de schemă) și câmpul `work` în rândul măsurătorii din descriere.
