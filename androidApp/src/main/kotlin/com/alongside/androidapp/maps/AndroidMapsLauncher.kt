package com.alongside.androidapp.maps

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.alongside.core.domain.maps.MapsLauncher

/**
 * Tries the `geo:` scheme first - resolves to whatever maps app is installed, and carries [name]
 * as the marker label, which the universal link's coordinate-only query string can't. Falls back
 * to the universal `google.com/maps` web link (no custom scheme, no
 * `LSApplicationQueriesSchemes`-style manifest entry needed) if nothing on the device handles
 * `geo:` at all - e.g. a bare AOSP emulator image with no Maps app installed.
 */
public class AndroidMapsLauncher(
    private val context: Context,
) : MapsLauncher {
    override fun openMaps(
        latitude: Double,
        longitude: Double,
        name: String,
    ) {
        val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(name)})")
        val geoIntent = Intent(Intent.ACTION_VIEW, geoUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(geoIntent)
        } catch (ignored: ActivityNotFoundException) {
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
