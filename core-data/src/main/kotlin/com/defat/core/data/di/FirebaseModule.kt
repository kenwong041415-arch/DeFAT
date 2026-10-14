package com.defat.core.data.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Memory cache, not Firestore's default on-disk cache. Room is this app's
     * offline store (phase1.5 plan D3); a second persistent copy of the user's
     * health data inside Firestore's own cache would be one more place for it to
     * outlive sign-out and account deletion. A write that cannot reach the server
     * stays pending in Room and is simply sent again on the next pass.
     *
     * Settings must be applied before the first use of the instance, which is
     * guaranteed here: this provider is the only way the app obtains one.
     */
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
            .build()
    }
}
