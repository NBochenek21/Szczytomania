"""Popularne szczyty turystyczne spoza KGP, które zawsze trafiają do aplikacji.

OSM nie mówi nic o popularności, więc tę listę prowadzimy ręcznie – można ją dowolnie
zmieniać. Współrzędne są przybliżone: skrypt szuka węzła o pasującej nazwie w promieniu
PROMIEN_SZUKANIA_M (z kgp.py). Szczyty przygraniczne (Pilsko, Wielka Racza…) bywają
w OSM tuż za granicą albo pod nazwą słowacką/czeską – dlatego aliasy.
"""

# (nazwa w aplikacji, inne nazwy w OSM, lat, lon)
POPULARNE = [
    ("Giewont", ["Wielki Giewont"], 49.2510, 19.9340),
    ("Kasprowy Wierch", ["Kasprov vrch"], 49.2318, 19.9817),
    ("Kościelec", [], 49.2253, 20.0145),
    ("Świnica", ["Svinica"], 49.2195, 20.0090),
    ("Kopa Kondracka", ["Kondratova kopa"], 49.2370, 19.9320),
    ("Wołowiec", ["Volovec"], 49.2075, 19.7615),
    ("Starorobociański Wierch", ["Klin"], 49.1960, 19.8040),
    ("Trzy Korony", ["Trzy Korony (Okrąglica)", "Okrąglica"], 49.4140, 20.4140),
    ("Sokolica", [], 49.4180, 20.4400),
    ("Pilsko", [], 49.5440, 19.3180),
    ("Wielka Racza", ["Veľká Rača"], 49.4130, 18.9690),
    ("Barania Góra", [], 49.6110, 19.0110),
    ("Klimczok", [], 49.7400, 18.9950),
    ("Szyndzielnia", [], 49.7520, 19.0000),
    ("Równica", [], 49.7250, 18.8570),
    ("Czantoria Wielka", ["Velká Čantoryje", "Veľká Čantoria"], 49.6800, 18.8040),
    ("Leskowiec", [], 49.7890, 19.4430),
    ("Luboń Wielki", [], 49.6540, 19.9920),
    ("Lubań", [], 49.4890, 20.3390),
    ("Gorc", [], 49.5650, 20.2530),
    ("Ćwilin", [], 49.6890, 20.1910),
    ("Jaworzyna Krynicka", [], 49.4180, 20.8960),
    ("Wielka Rawka", [], 49.1000, 22.5760),
    ("Halicz", [], 49.0720, 22.7690),
    ("Smerek", [], 49.1860, 22.4800),
    ("Szrenica", [], 50.7910, 15.5140),
    ("Wielki Szyszak", ["Vysoké kolo"], 50.7770, 15.5680),
    ("Chojnik", [], 50.8340, 15.6440),
    ("Łysiec", ["Łysa Góra", "Święty Krzyż"], 50.8620, 21.0470),
]
