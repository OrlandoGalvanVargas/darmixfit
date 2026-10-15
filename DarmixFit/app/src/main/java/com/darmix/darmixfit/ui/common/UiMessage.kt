package com.darmix.darmixfit.ui.common

enum class MessageType { SUCCESS, ERROR, INFO }

data class UiMessage(
    val text: String,
    val type: MessageType = MessageType.INFO
)
