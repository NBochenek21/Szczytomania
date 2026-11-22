# Szczytomania
Szczytomania to aplikacja na Androida dla osób, które chodzą po polskich górach i zapisuja zdobyte szczyty. Ekranem głównym jest mapa ze wszystkimi szczytami w Polsce i aktualną lokalizacją użytkownika. 


## Uruchomienie

1. Otwórz folder projektu w Android Studio (File → Open) i poczekaj na synchronizację Gradle.
2. Opcjonalnie dopisz do `local.properties` klucz MapTiler (darmowe konto na maptiler.com):
   ```
   MAPTILER_API_KEY=twój_klucz
   ```
   Bez klucza aplikacja używa darmowego stylu OpenFreeMap (też dane OpenStreetMap).
3. Uruchom konfigurację `app` na telefonie albo emulatorze (Android 8.0+).

Emulator musi używać grafiki sprzętowej (*Device Manager → Edit → Graphics: Hardware*). Przy grafice
programowej (`swiftshader`) MapLibre nie rysuje symboli: nie widać nazw, szczytów ani pozycji użytkownika.
Pozycję GPS w emulatorze ustawisz w *Extended Controls → Location*.

## Plik szczytów

Szczyty są w `app/src/main/assets/peaks/peaks.json` (dane z OpenStreetMap, działają bez zasięgu):

```json
{
  "version": 1,
  "source": "© OpenStreetMap contributors (ODbL)",
  "peaks": [
    { "id": "node/4366169405", "name": "Rysy", "elevation": 2499, "lat": 49.1795756, "lon": 20.0881081, "range": "Tatry" }
  ]
}
```

- `id` – identyfikator obiektu w OpenStreetMap (`node/…`, `way/…` lub `relation/…`), stały między importami,
- `elevation` – wysokość w metrach (liczba całkowita), `range` – pasmo górskie,
- `kgp` – `true` dla 28 szczytów Korony Gór Polski (brak pola = `false`),
- dodatkowe pola są ignorowane, więc można je dopisywać bez zmiany wersji.

Plik generują skrypty w `scripts/overpass/` (opis w [scripts/overpass/README.md](scripts/overpass/README.md)).

Test `PeaksAssetTest` (`./gradlew testDebugUnitTest`) sprawdza plik: unikalne id, współrzędne w Polsce,
wypełnione pola.

## Gotowe zestawy gór i wygląd

- Zestawy: `app/src/main/assets/sets/sets.json` (`id`, `name`, `description`, opcjonalnie `badge`,
  `peakIds` z `peaks.json` od najwyższego). `PeakSetsTest` sprawdza, czy szczyty istnieją.
- Zdobyte szczyty (postęp, trójkąty, profil) pochodzą z `data/repository/ConqueredPeaks.kt`, czyli ze zdobyczy
  w Firestore (sekcja *Dane w Firestore*).
- Kolory i czcionki z mockupów: `ui/theme/`, wspólne elementy: `ui/components/`. Czcionki z Google Fonts
  (licencja OFL, `docs/licencje-czcionek/OFL.txt`).

## Sprawdzanie GPS

Stuknij szczyt na mapie i wybierz *Sprawdź GPS*. Warunki zaliczenia (`location/SummitCheck.kt`):
odległość od wierzchołka do 150 m, dokładność GPS do 50 m, lokalizacja niesymulowana.

Build debug ma dodatkowy *Szczyt testowy AGH* przy Gmachu Głównym AGH (`src/debug/assets/peaks/peaks_test.json`).
Nie trafia on do wersji release ani do pliku generowanego skryptami.

Jak przetestować bez wychodzenia w teren:

- pod AGH – telefonem, na miejscu,
- własny szczyt testowy, np. przy domu – plik `app/src/debug/assets/peaks/peaks_local.json` w tym samym formacie
  co `peaks_test.json` (id zaczynające się od `test/`). Plik jest w `.gitignore`, więc współrzędne nie trafią
  do publicznego repozytorium,
