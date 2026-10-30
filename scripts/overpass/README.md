# Import szczytów z OpenStreetMap

Skrypty budują plik aplikacji `app/src/main/assets/peaks/peaks.json` z danych OpenStreetMap
(Overpass API). Wymagają tylko Pythona 3.8+ (bez dodatkowych bibliotek).

```
cd scripts/overpass
python pobierz.py        # 1. pobiera dane z Overpass do surowe/ (nie trafia do repozytorium)
python przygotuj_csv.py  # 2. czyści dane, wybiera szczyty, zapisuje szczyty.csv
python pasma_wikidata.py # 2b. (opcjonalnie) podpowiedzi pasm z Wikidanych -> kolumna pasmo_wikidata
                         #    -> ręcznie: uzupełnij kolumnę "pasmo", popraw "uwzglednij"
python zbuduj_json.py    # 3. z szczyty.csv buduje peaks.json
python -m unittest -v    # testy czyszczenia danych
```

Na koniec uruchom `./gradlew testDebugUnitTest`: test `PeaksAssetTest` sprawdza gotowy plik.

## Co robią skrypty

**Czyszczenie** (`czyszczenie.py`):
- wysokość (`ele`) zamieniana na liczbę całkowitą: `"1725 m"`, `"1725,3"`, `"1 725"`, `"1725 m n.p.m."`;
  odrzucane są stopy, zakresy (`"1200-1250"`) i wartości spoza 1–2499 m,
- odrzucane są węzły bez nazwy lub bez poprawnej wysokości,
- duplikaty: to samo id albo ta sama nazwa w odległości poniżej 1 km. Sama nazwa nie wystarczy,
  bo w Polsce jest wiele różnych szczytów o nazwie „Kopa” czy „Łysica”,
- nazwy dwujęzyczne (`"Veľká Rača / Wielka Racza"`): brany jest tag `name:pl` albo polska część
  nazwy; gdy nie da się tego rozstrzygnąć, w kolumnie `uwagi` jest „popraw nazwę”.

**Wybór ok. 200 szczytów** (`przygotuj_csv.py`):
1. wszystkie 28 szczytów Korony Gór Polski (`kgp.py`),
2. popularne szczyty turystyczne z listy prowadzonej ręcznie (`popularne.py`) – OSM nie ma
   informacji o popularności,
3. reszta: szczyty opisane w Wikidanych/Wikipedii, od najwyższych, najwyżej 5 z jednego
   obszaru ok. 20×20 km (inaczej byłyby prawie same Tatry).

Do CSV trafia 350 kandydatów, z czego 200 ma `uwzglednij = tak`. Najwyższe szczyty pasm, których
brakuje, zaznacz ręcznie (`tak`). `zbuduj_json.py` wypisuje na końcu listę pasm z najwyższym
szczytem w każdym, żeby łatwo to sprawdzić.

Szczyty przygraniczne (Rysy, Śnieżka, Pilsko…) mają węzeł czasem tuż poza obszarem Polski w OSM,
dlatego KGP i popularne są szukane osobnym zapytaniem w promieniu 5 km od podanych współrzędnych.

**Pasmo**: OSM go nie podaje. Dla KGP jest wpisane z listy, pozostałe uzupełnia się ręcznie
w `szczyty.csv` (separator `;`, kodowanie UTF-8 – otwiera się w Excelu). Szczyty bez pasma są
pomijane przy budowaniu `peaks.json`. Ponowne uruchomienie `przygotuj_csv.py` zachowuje ręcznie
wpisane nazwy, pasma i `uwzglednij`.

**Podpowiedzi z Wikidanych** (`pasma_wikidata.py`): dla szczytów z identyfikatorem Wikidanych
pobierana jest cecha P4552 „pasmo górskie”. Trafia do kolumny `pasmo_wikidata`, nigdy wprost do
`pasmo`. Przed przepisaniem trzeba ją ujednolicić z nazwami KGP:
- bywa zbyt szczegółowa („Tatry Wysokie”, „Tatry Zachodnie”, „Bieszczady Zachodnie”, „Pasmo Policy”)
  albo zbyt ogólna („Karpaty”, „Beskidy”, „Sudety”),
- przy szczytach granicznych bywa słowacka („Kysucké Beskydy”, „Oravské Beskydy”),
- kilka wartości jest rozdzielonych znakiem `|` – wybierz jedną.

Pasm nie mają szczyty bez wpisu w Wikidanych – te uzupełnia się z mapy turystycznej.

**Pole `kgp`** w `peaks.json`: `true` dla 28 szczytów Korony Gór Polski.

## Weryfikacja współrzędnych KGP

Współrzędne pochodzą z OSM. Każdy szczyt trzeba sprawdzić ręcznie (link „osm” – czy punkt leży na
wierzchołku i czy to właściwy szczyt) i odhaczyć. Wysokość „lista KGP” to wartość z listy
referencyjnej w `kgp.py`; różnice kilku metrów między źródłami są normalne.

