package com.alongside.app.capture

import androidx.compose.runtime.Composable

/**
 * Manual-capture entry point: launches the platform's file picker and calls [onPick] with the
 * selected content URIs (as strings). Android-only real implementation
 * (`ActivityResultContracts.OpenMultipleDocuments` - see the actual's KDoc for why); other
 * targets have no picker wired yet, so the launcher is a no-op there.
 */
@Composable
internal expect fun rememberPhotoPickerLauncher(onPick: (List<String>) -> Unit): () -> Unit
