"""Opcjonalny krok 2b: podpowiedzi pasm z Wikidanych.

    python pasma_wikidata.py

Dla wierszy szczyty.csv z wypełnioną kolumną wikidata pobiera cechę P4552 ("pasmo górskie")
i wpisuje jej polską nazwę do kolumny pasmo_wikidata. Kolumny pasmo NIE zmienia – podpowiedź
trzeba sprawdzić i przepisać ręcznie. Wikidane bywają bardziej szczegółowe niż podział
używany w KGP (np. "Tatry Wysokie" zamiast "Tatry"), więc warto ujednolicić nazwy.
"""

import csv
import json
import sys
import time
import urllib.parse
import urllib.request

from przygotuj_csv import CSV_PLIK, KOLUMNY

SPARQL_URL = "https://query.wikidata.org/sparql"
USER_AGENT = "Szczytomania-import/1.0 (projekt inzynierski)"
PACZKA = 150  # ile identyfikatorów w jednym zapytaniu


def zapytanie(ids):
    wartosci = " ".join("wd:" + i for i in ids)
    return (
        "SELECT ?item ?rangeLabel WHERE {\n"
        "  VALUES ?item { " + wartosci + " }\n"
        "  ?item wdt:P4552 ?range .\n"
        '  SERVICE wikibase:label { bd:serviceParam wikibase:language "pl,en". }\n'
        "}"
    )


def pobierz(ids, proby=3):
    dane = urllib.parse.urlencode({"query": zapytanie(ids), "format": "json"}).encode("utf-8")
    for proba in range(1, proby + 1):
        zadanie = urllib.request.Request(SPARQL_URL, data=dane, headers={
            "User-Agent": USER_AGENT,
            "Accept": "application/sparql-results+json",
        })
        try:
            with urllib.request.urlopen(zadanie, timeout=120) as odp:
                return json.loads(odp.read().decode("utf-8"))["results"]["bindings"]
        except Exception as e:  # serwer Wikidanych ogranicza liczbę zapytań – próbujemy ponownie
            print("  próba {}/{} nieudana: {}".format(proba, proby, e), file=sys.stderr)
            if proba == proby:
                raise
            time.sleep(10 * proba)


def main():
    with open(CSV_PLIK, encoding="utf-8-sig", newline="") as f:
        wiersze = list(csv.DictReader(f, delimiter=";"))

    ids = sorted({w["wikidata"].strip() for w in wiersze if w.get("wikidata", "").strip().startswith("Q")})
    print("Pobieram pasma z Wikidanych dla {} szczytów…".format(len(ids)))

    pasma = {}
    for i in range(0, len(ids), PACZKA):
        for wynik in pobierz(ids[i:i + PACZKA]):
            qid = wynik["item"]["value"].rsplit("/", 1)[-1]
            nazwa = wynik["rangeLabel"]["value"]
            # Brak etykiety pl/en – usługa zwraca sam identyfikator, który nic nie mówi.
            if not (nazwa.startswith("Q") and nazwa[1:].isdigit()):
                pasma.setdefault(qid, [])
                if nazwa not in pasma[qid]:
                    pasma[qid].append(nazwa)
        time.sleep(1)

    for w in wiersze:
        w["pasmo_wikidata"] = " | ".join(pasma.get(w.get("wikidata", "").strip(), []))

    with open(CSV_PLIK, "w", encoding="utf-8-sig", newline="") as f:
        pisarz = csv.DictWriter(f, fieldnames=KOLUMNY, delimiter=";", extrasaction="ignore")
        pisarz.writeheader()
        pisarz.writerows([{k: w.get(k, "") for k in KOLUMNY} for w in wiersze])

    zaznaczone_bez_pasma = [w for w in wiersze if w["uwzglednij"] == "tak" and not w["pasmo"]]
    z_podpowiedzia = sum(1 for w in zaznaczone_bez_pasma if w["pasmo_wikidata"])
    print("Zaznaczone szczyty bez pasma: {}, z podpowiedzią z Wikidanych: {}".format(
        len(zaznaczone_bez_pasma), z_podpowiedzia))
    print("Sprawdź kolumnę pasmo_wikidata, przepisz pasma do kolumny pasmo, potem: python zbuduj_json.py")


if __name__ == "__main__":
    main()
