# Spec tehnic — App „Lucrări" (pentru implementare în repo)

<aside>
🤖

**Destinatarul acestui document e un agent de cod / un dev**, nu utilizatorul final. Cercetarea și deciziile de produs sunt în pagina părinte. Aici e doar ce trebuie implementat, în ce ordine și cum se verifică. Regula generală: **dacă un detaliu nu e scris aici, se implementează varianta cea mai simplă care trece criteriile de acceptare din §10.**

</aside>

## 0. TL;DR

- **Ce construim:** aplicație Android nativă, un singur utilizator, **100% offline**, fără cont, fără cloud, instalată prin cablu. Interfață în română, textele generate pentru client/contabil în italiană.
- **Stack:** Kotlin + Jetpack Compose + Room. Build: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk` → `adb install -r`.
- **Regula de aur:** **nu se adaugă permisiunea `android.permission.INTERNET`.** Dacă o funcție are nevoie de rețea, nu intră în v1. Există un check automat pentru asta (§9).
- **Aplicația nu emite facturi.** Produce evidența și **textul** care ajunge pe factură. Zero XML, zero SdI, zero TVA, zero date fiscale.
- **Definition of done** pentru orice milestone: `./gradlew :app:testDebugUnitTest :app:assembleDebug` trece și criteriile de acceptare corespunzătoare din §10 sunt bifate.

## 1. Constrângeri de produs (non-negociabile)

1. **Offline total.** Toate scrierile merg local și instant. Nicio funcție nu depinde de semnal.
2. **Fără cont, fără parolă, fără onboarding.** La prima deschidere se intră direct în ecranul „Azi", cu date demo ștergăbile dintr-o apăsare.
3. **Maxim 2 apăsări** pentru orice acțiune zilnică (am lucrat azi aici / am de făcut ceva / am luat bani).
4. **Un singur utilizator, un singur dispozitiv.** Nicio sincronizare, niciun conflict de rezolvat.
5. **Datele nu se pierd:** backup automat zilnic local, cu rotație, plus export manual într-un fișier pe care îl poate trimite pe WhatsApp.
6. **Nimic legal în app:** fără TVA, bollo, XML, conservare, CF/P.IVA. Doar numărul facturii ca notă.
7. **Text liber peste tot.** Orice câmp în afară de titlul lucrării e opțional. Aplicația nu blochează niciodată salvarea pentru câmpuri lipsă.
8. **Sume în cenți, `Long`.** Niciodată `Double`/`Float` pentru bani. Cantitățile de măsurători sunt `Double`.

## 2. Stack, versiuni, cerințe de build

| Element | Valoare |
| --- | --- |
| Limbaj / UI | Kotlin 2.0.21 · Jetpack Compose (BOM 2024.10.01) · Material 3 |
| Build | AGP 8.7.x · Gradle wrapper 8.9 (comis în repo) · JDK 17 |
| SDK | compileSdk 35 · targetSdk 35 · **minSdk 26** (Android 8.0) |
| Persistență | Room 2.6.1 + KSP · `exportSchema = true` (schemele se comit în `app/schemas/`) |
| Navigare | navigation-compose 2.8.x, o singură `MainActivity` |
| Altele | kotlinx-serialization-json (backup) · coil-compose (poze locale) · WorkManager (memento-uri) |
| applicationId | `com.emanus.lucrari` (de confirmat) |
| Interzis | Firebase, analytics, crash reporting, orice SDK care cere rețea sau cont |

Comenzi pe care agentul local trebuie să le poată rula:

```bash
./gradlew :app:assembleDebug          # APK de instalat prin cablu
./gradlew :app:testDebugUnitTest      # teste de logică (bani, text factură, reguli)
./gradlew :app:connectedDebugAndroidTest   # doar dacă există emulator/telefon conectat
./gradlew :app:lintDebug
bash tools/check-no-internet.sh       # eșuează dacă a apărut permisiunea INTERNET
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Dependențele se declară exclusiv în `gradle/libs.versions.toml` (version catalog), nu inline în `build.gradle.kts`.

## 3. Structura repo

