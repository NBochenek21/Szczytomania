package pl.szczytomania.map.offline

/**
 * Uproszczony kontur Polski (długość, szerokość) do mapki na ekranie "Mapy offline".
 * Granica lądowa z OpenStreetMap (Nominatim, uproszczenie 0,06°), wybrzeże dorysowane ręcznie.
 */
object PolandOutline {
    val points: List<Pair<Double, Double>> = listOf(
        14.24 to 53.99, 14.19 to 53.91, 14.45 to 53.26, 14.35 to 53.05, 14.14 to 52.96, 14.12 to 52.84,
        14.64 to 52.57, 14.53 to 52.39, 14.72 to 52.24, 14.68 to 52.12, 14.76 to 52.07, 14.59 to 51.82,
        14.76 to 51.66, 14.73 to 51.53, 14.95 to 51.47, 15.04 to 51.27, 14.82 to 50.87, 15.00 to 50.87,
        14.99 to 51.01, 15.17 to 51.02, 15.37 to 50.78, 15.82 to 50.76, 16.02 to 50.60, 16.34 to 50.66,
        16.44 to 50.58, 16.20 to 50.43, 16.63 to 50.11, 17.03 to 50.23, 16.90 to 50.45, 17.35 to 50.26,
        17.69 to 50.33, 17.76 to 50.24, 17.60 to 50.17, 17.65 to 50.11, 17.87 to 49.97, 18.03 to 50.07,
        18.32 to 49.92, 18.57 to 49.92, 18.63 to 49.72, 18.81 to 49.68, 18.98 to 49.40, 19.19 to 49.41,
        19.47 to 49.61, 19.63 to 49.41, 19.79 to 49.41, 19.76 to 49.21, 20.08 to 49.18, 20.33 to 49.40,
        20.72 to 49.42, 20.92 to 49.30, 21.12 to 49.44, 21.63 to 49.45, 21.96 to 49.35, 22.06 to 49.21,
        22.86 to 49.00, 22.89 to 49.09, 22.71 to 49.17, 22.75 to 49.36, 22.64 to 49.53, 23.28 to 50.10,
        24.07 to 50.50, 24.08 to 50.71, 23.96 to 50.79, 24.15 to 50.87, 23.65 to 51.29, 23.70 to 51.42,
        23.54 to 51.60, 23.65 to 52.07, 23.18 to 52.28, 23.47 to 52.55, 23.94 to 52.71, 23.92 to 53.16,
        23.55 to 53.77, 23.49 to 54.15, 23.01 to 54.38, 21.45 to 54.32, 19.65 to 54.45,
        // Wybrzeże: Zalew Wiślany, Zatoka Gdańska, Rozewie, Łeba, Ustka, Kołobrzeg, Świnoujście.
        18.95 to 54.35, 18.55 to 54.50, 18.33 to 54.83, 17.55 to 54.77, 16.85 to 54.59, 15.55 to 54.18,
        14.70 to 54.02,
    )
}
