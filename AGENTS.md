# AGENTS.md — instrucțiuni pentru agentul care lucrează în acest repo

Sursa de adevăr pentru **ce** se construiește este [`SPEC.md`](SPEC.md). Citește-l înainte de orice modificare. Acest fișier spune **cum** se lucrează.

## Comenzi

```bash
./gradlew :app:assembleDebug          # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest      # logica pură (bani, text factură, reguli)
./gradlew :app:connectedDebugAndroidTest   # doar cu telefon sau emulator conectat (DbTest)
./gradlew :app:lintDebug
bash tools/check-no-internet.sh       # obligatoriu după build
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Cerințe: **JDK 17 sau 21**, **Android SDK 35**.

Nu schimba versiunea de JDK a mașinii ca să „respecți specul”. Bytecode-ul rămâne 17 în ambele cazuri, fixat prin `compileOptions` și `jvmTarget` din `app/build.gradle.kts`. Build-ul e verificat pe JDK 21. Nu adăuga `jvmToolchain(17)`: ar impune instalarea unui JDK 17 fără niciun câștig.

Wrapper-ul Gradle 8.9 **este comis**. Folosește `./gradlew`, niciodată `gradle` direct. Dacă lipsește după un clone incomplet, regenerează-l o singură dată cu `gradle wrapper --gradle-version 8.9`.

## Reguli care nu se încalcă niciodată

1. **Fără rețea.** Nu adăuga permisiuni de rețea (INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE). Dacă o funcție are nevoie de internet, nu intră în aplicație. Verifică cu `tools/check-no-internet.sh`.
2. **Banii se țin în cenți**, ca `Long`. Niciodată `Double`/`Float` pentru sume. Cantitățile de măsurători sunt `Double`.
	- Scrierea și citirea sumelor trec **doar** prin `domain/Money.kt`, iar cantitățile prin `domain/Measures.kt`. Fără `NumberFormat` și fără `String.format` pentru bani: formatul trebuie să fie identic pe orice telefon, indiferent de limba sistemului, pentru că testul golden din M6 compară șir cu șir.
	- Aritmetica banilor stă în `domain/Totals.kt`. Ecranele nu adună și nu scad sume singure.
3. **Aplicația nu emite facturi.** Produce evidența și *textul* care ajunge pe factură. Zero XML, zero SdI, zero TVA, zero date fiscale ale clientului.
4. **„Facturat” și „încasat” sunt două cifre separate.** Nu presupune niciodată că sunt egale (vezi SPEC §5.2). Bifa „încasată” de pe o factură nu creează o încasare și nu mișcă cifra de încasat: banii intrați se trec separat, cu data lor.
5. **Un extra intră în bani doar dacă e acceptat și se pune pe factură** (`accepted && billable`). SPEC §5.1 spunea inițial doar „billable”; regula corectă e cu amândouă, pentru că un extra pe care clientul nu l-a acceptat nu se facturează. Extra-ul rămâne scris în aplicație ca dovadă a muncii, dar nu intră în total.
6. **O sumă nu se arată de două ori pe același ecran.** Dacă o lucrare apare deja într-o listă de sus, nu se mai repetă în lista de dedesubt: cine se uită repede la telefon nu are cum să știe că e aceeași lucrare și citește dublu.
7. **Limbi:** cod și identificatori în engleză; textele din interfață în română, **doar** în `strings.xml`, niciodată scrise direct în composable-uri; textele generate pentru client sau contabil în italiană, doar în `domain/Descrizione.kt` și `domain/Dictionary.kt`.
	- Ghilimelele românești „…” se scriu doar în `strings.xml`. Într-un literal Kotlin, o ghilimea dreaptă ASCII folosită ca ghilimea de închidere termină string-ul mai devreme și produce erori derutante de tip `Unresolved reference`. Diacriticele nu sunt problema, ghilimelele sunt. Dacă ai nevoie de ghilimele în cod, scapă-le cu backslash.
	- Excepție: numele șabloanelor și ale etapelor din `domain/Templates.kt` sunt **date**, nu texte de interfață. Ele sunt și cheile din dicționarul RO → IT, deci rămân în cod și se scriu exact ca în SPEC §14.
8. **Logica de business stă în `domain/`**, pură, testabilă fără Android. Zero logică de calcul în composable-uri.
9. Fără `!!`, fără `GlobalScope`, fără operații de bază de date pe main thread.
10. Nicio dependență nouă fără justificare. Dependențele se declară **doar** în `gradle/libs.versions.toml`.

## Baza de date

- Room cu `exportSchema = true`. Schema se scrie în `app/schemas/` **abia după un build reușit** și trebuie comisă: e singurul mod în care migrările viitoare pot fi verificate.
- Fără `fallbackToDestructiveMigration`. Datele lui nu se șterg niciodată automat; la schimbarea schemei se scrie migrare.
- Interogările noi de citire (`@Query`) nu schimbă schema: `app/schemas/1.json` rămâne neatins cât timp entitățile și indecșii nu se schimbă.
- Enum-ul numit `Unit` în SPEC §4 se numește `MeasureUnit` în cod. Motivul e tehnic: un tip propriu numit `Unit` umbrește `kotlin.Unit` în orice fișier care îl importă, iar atunci orice lambda scrisă ca `() -> Unit` nu mai compilează. Restul modelului urmează specul literă cu literă.
- Datele demo (`domain/Seed.kt`) se scriu o singură dată, doar dacă tabela `clients` e goală. Cifrele sunt cele din scenariul de referință SPEC §10 și sunt folosite de testul golden din M6, deci nu se schimbă fără să se schimbe și specul.

## Regulile de status (SPEC §5.6)

Sunt funcții pure în `domain/Rules.kt`, acoperite de `RulesTest`. Regulile **propun**, nu impun: schimbarea de status o apasă omul.

- O zi salvată cu blocaj (`WorkDay.blocked`) trece lucrarea în `ASTEPTARE`, dar nu atinge lucrările deja închise.
- `TERMINAT` cu resturi nebifate cere confirmare, nu se refuză.
- Toate etapele bifate + resturi rămase → se propune `DE_FINISAT`.

## Interfața

Aplicația se folosește pe șantier, cu mâna murdară și din mers. De aici regulile care nu se negociază:

- **Bifa se schimbă doar din butonul de bifă**, niciodată prin apucarea rândului întreg. O atingere greșită nu are voie să debifeze tăcut o etapă terminată, un rest rezolvat sau o factură încasată. Apăsarea pe textul rândului deschide sau editează, atât.
- Butoanele principale au **56 dp** înălțime; butoanele-pictogramă se scriu ca `IconButton`, care are deja ținta de 48 dp.
- Acțiunea care șterge stă la marginea opusă a rândului față de cea care bifează. Unde rândul are deja cerc de bifă și editare, ștergerea stă în foaia de editare, nu pe rând.
- Fiecare lucrare apare o singură dată pe un ecran (regula 6 de mai sus).

## Definition of done pentru orice milestone

`./gradlew :app:testDebugUnitTest :app:assembleDebug` trece, `bash tools/check-no-internet.sh` trece, și criteriile din SPEC §10 pentru milestone-ul respectiv sunt bifate.

Commit-uri: Conventional Commits, ex. `feat(money): rest de incasat pe lucrare`.

## Stadiu

- **M0 — gata, build verificat local:** schelet Gradle, temă, navigare cu 5 ecrane, script de verificare offline.
- **M1 — gata:** modelul Room complet din SPEC §4 (12 entități, convertoare, DAO-uri), `domain/Seed.kt` cu lucrarea demo, `domain/Templates.kt` cu șabloanele din SPEC §14, ecranele Lucrări (listă, căutare după stradă, filtre, lucrare nouă în 4 câmpuri), Detaliu lucrare (status, etape, sunat, hartă, ștergere) și Clienți. `DbTest` acoperă seed-ul, căutarea și ștergerea în cascadă.
- **M2 — gata, verificat și pe emulator:** etape bifabile cu șabloane, „Am lucrat azi aici” dintr-o apăsare de pe ecranul Azi (idempotent pe zi), bara de avansare, estimat vs. real. `ProgressTest` 7/7, `DbTest` 4/4 pe API 36.
- **M3 — gata, verificat pe emulator:** rest de făcut per lucrare și pe tot ecranul Rest (grupat pe lucrare, cu motiv și termen), materiale, ziua cu blocaj și regulile de status din `domain/Rules.kt`. Regula blocaj → Așteptare și dialogul de confirmare la Terminat au fost confirmate pe dispozitiv.
- **M4 — gata, verificat pe emulator:** măsurători și extra în detaliul lucrării, `domain/Money.kt` (cenți ↔ text) și `domain/Measures.kt` (cantitate × preț unitar, rotunjit la cent). Extra are bifă de înțelegere, rând de dovadă și bifă separată de facturare. Teste: 32/32.
- **M5 — gata, verificat pe emulator:** `domain/Totals.kt` (baza după felul plății, extra acceptate și facturabile, total, de facturat, rest de încasat), încasări și evidența facturilor pe lucrare, ecranul Bani cu cele trei cifre mari și lista „De facturat”. Teste: 41/41 unitare, `DbTest` 4/4 pe dispozitiv, schema neschimbată.
- **Urmează M6:** „Descriere pentru factură” în italiană (`domain/Descrizione.kt`), dicționarul RO → IT din SPEC §15, testul golden byte cu byte, butonul de copiere și trimitere, legătura din detaliul lucrării către banii ei și curățarea ecranului Bani: lucrările din „De facturat” nu se mai repetă în lista de dedesubt, care devine „Restul lucrărilor”.
