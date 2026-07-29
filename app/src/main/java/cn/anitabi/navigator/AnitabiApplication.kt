package cn.anitabi.navigator

import android.app.Application
import org.maplibre.android.MapLibre

class AnitabiApplication : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
    }
}
