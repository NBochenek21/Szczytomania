"""Korona Gór Polski – 28 najwyższych szczytów pasm górskich Polski.

Współrzędne są przybliżone i służą tylko do odnalezienia właściwego węzła w OSM
(szukamy w promieniu PROMIEN_SZUKANIA_M). Wysokość referencyjna służy do ostrzeżeń,
gdy wartość z OSM mocno odbiega od listy KGP. Ostateczne współrzędne pochodzą z OSM
i wymagają ręcznej weryfikacji – patrz README.
"""

PROMIEN_SZUKANIA_M = 5000

# Różnica wysokości OSM vs lista KGP, powyżej której skrypt ostrzega.
TOLERANCJA_WYSOKOSCI_M = 15

# (nazwa, inne nazwy w OSM, pasmo, wysokość wg KGP, lat, lon)
KGP = [
    ("Rysy", [], "Tatry", 2499, 49.1796, 20.0881),
    ("Babia Góra", ["Diablak"], "Beskid Żywiecki", 1725, 49.5731, 19.5294),
    ("Śnieżka", ["Sněžka"], "Karkonosze", 1603, 50.7360, 15.7398),
    ("Śnieżnik", ["Králický Sněžník"], "Masyw Śnieżnika", 1425, 50.2072, 16.8475),
    ("Tarnica", [], "Bieszczady", 1346, 49.0748, 22.7263),
    ("Turbacz", [], "Gorce", 1310, 49.5428, 20.1114),
    ("Radziejowa", [], "Beskid Sądecki", 1266, 49.4493, 20.6041),
    ("Skrzyczne", [], "Beskid Śląski", 1257, 49.6846, 19.0303),
    ("Mogielica", [], "Beskid Wyspowy", 1171, 49.6553, 20.2767),
    ("Wysoka Kopa", [], "Góry Izerskie", 1126, 50.8506, 15.4206),
    ("Rudawiec", ["Rudný vrch"], "Góry Bialskie", 1106, 50.2440, 16.9750),
    ("Orlica", ["Vrchmezí"], "Góry Orlickie", 1084, 50.3536, 16.3606),
    ("Wysoka", ["Wysokie Skałki", "Vysoké skalky"], "Pieniny", 1050, 49.3803, 20.5550),
    ("Wielka Sowa", [], "Góry Sowie", 1015, 50.6803, 16.4856),
    ("Lackowa", [], "Beskid Niski", 997, 49.4270, 21.0960),
    ("Kowadło", ["Kovadlina"], "Góry Złote", 989, 50.2650, 16.9850),
    ("Jagodna", [], "Góry Bystrzyckie", 977, 50.2520, 16.5650),
    ("Skalnik", [], "Rudawy Janowickie", 945, 50.8086, 15.9017),
    ("Waligóra", [], "Góry Kamienne", 936, 50.6800, 16.2780),
    ("Czupel", [], "Beskid Mały", 933, 49.7680, 19.1600),
    ("Szczeliniec Wielki", [], "Góry Stołowe", 919, 50.4850, 16.3400),
    ("Lubomir", [], "Beskid Makowski", 904, 49.7660, 20.0570),
    ("Biskupia Kopa", ["Biskupská kupa"], "Góry Opawskie", 890, 50.2560, 17.4290),
    ("Chełmiec", [], "Góry Wałbrzyskie", 851, 50.7790, 16.2100),
    ("Kłodzka Góra", [], "Góry Bardzkie", 765, 50.4630, 16.7550),
    ("Skopiec", [], "Góry Kaczawskie", 724, 50.9440, 15.8560),
    ("Ślęża", [], "Masyw Ślęży", 718, 50.8650, 16.7090),
    ("Łysica", [], "Góry Świętokrzyskie", 612, 50.8920, 20.8970),
]

assert len(KGP) == 28, "Korona Gór Polski ma 28 szczytów"
