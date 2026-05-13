package com.nikesh.nammaraste

import android.app.Application
import com.nikesh.nammaraste.data.ReportDatabase

class NammaRasteApplication : Application() {
    val database: ReportDatabase by lazy { ReportDatabase.getDatabase(this) }
}
