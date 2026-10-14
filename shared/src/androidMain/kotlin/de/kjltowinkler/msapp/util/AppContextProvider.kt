package de.kjltowinkler.msapp.util

import android.app.Application
import android.content.Context

object AppContextProvider {
    private var applicationContext: Context? = null

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    val context: Context
        get() {
            if (applicationContext == null) {
                try {
                    val activityThreadClass = Class.forName("android.app.ActivityThread")
                    val currentApplicationMethod = activityThreadClass.getMethod("currentApplication")
                    val app = currentApplicationMethod.invoke(null) as? Application
                    if (app != null) {
                        applicationContext = app.applicationContext
                    }
                } catch (_: Throwable) {
                    // Ignoriert falls Reflection scheitert
                }
            }
            return checkNotNull(applicationContext) {
                "AppContextProvider wurde noch nicht initialisiert. Bitte AppContextProvider.init(context) aufrufen."
            }
        }
}