```
lucrari-app/
├─ AGENTS.md                  # instrucțiuni pentru agentul local: cum build-uiește și testează
├─ README.md                  # build + instalare prin cablu, în română
├─ SPEC.md                    # copia acestui spec (sursa de adevăr)
├─ .gitignore                 # /build, /.gradle, local.properties, *.apk, *.keystore
├─ settings.gradle.kts
├─ build.gradle.kts
├─ gradle/libs.versions.toml
├─ gradle/wrapper/{gradle-wrapper.jar,gradle-wrapper.properties}
├─ gradlew  gradlew.bat
├─ tools/check-no-internet.sh
└─ app/
   ├─ build.gradle.kts
   ├─ schemas/                # scheme Room exportate (comise)
   └─ src/
      ├─ main/AndroidManifest.xml
      ├─ main/res/            # xml/file_paths.xml (FileProvider), icoane, strings-ro
      ├─ main/java/com/emanus/lucrari/
      │  ├─ MainActivity.kt
      │  ├─ App.kt                     # DB singleton, provideri
      │  ├─ data/Entities.kt  data/Enums.kt  data/Daos.kt  data/AppDb.kt  data/Converters.kt
      │  ├─ data/repo/{JobRepo,MoneyRepo,BackupRepo}.kt
      │  ├─ domain/{Money,Descrizione,Dictionary,Templates,Rules,Seed}.kt
      │  ├─ ui/theme/{Color,Theme,Type}.kt
      │  ├─ ui/nav/AppNav.kt
      │  ├─ ui/screen/{Today,Jobs,JobDetail,Punch,Money,Invoice,Clients,Settings}Screen.kt
      │  ├─ ui/component/{BigButton,StatusChip,ProgressBar,MoneyStrip,BottomSheetForm}.kt
      │  └─ work/{ReminderWorker,BackupWorker}.kt
      ├─ test/java/…            # MoneyTest, DescrizioneTest, RulesTest
      └─ androidTest/java/…     # DbTest, MarioFlowTest
```

`AGENTS.md` din repo trebuie să conțină minim: comenzile din §2, regula „fără INTERNET", regula sumelor în cenți, și obligația de a rula testele înainte de commit.

## 4. Modelul de date (Room)

Id-uri: `String` (UUID generat în app, nu autoincrement — face backup/import idempotent). Datele calendaristice: `LocalDate` stocat ca `TEXT` ISO prin `TypeConverter`. Timestamp-urile: `Long` epoch millis.

