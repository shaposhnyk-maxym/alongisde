package com.alongside.core.domain.maps

/**
 * Seam over the platform's system maps app (docs/roadmap.md M21.6) - a plain interface, not
 * `expect`/`actual`, the same cross-platform pattern as
 * [com.alongside.core.domain.work.BackgroundWorkScheduler]: platform implementations are
 * injected via Koin at the composition root.
 */
public interface MapsLauncher {
    /** Opens [latitude]/[longitude] (labeled [name]) in whatever maps app (or browser) is installed. */
    public fun openMaps(
        latitude: Double,
        longitude: Double,
        name: String,
    )
}
