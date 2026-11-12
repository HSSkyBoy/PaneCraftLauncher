package com.mio.skin

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mio.datastore.appSettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

fun Context.restoreSkinSwitches(renderer: SkinRenderer) {
    if (this !is LifecycleOwner) return
    lifecycleScope.launch {
        renderer.overlayEnabled = appSettingsDataStore.data.first().skinAnimation.solidLayerEnabled
    }
}
