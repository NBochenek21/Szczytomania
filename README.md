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