- emulator: pozycja ustawiona w *Extended Controls → Location* jest prawdziwym odczytem dla systemu,
  więc da się sprawdzić odległość; około 100 m od szczytu testowego to np. `50.0654, 19.9233`, 200 m to `50.0663, 19.9233`,
- fałszywa lokalizacja: aplikacja podmieniająca GPS (*Opcje programisty → Aplikacja do pozorowania lokalizacji*)
  albo w emulatorze dostawca testowy:
  ```
  adb shell appops set 2000 android:mock_location allow
  adb shell cmd location providers add-test-provider gps
  adb shell cmd location providers set-test-provider-enabled gps true
  adb shell cmd location providers set-test-provider-location gps --location 50.0650,19.9233 --accuracy 5
  adb shell cmd location providers remove-test-provider gps   # po teście
  ```
  Ostatnią pozycję trzeba wysyłać kilka razy w trakcie sprawdzania, bo aplikacja czeka na świeży odczyt.

## Dane w Firestore

Reguły bezpieczeństwa są w `firebase/firestore.rules`. Po każdej zmianie trzeba je wkleić w konsoli Firebase
(*Firestore Database → Rules → Publish*). E-mail i hasło są tylko w Authentication, niewidoczne dla innych.

| Dokument | Pola |
|---|---|
| `users/{uid}` – profil | `nick` (3–20 znaków: litery bez polskich znaków, cyfry, `.` `_` `-`), `nickLower` (nick małymi literami – do wyszukiwania), `photoUrl` (na razie `null`), `createdAt` (czas serwera) |
| `nicks/{nick małymi literami}` – zajęty nick | `uid` właściciela |
| `friendships/{uid1_uid2}` – znajomość, jedna na parę osób | `users` (oba uid), `from` (kto zaprosił), `to`, `status` (`pending` – zaproszenie, `accepted` – znajomi), `createdAt`, `acceptedAt` (czas serwera) |
| `users/{uid}/conquests/{id}` – zdobycz, jedna na szczyt | `peakId` (id z pliku szczytów), `conqueredAt` (czas z telefonu), `serverTime` (czas serwera), `lat`, `lon`, `accuracy` (odczyt GPS, dokładność w m), `photoUrl` (link do zdjęcia, do wysłania `null`), `photoStatus` (`pending` – zdjęcie czeka na wysłanie, `uploaded` – wysłane) |

- `{id}` zdobyczy to `peakId` z `_` zamiast `/` (np. `node_4366169405`), bo ukośnik w Firestore oddziela
  kolekcje od dokumentów; dzięki temu ten sam szczyt zapisze się tylko raz,
- zdobycz zapisuje się po udanym sprawdzeniu GPS i zdjęciu (`ConquestViewModel.save`), także bez zasięgu –
  Firestore wyśle ją sam po odzyskaniu sieci,
- zdobycze zalogowanego użytkownika daje `ConquestRepository.conquests` (z polem `synced` –
  `false`, dopóki zapis czeka w telefonie na wysłanie), a same id szczytów – `conqueredPeakIds`,
- po wysłaniu zdjęcia `ConquestRepository.markPhotoUploaded` zapisuje link i status `uploaded`; reguły
  pozwalają zmienić w zdobyczy tylko te dwa pola i tylko z `pending` na `uploaded`.

### Bez zasięgu

Firestore trzyma w telefonie kopię danych i kolejkę niewysłanych zapisów (pamięć podręczna jest włączona
domyślnie). Zdobycz zapisana bez zasięgu od razu jest widoczna w aplikacji, a na serwer trafia sama,
gdy aplikacja jest uruchomiona i złapie sieć – także po zamknięciu i ponownym otwarciu aplikacji.
Logowanie i rejestracja wymagają sieci, ale zalogowany użytkownik zostaje zalogowany także bez niej.

Jak sprawdzić:

