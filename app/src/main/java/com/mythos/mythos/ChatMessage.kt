package com.mythos.mythos

data class ChatMessage(
    val text: String,
    val sender: Sender,
    var isLoading: Boolean = false // To show a loading indicator for AI responses
)

enum class Sender {
    USER, MODEL
}