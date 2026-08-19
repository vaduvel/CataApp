# CataApp — „Lucrări”

Aplicație Android nativă, un singur utilizator, **100% offline**, pentru gestionarea lucrărilor de construcții: lucrări, zile lucrate, rest de făcut, măsurători, extra, bani, textul pentru factură, poze, backup și memento-uri.

Specificația de implementare (sursa de adevăr): `SPEC.md`. Reguli de lucru pentru agenți: `AGENTS.md`.

## Reguli de aur

1. **Fără rețea.** Aplicația nu cere niciodată permisiuni de rețea. `tools/check-no-internet.sh` sparge build-ul dacă apare vreuna.
2. **Sumele se țin în cenți** (`Long`). Niciodată `Double`/`Float` pentru bani.
3. **Aplicația nu emite facturi.** Produce evidența și *textul* care ajunge pe factură. Zero XML, zero SdI, zero TVA.
4. **Facturat și încasat sunt două cifre separate.** Una spune ce a cerut pe hârtie, cealaltă ce a intrat în mână.
5. Interfața e în **română**. Textele generate pentru client/contabil rămân în **italiană**.
6. Fără cont, fără cloud, fără sincronizare. Instalare prin cablu.
7. Backup/export/import prin ZIP și selectorul Android; fotografiile rămân în spațiul privat al aplicației. Zero permisiuni de storage.

## Cerințe

- JDK 17 sau 21 (bytecode-ul produs rămâne 17 în ambele cazuri)
- Android SDK 35 (`compileSdk` / `targetSdk` 35, `minSdk` 26 = Android 8.0)

## Build și instalare prin cablu

Wrapper-ul Gradle 8.9 e comis, deci nu ai nevoie de Gradle instalat separat:

```bash
./gradlew :app:assembleDebug
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Verificări:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
./gradlew :app:lintDebug
./gradlew :app:connectedDebugAndroidTest   # cu telefon sau emulator conectat
bash tools/check-no-internet.sh
```

Schemele Room sunt comise în `app/schemas/`. M7 a adăugat numai DAO-uri/interogări peste entitățile existente; schema v1 a rămas byte-identică.

Păstrează `~/.android/debug.keystore` într-un loc sigur, în afara repo-ului. Dacă se pierde, aplicația instalată nu mai poate fi actualizată fără dezinstalare, iar dezinstalarea șterge datele.

## Unde stau datele

Totul rămâne pe telefon:

- baza de date Room, în spațiul privat al aplicației;
- pozele, în `files/photos/<uuid>.jpg`;
- backup-urile automate, în `files/backup/lucrari-YYYY-MM-DD.zip`, ultimele 7;
- memento-urile, în baza de date, programate local cu WorkManager.

Dezinstalarea sau „Șterge datele aplicației” șterge și backup-urile automate. De aceea exportul manual, salvat în afara aplicației, rămâne singura plasă de siguranță reală.

## M7 — verificat pe emulator

Verificat pe API 36, cu modul avion activ pe tot parcursul:

- 61 teste unitare și 7 instrumentate verzi; `assembleDebug` și `lintDebug` OK;
- `tools/check-no-internet.sh` OK pe 3 manifeste fuzionate;
- poza făcută prin aplicația cameră și `FileProvider`, legată de lucrare și de un rest, vizibilă după repornirea aplicației;
- export prin `ACTION_CREATE_DOCUMENT`, ștergerea datelor, apoi import **Înlocuiește tot**: starea a revenit identic, inclusiv pozele;
- import **Adaugă ce lipsește** rulat de două ori, fără duplicate;
- partajarea ultimei arhive prin `FileProvider`;
- schema v1 neschimbată.

De reverificat la orice modificare viitoare: rotația la 7 arhive, notificările pe Android 13+ și faptul că worker-ul de la 19:00 nu dublează un memento automat încă deschis pentru aceeași lucrare și regulă.

## Stadiu

- [x] M0 — schelet: Gradle, temă, navigare cu 5 ecrane, script de verificare
- [x] M1 — bază de date + clienți și lucrări
- [x] M2 — etape, șabloane, zile lucrate
- [x] M3 — rest de făcut, materiale, blocaje
- [x] M4 — măsurători și extra
- [x] M5 — bani: încasări, evidență facturi
- [x] M6 — „Descriere pentru factură” + copiere
- [x] M7 — poze, backup automat/export/import SAF și memento-uri
- [ ] M8 — finisaje opționale

Aplicația este gata de instalat pe telefon. M8 nu blochează livrarea.
