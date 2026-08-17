# AGENTS.md — instrucțiuni pentru agentul care lucrează în acest repo

Sursa de adevăr pentru **ce** se construiește este [`SPEC.md`](SPEC.md). Citește-l înainte de orice modificare. Acest fișier spune **cum** se lucrează.

## Comenzi

```bash
./gradlew :app:assembleDebug          # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest      # logica pură (bani, text factură, reguli)
./gradlew :app:lintDebug
bash tools/check-no-internet.sh       # obligatoriu după build
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Cerințe: **JDK 17**, **Android SDK 35**.

Wrapper-ul Gradle nu e comis (jarul e binar). O singură dată, la început:

```bash
gradle wrapper --gradle-version 8.9
```

## Reguli care nu se încalcă niciodată

1. **Fără rețea.** Nu adăuga `android.permission.INTERNET` sau altă permisiune de rețea. Dacă o funcție are nevoie de internet, nu intră în aplicație. Verifică cu `tools/check-no-internet.sh`.
2. **Banii se țin în cenți**, ca `Long`. Niciodată `Double`/`Float` pentru sume. Cantitățile de măsurători sunt `Double`.
3. **Aplicația nu emite facturi.** Produce evidența și *textul* care ajunge pe factură. Zero XML, zero SdI, zero TVA, zero date fiscale ale clientului.
4. **„Facturat" și „încasat" sunt două cifre separate.** Nu presupune niciodată că sunt egale (vezi SPEC §5.2).
5. **Limbi:** cod și identificatori în engleză, textele din interfață în română (doar în `strings.xml`), textele generate pentru client/contabil în italiană (doar în `domain/Descrizione.kt` și `domain/Dictionary.kt`).
6. **Logica de business stă în `domain/`**, pură, testabilă fără Android. Zero logică de calcul în composable-uri.
7. Fără `!!`, fără `GlobalScope`, fără operații de bază de date pe main thread.
8. Nicio dependență nouă fără justificare în descrierea PR-ului. Dependențele se declară **doar** în `gradle/libs.versions.toml`.

## Definition of done pentru orice milestone

`./gradlew :app:testDebugUnitTest :app:assembleDebug` trece, `bash tools/check-no-internet.sh` trece, și criteriile din SPEC §10 pentru milestone-ul respectiv sunt bifate.

Commit-uri: Conventional Commits, ex. `feat(money): rest de incasat pe lucrare`. Un PR per milestone.

## Stadiu

- **M0 — gata:** schelet Gradle, temă, navigare cu 5 ecrane, script de verificare offline. Ecranele sunt goale, intenționat.
- **Urmează M1:** modelul Room complet din SPEC §4, datele demo (`domain/Seed.kt`), ecranele Clienți și Lucrări.
