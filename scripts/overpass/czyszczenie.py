"""Czyszczenie danych o szczytach z OpenStreetMap: wysokość, nazwy, duplikaty."""

import math
import re
import unicodedata

# Rozsądne granice dla polskich gór – wszystko poza nimi to błąd w danych OSM.
MIN_WYSOKOSC = 1
MAX_WYSOKOSC = 2499  # Rysy

# Dwa węzły o tej samej nazwie bliżej niż tyle metrów uznajemy za ten sam szczyt.
PROMIEN_DUPLIKATU_M = 1000

_LICZBA = re.compile(r"-?\d+(?:[.,]\d+)?")


def parsuj_wysokosc(tekst):
    """Zamienia wartość tagu `ele` na liczbę całkowitą metrów albo zwraca None.

    Obsługuje m.in. "1725", "1725 m", "1725,3", "1725.3", "1 725", "1725 m n.p.m.".
    Wartości w stopach, zakresy ("1200-1250") i inne dziwne formaty odrzucamy.
    """
    if tekst is None:
        return None
    t = str(tekst).strip().lower()
    if not t or "ft" in t or "'" in t:
        return None
    # Spacje (także twarde) jako separator tysięcy: "1 725" -> "1725".
    t = re.sub(r"(?<=\d)[\s  ](?=\d{3}\b)", "", t)
    liczby = _LICZBA.findall(t)
    if len(liczby) != 1:
        return None
    try:
        wartosc = float(liczby[0].replace(",", "."))
    except ValueError:
        return None
    wysokosc = int(math.floor(wartosc + 0.5))  # zaokrąglenie "szkolne", nie bankierskie
    if not MIN_WYSOKOSC <= wysokosc <= MAX_WYSOKOSC:
        return None
    return wysokosc


def normalizuj(tekst):
    """Małe litery bez polskich znaków: "Śnieżnik" -> "snieznik". Do porównywania nazw."""
    t = unicodedata.normalize("NFD", tekst.strip().lower())
    t = "".join(z for z in t if unicodedata.category(z) != "Mn")
    # "ł" nie rozkłada się na literę + znak diakrytyczny, więc zamieniamy ją ręcznie.
    return t.replace("ł", "l")


def odleglosc_m(lat1, lon1, lat2, lon2):
    """Odległość po powierzchni Ziemi (wzór haversine), w metrach."""
    r = 6371000.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp = p2 - p1
    dl = math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(a))


_POLSKIE = set("ąćęłńóśźż")
_CZESKIE_SLOWACKIE = set("áäčďéěíĺľňôŕřšťúůýž") - _POLSKIE


def wybierz_nazwe(tagi):
    """Polska nazwa szczytu.

    Szczyty przygraniczne mają w OSM nazwy dwujęzyczne, np. "Kasprov vrch / Kasprowy Wierch".
    Najpierw bierzemy tag name:pl, a gdy go nie ma – tę część nazwy, która wygląda na polską
    (polskie litery, brak czeskich/słowackich, bez cyrylicy). Gdy nie da się zdecydować,
    zostaje pełna nazwa do ręcznej poprawki w CSV.
    """
    nazwa_pl = (tagi.get("name:pl") or "").strip()
    if nazwa_pl:
        return nazwa_pl
    nazwa = (tagi.get("name") or "").strip()
    if " / " not in nazwa:
        return nazwa

    def ocena(czesc):
        male = czesc.lower()
        if any("Ѐ" <= z <= "ӿ" for z in male):  # cyrylica
            return -100
        return sum(z in _POLSKIE for z in male) - sum(z in _CZESKIE_SLOWACKIE for z in male)

    czesci = [c.strip() for c in nazwa.split(" / ") if c.strip()]
    oceny = sorted(((ocena(c), c) for c in czesci), reverse=True)
    if len(oceny) > 1 and oceny[0][0] == oceny[1][0]:
        return nazwa
    return oceny[0][1]


def z_elementu_osm(element):
    """Element z odpowiedzi Overpass -> słownik szczytu albo None, gdy brak nazwy lub wysokości."""
    tagi = element.get("tags", {})
    nazwa = wybierz_nazwe(tagi)
    wysokosc = parsuj_wysokosc(tagi.get("ele"))
    if not nazwa or wysokosc is None or "lat" not in element or "lon" not in element:
        return None
    return {
        "id": "node/{}".format(element["id"]),
        "name": nazwa,
        "elevation": wysokosc,
        "lat": round(float(element["lat"]), 7),
        "lon": round(float(element["lon"]), 7),
        "wikidata": tagi.get("wikidata", ""),
        "wikipedia": tagi.get("wikipedia", ""),
    }


def _waga(szczyt):
    """Który z duplikatów zostawić: najpierw ten opisany w Wikidanych/Wikipedii, potem wyższy."""
    return (bool(szczyt.get("wikidata")) or bool(szczyt.get("wikipedia")), szczyt["elevation"])


def usun_duplikaty(szczyty, promien_m=PROMIEN_DUPLIKATU_M):
    """Usuwa powtórzone id oraz węzły o tej samej nazwie leżące blisko siebie.

    Samej nazwy nie wystarczy porównać – w Polsce jest wiele różnych szczytów o nazwie
    "Kopa" czy "Łysica", więc duplikat to ta sama nazwa ORAZ odległość poniżej `promien_m`.
    """
    po_id = {}
    for s in szczyty:
        po_id.setdefault(s["id"], s)

    wynik = []
    for s in sorted(po_id.values(), key=_waga, reverse=True):
        klucz = normalizuj(s["name"])
        duplikat = any(
            normalizuj(z["name"]) == klucz
            and odleglosc_m(s["lat"], s["lon"], z["lat"], z["lon"]) < promien_m
            for z in wynik
        )
        if not duplikat:
            wynik.append(s)
    return wynik
