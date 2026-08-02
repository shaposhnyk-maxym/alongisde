package com.alongside.app.maps

import com.alongside.core.domain.maps.MapsLauncher
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * Opens the universal Google Maps web link - no `LSApplicationQueriesSchemes` entry needed
 * (unlike a custom `comgooglemaps://` scheme), and iOS opens it in whatever maps app is
 * installed, or Safari if none is. [name] isn't part of this URL format (`google.com/maps`'s
 * coordinate-only query string has no separate label param) - kept on the signature only to
 * match [MapsLauncher] and [com.alongside.androidapp.maps.AndroidMapsLauncher], whose `geo:`
 * scheme does use it as the marker label.
 *
 * `openURL(url:)` (single-arg) is deprecated and, confirmed live on iOS 18 Simulator, now a
 * silent no-op ("BUG IN CLIENT OF UIKIT... Force returning false") rather than merely a
 * deprecation warning - `openURL(url:options:completionHandler:)` is the one that actually
 * still works.
 */
public class IosMapsLauncher : MapsLauncher {
    override fun openMaps(
        latitude: Double,
        longitude: Double,
        name: String,
    ) {
        val url = NSURL(string = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
    }
}
