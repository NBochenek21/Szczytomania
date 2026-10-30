"""Krok 2: czyści surowe dane, wybiera ok. 200 szczytów i zapisuje szczyty.csv do ręcznej edycji.

    python przygotuj_csv.py

Wybór szczytów:
  1. wszystkie 28 szczytów Korony Gór Polski (kgp.py),
  2. popularne szczyty turystyczne z listy prowadzonej ręcznie (popularne.py),
  3. reszta do ok. 200: szczyty opisane w Wikidanych/Wikipedii, od najwyższych, z limitem
     na obszar mapy, żeby nie wybrać samych Tatr.

W CSV (separator ";", otwiera się w Excelu) uzupełniasz ręcznie:
  - pasmo       – OSM go nie podaje (dla KGP jest wpisane z listy),
  - uwzglednij  – "tak" / "nie": czy szczyt trafi do aplikacji; tak wybierasz też
                  najwyższe szczyty pasm, których brakuje.

Ponowne uruchomienie NIE kasuje ręcznych zmian: nazwa, pasmo i uwzglednij są przepisywane
z istniejącego pliku dla tych samych id.
"""

import csv
import json
import os
import sys

from czyszczenie import normalizuj, odleglosc_m, parsuj_wysokosc, usun_duplikaty, wybierz_nazwe, z_elementu_osm
from kgp import KGP, PROMIEN_SZUKANIA_M, TOLERANCJA_WYSOKOSCI_M
from popularne import POPULARNE

TU = os.path.dirname(os.path.abspath(__file__))
SUROWE = os.path.join(TU, "surowe")
CSV_PLIK = os.path.join(TU, "szczyty.csv")
OBECNY_JSON = os.path.join(TU, "..", "..", "app", "src", "main", "assets", "peaks", "peaks.json")

# Ilu kandydatów trafia do CSV i ilu z nich jest domyślnie zaznaczonych.
LICZBA_KANDYDATOW = 350
LICZBA_WYBRANYCH = 200

# Tatry mają ponad sto nazwanych wierzchołków i wybór "po wysokości" zawierałby prawie same
# Tatry. Dlatego dzielimy mapę na kratki ok. 20×20 km i z jednej kratki bierzemy najwyżej
# tyle szczytów (KGP i popularne nie wliczają się do limitu).
KRATKA_LAT = 0.2
KRATKA_LON = 0.3
LIMIT_NA_KRATKE = 5

# pasmo_wikidata wypełnia opcjonalny skrypt pasma_wikidata.py (podpowiedź do ręcznego sprawdzenia).
KOLUMNY = ["id", "nazwa", "wysokosc", "lat", "lon", "kgp", "pasmo", "pasmo_wikidata", "uwzglednij", "wikidata", "uwagi"]


def wczytaj_elementy(nazwa):
    sciezka = os.path.join(SUROWE, nazwa)
    if not os.path.exists(sciezka):
        sys.exit("Brak pliku {} – najpierw uruchom: python pobierz.py".format(sciezka))
    with open(sciezka, encoding="utf-8") as f:
        return json.load(f).get("elements", [])


def znajdz_wezel(elementy, nazwy, lat, lon):
    """Węzeł o jednej z podanych nazw, najbliższy punktowi (lat, lon), w promieniu PROMIEN_SZUKANIA_M."""
    klucze = {normalizuj(n) for n in nazwy}
    najlepszy, najblizej = None, None
    for e in elementy:
        tagi = e.get("tags", {})
        nazwy_wezla = {normalizuj(tagi.get(k, "")) for k in ("name", "name:pl", "official_name")}
        nazwy_wezla.add(normalizuj(wybierz_nazwe(tagi)))
        nazwy_wezla |= {normalizuj(c) for c in tagi.get("name", "").split(" / ")}
        if not klucze & nazwy_wezla:
            continue
        d = odleglosc_m(lat, lon, e["lat"], e["lon"])
        if d <= PROMIEN_SZUKANIA_M and (najblizej is None or d < najblizej):
            najlepszy, najblizej = e, d
    return najlepszy


def szczyt_z_wezla(e, nazwa, uwagi):
    return {
        "id": "node/{}".format(e["id"]),
        # Nazwa z naszej listy: w OSM węzeł przygraniczny może mieć nazwę czeską/słowacką.
        "name": nazwa,
        "elevation": parsuj_wysokosc(e.get("tags", {}).get("ele")),
        "lat": round(float(e["lat"]), 7),
        "lon": round(float(e["lon"]), 7),
        "wikidata": e.get("tags", {}).get("wikidata", ""),
        "uwagi": uwagi,
    }


def dopasuj_kgp(elementy):
    wynik, brakujace = {}, []
    for nazwa, aliasy, pasmo, wys_ref, lat, lon in KGP:
        e = znajdz_wezel(elementy, [nazwa] + aliasy, lat, lon)
        if e is None:
            brakujace.append(nazwa)
            continue
        s = szczyt_z_wezla(e, nazwa, "KGP: sprawdź współrzędne")
        if s["elevation"] is None:
            s["elevation"] = wys_ref
            s["uwagi"] += "; brak ele w OSM, przyjęto {} m z listy KGP".format(wys_ref)
        elif abs(s["elevation"] - wys_ref) > TOLERANCJA_WYSOKOSCI_M:
            s["uwagi"] += "; OSM {} m, lista KGP {} m".format(s["elevation"], wys_ref)
        s["kgp"] = True
        s["range"] = pasmo
        wynik[s["id"]] = s
    return wynik, brakujace


def dopasuj_popularne(elementy, juz_wybrane):
    wynik, brakujace = {}, []
    for nazwa, aliasy, lat, lon in POPULARNE:
        e = znajdz_wezel(elementy, [nazwa] + aliasy, lat, lon)
        if e is None:
            brakujace.append(nazwa)
            continue
        s = szczyt_z_wezla(e, nazwa, "popularny")
        if s["id"] in juz_wybrane:
            continue
        if s["elevation"] is None:
            s["uwagi"] += "; brak ele w OSM – wpisz wysokość"
            s["elevation"] = 0
        wynik[s["id"]] = s
    return wynik, brakujace


