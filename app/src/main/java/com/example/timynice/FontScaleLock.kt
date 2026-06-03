package com.example.timynice

import android.content.Context
import android.content.res.Configuration

/**
 * Freezes font scale to the value at first app launch in this process.
 * Keeps the layout exactly as on the user's device before system font-size changes.
 */
object FontScaleLock {
    @Volatile
    private var lockedScale: Float? = null

    fun ensureLocked(systemFontScale: Float) {
        if (lockedScale == null) {
            lockedScale = systemFontScale
        }
    }

    val scale: Float
        get() = lockedScale ?: 1f

    fun wrapContext(base: Context): Context {
        ensureLocked(base.resources.configuration.fontScale)
        val config = Configuration(base.resources.configuration)
        if (config.fontScale == scale) return base
        config.fontScale = scale
        return base.createConfigurationContext(config)
    }
}
