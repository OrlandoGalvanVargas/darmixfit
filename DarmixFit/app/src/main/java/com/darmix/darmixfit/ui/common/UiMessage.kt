package com.darmix.darmixfit.ui.common

enum class MessageType { SUCCESS, ERROR, INFO }

/**
 * Mensaje efímero para mostrar en un snackbar.
 * Lo emiten los ViewModels y lo consume cualquier pantalla que tenga un SnackbarHost.
 */
data class UiMessage(
    val text: String,
    val type: MessageType = MessageType.INFO,
)