| # | Szczyt | Pasmo | Wys. OSM | Wys. lista KGP | lat, lon | OSM | OK |
|---|---|---|---|---|---|---|---|
| 1 | Rysy | Tatry | 2499 | 2499 | 49.1795756, 20.0881081 | [osm](https://www.openstreetmap.org/node/4366169405) | ☐ |
| 2 | Babia Góra | Beskid Żywiecki | 1723 | 1725 | 49.5730231, 19.5293303 | [osm](https://www.openstreetmap.org/node/477984782) | ☐ |
| 3 | Śnieżka | Karkonosze | 1603 | 1603 | 50.7360326, 15.7396072 | [osm](https://www.openstreetmap.org/node/1850303773) | ☐ |
| 4 | Śnieżnik | Masyw Śnieżnika | 1423 | 1425 | 50.2074081, 16.847424 | [osm](https://www.openstreetmap.org/node/26863429) | ☐ |
| 5 | Tarnica | Bieszczady | 1346 | 1346 | 49.0747705, 22.726323 | [osm](https://www.openstreetmap.org/node/1347929065) | ☐ |
| 6 | Turbacz | Gorce | 1310 | 1310 | 49.5428239, 20.1113839 | [osm](https://www.openstreetmap.org/node/26864983) | ☐ |
| 7 | Radziejowa | Beskid Sądecki | 1266 | 1266 | 49.4493044, 20.6041221 | [osm](https://www.openstreetmap.org/node/1198299196) | ☐ |
| 8 | Skrzyczne | Beskid Śląski | 1257 | 1257 | 49.6845736, 19.0302741 | [osm](https://www.openstreetmap.org/node/26864701) | ☐ |
| 9 | Mogielica | Beskid Wyspowy | 1171 | 1171 | 49.6553199, 20.2766736 | [osm](https://www.openstreetmap.org/node/1236685520) | ☐ |
| 10 | Wysoka Kopa | Góry Izerskie | 1127 | 1126 | 50.85, 15.4202778 | [osm](https://www.openstreetmap.org/node/707906070) | ☐ |
| 11 | Rudawiec | Góry Bialskie | 1106 | 1106 | 50.2448039, 16.9768585 | [osm](https://www.openstreetmap.org/node/1793427200) | ☐ |
| 12 | Orlica | Góry Orlickie | 1084 | 1084 | 50.3532494, 16.3606287 | [osm](https://www.openstreetmap.org/node/2418877544) | ☐ |
| 13 | Wysoka | Pieniny | 1050 | 1050 | 49.3802606, 20.5555018 | [osm](https://www.openstreetmap.org/node/615627232) | ☐ |
| 14 | Wielka Sowa | Góry Sowie | 1015 | 1015 | 50.6802778, 16.4855556 | [osm](https://www.openstreetmap.org/node/458891184) | ☐ |
| 15 | Lackowa | Beskid Niski | 997 | 997 | 49.4265468, 21.1024789 | [osm](https://www.openstreetmap.org/node/4366115862) | ☐ |
| 16 | Kowadło | Góry Złote | 988 | 989 | 50.2644312, 17.0132176 | [osm](https://www.openstreetmap.org/node/2421373077) | ☐ |
| 17 | Jagodna | Góry Bystrzyckie | 977 | 977 | 50.2524573, 16.5644186 | [osm](https://www.openstreetmap.org/node/2418865417) | ☐ |
| 18 | Skalnik | Rudawy Janowickie | 945 | 945 | 50.8084793, 15.9000581 | [osm](https://www.openstreetmap.org/node/1219704819) | ☐ |
| 19 | Waligóra | Góry Kamienne | 934 | 936 | 50.6808246, 16.2780881 | [osm](https://www.openstreetmap.org/node/2470310941) | ☐ |
| 20 | Czupel | Beskid Mały | 930 | 933 | 49.767881, 19.1610286 | [osm](https://www.openstreetmap.org/node/3046822323) | ☐ |
| 21 | Szczeliniec Wielki | Góry Stołowe | 922 | 919 | 50.4848143, 16.3410065 | [osm](https://www.openstreetmap.org/node/492775426) | ☐ |
| 22 | Lubomir | Beskid Makowski | 904 | 904 | 49.7668805, 20.0596718 | [osm](https://www.openstreetmap.org/node/2845608415) | ☐ |
| 23 | Biskupia Kopa | Góry Opawskie | 889 | 890 | 50.2566949, 17.4285752 | [osm](https://www.openstreetmap.org/node/991934327) | ☐ |
| 24 | Chełmiec | Góry Wałbrzyskie | 851 | 851 | 50.7795495, 16.2109745 | [osm](https://www.openstreetmap.org/node/428725380) | ☐ |
| 25 | Kłodzka Góra | Góry Bardzkie | 757 | 765 | 50.45167, 16.7532205 | [osm](https://www.openstreetmap.org/node/529512640) | ☐ |
| 26 | Skopiec | Góry Kaczawskie | 724 | 724 | 50.9439267, 15.8847061 | [osm](https://www.openstreetmap.org/node/4593556759) | ☐ |
| 27 | Ślęża | Masyw Ślęży | 718 | 718 | 50.8640875, 16.7074963 | [osm](https://www.openstreetmap.org/node/349062696) | ☐ |
| 28 | Łysica | Góry Świętokrzyskie | 613 | 612 | 50.8916162, 20.8965979 | [osm](https://www.openstreetmap.org/node/1844240120) | ☐ |

Do szczególnego sprawdzenia: **Kłodzka Góra** (OSM 757 m, lista 765 m – większa różnica) oraz
szczyty na granicy, gdzie wierzchołek może leżeć po drugiej stronie: Rysy, Babia Góra, Śnieżka,
Śnieżnik, Rudawiec, Orlica, Wysoka, Lackowa, Kowadło, Biskupia Kopa.

## Licencja danych

Dane © OpenStreetMap contributors, licencja ODbL. Informacja o źródle jest w polu `source`
pliku `peaks.json` i musi być widoczna w aplikacji.
