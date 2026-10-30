package de.ruff.dimension

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Einstiegspunkt für Hilt; im Manifest als `android:name` eingetragen. */
@HiltAndroidApp
class RuffApplication : Application()
