package com.mythos.mythos

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val text: String,
    val sender: Sender,
    val isLoading: Boolean = false
)

// Esta anotación es CLAVE para que la librería pueda convertir esto a JSON
@Serializable
data class GameSession(
    // METADATA de la partida
    var metadata: GameMetadata,

    // HISTORIAL de la partida
    val history: MutableList<ChatMessage>
)

@Serializable
data class GameMetadata(
    var gameId: String, // Un ID único para la partida
    var gameName: String, // "La Sombra sobre Aerthos" (Generado por IA)
    val userId: String, // <-- NUEVO CAMPO: El ID del usuario dueño de la partida
    var summary: String, // "Un caballero caído que explora un templo..." (Generado por IA)
    var lastUpdated: Long, // Para saber cuándo se guardó por última vez

    // Parámetros elegidos por el usuario
    val userContext: String,
    val userCharacter: String,
    val tone: Int,
    val dialogueStyle: Int
)

