package com.alongside.app.capture

import androidx.compose.runtime.Composable

@Composable
internal actual fun rememberPhotoPickerLauncher(onPick: (List<String>) -> Unit): () -> Unit = {}
