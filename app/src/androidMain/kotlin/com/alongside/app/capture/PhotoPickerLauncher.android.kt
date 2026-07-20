package com.alongside.app.capture

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * `ActivityResultContracts.PickMultipleVisualMedia` (Android's unified Photo Picker) strips GPS
 * EXIF from returned URIs for privacy, regardless of ACCESS_MEDIA_LOCATION - confirmed empirically
 * during the M12.5 manual smoke test (a photo with real GPS in its raw bytes read back with null
 * latLong through that picker). `OpenMultipleDocuments` (SAF, routes through the Files app) reads
 * back the real, unredacted EXIF.
 */
@Composable
internal actual fun rememberPhotoPickerLauncher(onPick: (List<String>) -> Unit): () -> Unit {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) {
                onPick(uris.map { it.toString() })
            }
        }
    return { launcher.launch(arrayOf("image/*")) }
}