1. Zaloguj się przy włączonym internecie.
2. Włącz tryb samolotowy (GPS działa dalej) i zdobądź szczyt testowy – karta od razu pokazuje „zdobyty”,
   a w konsoli Firebase dokumentu jeszcze nie ma.
3. Wyłącz tryb samolotowy przy otwartej aplikacji – po chwili dokument pojawia się w konsoli, a `serverTime`
   jest późniejszy niż `conqueredAt`.

## Zdjęcia ze szczytu

Po udanym sprawdzeniu GPS ekran zdobywania pokazuje w ramce podgląd na żywo z tylnego aparatu (CameraX,
`camera/CameraViewfinder.kt`, uprawnienie CAMERA – o zgodę aplikacja pyta przy pierwszym zdjęciu). Zdjęcie
ma ten sam kadr co ramka. W emulatorze aparat ustawia się w *Device Manager → Edit → Camera* (np. *VirtualScene*
albo kamera laptopa). Zdjęcie:

1. jest zmniejszane do 1600 px dłuższego boku i zapisywane jako JPEG (ok. 0,3 MB) bez danych EXIF, czyli
   bez pozycji GPS i modelu telefonu – `camera/PhotoShrink.kt`,
2. zostaje w pamięci aplikacji (`files/photos/{uid}/{id}.jpg`), a zdobycz zapisuje się ze statusem `pending`,
3. trafia do Cloud Storage w tle (`work/PhotoUploadWorker.kt`, WorkManager): dopiero gdy jest internet,
   także po zamknięciu aplikacji, z ponawianiem po błędzie. Potem zdobycz dostaje `photoUrl` i status `uploaded`.

Cloud Storage wymaga planu Blaze (lokalizacja `us-central1` – w niej działa darmowy limit 5 GB).
Zdjęcia są w `users/{uid}/conquests/{id}.jpg`. Reguły w `firebase/storage.rules` wkleja się w konsoli
(*Storage → Rules → Publish*): wysłać może tylko właściciel, tylko JPEG do 2 MB.

Jak sprawdzić: zdobądź szczyt testowy, zrób zdjęcie i zapisz. Po chwili zdjęcie jest w konsoli (*Storage*),
a zdobycz w Firestore ma `photoStatus: uploaded`. W trybie samolotowym zdjęcie czeka w telefonie i wysyła się
samo po powrocie zasięgu.

## Mapy offline

Ekran *Mapy offline* (menu) pobiera mapę wybranego regionu na telefon. Bez internetu MapLibre bierze
kafelki z bazy na telefonie, więc mapa pobranego regionu działa w górach bez zasięgu.

- Regiony są w `map/offline/MapRegion.kt` (`MapRegions.all`). Test `MapRegionsTest` pilnuje, żeby każdy
  szczyt z `peaks.json` leżał w którymś regionie – po dodaniu szczytów z nowego pasma trzeba dopisać region.
- Pobierane są przybliżenia 6–14; na 14 kafelki wektorowe mają już wszystkie szczegóły (szlaki, ścieżki).
- Pobieranie trwa także po wyjściu z ekranu. Przerwane (np. zamknięcie aplikacji) można wznowić.
- Na danych komórkowych aplikacja pyta przed pobraniem.
- Pobrany region działa tylko z tym samym stylem mapy. Po dopisaniu lub zmianie `MAPTILER_API_KEY`
  stare regiony są usuwane i trzeba je pobrać ponownie.

Jak przetestować w emulatorze: pobierz region, wyłącz internet (`adb shell svc wifi disable` i
`adb shell svc data disable`), uruchom aplikację ponownie i otwórz szczyt z tego regionu z listy
*Wszystkie szczyty*. Szczyt z niepobranego regionu pokaże pustą mapę. Wolne łącze, żeby zdążyć przerwać
pobieranie: `adb emu network speed edge` (z powrotem: `full`).

### Mapa bez internetu

- Pasek pod górnym paskiem mapy mówi, że nie ma internetu, a przy przybliżeniu na pasmo – czy ten
  obszar jest pobrany. Gdy nie jest, ma przycisk *Mapy offline* (`map/offline/OfflineHint.kt`).