def kratka(s):
    return (int(s["lat"] // KRATKA_LAT), int(s["lon"] // KRATKA_LON))


def wybierz_pozostale(szczyty, ile):
    """Szczyty z Wikidanych/Wikipedii (przybliżenie popularności), od najwyższych, z limitem na kratkę."""
    wynik, zajete = [], {}
    kolejka = [s for s in szczyty if s.get("wikidata") or s.get("wikipedia")]
    kolejka.sort(key=lambda s: (-s["elevation"], s["name"]))
    for s in kolejka:
        if len(wynik) >= ile:
            break
        k = kratka(s)
        if zajete.get(k, 0) >= LIMIT_NA_KRATKE:
            continue
        zajete[k] = zajete.get(k, 0) + 1
        wynik.append(s)
    return wynik


def blisko_tego_samego(s, wybrane):
    """Ten sam szczyt co już wybrany: ta sama nazwa i blisko albo ten sam węzeł."""
    return any(
        s["id"] == w["id"]
        or (normalizuj(s["name"]) == normalizuj(w["name"])
            and odleglosc_m(s["lat"], s["lon"], w["lat"], w["lon"]) < PROMIEN_SZUKANIA_M)
        for w in wybrane
    )


def wczytaj_obecny_csv():
    if not os.path.exists(CSV_PLIK):
        return {}
    with open(CSV_PLIK, encoding="utf-8-sig", newline="") as f:
        return {w["id"]: w for w in csv.DictReader(f, delimiter=";")}


def wczytaj_pasma_z_json():
    """Pasma szczytów, które już są w aplikacji – żeby nie wpisywać ich drugi raz."""
    if not os.path.exists(OBECNY_JSON):
        return {}
    with open(OBECNY_JSON, encoding="utf-8") as f:
        return {p["id"]: p.get("range", "") for p in json.load(f).get("peaks", [])}


def main():
    elementy_pl = wczytaj_elementy("osm_polska.json")
    elementy_wybrane = wczytaj_elementy("osm_wybrane.json")

    surowe = [z_elementu_osm(e) for e in elementy_pl]
    odrzucone = sum(1 for s in surowe if s is None)
    szczyty = usun_duplikaty([s for s in surowe if s is not None])
    print("OSM: {} węzłów, odrzucono {} (brak nazwy lub zła wysokość), po usunięciu duplikatów {}".format(
        len(elementy_pl), odrzucone, len(szczyty)))

    wszystkie = elementy_wybrane + elementy_pl
    kgp, brak_kgp = dopasuj_kgp(wszystkie)
    print("KGP: dopasowano {}/28".format(len(kgp)))
    popularne, brak_pop = dopasuj_popularne(wszystkie, kgp)
    print("Popularne: dopasowano {}/{}".format(len(popularne), len(POPULARNE)))
    for nazwa in brak_kgp + brak_pop:
        print("  UWAGA: nie znaleziono w OSM: {} – dopisz ręcznie do CSV".format(nazwa))

    pewne = list(kgp.values()) + list(popularne.values())
    reszta = [s for s in szczyty if not blisko_tego_samego(s, pewne)]
    kandydaci = pewne + wybierz_pozostale(reszta, LICZBA_KANDYDATOW - len(pewne))

    stary_csv = wczytaj_obecny_csv()
    pasma_json = wczytaj_pasma_z_json()
    wiersze = []
    for i, s in enumerate(kandydaci):
        stary = stary_csv.get(s["id"], {})
        uwagi = s.get("uwagi", "")
        if " / " in s["name"]:
            uwagi = "; ".join(x for x in [uwagi, "popraw nazwę"] if x)
        wiersze.append({
            "id": s["id"],
            "nazwa": stary.get("nazwa") or s["name"],
            "wysokosc": s["elevation"],
            "lat": s["lat"],
            "lon": s["lon"],
            "kgp": "tak" if s.get("kgp") else "nie",
            "pasmo": stary.get("pasmo") or s.get("range") or pasma_json.get(s["id"], ""),
            "pasmo_wikidata": stary.get("pasmo_wikidata", ""),
            "uwzglednij": stary.get("uwzglednij") or ("tak" if i < LICZBA_WYBRANYCH else "nie"),
            "wikidata": s.get("wikidata", ""),
            "uwagi": uwagi,
        })

    # Wiersze dopisane ręcznie albo takie, których nie ma już w nowym imporcie – nie gubimy ich.
    nowe_id = {w["id"] for w in wiersze}
    for id_, stary in stary_csv.items():
        if id_ not in nowe_id:
            stary = {k: stary.get(k, "") for k in KOLUMNY}
            if "nie ma w nowym imporcie" not in stary["uwagi"]:
                stary["uwagi"] = "; ".join(x for x in [stary["uwagi"], "nie ma w nowym imporcie"] if x)
            wiersze.append(stary)

    wiersze.sort(key=lambda w: (w["kgp"] != "tak", w["uwzglednij"] != "tak", -int(w["wysokosc"] or 0)))

    with open(CSV_PLIK, "w", encoding="utf-8-sig", newline="") as f:
        pisarz = csv.DictWriter(f, fieldnames=KOLUMNY, delimiter=";")
        pisarz.writeheader()
        pisarz.writerows(wiersze)

    zaznaczone = [w for w in wiersze if w["uwzglednij"] == "tak"]
    print("Zapisano {} ({} wierszy, zaznaczonych {}, w tym bez pasma {})".format(
        CSV_PLIK, len(wiersze), len(zaznaczone), sum(1 for w in zaznaczone if not w["pasmo"])))
    print("Uzupełnij kolumnę pasmo, popraw uwzglednij, potem: python zbuduj_json.py")


if __name__ == "__main__":
    main()
