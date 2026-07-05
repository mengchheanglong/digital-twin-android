package com.transcendiverse.digitaltwin

import android.app.Application
import androidx.work.Configuration

class DigitalTwinApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}
