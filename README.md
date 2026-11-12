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
| `users/{uid}` – profil | `nick` (3–20 znaków), `photoUrl` (na razie `null`), `createdAt` (czas serwera) |
| `users/{uid}/conquests/{id}` – zdobycz, jedna na szczyt | `peakId` (id z pliku szczytów), `conqueredAt` (czas z telefonu), `serverTime` (czas serwera), `lat`, `lon`, `accuracy` (odczyt GPS, dokładność w m), `photoUrl` (link do zdjęcia, do wysłania `null`), `photoStatus` (`pending` – zdjęcie czeka na wysłanie, `uploaded` – wysłane) |

- `{id}` zdobyczy to `peakId` z `_` zamiast `/` (np. `node_4366169405`), bo ukośnik w Firestore oddziela
  kolekcje od dokumentów; dzięki temu ten sam szczyt zapisze się tylko raz,
- zdobycz zapisuje się po udanym sprawdzeniu GPS (`MapViewModel.checkSummit`), także bez zasięgu –
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
