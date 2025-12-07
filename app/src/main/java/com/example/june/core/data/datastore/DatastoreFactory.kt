package com.example.june.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

internal const val APP_DATASTORE = "june.app.preferences_pb"

class DatastoreFactory(private val context: Context) {
    fun getPreferencesDataStore(): DataStore<Preferences> = createDataStore(
            producePath = { context.filesDir.resolve(APP_DATASTORE).absolutePath }
        )
}

fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })