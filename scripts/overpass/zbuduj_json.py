"""Krok 3: z szczyty.csv buduje plik aplikacji app/src/main/assets/peaks/peaks.json.

    python zbuduj_json.py

Do pliku trafiają wiersze z uwzglednij = "tak". Wiersze bez pasma są pomijane (z ostrzeżeniem),
bo aplikacja wymaga pasma – sprawdza to test PeaksAssetTest.
"""

import csv
import json
import os
import re
import sys

TU = os.path.dirname(os.path.abspath(__file__))
CSV_PLIK = os.path.join(TU, "szczyty.csv")
JSON_PLIK = os.path.join(TU, "..", "..", "app", "src", "main", "assets", "peaks", "peaks.json")

WERSJA = 1
ZRODLO = "© OpenStreetMap contributors (ODbL)"
OSM_ID = re.compile(r"^(node|way|relation)/\d+$")
# Te same granice co w PeaksAssetTest.
LAT = (49.0, 54.9)
LON = (14.1, 24.2)


def wiersz_na_szczyt(w):
    """Zwraca (szczyt, None) albo (None, powód odrzucenia)."""
    id_ = w["id"].strip()
    nazwa = w["nazwa"].strip()
    pasmo = w["pasmo"].strip()
    if not OSM_ID.match(id_):
        return None, "złe id"
    if not nazwa:
        return None, "brak nazwy"
    if not pasmo:
        return None, "brak pasma"
    try:
        wysokosc = int(w["wysokosc"])
        lat = float(str(w["lat"]).replace(",", "."))
        lon = float(str(w["lon"]).replace(",", "."))
    except ValueError:
        return None, "zła liczba"
    if not 1 <= wysokosc <= 2499:
        return None, "wysokość {} m".format(wysokosc)
    if not (LAT[0] <= lat <= LAT[1] and LON[0] <= lon <= LON[1]):
        return None, "współrzędne poza Polską"
    return {
        "id": id_,
        "name": nazwa,
        "elevation": wysokosc,
        "lat": lat,
        "lon": lon,
        "range": pasmo,
        "kgp": w["kgp"].strip().lower() == "tak",
    }, None


def do_json(szczyty):
    """Jeden szczyt w linii – łatwiej przeglądać zmiany w pull requeście."""
    linie = [json.dumps(s, ensure_ascii=False, separators=(", ", ": ")) for s in szczyty]
    return (
        "{\n"
        '  "version": ' + str(WERSJA) + ",\n"
        '  "source": ' + json.dumps(ZRODLO, ensure_ascii=False) + ",\n"
        '  "peaks": [\n    ' + ",\n    ".join(linie) + "\n  ]\n}\n"
    )


def main():
    if not os.path.exists(CSV_PLIK):
        sys.exit("Brak {} – najpierw uruchom: python przygotuj_csv.py".format(CSV_PLIK))
    with open(CSV_PLIK, encoding="utf-8-sig", newline="") as f:
        wiersze = [w for w in csv.DictReader(f, delimiter=";") if w["uwzglednij"].strip().lower() == "tak"]

    szczyty, odrzucone = [], []
    for w in wiersze:
        s, powod = wiersz_na_szczyt(w)
        if s:
            szczyty.append(s)
        else:
            odrzucone.append((w["nazwa"], powod))

    ids = [s["id"] for s in szczyty]
    powtorzone = {i for i in ids if ids.count(i) > 1}
    if powtorzone:
        sys.exit("Powtórzone id w CSV: {}".format(", ".join(sorted(powtorzone))))

    szczyty.sort(key=lambda s: (-s["elevation"], s["name"]))
    with open(JSON_PLIK, "w", encoding="utf-8", newline="\n") as f:
        f.write(do_json(szczyty))

    kgp = sum(1 for s in szczyty if s["kgp"])
    print("Zapisano {}: {} szczytów, w tym KGP {}/28".format(os.path.normpath(JSON_PLIK), len(szczyty), kgp))
    if odrzucone:
        print("Pominięto {} zaznaczonych wierszy:".format(len(odrzucone)))
        for nazwa, powod in odrzucone:
            print("  - {}: {}".format(nazwa, powod))

    pasma = {}
    for s in szczyty:
        pasma.setdefault(s["range"], []).append(s)
    print("Pasma ({}), najwyższy szczyt w każdym:".format(len(pasma)))
    for pasmo, lista in sorted(pasma.items(), key=lambda p: -p[1][0]["elevation"]):
        print("  {:<28} {:>3} szt.  {} ({} m)".format(pasmo, len(lista), lista[0]["name"], lista[0]["elevation"]))


if __name__ == "__main__":
    main()