```kotlin
@Entity(tableName = "clients")
data class Client(
  @PrimaryKey val id: String = uuid(),
  val name: String,                 // nume sau poreclă, singurul câmp obligatoriu
  val phone: String? = null,
  val note: String? = null,         // „cheia la vecin", „câine", „parcare grea"
  val createdAt: Long = now(),
)

@Entity(
  tableName = "jobs",
  foreignKeys = [ForeignKey(Client::class, ["id"], ["clientId"], onDelete = ForeignKey.CASCADE)],
  indices = [Index("clientId"), Index("status")],
)
data class Job(
  @PrimaryKey val id: String = uuid(),
  val clientId: String,
  val title: String,                // ex. „Rifacimento bagno"
  val street: String? = null,       // „Via 23" — el caută după stradă, nu după client
  val city: String? = null,
  val addrNote: String? = null,     // scară, apartament, interfon
  val type: String? = null,         // cheia șablonului din Templates
  val status: JobStatus = JobStatus.OFERTAT,
  val plannedStart: LocalDate? = null,
  val estDays: Int? = null,         // zile estimate
  val billing: Billing = Billing.CORP,
  val agreedPriceCents: Long? = null,
  val dayRateCents: Long? = null,   // folosit doar la Billing.ZILE
  val note: String? = null,
  val createdAt: Long = now(),
  val closedAt: Long? = null,
)

@Entity(tableName = "stages")   // etapele lucrării (voci di lavoro)
data class Stage(@PrimaryKey val id: String = uuid(), val jobId: String, val name: String,
                 val sort: Int, val done: Boolean = false, val doneAt: Long? = null)

@Entity(tableName = "work_days")
data class WorkDay(@PrimaryKey val id: String = uuid(), val jobId: String, val date: LocalDate,
                   val hours: Double? = null, val what: String? = null,
                   val isExtra: Boolean = false,      // muncă în afara ofertei
                   val blocked: String? = null)       // ce a blocat treaba azi

@Entity(tableName = "todos")     // „rest de făcut" / punch list
data class Todo(@PrimaryKey val id: String = uuid(), val jobId: String, val place: String? = null,
                val what: String, val reason: Reason? = null, val due: LocalDate? = null,
                val done: Boolean = false, val doneAt: Long? = null)

@Entity(tableName = "materials")
data class Material(@PrimaryKey val id: String = uuid(), val jobId: String, val what: String,
                    val qty: String? = null, val shop: String? = null, val bought: Boolean = false)

@Entity(tableName = "measures")  // libretul de măsuri
data class Measure(@PrimaryKey val id: String = uuid(), val jobId: String, val place: String,
                   val work: String? = null, val qty: Double, val unit: Unit,
                   val unitPriceCents: Long? = null, val date: LocalDate = today())

@Entity(tableName = "extras")
data class Extra(@PrimaryKey val id: String = uuid(), val jobId: String, val what: String,
                 val date: LocalDate = today(), val priceCents: Long = 0,
                 val accepted: Boolean = false, val proof: String? = null,  // „vocală WhatsApp 12/08"
                 val billable: Boolean = true)

@Entity(tableName = "payments")  // încasări reale
data class Payment(@PrimaryKey val id: String = uuid(), val jobId: String, val date: LocalDate,
                   val amountCents: Long, val method: Method = Method.CASH, val note: String? = null)

@Entity(tableName = "invoices")  // DOAR evidență, nu emitere
data class InvoiceRef(@PrimaryKey val id: String = uuid(), val jobId: String, val number: String? = null,
                      val date: LocalDate? = null, val amountCents: Long = 0,
                      val kind: InvoiceKind = InvoiceKind.SALDO,
                      val due: LocalDate? = null, val paid: Boolean = false)

@Entity(tableName = "photos")
data class Photo(@PrimaryKey val id: String = uuid(), val jobId: String, val dayId: String? = null,
                 val todoId: String? = null, val path: String, val phase: Phase = Phase.DURING,
                 val takenAt: Long = now())

@Entity(tableName = "reminders")
data class Reminder(@PrimaryKey val id: String = uuid(), val jobId: String? = null,
                    val clientId: String? = null, val text: String, val dueAt: Long,
                    val auto: Boolean = false, val done: Boolean = false)
```

```kotlin
enum class JobStatus { OFERTAT, PROGRAMAT, IN_LUCRU, ASTEPTARE, DE_FINISAT, TERMINAT, ANULAT }
enum class Billing   { CORP, MASURA, ZILE }
enum class Reason    { MATERIAL, DECIZIE_CLIENT, ALT_MESERIAS, VREMEA, LIPSA_TIMP, ALTUL }
enum class Unit      { M2, ML, BUC, ORE, ZILE }
enum class Method    { CASH, BONIFICO, ALTUL }
enum class InvoiceKind { ACONTO, SALDO, UNICA }
enum class Phase     { BEFORE, DURING, AFTER }
```

DAO-uri: fiecare entitate are `upsert`, `delete`, `observeByJob(jobId): Flow<List<T>>`. În plus, obligatoriu:

- `JobDao.observeBoard(): Flow<List<JobWithTotals>>` — lucrările + zile lucrate + sume, printr-un `@Transaction` cu relații, nu N+1 interogări.
- `TodoDao.observeOpenAll(): Flow<List<TodoWithJob>>` — pentru ecranul global „Rest de făcut".
- `JobDao.searchByStreetOrClient(q: String)` — căutare `LIKE` pe titlu, stradă și nume client.

## 5. Regulile de business (aici se pierd banii — implementați exact)

### 5.1 Două axe independente

