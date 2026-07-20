package com.alongside.core.ui.component

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory

/**
 * Installs the shared [ImageLoader] every [AsyncPhotoTile] draws through - reuses this project's
 * existing Ktor stack for the network fetcher instead of pulling in a second HTTP client (OkHttp)
 * just for images. Call once, as early as possible (Application.onCreate on Android).
 */
public fun installAlongsideImageLoader(context: PlatformContext) {
    SingletonImageLoader.setSafe {
        ImageLoader
            .Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }
}
