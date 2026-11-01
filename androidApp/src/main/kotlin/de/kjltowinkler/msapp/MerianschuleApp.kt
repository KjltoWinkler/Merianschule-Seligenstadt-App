package de.kjltowinkler.msapp

import android.app.Application
import de.kjltowinkler.msapp.util.AppContextProvider
import de.kjltowinkler.msapp.worker.SubstitutionWorkScheduler

/**
 * Anwendungs-Klasse für Merianschule Seligenstadt App.
 * Initialisiert den Kontext und startet die periodische Vertretungsplan-Synchronisation per WorkManager.
 */
class MerianschuleApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContextProvider.init(this)
        SubstitutionWorkScheduler.schedulePeriodicSync(this)
    }
}
