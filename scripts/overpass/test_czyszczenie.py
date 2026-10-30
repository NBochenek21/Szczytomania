"""Testy czyszczenia danych. Uruchomienie: python -m unittest -v (w katalogu scripts/overpass)."""

import unittest

from czyszczenie import normalizuj, odleglosc_m, parsuj_wysokosc, usun_duplikaty, wybierz_nazwe, z_elementu_osm
from zbuduj_json import wiersz_na_szczyt


class ParsujWysokoscTest(unittest.TestCase):

    def test_formaty_z_osm(self):
        self.assertEqual(parsuj_wysokosc("1725"), 1725)
        self.assertEqual(parsuj_wysokosc("1725 m"), 1725)
        self.assertEqual(parsuj_wysokosc("1725m"), 1725)
        self.assertEqual(parsuj_wysokosc("1725,3"), 1725)
        self.assertEqual(parsuj_wysokosc("1725.6"), 1726)
        self.assertEqual(parsuj_wysokosc("1 725"), 1725)
        self.assertEqual(parsuj_wysokosc("1725 m n.p.m."), 1725)
        self.assertEqual(parsuj_wysokosc(" 612 "), 612)

    def test_zaokraglenie_polowki_w_gore(self):
        self.assertEqual(parsuj_wysokosc("1170.5"), 1171)

    def test_odrzuca_bledne(self):
        for zle in [None, "", "brak", "5000 ft", "1200-1250", "0", "3000", "-5"]:
            self.assertIsNone(parsuj_wysokosc(zle), zle)


class NormalizujTest(unittest.TestCase):

    def test_polskie_znaki(self):
        self.assertEqual(normalizuj("Śnieżnik"), "snieznik")
        self.assertEqual(normalizuj("Łysica"), "lysica")
        self.assertEqual(normalizuj("  Kłodzka GÓRA "), "klodzka gora")


class WybierzNazweTest(unittest.TestCase):

    def test_name_pl_ma_pierwszenstwo(self):
        self.assertEqual(wybierz_nazwe({"name": "Svinica / Świnica", "name:pl": "Świnica"}), "Świnica")

    def test_polska_czesc_nazwy_dwujezycznej(self):
        self.assertEqual(wybierz_nazwe({"name": "Veľká Rača / Wielka Racza"}), "Wielka Racza")
        self.assertEqual(wybierz_nazwe({"name": "Babia Góra / Babia hora"}), "Babia Góra")
        self.assertEqual(wybierz_nazwe({"name": "Rozsypaniec / Розсипанець"}), "Rozsypaniec")

    def test_nierozstrzygniete_zostaja_w_calosci(self):
        self.assertEqual(wybierz_nazwe({"name": "Kasprov vrch / Kasprowy Wierch"}), "Kasprov vrch / Kasprowy Wierch")

    def test_zwykla_nazwa(self):
        self.assertEqual(wybierz_nazwe({"name": "Turbacz"}), "Turbacz")


class ZElementuOsmTest(unittest.TestCase):

    def test_poprawny_element(self):
        s = z_elementu_osm({"id": 1, "lat": 49.5, "lon": 20.1, "tags": {"name": "Turbacz", "ele": "1310 m"}})
        self.assertEqual(s["id"], "node/1")
        self.assertEqual(s["elevation"], 1310)

    def test_odrzuca_bez_nazwy_lub_wysokosci(self):
        self.assertIsNone(z_elementu_osm({"id": 1, "lat": 49.5, "lon": 20.1, "tags": {"ele": "1310"}}))
        self.assertIsNone(z_elementu_osm({"id": 1, "lat": 49.5, "lon": 20.1, "tags": {"name": "X", "ele": "?"}}))
        self.assertIsNone(z_elementu_osm({"id": 1, "lat": 49.5, "lon": 20.1, "tags": {"name": " ", "ele": "500"}}))


def szczyt(id_, nazwa, lat, lon, wys=1000, wikidata=""):
    return {"id": id_, "name": nazwa, "elevation": wys, "lat": lat, "lon": lon, "wikidata": wikidata}


class UsunDuplikatyTest(unittest.TestCase):

    def test_ta_sama_nazwa_blisko_to_duplikat(self):
        wynik = usun_duplikaty([
            szczyt("node/1", "Turbacz", 49.5428, 20.1114, 1310),
            szczyt("node/2", "turbacz", 49.5430, 20.1120, 1305, wikidata="Q1"),
        ])
        # Zostaje węzeł opisany w Wikidanych.
        self.assertEqual([s["id"] for s in wynik], ["node/2"])

    def test_ta_sama_nazwa_daleko_to_rozne_szczyty(self):
        wynik = usun_duplikaty([
            szczyt("node/1", "Kopa", 50.0, 16.0),
            szczyt("node/2", "Kopa", 49.5, 20.0),
        ])
        self.assertEqual(len(wynik), 2)

    def test_powtorzone_id(self):
        wynik = usun_duplikaty([szczyt("node/1", "A", 50.0, 16.0), szczyt("node/1", "A", 50.0, 16.0)])
        self.assertEqual(len(wynik), 1)

    def test_odleglosc(self):
        # Rysy – Turbacz: ok. 40 km.
        self.assertAlmostEqual(odleglosc_m(49.1796, 20.0881, 49.5428, 20.1114) / 1000, 40.4, delta=1)


class WierszNaSzczytTest(unittest.TestCase):

    def wiersz(self, **zmiany):
        w = {"id": "node/1", "nazwa": "Tarnica", "wysokosc": "1346", "lat": "49.07", "lon": "22.72",
             "kgp": "tak", "pasmo": "Bieszczady"}
        w.update(zmiany)
        return w

    def test_poprawny(self):
        s, powod = wiersz_na_szczyt(self.wiersz())
        self.assertIsNone(powod)
        self.assertTrue(s["kgp"])
        self.assertEqual(s["range"], "Bieszczady")

    def test_brak_pasma(self):
        self.assertEqual(wiersz_na_szczyt(self.wiersz(pasmo=" "))[1], "brak pasma")

    def test_poza_polska(self):
        self.assertEqual(wiersz_na_szczyt(self.wiersz(lat="48.5"))[1], "współrzędne poza Polską")


if __name__ == "__main__":
    unittest.main()