Statusul lucrării (`JobStatus`) și starea banilor **nu se amestecă**. O lucrare poate fi `TERMINAT` și neîncasată. Starea banilor se **calculează**, nu se stochează: `OFERTAT → ACCEPTAT → ACONT_INCASAT → DE_FACTURAT → FACTURAT → INCASAT_PARTIAL → INCASAT`.

### 5.2 Calculul banilor

```kotlin
object Money {
  fun base(job: Job, workedDays: Int, measures: List<Measure>): Long = when (job.billing) {
    Billing.CORP   -> job.agreedPriceCents ?: 0L
    Billing.ZILE   -> (job.dayRateCents ?: 0L) * workedDays
    Billing.MASURA -> measures.sumOf { Math.round(it.qty * (it.unitPriceCents ?: 0L)) }
  }
  fun extras(list: List<Extra>): Long   = list.filter { it.billable }.sumOf { it.priceCents }
  fun total(base: Long, extras: Long)   = base + extras
  fun invoiced(inv: List<InvoiceRef>)   = inv.sumOf { it.amountCents }
  fun collected(pay: List<Payment>)     = pay.sumOf { it.amountCents }

  fun toInvoice(total: Long, invoiced: Long)  = total - invoiced      // „De facturat"
  fun outstanding(total: Long, collected: Long) = total - collected   // „Rest de încasat"
}
```

<aside>
⚠️

**„Facturat" și „încasat" sunt două cifre diferite și nu se deduc una din alta.** Motiv practic: la plata prin *bonifico parlante* pentru bonusuri edilizi, banca reține 11%, deci în cont intră mai puțin decât scrie pe factură. Nu implementați nicio regulă care presupune `collected == invoiced`.

</aside>

### 5.3 Ce intră automat în lista „De facturat"

O lucrare apare în lista „De facturat" dacă `toInvoice > 0` și cel puțin una din condiții:

1. există un `Payment` care nu e acoperit de `InvoiceRef` (acont încasat, nefacturat);
2. `status == TERMINAT`;
3. utilizatorul a bifat manual „gata de facturat" pe lucrare.

Elementul din listă arată: client, adresă, titlu, perioada, sumă, tipul propus (`ACONTO` dacă lucrarea nu e terminată, altfel `SALDO`) și butonul **„Copiază pentru contabil"** care pune în clipboard textul din §5.5.

### 5.4 Memento-uri automate (`ReminderWorker`)

Worker periodic zilnic (WorkManager, ora 19:00, fără constrângere de rețea) care creează `Reminder(auto = true)` pentru:

- lucrare `TERMINAT` de 3+ zile cu `toInvoice > 0` → „De trimis la facturat: {titlu}";
- factură cu `due < azi` și `paid == false` → „Neîncasat de {n} zile: {titlu}";
- ofertă (`status == OFERTAT`) fără răspuns la 3, 7 și 14 zile de la creare → „Sună în legătură cu oferta";
- `Todo` cu `due == azi`.

Dedup: maxim un memento automat per (jobId, tip) nefinalizat.

### 5.5 Generatorul „Descriere pentru factură" (nucleul aplicației)

```kotlin
fun descrizione(
  job: Job, client: Client, days: List<WorkDay>, stages: List<Stage>,
  measures: List<Measure>, extras: List<Extra>, invoices: List<InvoiceRef>,
): String
```

Reguli, în ordine:

1. Linia 1: `"{title} — {client.name}, {street}, {city}"` (părțile lipsă se omit, fără virgule duble).
2. Dacă există zile lucrate: `Periodo: dd/MM – dd/MM/yyyy (N giornate)`, cu min/max din `days.date` și `N = days.size`. Cu o singură zi: `Data: dd/MM/yyyy (1 giornata)`.
3. `Lavorazioni eseguite:` și câte un rând `- {traducere(stage.name)}` pentru fiecare etapă cu `done == true`, în ordinea `sort`. Dacă nicio etapă nu e bifată, se folosesc în schimb textele din `WorkDay.what` (distincte, în ordine cronologică).
4. Dacă există măsurători: `Misure:` și `- {place}: {qty} {unit}`. Cantitatea cu virgulă zecimală, maxim 2 zecimale, fără zerouri inutile (`12,4` → `12,40` doar dacă există zecimale reale; folosiți `NumberFormat` cu `Locale.ITALY`).
5. Dacă `billing == ZILE`: `Manodopera: N giornate × {tarif}`.
6. Dacă există extra cu `billable == true`: `Extra concordati:` și `- {what} — {preț}`.
7. Rând gol, apoi blocul de sume:
    - `Concordato: {base}` și, dacă există extra,  `+ extra {extra} = {total}`
    - `Acconti già fatturati: {invoiced}` (doar dacă > 0)
    - `Da fatturare: {total - invoiced}`
