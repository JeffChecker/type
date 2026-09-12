package com.jeffchecker

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class ZDFPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(ZDF())
        registerMainAPI(ZDFneo())
        registerMainAPI(ZDFinfo())
        registerMainAPI(DreiSat())
        registerMainAPI(KiKA())
        registerMainAPI(Phoenix())
    }
}
