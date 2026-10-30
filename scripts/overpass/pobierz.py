"""Krok 1: pobiera szczyty z Overpass API i zapisuje surowe odpowiedzi w surowe/.

    python pobierz.py

Robi dwa zapytania:
  - osm_polska.json – wszystkie szczyty z nazwą i wysokością w granicach Polski,
  - osm_wybrane.json – szczyty w promieniu kilku km od każdego szczytu KGP i z listy popularnych.
                       Część z nich leży na granicy (Rysy, Śnieżka, Pilsko…) i ich węzeł bywa
                       tuż poza obszarem Polski w OSM, więc pierwsze zapytanie by ich nie znalazło.
"""

import json
import os
import sys
import time
import urllib.parse
import urllib.request

from kgp import KGP, PROMIEN_SZUKANIA_M
from popularne import POPULARNE

OVERPASS_URL = "https://overpass-api.de/api/interpreter"
USER_AGENT = "Szczytomania-import/1.0 (projekt inzynierski)"
KATALOG = os.path.join(os.path.dirname(os.path.abspath(__file__)), "surowe")

ZAPYTANIE_POLSKA = """
[out:json][timeout:120];
area["ISO3166-1"="PL"][admin_level=2]->.pl;
node["natural"="peak"]["name"]["ele"](area.pl);
out body;
"""


def zapytanie_wybrane():
    punkty = [(k[4], k[5]) for k in KGP] + [(p[2], p[3]) for p in POPULARNE]
    czesci = [
        'node["natural"="peak"]["name"](around:{},{},{});'.format(PROMIEN_SZUKANIA_M, lat, lon)
        for (lat, lon) in punkty
    ]
    return "[out:json][timeout:120];\n(\n  " + "\n  ".join(czesci) + "\n);\nout body;\n"


def pobierz(zapytanie, proby=3):
    dane = urllib.parse.urlencode({"data": zapytanie}).encode("utf-8")
    for proba in range(1, proby + 1):
        zadanie = urllib.request.Request(OVERPASS_URL, data=dane, headers={"User-Agent": USER_AGENT})
        try:
            with urllib.request.urlopen(zadanie, timeout=180) as odp:
                return json.loads(odp.read().decode("utf-8"))
        except Exception as e:  # serwer Overpass bywa przeciążony (429/504) – próbujemy ponownie
            print("  próba {}/{} nieudana: {}".format(proba, proby, e), file=sys.stderr)
            if proba == proby:
                raise
            time.sleep(15 * proba)


def zapisz(nazwa, odpowiedz):
    os.makedirs(KATALOG, exist_ok=True)
    sciezka = os.path.join(KATALOG, nazwa)
    with open(sciezka, "w", encoding="utf-8") as f:
        json.dump(odpowiedz, f, ensure_ascii=False)
    print("  zapisano {} ({} elementów)".format(sciezka, len(odpowiedz.get("elements", []))))


def main():
    print("Pobieram szczyty Polski z Overpass API…")
    zapisz("osm_polska.json", pobierz(ZAPYTANIE_POLSKA))
    print("Pobieram okolice szczytów KGP i popularnych…")
    zapisz("osm_wybrane.json", pobierz(zapytanie_wybrane()))
    print("Gotowe. Następny krok: python przygotuj_csv.py")


if __name__ == "__main__":
    main()
