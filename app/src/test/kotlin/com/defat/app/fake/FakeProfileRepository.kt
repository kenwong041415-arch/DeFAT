package com.defat.app.fake

import com.defat.core.domain.model.UserProfile
import com.defat.core.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class FakeProfileRepository(initial: UserProfile? = null) : ProfileRepository {

    private val _profile = MutableStateFlow(initial)
    val profileState: StateFlow<UserProfile?> = _profile

    var saveCallCount: Int = 0
        private set
    var lastSaved: UserProfile? = null
        private set

    override val profile: StateFlow<UserProfile?> = _profile

    override val onboardingComplete = _profile.map { it != null }

    override suspend fun save(profile: UserProfile) {
        saveCallCount++
        lastSaved = profile
        _profile.value = profile
    }

    override suspend fun updateCurrentBody(weightKg: Double, bodyFatPct: Double?) {
        val current = _profile.value ?: return
        _profile.value = current.copy(weightKg = weightKg, bodyFatPct = bodyFatPct)
    }

    override suspend fun clear() {
        _profile.value = null
    }
}
