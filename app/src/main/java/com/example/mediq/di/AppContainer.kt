package com.example.mediq.di

import android.content.Context
import com.example.mediq.data.api.RetrofitClient
import com.example.mediq.data.api.TokenStore
import com.example.mediq.data.repository.RetrofitAppointmentRepository
import com.example.mediq.data.repository.RetrofitAuthRepository
import com.example.mediq.data.repository.RetrofitDoctorRepository
import com.example.mediq.data.repository.RetrofitNotificationRepository
import com.example.mediq.data.repository.RetrofitProfileRepository
import com.example.mediq.domain.repository.AppointmentRepository
import com.example.mediq.domain.repository.AuthRepository
import com.example.mediq.domain.repository.DoctorRepository
import com.example.mediq.domain.repository.NotificationRepository
import com.example.mediq.domain.repository.ProfileRepository

/**
 * Holds the one set of repository implementations the app uses.
 *
 * Plain constructor injection rather than a DI framework — there are five
 * objects and no scoping to speak of, so a framework would cost build time and
 * a plugin without buying anything yet. If this grows scopes or per-screen
 * qualifiers, swapping to Hilt should stay a one-file change, because nothing
 * outside this file knows which implementation it got.
 *
 * [init] must be called from [com.example.mediq.MediQApp.onCreate] before any
 * ViewModel or repository is accessed. All properties are lateinit so a crash
 * at access time is clear about what went wrong.
 */
object AppContainer {

    lateinit var tokenStore: TokenStore
    lateinit var doctorRepository: DoctorRepository
    lateinit var appointmentRepository: AppointmentRepository
    lateinit var notificationRepository: NotificationRepository
    lateinit var profileRepository: ProfileRepository
    lateinit var authRepository: AuthRepository

    fun init(context: Context) {
        tokenStore = TokenStore(context.applicationContext)
        val api = RetrofitClient.create(tokenStore)
        doctorRepository      = RetrofitDoctorRepository(api)
        appointmentRepository  = RetrofitAppointmentRepository(api)
        notificationRepository = RetrofitNotificationRepository(api)
        profileRepository      = RetrofitProfileRepository(api)
        authRepository         = RetrofitAuthRepository(api, tokenStore)
    }
}