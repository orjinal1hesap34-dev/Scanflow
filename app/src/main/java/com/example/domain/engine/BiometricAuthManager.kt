package com.example.domain.engine

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricStatus {
    AVAILABLE,
    NOT_ENROLLED,
    NO_HARDWARE,
    HW_UNAVAILABLE,
    UNKNOWN
}

class BiometricAuthManager(private val context: Context) {

    fun checkBiometricAvailability(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HW_UNAVAILABLE
            else -> BiometricStatus.UNKNOWN
        }
    }

    fun isBiometricSupported(): Boolean {
        val status = checkBiometricAvailability()
        return status == BiometricStatus.AVAILABLE || status == BiometricStatus.NOT_ENROLLED
    }

    fun isBiometricAvailable(): Boolean {
        return checkBiometricAvailability() == BiometricStatus.AVAILABLE
    }

    fun promptBiometric(
        activity: FragmentActivity,
        title: String = "ScanFlow Biyometrik Doğrulama",
        subtitle: String = "Parmak izinizi veya yüzünüzü doğrulayın",
        description: String = "Hassas belgelere erişmek için kimliğinizi onaylayın",
        negativeButtonText: String = "PIN ile Doğrula",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onNegativeClick: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                    onNegativeClick()
                } else {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Biyometrik kimlik tanınmadı. Lütfen tekrar deneyin.")
            }
        }

        try {
            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .setConfirmationRequired(false)
                .build()

            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Biyometrik doğrulama başlatılamadı")
        }
    }

    fun promptBiometricForDocument(
        activity: FragmentActivity,
        documentTitle: String,
        onSuccess: () -> Unit,
        onPinFallback: () -> Unit = {},
        onError: (String) -> Unit
    ) {
        promptBiometric(
            activity = activity,
            title = "Belge Kilidi: $documentTitle",
            subtitle = "Biyometrik kimlik doğrulama",
            description = "Bu taranmış hassas belgeye erişmek için parmak izinizi veya yüzünüzü okutun.",
            negativeButtonText = "PIN Kodu ile Aç",
            onSuccess = onSuccess,
            onError = onError,
            onNegativeClick = onPinFallback
        )
    }
}
