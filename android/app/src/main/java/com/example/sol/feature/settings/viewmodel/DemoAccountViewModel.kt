package com.example.sol.feature.settings.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.sol.feature.auth.viewmodel.ProfileDraft

/** One in-memory demo account shared by onboarding and My Info. No persistence or tokens. */
class DemoAccountViewModel : ViewModel() {
    var email by mutableStateOf("")
        private set
    var consents by mutableStateOf<Set<String>>(emptySet())
        private set
    var profile by mutableStateOf<ProfileDraft?>(null)
        private set
    fun start(email: String, consents: Set<String>) {
        this.email = email
        this.consents = consents.toSet()
        profile = null
    }
    fun save(draft: ProfileDraft) { profile = draft.copy(errors = emptyMap(), previewComplete = false) }
    fun clear() { email = ""; consents = emptySet(); profile = null }
}
