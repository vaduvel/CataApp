# CataApp — „Lucrări”

Aplicație Android nativă, un singur utilizator, **100% offline**, pentru gestionarea lucrărilor de construcții: lucrări, zile lucrate, rest de făcut, măsurători, extra, bani și textul care ajunge pe factură.

Specificația de implementare (sursa de adevăr): `SPEC.md`. Reguli de lucru pentru agenți: `AGENTS.md`.

## Reguli de aur

1. **Fără rețea.** Aplicația nu cere niciodată permisiuni de rețea. `tools/check-no-internet.sh` sparge build-ul dacă apare vreuna.
2. **Sumele se țin în cenți** (`Long`). Niciodată `Double`/`Float` pentru bani.
3. **Aplicația nu emite facturi.** Produce evidența și *textul* care ajunge pe factură. Zero XML, zero SdI, zero TVA.
4. Interfața e în **română**. Textele generate pentru client/contabil rămân în **italiană**.
5. Fără cont, fără cloud, fără sincronizare. Instalare prin cablu.

## Cerințe

- JDK 17 sau 21 (bytecode-ul produs rămâne 17 în ambele cazuri)
- Android SDK 35 (`compileSdk` / `targetSdk` 35, `minSdk` 26 = Android 8.0)

## Build și instalare prin cablu

Wrapper-ul Gradle 8.9 e comis, deci nu ai nevoie de Gradle instalat separat:

```bash
./gradlew :app:assembleDebug
adb devices                 # telefonul trebuie să apară, cu „USB debugging” activat
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Verificări:

```bash
./gradlew :app:testDebugUnitTest      # logica de bani, textul de factură, regulile
./gradlew :app:connectedDebugAndroidTest   # DbTest, cu telefon sau emulator conectat
bash tools/check-no-internet.sh       # eșuează dacă a apărut o permisiune de rețea
```

După primul build cu Room, comite și folderul generat `app/schemas/`.

Păstrează `~/.android/debug.keystore` într-un loc sigur, în afara repo-ului. Dacă se pierde, aplicația instalată nu mai poate fi actualizată fără dezinstalare, iar dezinstalarea șterge datele.

## Stadiu

- [x] M0 — schelet: Gradle, temă, navigare cu 5 ecrane, script de verificare
- [x] M1 — bază de date + clienți și lucrări
- [x] M2 — etape, șabloane, zile lucrate
- [ ] M3 — rest de făcut, materiale, blocaje
- [ ] M4 — măsurători și extra
- [ ] M5 — bani: încasări, evidență facturi
- [ ] M6 — „Descriere pentru factură” + copiere
- [ ] M7 — poze, backup, memento-uri (obligatoriu înainte de livrare)
- [ ] M8 — finisaje
