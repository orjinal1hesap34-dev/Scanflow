package com.example.domain.engine

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

data class AuthUserState(
    val isLoggedIn: Boolean = false,
    val uid: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val isAnonymous: Boolean = false
)

class FirebaseAuthManager(private val context: Context) {

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            null
        }
    }
    private val credentialManager: CredentialManager by lazy { CredentialManager.create(context) }

    private val _userState = MutableStateFlow(getCurrentUserState())
    val userState: StateFlow<AuthUserState> = _userState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        try {
            auth?.addAuthStateListener { firebaseAuth ->
                _userState.value = mapUserToState(firebaseAuth.currentUser)
            }
        } catch (e: Throwable) {
            // Firebase init fallback
        }
    }

    private fun getCurrentUserState(): AuthUserState {
        return try {
            mapUserToState(auth?.currentUser)
        } catch (e: Throwable) {
            AuthUserState(isLoggedIn = false)
        }
    }

    private fun mapUserToState(user: FirebaseUser?): AuthUserState {
        return if (user != null) {
            AuthUserState(
                isLoggedIn = true,
                uid = user.uid,
                email = user.email,
                displayName = user.displayName ?: user.email?.substringBefore("@") ?: "ScanFlow Kullanıcısı",
                photoUrl = user.photoUrl?.toString(),
                isAnonymous = user.isAnonymous
            )
        } else {
            AuthUserState(isLoggedIn = false)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _errorMessage.value = null
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            _isLoading.value = false
            val err = "Firebase yapılandırması mevcut değil."
            _errorMessage.value = err
            return@withContext Result.failure(Exception(err))
        }
        try {
            suspendCancellableCoroutine { cont ->
                firebaseAuth.signInWithEmailAndPassword(email.trim(), password)
                    .addOnSuccessListener { result ->
                        _isLoading.value = false
                        val user = result.user
                        if (user != null) {
                            _userState.value = mapUserToState(user)
                            cont.resume(Result.success(user))
                        } else {
                            cont.resume(Result.failure(Exception("Kullanıcı bilgisi alınamadı")))
                        }
                    }
                    .addOnFailureListener { exception ->
                        _isLoading.value = false
                        val msg = parseAuthError(exception)
                        _errorMessage.value = msg
                        cont.resume(Result.failure(Exception(msg)))
                    }
            }
        } catch (e: Exception) {
            _isLoading.value = false
            _errorMessage.value = e.localizedMessage ?: "Giriş yapılamadı"
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _errorMessage.value = null
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            _isLoading.value = false
            val err = "Firebase yapılandırması mevcut değil."
            _errorMessage.value = err
            return@withContext Result.failure(Exception(err))
        }
        try {
            suspendCancellableCoroutine { cont ->
                firebaseAuth.createUserWithEmailAndPassword(email.trim(), password)
                    .addOnSuccessListener { result ->
                        val user = result.user
                        if (user != null) {
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(displayName.trim())
                                .build()
                            user.updateProfile(profileUpdates).addOnCompleteListener {
                                _isLoading.value = false
                                _userState.value = mapUserToState(firebaseAuth.currentUser)
                                cont.resume(Result.success(user))
                            }
                        } else {
                            _isLoading.value = false
                            cont.resume(Result.failure(Exception("Hesap oluşturulamadı")))
                        }
                    }
                    .addOnFailureListener { exception ->
                        _isLoading.value = false
                        val msg = parseAuthError(exception)
                        _errorMessage.value = msg
                        cont.resume(Result.failure(Exception(msg)))
                    }
            }
        } catch (e: Exception) {
            _isLoading.value = false
            _errorMessage.value = e.localizedMessage ?: "Kayıt başarısız"
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _errorMessage.value = null
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            _isLoading.value = false
            val err = "Firebase yapılandırması mevcut değil."
            _errorMessage.value = err
            return@withContext Result.failure(Exception(err))
        }
        try {
            suspendCancellableCoroutine { cont ->
                firebaseAuth.sendPasswordResetEmail(email.trim())
                    .addOnSuccessListener {
                        _isLoading.value = false
                        cont.resume(Result.success(Unit))
                    }
                    .addOnFailureListener { exception ->
                        _isLoading.value = false
                        val msg = parseAuthError(exception)
                        _errorMessage.value = msg
                        cont.resume(Result.failure(Exception(msg)))
                    }
            }
        } catch (e: Exception) {
            _isLoading.value = false
            _errorMessage.value = e.localizedMessage ?: "Şifre sıfırlama e-postası gönderilemedi"
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(activityContext: Context, webClientId: String = ""): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _errorMessage.value = null
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            _isLoading.value = false
            val err = "Firebase yapılandırması mevcut değil."
            _errorMessage.value = err
            return@withContext Result.failure(Exception(err))
        }
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .apply {
                    if (webClientId.isNotBlank()) {
                        setServerClientId(webClientId)
                    }
                }
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(activityContext, request)
            val credential = response.credential

            if (credential is androidx.credentials.CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)

                suspendCancellableCoroutine { cont ->
                    firebaseAuth.signInWithCredential(authCredential)
                        .addOnSuccessListener { result ->
                            _isLoading.value = false
                            val user = result.user
                            if (user != null) {
                                _userState.value = mapUserToState(user)
                                cont.resume(Result.success(user))
                            } else {
                                cont.resume(Result.failure(Exception("Google kullanıcısı alınamadı")))
                            }
                        }
                        .addOnFailureListener { ex ->
                            _isLoading.value = false
                            val msg = parseAuthError(ex)
                            _errorMessage.value = msg
                            cont.resume(Result.failure(Exception(msg)))
                        }
                }
            } else {
                _isLoading.value = false
                val err = "Geçersiz kimlik bilgisi türü"
                _errorMessage.value = err
                Result.failure(Exception(err))
            }
        } catch (e: GetCredentialCancellationException) {
            _isLoading.value = false
            Result.failure(Exception("Giriş iptal edildi"))
        } catch (e: GetCredentialException) {
            _isLoading.value = false
            // Fallback for simulation / emulator if google play services auth is unavailable
            _errorMessage.value = "Google Girişi: ${e.message}"
            Result.failure(e)
        } catch (e: Exception) {
            _isLoading.value = false
            _errorMessage.value = e.localizedMessage ?: "Google ile giriş başarısız"
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Throwable) {
            // Ignore
        }
        _userState.value = AuthUserState(isLoggedIn = false)
    }

    private fun parseAuthError(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("The email address is already in use", ignoreCase = true) -> "Bu e-posta adresi zaten kullanımda."
            msg.contains("The email address is badly formatted", ignoreCase = true) -> "Geçersiz e-posta adresi biçimi."
            msg.contains("There is no user record", ignoreCase = true) || msg.contains("user-not-found", ignoreCase = true) -> "Bu e-posta ile kayıtlı kullanıcı bulunamadı."
            msg.contains("wrong-password", ignoreCase = true) || msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) -> "Hatalı şifre veya e-posta."
            msg.contains("network error", ignoreCase = true) -> "Ağ bağlantı hatası. Lütfen internetinizi kontrol edin."
            msg.contains("WEAK_PASSWORD", ignoreCase = true) || msg.contains("Password should be at least", ignoreCase = true) -> "Şifre en az 6 karakter olmalıdır."
            else -> msg.ifBlank { "Kimlik doğrulama işlemi gerçekleştirilemedi." }
        }
    }
}
