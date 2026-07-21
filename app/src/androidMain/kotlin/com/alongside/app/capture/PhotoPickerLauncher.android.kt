package com.alongside.app.capture

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * `ActivityResultContracts.PickMultipleVisualMedia` (Android's unified Photo Picker) strips GPS
 * EXIF from returned URIs for privacy, regardless of ACCESS_MEDIA_LOCATION - confirmed empirically
 * during the M12.5 manual smoke test (a photo with real GPS in its raw bytes read back with null
 * latLong through that picker). `OpenMultipleDocuments` (SAF, routes through the Files app) reads
 * back the real, unredacted EXIF.
 *
 * `OpenMultipleDocuments`'s grant is transient (tied to this task) unless taken as persistable
 * right here, synchronously on return - a background retry (WorkManager, possibly in a freshly
 * restarted process after the app was killed) otherwise hits a SecurityException reading the URI,
 * exactly the failure M12.11's manual retest surfaced.
 */
@Composable
internal actual fun rememberPhotoPickerLauncher(onPick: (List<String>) -> Unit): () -> Unit {
    val contentResolver = LocalContext.current.contentResolver
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) {
                uris.forEach { uri ->
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                onPick(uris.map { it.toString() })
            }
        }
    return { launcher.launch(arrayOf("image/*")) }
}
