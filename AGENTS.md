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
- DAO-urile și interogările adăugate în M7 nu schimbă schema: `app/schemas/com.emanus.lucrari.data.AppDb/1.json` trebuie să rămână neschimbat.
- Enum-ul `Unit` din spec se numește `MeasureUnit` ca să nu umbrească `kotlin.Unit`.
- Importul verifică `schemaVersion` înainte să șteargă ceva.
- Modul „Adaugă ce lipsește” folosește `INSERT IGNORE` pe UUID și trebuie să fie idempotent.
- Arhiva este ZIP cu `data.json` și `photos/<photoId>.jpg`; backup-urile automate sunt `files/backup/lucrari-YYYY-MM-DD.zip`, ultimele 7.
- Export/import exclusiv prin SAF. Zero permisiuni de storage. Partajarea și camera folosesc exclusiv `FileProvider`.

## Poze și notificări

- Camera externă scrie în `files/photos/<uuid>.jpg` prin `ActivityResultContracts.TakePicture`/`FileProvider`; nu cere permisiune CAMERA sau storage.
- Imaginea salvată are latura maximă 1600 px și JPEG quality 80.
- `ReminderWorker` rulează periodic cu țintă 19:00 și fără constrângere de rețea.
- Dedup: maxim un memento automat deschis per `(jobId, ReminderKind)`.
- `BackupWorker` rulează zilnic și rotește arhivele.
- Pe Android 13+ singura permisiune nouă este `POST_NOTIFICATIONS`; dacă e refuzată, memento-ul rămâne în aplicație.

## Interfața

- Bifa se schimbă numai din butonul de bifă; atingerea textului nu bifează/debifează.
- Butoanele principale au minimum 56 dp; `IconButton` asigură ținta de 48 dp.
- Ștergerea stă la marginea opusă bifei sau în foaia de editare.
- Textul pentru contabil se compune din datele curente și nu se salvează separat.
- Cele cinci destinații din bara de jos sunt fixe: Azi, Lucrări, Rest, Bani, Mai mult. Calendarul și memento-urile nu intră în bara de jos.
- Cele 7 statusuri se afișează cu etichetă text și pictogramă, nu numai prin culoare.

## Definition of done

Pentru orice milestone: unit tests + assemble + lint + scriptul offline trec; cu emulator/telefon rulează și testele instrumentate. Commit-uri Conventional Commits.

## Stadiu

- **M0–M5 — gata și verificate.**
- **M6 — gata și verificat:** descriere în italiană, copiere și trimitere, navigare, dedublarea ecranului Bani.
- **M7 — gata și verificat complet pe HEAD `4550092`:**
  - poze locale legate de lucrare și opțional de un rest, prin `FileProvider`;
  - backup ZIP zilnic, rotație 7, export/import SAF cu Înlocuiește tot / Adaugă ce lipsește și partajare;
  - `ReminderWorker` la 19:00, notificări și deduplicare;
  - **61 teste unitare** și **7 instrumentate** verzi; `assembleDebug` și `lintDebug` OK;
  - scriptul offline OK pe 3 manifeste fuzionate, după eliminarea `ACCESS_NETWORK_STATE` adusă de WorkManager;
  - schema Room v1 neschimbată față de `eaafada`;
  - scenariul export → ștergerea datelor → import verificat pe emulator API 36, cu modul avion activ.
- **Următorul pas:** instalarea pe telefonul destinatarului.
- **M8 — opțional:** finisaje vizuale pe baza noului design system, calendar lunar, statistici simple, widget, „spațiu folosit” în ecranul Mai mult și eventual câmpul `work` în rândul măsurătorii din descriere.