8. Bani formatați `Locale.ITALY`, 2 zecimale, sufix  `€`: `2.400,00 €`.
9. **Textul nu conține niciodată** TVA, cote, date fiscale, număr de factură sau suma încasată în numerar. „Rest de încasat" se vede doar în aplicație.

**Test golden (obligatoriu, `DescrizioneTest`)** — pentru lucrarea demo, funcția trebuie să returneze exact:

```
Rifacimento bagno — Mario, Via 23, Milano
Periodo: 10/08 – 12/08/2026 (2 giornate)
Lavorazioni eseguite:
- demolizione
- tracce impianti
Misure:
- Bagno — pavimento: 12,40 m²
Extra concordati:
- nicchia doccia + spostamento presa — 180,00 €

Concordato: 2.400,00 € + extra 180,00 € = 2.580,00 €
Acconti già fatturati: 800,00 €
Da fatturare: 1.780,00 €
```

### 5.6 Rest de făcut și blocaje

- Un `Todo` se adaugă din orice ecran de lucrare în maxim 2 apăsări și are nevoie doar de `what`.
- Dacă o lucrare are `Todo` deschise și toate etapele bifate, aplicația propune trecerea la `DE_FINISAT`, nu la `TERMINAT`.
- Trecerea la `TERMINAT` cu `Todo` deschise cere confirmare („mai ai 3 lucruri nefăcute — sigur?").
- `WorkDay.blocked` completat pune automat lucrarea în `ASTEPTARE` (cu posibilitate de anulare imediată).

### 5.7 Estimat vs. real

Pe fiecare lucrare: `estDays` vs `days.size`. În ecranul de statistici, media abaterii pe `type` de lucrare. Nicio altă raportare în v1.

## 6. Ecrane și navigare

Bottom bar cu 5 destinații: **Azi · Lucrări · Rest · Bani · Mai mult**.

| Rută | Conținut | Acțiuni principale |
| --- | --- | --- |
| `today` | Data de azi, lucrările active (`IN_LUCRU`, `DE_FINISAT`, `ASTEPTARE`), memento-urile de azi | **Am lucrat azi aici** (1 tap pe lucrare + text opțional), Adaugă rest, Fotografiază, Lucrare nouă |
| `jobs` | Listă filtrabilă pe status, cu culori, bară de avansare și „rest de încasat" | Căutare după client **și stradă**, filtre rapide, lucrare nouă |
| `job/{id}` | Header (client, adresă, status, avansare, bandă de bani: convenit · facturat · încasat · rest) + taburi: Etape · Zile · Rest · Măsuri · Materiale · Poze · Bani | Buton hartă, buton apel, schimbare status, editare, ștergere |
| `punch` | Toate `Todo` deschise din toate lucrările, grupate pe client/stradă, sortate pe `due` | Bifă, reprogramare, salt la lucrare |
| `money` | Trei cifre mari: *De încasat total* · *Restanțe peste 30 zile* · *Încasat luna asta*; apoi lista pe lucrări și facturile neplătite | Adaugă încasare, marchează factură plătită |
| `invoice` | Lista „De facturat" (§5.3) și previzualizarea textului generat | **Copiază pentru contabil**, Trimite (share sheet), marchează „facturat" cu nr. și sumă |
| `clients` | Clienți + lucrările fiecăruia, istoric | Client nou, apel, notă |
| `settings` | Backup/export/import, șabloane de etape, șterge datele demo, versiune | Export fișier, Import, Reset |

Toate formularele sunt bottom sheet-uri (`BottomSheetForm`) cu același comportament: câmpuri mari, „Salvează" activ mereu, `Cancel` la swipe.

## 7. Reguli de UX (se verifică la review)

- Ținte minime **56 dp**; distanță între ținte 8 dp.
- Formular „Lucrare nouă" = **4 câmpuri**: client, stradă, ce lucrare (șablon), câte zile. Restul se completează mai târziu.
- Alegerea șablonului precompletează etapele din `Templates` (§14) — editabile.
- Durata se exprimă în **zile**, nu ore. Orele sunt opționale și ascunse implicit.
- Data se completează automat cu „azi", editabilă cu un tap.
- Toate câmpurile de text acceptă dictare (tastatura Android o oferă; nu se implementează nimic custom).
- Culorile statusurilor sunt cele din §5.1 și se folosesc consecvent în liste, chip-uri și calendar.
- Zero dialoguri de tip „ești sigur?" în afară de: ștergere lucrare, `TERMINAT` cu resturi deschise, import backup.
- Text în română în `strings.xml`. Textele generate pentru client/contabil rămân în italiană (§5.5), niciodată traduse.

## 8. Backup, export, share

- **Automat:** `BackupWorker` zilnic scrie `filesDir/backup/lucrari-YYYY-MM-DD.zip`, păstrând ultimele **7** (șterge restul).
- **Manual:** export prin `ACTION_CREATE_DOCUMENT` (SAF) — utilizatorul alege unde salvează; import prin `ACTION_OPEN_DOCUMENT`. **Zero permisiuni de storage.**
- **Structura arhivei:**

```
lucrari-2026-08-17.zip
├─ data.json          # { schemaVersion, exportedAt, clients, jobs, stages, workDays, todos,
│                    #   materials, measures, extras, payments, invoices, photos, reminders }
└─ photos/<photoId>.jpg
```

- Import: două opțiuni explicite — **Înlocuiește tot** sau **Adaugă ce lipsește** (dedup pe `id`). Niciodată merge automat.
- `schemaVersion` incompatibil → mesaj clar, fără crash și fără pierdere de date existente.
- Share text (descrierea pentru factură, rezumat lucrare) prin `Intent.ACTION_SEND`, `text/plain`. Share fișiere prin `FileProvider` (`xml/file_paths.xml`).
- Poze: `ACTION_IMAGE_CAPTURE` + `FileProvider`, salvate în `filesDir/photos/<uuid>.jpg`, redimensionate la max 1600 px pe latura mare, calitate 80.

## 9. Permisiuni și privacy

- Permise: `CAMERA`, `POST_NOTIFICATIONS` (Android 13+).
- **Interzise:** `INTERNET`, `ACCESS_NETWORK_STATE`, orice permisiune de locație, contacte sau storage.
- `tools/check-no-internet.sh` inspectează manifestul fuzionat (`app/build/intermediates/merged_manifests/**/AndroidManifest.xml`) și întoarce cod de eroare dacă apare `android.permission.INTERNET`. Se rulează după `assembleDebug` și în orice CI.
- Datele clientului în app: nume/poreclă, stradă, opțional telefon și notă. **Fără** CF, P.IVA, adresă de facturare, e-mail.

## 10. Criterii de acceptare

**Logică (teste unitare, obligatorii):**

1. `Money.base` întoarce corect pentru toate cele 3 moduri de facturare, inclusiv liste goale.
2. `MASURA`: `2 m² × 3.333 cenți` se rotunjește la cel mai apropiat cent, fără erori de virgulă flotantă.
3. `outstanding` și `toInvoice` sunt independente: cu `total = 2580`, `invoiced = 800`, `collected = 0` → `toInvoice = 1780`, `outstanding = 2580`.
4. Extra cu `billable = false` nu intră în niciun total.
5. `descrizione` întoarce **exact** textul golden din §5.5.
6. Reguli §5.3: lucrare cu acont încasat și zero facturi apare în „De facturat" chiar dacă nu e terminată, cu `kind = ACONTO`.
7. `ReminderWorker` nu creează duplicate la rulări repetate în aceeași zi.

**Flux end-to-end (`MarioFlowTest`, scenariul de referință):**

> Client nou „Mario", stradă „Via 23, Milano" → lucrare „Rifacimento bagno", șablon *Baie completă*, 3 zile estimate, la corp, 2.400 € → bifez „Demolare" și „Trasee instalații" → două zile lucrate (10 și 12 august) → măsurătoare „Bagno — pavimento 12,4 m²" → extra 180 € acceptat → încasare 800 € → factură de acont 800 € înregistrată → ecranul „De facturat" arată **1.780 €**, ecranul „Bani" arată **rest de încasat 1.780 €**, iar butonul „Copiază pentru contabil" pune în clipboard textul golden.
> 

**Manual (checklist înainte de livrare):**

- Mod avion pornit: absolut toate ecranele funcționează, inclusiv export.
- Rotația ecranului și revenirea din background nu pierd nimic din formularele deschise.
- Kill și redeschidere: datele sunt acolo.
- Export → wipe → import: starea revine identic, inclusiv pozele.
- Un ciclu complet „am lucrat azi aici" se face în **2 apăsări**.

## 11. Plan de lucru (un PR per milestone)

| # | Milestone | Definition of done |
| --- | --- | --- |
| M0 | Schelet: Gradle, version catalog, temă M3, `MainActivity`, bottom nav cu 5 ecrane goale, `tools/check-no-internet.sh` | `assembleDebug` produce APK instalabil; scriptul de check trece |
| M1 | Room complet (§4) + `Seed` cu datele demo + Clienți și Lucrări (listă, creare, detaliu, status) | `DbTest` verde; lucrare nouă în 4 câmpuri; căutare după stradă |
| M2 | Etape cu șabloane + Zile lucrate („Am lucrat azi aici") + bară de avansare + estimat vs. real | Criteriul „2 apăsări"; ziua se leagă mereu de o lucrare |
| M3 | Rest de făcut (per lucrare + ecran global) cu motive și termene; Materiale; reguli §5.6 | `ASTEPTARE` automat la blocaj; confirmările cerute |
| M4 | Măsurători și Extra, cu dovada acceptării | Teste 1–4 din §10 verzi |
| M5 | Bani: încasări, evidență facturi, ecranul „Bani", lista „De facturat" | Testele 3 și 6; cele trei cifre mari corecte |
| M6 | „Descriere pentru factură" + copiere/share + dicționar RO→IT | Testul golden trece byte cu byte |
| M7 | Poze, backup automat + export/import SAF, memento-uri (`ReminderWorker`) | Ciclul export→wipe→import; fără duplicate de memento |
| M8 | Finisaje: calendar lunar, statistici simple, widget „Am lucrat azi la…" | Opțional; nu blochează livrarea |

Ordinea e obligatorie: M0→M6 formează aplicația utilă. M7 înainte de a i-o da în mână (fără backup nu se livrează).

## 12. Convenții

- Commit-uri Conventional Commits: `feat(money): rest de incasat pe lucrare`. Un PR per milestone, cu descrierea criteriilor bifate.
- Cod și identificatori în engleză; texte UI în română, exclusiv în `strings.xml`; textele generate în italiană, exclusiv în `Descrizione.kt` și `Dictionary.kt`.
- Compose: state hoisting, `ViewModel` + `StateFlow`, zero logică de business în composable-uri. Logica de bani și de text stă în `domain/`, pură și testabilă fără Android.
- Fără `!!`, fără `GlobalScope`, fără operații de DB pe main thread.
- Nicio dependență nouă fără justificare în PR. Niciun apel de rețea, niciodată.

## 13. Ce NU intră în aplicație

Emiterea facturii electronice (XML, SdI), TVA și cote, marca da bollo, conservare, reverse charge, rețineri, date fiscale ale clientului, contracte, documente de siguranță, cont și sincronizare cloud, multi-utilizator, multi-dispozitiv, Play Store, analytics.

## 14. Seed: șabloanele de etape (`Templates.kt`)

```json
{
  "Baie completă": ["Demolare","Trasee instalații","Impermeabilizare","Gresie / faianță","Sanitare","Silicon / finisaje","Curățenie"],
  "Tencuială": ["Pregătire pereți","Colțare / plase","Tencuială","Glet","Șlefuit"],
  "Gresie / pavaj": ["Pregătire suport","Trasare","Montaj","Chituit","Plinte"],
  "Rigips": ["Structură","Placare","Bandă / masă","Șlefuit"],
  "Zugrăveală": ["Protejare","Amorsă","Strat 1","Strat 2","Retușuri"],
  "Șapă": ["Curățare suport","Nivele","Turnare","Uscare / verificare"],
  "Termosistem": ["Schelă","Lipit plăci","Dibluit","Plasă / masă","Amorsă","Decorativ"],
  "Gard / zid": ["Trasare","Fundație","Zidărie","Rostuit / finisaj"],
  "Mansardă": ["Structură","Izolație","Barieră vapori","Placare","Finisaje"],
  "Reparații diverse": ["De văzut","De reparat","Verificare finală"]
}
```

## 15. Dicționar RO → IT pentru textul de factură (`Dictionary.kt`)

Dacă o etapă nu e în dicționar, textul se folosește **exact cum l-a scris el**, nu se traduce automat.

```json
{
  "Demolare":"demolizione", "Trasee instalații":"tracce impianti",
  "Impermeabilizare":"impermeabilizzazione", "Gresie / faianță":"posa gres e rivestimento",
  "Sanitare":"posa sanitari", "Silicon / finisaje":"silicone e finiture",
  "Curățenie":"pulizia finale", "Pregătire pereți":"preparazione pareti",
  "Colțare / plase":"paraspigoli e rete", "Tencuială":"intonaco", "Glet":"rasatura",
  "Șlefuit":"carteggiatura", "Pregătire suport":"preparazione sottofondo",
  "Trasare":"tracciamento", "Montaj":"posa", "Chituit":"stuccatura", "Plinte":"battiscopa",
  "Structură":"struttura metallica", "Placare":"lastratura", "Bandă / masă":"nastro e rasatura",
  "Protejare":"protezione superfici", "Amorsă":"primer", "Strat 1":"prima mano",
  "Strat 2":"seconda mano", "Retușuri":"ritocchi", "Curățare suport":"pulizia sottofondo",
  "Nivele":"quote e livelli", "Turnare":"getto massetto", "Uscare / verificare":"asciugatura e verifica",
  "Schelă":"ponteggio", "Lipit plăci":"incollaggio pannelli", "Dibluit":"tassellatura",
  "Plasă / masă":"rete e rasante", "Decorativ":"finitura decorativa",
  "Fundație":"fondazione", "Zidărie":"muratura", "Rostuit / finisaj":"stilatura giunti",
  "Izolație":"isolamento", "Barieră vapori":"barriera vapore", "Finisaje":"finiture",
  "De văzut":"sopralluogo", "De reparat":"riparazione", "Verificare finală":"verifica finale"
}
```

## 16. Instalarea pe telefonul lui (prin cablu)

```bash
./gradlew :app:assembleDebug
adb devices                 # telefonul trebuie să apară; „USB debugging" activat
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

<aside>
🔑

**Păstrează keystore-ul de semnare.** APK-ul de debug e semnat cu `~/.android/debug.keystore`. Dacă acel fișier se pierde sau se schimbă mașina de build, update-ul peste aplicația instalată eșuează (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`) și ar trebui dezinstalare — adică pierderea datelor lui. Fă o copie a keystore-ului în afara repo-ului și **nu îl comite niciodată**.

</aside>

## 17. Ce îmi trebuie ca să pot lucra direct în repo

1. `owner/repo` și dacă e privat (recomandat: privat).
2. Token cu drept de scriere pe acel repo (fine-grained: **Contents: read & write**, **Pull requests: read & write**, **Metadata: read**).
3. Branch-ul de bază (`main`) și dacă vrei PR-uri sau commit direct pe `main`.
4. Confirmarea `applicationId` (`com.emanus.lucrari`) și a numelui afișat al aplicației.
5. Versiunea de Android a telefonului lui (ca să fixez `minSdk` — acum e 26 / Android 8).