- Gdy przy starcie nie ma internetu, a stylu mapy nie ma w telefonie (żaden region nie był pobrany),
  mapa przełącza się na styl zapasowy: tło i kontur Polski, na nich szczyty i pozycja użytkownika
  (`MapStyle.fallbackJson`). Po powrocie internetu sama wczytuje właściwy styl.
- Zdobyte szczyty są na mapie zielone, do zdobycia – szare (legenda z mockupu mapy).
- Szczyt wybrany z listy (szczytów, zestawu) przybliża mapę, także gdy był widoczny na oddalonej mapie.
  Stuknięcie trójkąta na mapie tylko otwiera kartę – kamera zostaje.

## Znajomi

Menu → *Znajomi* (mockupy 08–10). Czerwone kółko w menu to liczba zaproszeń czekających na odpowiedź.

- *Dodaj* – wyszukiwanie po początku nicku, bez względu na wielkość liter (`UserRepository.searchByNick`),
  przycisk *Zaproś*, a przy osobie, która już nas zaprosiła – *Akceptuj*. Niżej własny nick z *Udostępnij*.
  Po e-mailu nie szukamy: adresy są tylko w Authentication i inni ich nie widzą (karta projektu),
- zaproszenie: karta z *Akceptuj* / *Odrzuć*; wysłane zaproszenia można anulować,
- *Ostatnio na szczytach* – najnowsze zdobycze znajomych, lista znajomych z liczbą szczytów i postępem w KGP,
- profil znajomego: statystyki, wspólne szczyty, wyścig w Koronie Gór Polski (paski rosną animacją
  `animateFloatAsState`), jego zdobycze z oznaczeniem *Ty też*, *Usuń ze znajomych*.

Reguły (`firebase/firestore.rules`):

- zaprosić można tylko we własnym imieniu i tylko istniejącego użytkownika; id dokumentu to posortowane
  uid obu osób (`FriendRepository.pairId`), więc ta sama para nie dostanie dwóch zaproszeń,
- zaakceptować może tylko zaproszony; odrzucić, anulować i usunąć ze znajomych – każda z dwóch osób,
- zdobycze widzi właściciel, jego znajomi i osoba, którą właściciel zaprosił (żeby przy zaproszeniu widziała,
  ile ma szczytów),
- nicki są unikalne: profil i dokument w `nicks` zapisują się razem, a dokument nicku może powstać tylko raz.
  Rejestracja i zmiana nicku sprawdzają wcześniej, czy nick jest wolny (wymaga internetu). Konta założone
  wcześniej dostają `nickLower` i wpis w `nicks` same po zalogowaniu (`UserRepository.ensureSearchable`).

Jak sprawdzić (dwa konta, np. telefon i emulator): na koncie A *Dodaj* → wpisz początek nicku B → *Zaproś*.
Na koncie B w menu pojawia się czerwone *1*, na ekranie Znajomi – zaproszenie z liczbą szczytów A → *Akceptuj*.
Obie osoby widzą się na liście, a po dotknięciu – profil z wyścigiem w KGP i zdobyczami.

## Powiadomienia push

Powiadomienia wysyłają Cloud Functions (`firebase/functions`) przez Firebase Cloud Messaging, gdy:

- znajomy zdobył szczyt (`users/{uid}/conquests` – nowy dokument; zdobycz zapisana w górach bez zasięgu
  trafia na serwer później, wtedy też dopiero idzie powiadomienie),
- ktoś zaprasza do znajomych (`friendships` – nowy dokument),
- ktoś przyjął nasze zaproszenie (`friendships` – status `pending` → `accepted`).

Funkcja wysyła same dane (typ, uid i nick nadawcy, id szczytu), a tekst składa aplikacja – ma plik szczytów,
więc pokazuje np. „kuba na szczycie! · Rysy · 2499 m · Tatry” (`notifications/Notifications.kt`).
Dotknięcie otwiera profil znajomego, a przy zaproszeniu – listę Znajomi.

