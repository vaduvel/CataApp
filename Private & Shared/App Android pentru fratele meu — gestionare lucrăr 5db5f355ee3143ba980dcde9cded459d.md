# App Android pentru fratele meu — gestionare lucrări (cercetare + spec)

<aside>
🎯

**Scopul aplicației:** o aplicație Android personală (instalată prin cablu, fără Play Store) care ține minte **doar lucrările**: la ce client, pe ce stradă, ce se lucrează, cât ar trebui să dureze, ce s-a lucrat în fiecare zi și **ce a rămas nefăcut**. În plus ține **partea de bani**: cât s-a convenit, ce s-a încasat, ce a rămas de luat și ce trebuie facturat — dar **nu emite facturi** (aceea trece obligatoriu prin SdI / contabil, vezi secțiunea 12). Testul aplicației: la finalul unei lucrări să poată scrie **exact ce a muncit**, ca să intre pe factură.

</aside>

## 1. Ce am aflat din cercetare (concluziile care contează)

1. **Un om singur nu are „proiecte", are un șir de lucrări mici care se întrerup între ele.** Ghidurile italiene de *gestione cantiere* descriu 7 faze (analiza lucrării → pregătire → deschidere → execuție → control și reprogramare → avansare → închidere cu „reziduuri"). Pentru un *artigiano* care e și muncitor și administrator, din tot fluxul rămân relevante 3 momente: **ce am promis**, **ce am făcut azi**, **ce a mai rămas**.
2. **„Ce a rămas nefăcut" are un nume în meserie: punch list / snag list / lista difetti.** Este lista de lucruri incomplete sau de corectat înainte de predare. O poziție bine făcută are: loc (camera), descriere, foto, cine rezolvă, termen, status. Practica modernă e *rolling punch list*: se notează pe măsură, nu la final — altfel la predare apar 30 de „mici lucruri".
3. **Unitatea de bază a evidenței este ziua de lucru (rapportino).** Data + lucrarea + ce s-a executat + materiale + foto + ce a blocat treaba. Dacă ziua nu e legată de o lucrare, informația se pierde și nu mai poți răspunde la „când ai fost la Mario?".
4. **Diferența estimat vs. real este singurul „raport" de care are nevoie.** În gestionale mari se numește preventiv / angajat / realizat / estimat la finalizare. Pentru el se reduce la: *am zis 3 zile, am făcut 5*. După 20 de lucrări începe să estimeze corect — și asta îi aduce bani, fără nicio hârtie.
5. **Ce se pierde în realitate la cei care lucrează singuri:** ofertele fără follow-up, clientul care repovestește casa de la zero, pozele care nu se mai găsesc când clientul reclamă după 8 luni, și WhatsApp-ul în care totul e amestecat (mesaje, vocale, poze) și nimic nu se poate regăsi.
6. **Trebuie să funcționeze offline** — pivnițe, hale, subsoluri, sate: fără semnal, aplicația trebuie să scrie local și să nu ceară cont.
7. **10 șabloane de lucrări acoperă ~80% din ce face** (sfat dat artizanilor pentru preventive, dar se aplică identic la etapele lucrării): baie completă, tencuială, pavaj/gresie, rigips, zugrăveală, șapă, termosistem, gard/zid, mansardă, reparații diverse.

## 2. Fluxul real, tradus pe cazul lui

| Moment | Ce se întâmplă la el în realitate | Ce trebuie să facă aplicația |
| --- | --- | --- |
| Sună clientul | Notează pe telefon sau ține minte | Adaugă lucrare nouă în 15 secunde: nume + stradă + ce lucrare |
| Se duce să vadă | Măsoară, face poze, spune un preț pe loc | Poze + notă vocală + „durată estimată: 3 zile" |
| Clientul zice da | Îi zice „vin luna viitoare" | Status **De programat**  • dată aproximativă de început |
| Lucrează | Lucrează 2 zile, sare la altă lucrare, revine | O zi lucrată = 1 apăsare: azi + lucrarea + ce s-a făcut |
| Se blochează | Lipsă material, decizie client, alt meseriaș, vremea | Status **În așteptare**  • motivul blocajului |
| Aproape gata | Rămân silicon, un plint, o retușare | **Rest de făcut** (punch list) cu poză și termen |
| Gata | Uită să mai treacă pe la client | Status **Terminat**  • arhivă cu poze pentru garanție |

## 3. Ce memorează aplicația (modelul de date)

| Entitate | Câmpuri | De ce contează |
| --- | --- | --- |
| **Client** | nume/porecla, telefon (opțional), notă („cheia la vecin", „câine", „parcare grea") | Ca să nu mai reia povestea de la zero la lucrarea următoare |
| **Adresă / punct de lucru** | stradă + număr, oraș, scară/apartament, link hartă | El gândește „cel de pe via Roma", nu „clientul nr. 42" |
| **Lucrare** | titlu, client, adresă, tip lucrare, status, dată început prevăzută, **zile estimate**, **zile lucrate** (calculat), prioritate, notă | Nucleul aplicației |
| **Etape (voci di lavoro)** | listă bifabilă în interiorul lucrării: demolare, trasee instalații, tencuială, gresie, sanitare, zugrăveală, curățenie | Dă procentul de avansare fără să calculeze nimic |
| **Zi lucrată** | dată, lucrare, ore (opțional), ce s-a executat, poze, blocaje | Răspunde la „când și cât am lucrat acolo" |
| **Rest de făcut** | lucrare, cameră/loc, descriere, motiv (material / decizie client / alt meseriaș / vremea / lipsă timp), poză, „revin pe data de…", bifat | Exact ce cerea întrebarea: ce a rămas neterminat |
| **Materiale de luat** | lucrare, articol, cantitate, magazin, cumpărat da/nu | Prima cauză de blocaj la un om singur |
| **Foto** | legate de lucrare / zi / rest, etichetă înainte–în timpul–după | Garanție, reclamații, portofoliu |
| **Memento** | text, dată, legat de client sau lucrare | „De sunat Mario", „revin la silicon pe 12" |

### Exemplu concret (cazul din întrebare)

- **Client:** Mario · **Adresă:** Via 23, Milano
- **Lucrare:** „Rifacimento bagno" · **Zile estimate:** 3 · **Status:** În lucru
- **Etape:** demolare ✅ · trasee ✅ · impermeabilizare ⬜ · gresie ⬜ · sanitare ⬜ · silicon ⬜
- **Zi lucrată:** 30 august — „demolat placa veche și scos vasul" + 4 poze
- **Rest de făcut:** „lipsește bateria dușului — o alege clientul" → motiv: decizie client → revin pe 5 septembrie
- **Bani:** convenit 2.400 € (la corp) · acont 800 € încasat pe 30 aug · rest de încasat 1.600 €
- **Facturare:** „acont încasat → de trimis contabilului" bifat pe 30 aug · la final apare „De facturat: 1.600 €"

## 4. Statusuri (puține și colorate)

1. **De ofertat** — am văzut lucrarea, i-am zis un preț, aștept răspuns
2. **De programat** — a acceptat, nu am dată fixă
3. **În lucru**
4. **În așteptare** — blocat (material / client / alt meseriaș / vremea)
5. **De finisat** — practic gata, mai sunt mărunțișuri (aici trăiește punch list-ul)
6. **Terminat**
7. **Pierdut / anulat**

<aside>
💰

**A doua axă, complet independentă de status: banii.** O lucrare poate fi „Terminat" și „Neîncasat" — exact cazul care doare. Stări de bani: **Ofertat → Acceptat → Acont încasat → De facturat → Facturat → Încasat parțial → Încasat.** Un singur ecran trebuie să răspundă la „cine îmi mai datorează bani?".

</aside>

## 5. Ecranele aplicației

1. **Azi** — lucrarea zilei, butoane mari: „Am lucrat azi aici", „Adaugă rest", „Fă poză", „Adaugă material", plus memento-urile de azi.
2. **Lucrări** — listă filtrabilă pe status, cu culori; căutare după nume client **și după stradă**.
3. **Detaliu lucrare** — sus: client, adresă (buton hartă + buton apel), bara de avansare; tab-uri: Etape · Zile lucrate · Rest de făcut · Poze · Materiale · Notă.
4. **Rest de făcut (global)** — toate lucrurile neterminate din toate lucrările, grupate pe client/stradă. Ecranul care răspunde la „ce mi-a mai rămas nefăcut?".
5. **Client** — datele lui + toate lucrările + istoricul (util și pentru lucrări viitoare la același om).
6. **Calendar** — luna cu zilele lucrate colorate pe lucrare; vede unde s-au dus zilele.
7. **Setări / Backup** — export, import, șabloane de etape.

## 6. Reguli de UX pentru un om cu mâinile murdare

- Butoane mari (min. 56 dp), maxim 2 apăsări pentru orice acțiune zilnică.
- Formulare scurte: lucrare nouă = 4 câmpuri (client, stradă, ce lucrare, câte zile). Restul se completează pe drum.
- **Dictare vocală** pentru note (tastatura Android o are deja) și poza ca notă.
- Durata în **zile**, nu în ore — așa gândește el.
- Data se completează automat cu „azi", cu posibilitate de a corecta.
- Fără cont, fără parolă, fără internet, fără reclame, fără ecran de „onboarding".
- Text în română, dar numele clienților/lucrărilor scrise cum vrea el (italiană) — eventual un comutator RO/IT pentru etichete.

## 7. Offline, backup, instalare prin cablu

- Bază de date locală (SQLite / Room), fără server. Toate scrierile merg local, instant.
- Pozele în folderul aplicației, cu miniaturi generate, ca să nu se blocheze lista.
- **Backup automat zilnic** într-un fișier ZIP (JSON + poze) în `Download`, cu păstrarea ultimelor 7; plus buton „Trimite backup pe WhatsApp/Drive".
- Import din backup, ca să nu piardă nimic la schimbarea telefonului.
- La instalare prin cablu: trebuie activat „Instalare aplicații din surse necunoscute"; **păstrează cheia de semnare** (keystore) — fără ea nu mai poți face update peste aplicația existentă, ar trebui dezinstalată și ar pierde datele.
- Permisiuni: doar cameră (+ notificări pe Android 13+ pentru memento-uri). Nimic altceva.

## 8. Roadmap

### v1 — minimul care e deja util

- [ ]  Clienți + adrese
- [ ]  Lucrări cu status, zile estimate, notă
- [ ]  Etape bifabile (cu șabloane pre-completate)
- [ ]  Zile lucrate (1 apăsare)
- [ ]  Rest de făcut + motiv blocaj
- [ ]  Poze legate de lucrare
- [ ]  Listă materiale de luat
- [ ]  Mod de facturare pe lucrare (la corp / la măsură / pe zile)
- [ ]  Măsurători pe cameră, cu cantități și poză
- [ ]  Marcaj „extra" pe fiecare lucrare și zi lucrată
- [ ]  Preț convenit + acont + încasări → „rest de încasat" pe lucrare
- [ ]  „Descriere pentru factură" generată din ofertă + măsurători + extra + zile
- [ ]  Backup / export

### v2 — după ce îl vede folosind

- [ ]  Calendar lunar
- [ ]  Memento-uri cu notificări
- [ ]  Căutare globală
- [ ]  Ofertă (preventivo) cu poziții și total, export PDF pentru WhatsApp
- [ ]  Follow-up automat la ofertele fără răspuns (3 / 7 / 14 zile)
- [ ]  Lucrări extra cerute pe parcurs, marcate „de adăugat la plată"
- [ ]  Ecran „Bani": de încasat, restanțe peste 30 de zile
- [ ]  „Rezumat lucrare" ca text de trimis pe WhatsApp clientului (ce s-a făcut, ce urmează, ce s-a încasat)
- [ ]  Estimat vs. real, pe tipuri de lucrări

### v3 — bonusuri

- [ ]  Widget pe ecranul principal: „Am lucrat azi la…"
- [ ]  Note vocale salvate ca fișier
- [ ]  Statistici simple: zile lucrate pe lună, clienți recurenți

## 9. Cum validezi fluxul fără să-l întrebi

- Fă v1 să funcționeze ca un **carnet digital**: orice câmp opțional, orice text liber. Dacă îl obligi la o metodă, o abandonează în 3 zile.
- Pornește de la ce vezi deja: cum vorbește despre lucrări („ăla din via 23", „mai am de dat silicon"), asta e chiar limbajul din interfață.
- Pune 10 șabloane de etape gata făcute și lasă-l să le modifice — nu-l pune să construiască nimic la prima deschidere.
- Când i-o dai, stai 10 minute cu el și introduceți împreună 2 lucrări reale, apoi mai vorbiți după 2 săptămâni: prima versiune reală se scrie după ce o folosește, nu înainte.

## 10. Cum țin evidența firmele de 1–2 oameni

La 1–2 oameni „gestiune serioasă" nu înseamnă software de firmă mare. Meseria are deja cinci documente care duc la factură — doar că la microfirme toate cinci trăiesc într-un caiet și în cap.

| Documentul din meserie | Ce este de fapt | Cum arată la un om singur |
| --- | --- | --- |
| **Giornale dei lavori** | Ce s-a întâmplat pe șantier, zi cu zi | Caietul din mașină, mesaje și poze pe WhatsApp |
| **Libretto delle misure** | Cantitățile executate, clasificate pe pozițiile din ofertă: ce lucrare, în ce loc, cât | Măsurători scrise pe spatele ofertei sau poze cu ruleta pe zid |
| **SAL (stadiu de avansare)** | Cât s-a lucrat până la o dată, ca să poată cere bani | „Dă-mi încă un acont, am ajuns la gresie" |
| **Conto finale** | Recapitulare: fiecare poziție cu cantitate × preț unitar, minus acontele deja primite | Socoteala făcută din memorie seara, înainte de a vorbi cu contabilul |
| **Factura** | Descrierea muncii executate + suma | Rezultatul care trebuie să iasă corect din toate cele de sus |

### Descoperirea cea mai utilă din cercetare

Firmele mici care completau **zilnic** un *rapportino* cu lucrările făcute și resursele folosite, marcând de fiecare dată dacă era **poziție din contract sau extra**, construiau fără să-și dea seama exact primele două documente din tabel (libretul de măsuri + jurnalul lucrărilor). Rezultatul: **nu pierdeau niciun extra** și discuția despre bani cu clientul devenea simplă, pentru că aveau hârtia în față. Asta e tot „secretul" gestiunii serioase la 1–2 oameni: **o notă pe zi, nu un sistem.**

### Rapportino-ul standard are 9 câmpuri. La el se reduc la 5

- **Standard** (firme cu angajați): dată · șantier/comandă · cine a lucrat · ore normale și suplimentare · lucrări executate · **cantități produse** · materiale și avize · foto și note · imprevizibile (vremea, întârzieri, materiale lipsă) · semnătura șefului de șantier.
- **La el, care e singur:** dată · lucrarea · ce am făcut · cât (dacă se măsoară) · extra da/nu. Restul nu are sens.
- Regula pe care o repetă toate ghidurile: **se completează la sfârșitul zilei.** Dacă amâni chiar și o zi, uiți ore, materiale și lucrări — iar greșeala ajunge direct în factură.
- La un om singur nu are cine să semneze rapportinul. Substitutul practic e **poza cu dată**: în discuțiile cu clientul, poza de pe 30 august închide subiectul mai repede decât orice text.

### Ce folosesc în realitate azi

Caiet, agendă, Excel (circulă chiar modele de *contabilità di cantiere* în Excel), WhatsApp — iar cei care au trecut pe telefon folosesc aplicații foarte simple: *Preventivi Edili Pro* (100% offline, datele rămân pe telefon, export PDF), *PreventivOk*, *Artigiano PRO*, iar dintre cele de șantier *myAEDES* e gratuit până la 3 șantiere. Toate confirmă același lucru: la microfirme se cere **ofertă + lucrări + poze**, nu module de ERP. Iar modul clasic în care cade un om singur e descris chiar de consultanții din branșă: *„preventive în cap, prețuri în cap, scadențe în cap"* — firma stă în picioare doar cât ține memoria lui.

## 11. De la ziua de lucru la rândul din factură

Aici e legătura care lipsea: **modul de facturare decide ce trebuie notat zilnic.** Sunt trei variante, și aplicația trebuie să întrebe o singură dată, când se deschide lucrarea.

| Mod de facturare | Factura se scrie din… | Deci în timpul lucrării notează |
| --- | --- | --- |
| **La corp** — preț fix pe lucrare | Descrierea din ofertă + extra-urile acceptate | Doar ce s-a executat și extra-urile (altfel le dă gratis) |
| **La măsură** — m² / ml / buc × preț unitar | Cantitățile efectiv executate | Măsurătorile, pe cameră, cu poză: 12,40 m² baie, 8 ml plintă |
| **Pe zile / manoperă** | Număr de zile × tarif | Fiecare zi lucrată și ce s-a făcut în ea |

### Ce se scrie efectiv pe factură

Descrierea trebuie să corespundă muncii executate, iar la un artizan **materialul se facturează împreună cu munca**, nu separat ca la comerț: formularea tipică e *„fornitura di beni e manodopera per il rifacimento del bagno…"*. Nu e obligatoriu să despartă manopera de materiale. Singura excepție practică sunt **„beni significativi"** (centrală, sanitare, aparate), unde valoarea obiectului se evidențiază separat — dar aia e treaba contabilului; aplicației îi ajunge să știe ce obiecte scumpe a montat.

### Butonul care închide cercul: „Descriere pentru factură"

Din ofertă + zile lucrate + măsurători + extra, aplicația scoate un text gata de trimis:

```
Rifacimento bagno — Mario, Via 23, Milano
Periodo: 30/08 – 06/09/2026 (5 giornate)
- demolizione pavimento e rivestimento esistenti
- impermeabilizzazione e posa gres 12,40 m2
- posa sanitari (fornitura beni e manodopera)
- silicone e finiture
Extra concordati: nicchia doccia + spostamento presa — 180 €
Concordato 2.400 € + extra 180 € − acconto incassato 800 € (30/08)
Da fatturare a saldo: 1.780 €
```

Textul iese în italiană (așa merge la client și la contabil), chiar dacă interfața e în română. Și ăsta e testul aplicației: **dacă la finalul lucrării poate genera textul de mai sus fără ca el să-și amintească nimic, evidența e serioasă. Dacă nu, e doar un carnet frumos.**

## 12. Partea de bani — cât de „serioasă" o facem

<aside>
⚠️

**Limita pe care nu o putem ocoli:** în Italia factura nu e un PDF, e un fișier XML trimis prin **SdI** (Sistema di Interscambio), obligatoriu **și pentru forfettari** de la 1 ianuarie 2024. Se trimite din portalul gratuit „Fatture e Corrispettivi", prin PEC sau printr-un soft/intermediar acreditat, iar facturile trebuie **conservate electronic 10 ani** (serviciul Agenției e gratuit, dar nu e retroactiv — acordul se semnează înainte). Concluzie de design: **aplicația nu emite și nu trimite facturi.** Face partea care de fapt lipsește: **ce trebuie facturat, ce s-a încasat, ce a rămas de luat.**

</aside>

Aplicația e stratul **de dinainte** și **de după** factură — factura în sine rămâne unde e deja (contabil sau softul de facturare pe care îl folosește).

| Aplicația face | Aplicația NU face |
| --- | --- |
| Ține prețul convenit și cum a fost calculat (la corp / la m² / pe zi) | Nu calculează TVA (10% / 22%), nu pune bollo, nu aplică reverse charge |
| Înregistrează încasările: dată, sumă, cash / bonifico | Nu emite chitanțe și nu ține contabilitate |
| Semnalează momentul „acum trebuie factură" (acont și final) | Nu generează XML și nu trimite nimic la SdI |
| Reține numărul și data facturii ca simplă evidență, plus „plătită da/nu" | Nu face conservarea legală pe 10 ani |
| Pregătește un text / PDF de trimis contabilului într-o apăsare | Nu stochează date fiscale ale clientului (CF, P.IVA, adresă de facturare) |

### Ce declanșează o factură (ca să nu-l prindă pe picior greșit)

1. **Acont încasat = factură pentru suma încasată.** La prestări de servicii TVA se datorează la plată, deci acontul se facturează când intră banii. În aplicație: bifează „am luat 800 € acont" → apare automat în lista „De trimis contabilului".
2. **La final, factura de saldo scade acontele deja facturate.** Aplicația arată calculul simplu: `convenit + extra − facturat până acum = de facturat acum`.
3. **Termen:** factura imediată se emite în **12 zile** de la data operațiunii, cea amânată până pe **15 ale lunii următoare**. În aplicație: memento la 3 zile după ce lucrarea trece în „Terminat", dacă nu e marcată „Facturat".
4. **Uneori încasează mai puțin decât a facturat:** dacă clientul plătește prin *bonifico parlante* pentru un bonus edilizi, banca reține **11%** (din 1 martie 2024). De asta „factură" și „încasat" trebuie să fie două cifre separate, nu una.

### Cel mai mare pierdut de bani la un om singur: extra-urile

„Fă-mi și asta cât ești aici" — 20 de minute aici, un rând de gresie acolo, și la final nu mai știe ce a dat gratis. Fiecare extra primește: descriere, dată, preț, „acceptat de client (mesaj / vocală)", inclus la plată da/nu. Pe ecranul final apare **preț de bază + extra**, ca să nu se mai facă reduceri din uitare.

## 13. Câmpurile noi din model

| Entitate | Câmpuri | Rol |
| --- | --- | --- |
| **Ofertă (preventivo)** | poziții (descriere, cantitate, preț unitar), total, dată trimisă, valabilitate, status (trimisă / acceptată / refuzată) | Ofertele fără răspuns sunt prima gaură de venit; aici primesc follow-up |
| **Acord de plată** | mod de calcul (la corp / la m² / pe zi), acont %, tranșe pe stadii (ex. 30% la start – 40% la jumătate – 30% la final), termen de plată | Fixează așteptările înainte de a începe, nu după |
| **Mod de facturare** | pe lucrare: la corp / la măsură (cantitate × preț unitar) / pe zile (nr. zile × tarif) | Decide ce trebuie notat zilnic — nimic special, cantități sau zile |
| **Măsurători** | cameră/zonă, lucrare, cantitate (m² / ml / buc), dată, poză cu ruleta | Fără ele nu poate factura o lucrare „la măsură" și nu poate dovedi cantitatea |
| **Extra** | descriere, dată, preț, acceptat de client da/nu, inclus la plată da/nu | Zona în care se pierd cei mai mulți bani |
| **Încasare** | dată, sumă, mod (cash / bonifico), lucrare, notă | Rest de încasat = convenit + extra − încasat |
| **De facturat** | se generează singur: acont încasat · stadiu atins · lucrare terminată | Lista pe care o trimite contabilului, fără să se gândească |
| **Factură (doar evidență)** | număr, dată, sumă, lucrare, scadență, plătită da/nu | Ca să știe ce e neîncasat, fără să intre în contabilitate |

## 14. Ecranele noi

1. **Bani** — trei cifre mari sus: *De încasat total* · *Restanțe peste 30 de zile* · *Încasat luna asta*. Dedesubt, lista pe lucrări.
2. **De facturat** — lista cu buton „Copiază pentru contabil": pe lângă sumă și tip (acont / saldo), generează **descrierea lucrărilor executate** — pozițiile din ofertă efectiv făcute, cu cantități, plus extra-urile și zilele lucrate. Și buton „Trimite pe WhatsApp".
3. **Ofertă** — poziții luate din șabloane, total automat, export PDF; butonul „Am trimis" pornește follow-up-ul.
4. În **Detaliu lucrare** apare o bară nouă: *convenit · facturat · încasat · rest*.

## 15. Ce rămâne intenționat afară

- Emiterea și trimiterea facturii la SdI, fișierul XML, conservarea pe 10 ani, TVA, bollo, reverse charge, rețineri — rămân la contabil sau în serviciul de facturare pe care îl folosește deja.
- Contracte, documente de siguranță, date fiscale ale clientului — nu intră în aplicație. În aplicație clientul are doar nume/poreclă, stradă și, opțional, telefon.
- Dacă vreodată vrea facturare completă din telefon, drumul corect nu e să o scriem noi (numerotare, XML, conservare, sancțiuni de la 1.000 la 8.000 €), ci un serviciu acreditat — iar în aplicație rămâne doar numărul facturii.

---

### Surse principale

- [Gestione del cantiere edile — fazele lucrării (Infominds)](https://infominds.eu/la-gestione-del-cantiere-di-ieri-contro-quella-di-oggi/)
- [Punch list / snag list în cantiere (PlanRadar)](https://www.planradar.com/it/punch-list-cantiere/) · [Punch list — ce câmpuri are o poziție](https://punchlistapp.net/) · [Rolling punch list](https://www.fieldpie.com/blog/construction-punch-list/)
- [Preventivo edile: șabloane care acoperă 80% din lucrări (Mela Work)](https://www.mela.work/it/blog/preventivo-edile-a-cosa-serve-come-farlo-e-come-ottimizzare-il-processo)
- [De la ofertă la încasare: fluxul standard al unei firme mici](https://siteview.build/resources/estimate-to-invoice-workflow) · [Statusuri de lucrare într-un pipeline](https://workmansdashboard.com/product-overview/product)
- [Semnele că ai depășit bilețelele lipite — CRM pentru meseriași](https://www.buildwithdave.com/resources/contractor-terms-glossary/customer-relationship-management)
- [De ce WhatsApp nu ține locul unei evidențe de șantier](https://infominds.eu/app-per-gestione-cantiere-vs-whatsapp-confronto-aggiornato/) · [Cum se gestionează pozele de șantier](https://www.mela.work/it/blog/app-utili-per-cantieri-foto-2-3)
- [Aplicații care funcționează fără semnal, sincronizate la revenire (Artesan)](https://artesan.pro/)
- **Partea de bani:** [termene de emitere — 12 zile / până pe 15 ale lunii următoare (Agenzia delle Entrate)](https://www.agenziaentrate.gov.it/portale/schede/comunicazioni/fatture-e-corrispettivi/faq-fe/risposte-alle-domande-piu-frequenti-categoria/emissione-delle-fatture-elettroniche) · [explicație practică (Stripe)](https://stripe.com/it/resources/more/issuing-invoice-after-payment-italy)
- [Conservarea electronică obligatorie 10 ani + serviciul gratuit al Agenției](https://www.agenziaentrate.gov.it/portale/aree-tematiche/fatturazione-elettronica/guida-fatturazione-elettronica/come-predisporre-inviare-ricevere-fe/come-si-conservano-fe) · [obligatoriu și pentru forfettari, sancțiuni 1.000–8.000 €](https://www.informazionefiscale.it/conservazione-fatture-elettroniche-scadenza-2026-forfettari)
- [Factură de acont și factură la saldo (Aruba)](https://guide.aruba.it/soluzioni-fatturazione-elettronica/fe/fatture-documenti/casi-uso-creazione-fatture/creazione-fatture-elettroniche-acconto-saldo) · [cum se gestionează TVA-ul între acont și saldo (Biblus ACCA)](https://biblus.acca.it/fattura-di-acconto-fattura-a-saldo-come-viene-gestita-l-iva/)
- [Plăți pe stadii de avansare (SAL) și scăderea acontelor](https://biblus.acca.it/stato-avanzamento-lavori/) · [ce câmpuri are o evidență de încasări (scadenzario)](https://www.atlantisevo.com/scadenzario-aziendale-e-gestione-scadenze-pagamenti-e-incassi/)
- [Fatturazione elettronică obligatorie pentru toți forfettarii din 2024](https://aequotax.com/fattura-elettronica-forfettari) · [reținerea din bonifico parlante a crescut la 11% (din 1 martie 2024)](https://ediltecnico.it/aumento-ritenuta-bonifici-bonus-edilizi-11/)
- **Cum se ține evidența:** [Giornale dei lavori — ce document face ce (Biblus ACCA)](https://biblus.acca.it/giornale-dei-lavori/) · [Libretto delle misure](https://biblus.acca.it/libretto-delle-misure/) · [ce conține o înregistrare de măsurători (art. 183)](https://www.studiopetrillo.com/contabilita-lavori-pubblici.html)
- [Rapportino zilnic care devine libret de măsuri + jurnal, cu extra marcate (Mela Work)](https://www.mela.work/it/blog/come-si-compila-un-rapportino-di-cantiere) · [checklist rapportino, 9 câmpuri](https://www.oretrack.ch/it/ccnl/cantoni/rapportino-giornaliero-cantiere) · [se completează la sfârșitul zilei (Infominds)](https://infominds.eu/rapportino-di-cantiere/)
- [Conto finale: cantitate × preț unitar, minus acontele (Pedago)](https://www.pedago.it/blog/conto-finale-lavori.htm) · [contabilitate de șantier în Excel la microfirme](https://www.myaedes.com/blog/contabilita-di-cantiere-excel-gratis/)
- **Ce se scrie în factură:** [bunuri + manoperă împreună la artizan](https://flextax.it/cosa-devo-scrivere-sulla-fattura-in-caso-di-fornitura-di-materiale-al-cliente/) · [nu e nevoie să separi manopera (Il Sole 24 ORE)](https://ntpluscondominio.ilsole24ore.com/art/fattura-senza-indicazione-costo-manodopera-AB5TDVvD) · [excepția „beni significativi"](https://www.fiscoetasse.com/domande-e-risposte/12025-beni-significativi-in-edilizia-ecco-come-fare-la-fattura.html)
- **Ce folosesc deja zidarii pe telefon:** [Preventivi Edili Pro (offline)](https://play.google.com/store/apps/details?id=com.amird.muratoripro&hl=it) · [Artigiano PRO](https://www.appartigianopro.it/) · [„preventive în cap, prețuri în cap" — cum cade firma de un om](https://www.florinandriciuc.com/blog)