package com.alongside.feature.diary.capture

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

public class AndroidPhotoByteReader(
    private val contentResolver: ContentResolver,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PhotoByteReader {
    override suspend fun readBytes(uri: String): ByteArray =
        withContext(dispatcher) {
            contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() }
                ?: error("Unable to open photo URI: $uri")
        }
}