- Token telefonu jest w `users/{uid}/devices/{token}` (`notifications/PushTokens.kt`) – widzi go tylko
  właściciel, bo profil czyta każdy zalogowany. Funkcja usuwa tokeny, których FCM już nie zna.
- O zgodę na powiadomienia (Android 13+) aplikacja pyta raz, przy pierwszym wejściu na ekran Znajomi.
  W profilu jest przełącznik *Powiadomienia*; wyłączony usuwa token z Firestore.
- Przy wylogowaniu token jest usuwany i unieważniany – telefon nie dostaje powiadomień tamtego konta.

Wdrożenie (raz, potem po zmianach w `firebase/functions` albo regułach) – wymaga Node.js 22 i Firebase CLI
(`npm install -g firebase-tools`, `firebase login`):

```
cd firebase/functions && npm install && cd ../..
firebase deploy --only functions,firestore:rules
```

Region funkcji (`firebase/functions/index.js`) musi pasować do lokalizacji bazy Firestore – domyślnie
`us-central1`. Testy logiki: `cd firebase/functions && npm test`.

Jak sprawdzić bez serwera (build debug): udawana wiadomość push przez `notifications/DebugPushReceiver.kt`:

```
adb shell am broadcast -n pl.szczytomania/.notifications.DebugPushReceiver --es type friend_conquest --es uid abc --es nick kuba --es peakId node/4366169405
```

`type` to też `friend_invite` i `friend_accepted` (bez `peakId`). Z serwerem: dwa konta znajomych na dwóch
telefonach – zdobycz jednego daje powiadomienie na drugim.

## Struktura projektu

```
app/src/main/java/pl/szczytomania/
├── data/                 # Jakub – serwer i dane
│   ├── model/            # klasy danych: szczyt, zestaw, zdobycz, znajomy, wyzwanie
│   ├── auth/             # Firebase Authentication
│   ├── firestore/        # dostęp do Cloud Firestore
│   ├── storage/          # Cloud Storage (zdjęcia)
│   ├── peaks/            # wczytywanie pliku szczytów z assets
│   └── repository/       # repozytoria – jedno miejsce dostępu do danych
├── location/             # Kacper – GPS, promień 150 m, dokładność, fałszywa lokalizacja
├── camera/               # Kacper – zdjęcie z aparatu, zmniejszanie
├── map/                  # Kacper – MapLibre
│   └── offline/          # pobieranie map regionów
├── work/                 # Kacper – WorkManager (wysyłanie zdjęć w tle)
├── notifications/        # powiadomienia push (FCM)
└── ui/                   # Norbert – interfejs (Jetpack Compose, MVVM)
    ├── theme/            # kolory, typografia, motyw jasny/ciemny
    ├── navigation/       # nawigacja, menu (hamburger)
    ├── components/       # wspólne komponenty
    └── screens/          # ekran + ViewModel w jednym pakiecie
        ├── auth/         # rejestracja i logowanie
        ├── map/          # ekran główny – mapa i karta szczytu
        ├── conquest/     # zdobywanie szczytu (GPS + zdjęcie)
        ├── sets/         # zestawy gór
        ├── peaks/        # wszystkie szczyty
        ├── myconquests/  # moje zdobycze
        ├── friends/      # znajomi
        ├── challenges/   # wyzwania
        ├── profile/      # profil i statystyki
        ├── settings/     # ustawienia, mapy offline, prywatność
        └── about/        # o aplikacji
app/src/main/assets/peaks/  # plik ze szczytami (import z OpenStreetMap)
app/src/main/res/           # zasoby Androida
app/src/test/               # testy jednostkowe
app/src/androidTest/        # testy na urządzeniu
firebase/                   # reguły bezpieczeństwa Firestore/Storage
└── functions/              # Cloud Functions (powiadomienia)
scripts/overpass/           # skrypt importu szczytów z Overpass API
docs/                       # karta projektu, mockupy
```
