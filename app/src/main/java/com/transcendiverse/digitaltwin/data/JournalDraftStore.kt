package com.transcendiverse.digitaltwin.data

import android.content.Context

interface JournalDraftStore {
    fun load(): String
    fun save(draft: String)
}

class SharedPreferencesJournalDraftStore(
    context: Context,
) : JournalDraftStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun load(): String = preferences.getString(KEY_DRAFT, "").orEmpty()

    override fun save(draft: String) {
        preferences.edit()
            .putString(KEY_DRAFT, draft)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "launcher_journal_draft"
        const val KEY_DRAFT = "draft"
    }
}
