# CataApp — „Lucrări"

Aplicație Android nativă, un singur utilizator, **100% offline**, pentru gestionarea lucrărilor de construcții: lucrări, zile lucrate, rest de făcut, măsurători, extra, bani și textul care ajunge pe factură.

Specificația de implementare (sursa de adevăr): `SPEC.md`.

## Reguli de aur

1. **Fără rețea.** Nu se adaugă niciodată `android.permission.INTERNET`. `tools/check-no-internet.sh` sparge build-ul dacă apare.
2. **Sumele se țin în cenți** (`Long`). Niciodată `Double`/`Float` pentru bani.
3. **Aplicația nu emite facturi.** Produce evidența și *textul* care ajunge pe factură. Zero XML, zero SdI, zero TVA.
4. Interfața e în **română**. Textele generate pentru client/contabil rămân în **italiană**.
5. Fără cont, fără cloud, fără sincronizare. Instalare prin cablu.

## Cerințe

- JDK 17
- Android SDK 35 (`compileSdk` / `targetSdk` 35, `minSdk` 26 = Android 8.0)

## Build și instalare prin cablu

```bash
./gradlew :app:assembleDebug
adb devices                 # telefonul trebuie să apară, cu „USB debugging" activat
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Verificări:

```bash
./gradlew :app:testDebugUnitTest      # logica de bani, textul de factură, regulile
bash tools/check-no-internet.sh       # eșuează dacă a apărut permisiunea INTERNET
```

> Wrapper-ul Gradle (`gradlew`, `gradle/wrapper/gradle-wrapper.jar`) este fișier binar și nu poate fi comis prin API. Se generează local o singură dată:
> `gradle wrapper --gradle-version 8.9`

## Stadiu

- [ ] M0 — schelet: Gradle, temă, navigare cu 5 ecrane, script de verificare
- [ ] M1 — bază de date + clienți și lucrări
- [ ] M2 — etape, șabloane, zile lucrate
- [ ] M3 — rest de făcut, materiale, blocaje
- [ ] M4 — măsurători și extra
- [ ] M5 — bani: încasări, evidență facturi
- [ ] M6 — „Descriere pentru factură" + copiere
- [ ] M7 — poze, backup, memento-uri (obligatoriu înainte de livrare)
- [ ] M8 — finisaje
