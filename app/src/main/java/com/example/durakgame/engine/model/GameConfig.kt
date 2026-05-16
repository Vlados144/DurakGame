package com.example.durakgame.engine.model

data class GameConfig(
    val gameMode: GameMode = GameMode.PODKIDNOY,
    val maxPlayers: Int = 2,
    val betAmount: Long = 0,
    val turnTimerSeconds: Int = 0,
    val deckSize: DeckSize = DeckSize.STANDARD_36
) {
    enum class GameMode(val displayName: String) {
        PODKIDNOY("Подкидной"),
        PEREVODNOY("Переводной")
    }

    enum class DeckSize(val cards: Int) {
        STANDARD_36(36),
        FULL_52(52)
    }
}