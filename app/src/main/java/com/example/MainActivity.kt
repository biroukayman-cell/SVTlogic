package com.example

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.SvtWallpaperContainer
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.AppLanguage
import com.example.core.AuthSessionDraft
import com.example.core.BiometricAuthHelper
import com.example.core.DeviceSessionManager
import com.example.core.LanguagePreferences
import com.example.core.NetworkConnectivityObserver
import com.example.core.NotificationHelper
import com.example.core.NotificationSyncScheduler
import com.example.core.SecurityLockManager
import com.example.core.ThemeConfig
import com.example.core.ThemePreferences
import com.example.core.configureAppCheck
import com.example.core.rememberLocalizedContext
import com.example.data.ai.SvtAiRepository
import com.example.data.offline.OfflineStorageManager
import com.example.data.repository.SvtRepository
import com.example.ui.components.SecurityPinLockScreen
import com.example.ui.screens.AccountSetupScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DeviceLockedBlockedScreen
import com.example.ui.screens.GuestMainDashboardScreen
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.screens.PendingApprovalScreen
import com.example.ui.screens.QrJoinClassScreen
import com.example.ui.screens.TeacherPendingApprovalScreen
import com.example.ui.screens.TermsAcceptanceScreen
import com.example.ui.screens.signOutUser
import com.example.ui.theme.BioTerreTheme
import com.example.ui.viewmodel.ProfileGateState
import com.example.ui.viewmodel.SvtMainViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Anti-spying: block screenshots, screen recording spyware & recents previews
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        // Step 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase operations
        configureAppCheck(this, intent)
        NotificationHelper.ensureChannel(this)
        enableEdgeToEdge()

        setContent {
            val activityContext = this
            var currentLanguage by remember {
                mutableStateOf(LanguagePreferences.loadLanguage(activityContext))
            }
            var currentTheme by remember {
                mutableStateOf(ThemePreferences.loadThemeConfig(activityContext))
            }
            val localizedContext = rememberLocalizedContext(activityContext, currentLanguage)

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalLayoutDirection provides currentLanguage.layoutDirection,
                LocalActivityResultRegistryOwner provides activityContext,
                LocalOnBackPressedDispatcherOwner provides activityContext
            ) {
                BioTerreTheme(themeConfig = currentTheme) {
                    SvtWallpaperContainer(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Transparent
                        ) {
                            val isFirebaseInitialized = remember {
                                FirebaseApp.getApps(activityContext).isNotEmpty() ||
                                    FirebaseApp.initializeApp(activityContext) != null
                            }
                            if (!isFirebaseInitialized) {
                                MissingFirebaseConfigScreen()
                            } else {
                                AppNavigation(
                                    activityContext = activityContext,
                                    currentLanguage = currentLanguage,
                                    onLanguageSelected = { lang ->
                                        LanguagePreferences.saveLanguage(activityContext, lang)
                                        currentLanguage = lang
                                    },
                                    currentTheme = currentTheme,
                                    onThemeChanged = { cfg ->
                                        ThemePreferences.saveThemeConfig(activityContext, cfg)
                                        currentTheme = cfg
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (FirebaseApp.getApps(this).isNotEmpty() && Firebase.auth.currentUser != null) {
            NotificationSyncScheduler.scheduleBackgroundCheckOnLeave(this)
        }
    }
}

@Composable
private fun MissingFirebaseConfigScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.widthIn(max = 460.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.firebase_missing_config_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(id = R.string.firebase_missing_config_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AppNavigation(
    activityContext: Context,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    currentTheme: ThemeConfig,
    onThemeChanged: (ThemeConfig) -> Unit,
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    var isGuestMode by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(user?.uid) {
        if (user == null) {
            NotificationSyncScheduler.cancelAll(activityContext)
        } else {
            isGuestMode = false
            NotificationSyncScheduler.schedulePeriodicSync(activityContext)
        }
    }

    if (user == null) {
        if (isGuestMode) {
            BackHandler { isGuestMode = false }
            GuestMainDashboardScreen(
                currentLanguage = currentLanguage,
                onLanguageSelected = onLanguageSelected,
                currentTheme = currentTheme,
                onThemeChanged = onThemeChanged,
                onExitGuest = { isGuestMode = false }
            )
        } else {
            AuthScreen(
                activityContext = activityContext,
                currentLanguage = currentLanguage,
                onLanguageSelected = onLanguageSelected,
                onAuthSuccess = {},
                onEnterGuestMode = { isGuestMode = true }
            )
        }
    } else {
        key(user.uid) {
            AuthenticatedSvtFlow(
                activityContext = activityContext,
                currentUser = user,
                currentLanguage = currentLanguage,
                onLanguageSelected = onLanguageSelected,
                currentTheme = currentTheme,
                onThemeChanged = onThemeChanged
            )
        }
    }
}

@Composable
private fun AuthenticatedSvtFlow(
    activityContext: Context,
    currentUser: FirebaseUser,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    currentTheme: ThemeConfig,
    onThemeChanged: (ThemeConfig) -> Unit
) {
    val scope = rememberCoroutineScope()
    val viewModel: SvtMainViewModel = viewModel(
        key = currentUser.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId).apply {
                    try {
                        firestoreSettings = firestoreSettings {
                            setLocalCacheSettings(persistentCacheSettings { })
                        }
                    } catch (_: IllegalStateException) {
                    }
                }
                SvtMainViewModel(
                    appContext = app,
                    repository = SvtRepository(db),
                    aiRepository = SvtAiRepository(),
                    offlineStorage = OfflineStorageManager(app, currentUser.uid, db),
                    connectivityObserver = NetworkConnectivityObserver(app),
                    currentUserId = currentUser.uid
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val studentRoster by viewModel.studentRoster.collectAsStateWithLifecycle()
    val studentEnrollment by viewModel.studentEnrollment.collectAsStateWithLifecycle()
    val isSettingUpProfile by viewModel.isSettingUpProfile.collectAsStateWithLifecycle()
    val isJoiningClass by viewModel.isJoiningClass.collectAsStateWithLifecycle()
    val isAcceptingTerms by viewModel.isAcceptingTerms.collectAsStateWithLifecycle()
    val isOwnerAdminEmail by viewModel.isOwnerAdminEmail.collectAsStateWithLifecycle()
    val isVerifiedOwnerAdmin by viewModel.isVerifiedOwnerAdmin.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val localizedUiContext = LocalContext.current
    val invalidCodeMessage = localizedUiContext.getString(R.string.qr_invalid_code)
    var authDraft by remember(currentUser.uid) { mutableStateOf(AuthSessionDraft.load(activityContext)) }
    var onboardingLocalError by remember(currentUser.uid) { mutableStateOf<String?>(null) }
    var autoSetupAttempted by remember(currentUser.uid) { mutableStateOf(false) }
    var autoJoinAttempted by remember(currentUser.uid) { mutableStateOf(false) }
    var isSecurityUnlocked by rememberSaveable(currentUser.uid) {
        mutableStateOf(!SecurityLockManager.isPinEnabled(activityContext, currentUser.uid))
    }

    val onSignOut: () -> Unit = {
        signOutUser(activityContext, scope)
    }

    if (SecurityLockManager.isPinEnabled(activityContext, currentUser.uid) && !isSecurityUnlocked) {
        val unlockedProfileName = (profileState as? ProfileGateState.Ready)?.profile?.fullName
            ?: authDraft.fullName.ifBlank { currentUser.displayName.orEmpty() }
        SecurityPinLockScreen(
            userId = currentUser.uid,
            userName = unlockedProfileName,
            onUnlocked = { isSecurityUnlocked = true },
            onSignOut = onSignOut
        )
        return
    }

    val handleAccountSetupSubmit: (
        String,
        String,
        String,
        String,
        String,
        String,
        String,
        String?
    ) -> Unit = { fullName, phone, role, massar, birthDate, email, establishment, newPassword ->
        viewModel.clearError()
        onboardingLocalError = null
        scope.launch {
            var mustChange = role == "student"
            if (!newPassword.isNullOrBlank()) {
                try {
                    currentUser.updatePassword(newPassword).await()
                    BiometricAuthHelper.updatePasswordIfSaved(activityContext, currentUser.uid, newPassword)
                    mustChange = false
                } catch (e: Exception) {
                    onboardingLocalError = e.localizedMessage
                    return@launch
                }
            }
            viewModel.setupProfile(
                fullName = fullName,
                phone = phone,
                role = role,
                massarNumber = massar,
                birthDate = birthDate,
                email = email,
                establishment = establishment,
                mustChangePassword = mustChange,
                pendingQrToken = authDraft.pendingClassQrToken,
                onComplete = { ok ->
                    if (ok) {
                        AuthSessionDraft.clear(activityContext)
                        authDraft = AuthSessionDraft.load(activityContext)
                    }
                }
            )
        }
    }

    when (val state = profileState) {
        ProfileGateState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        ProfileGateState.NeedsSetup -> {
            val canAutoEnrollByQr = authDraft.role == "student" &&
                authDraft.pendingClassQrToken.isNotBlank() &&
                authDraft.fullName.trim().length >= 2 &&
                !autoSetupAttempted
            val canAutoSetupTeacher = authDraft.role == "teacher" &&
                authDraft.fullName.trim().length >= 2 &&
                authDraft.phoneNumber.trim().length >= 6 &&
                authDraft.establishment.trim().length >= 2 &&
                !autoSetupAttempted

            if (canAutoEnrollByQr || canAutoSetupTeacher || isSettingUpProfile) {
                LaunchedEffect(currentUser.uid, canAutoEnrollByQr, canAutoSetupTeacher) {
                    if (canAutoEnrollByQr) {
                        autoSetupAttempted = true
                        viewModel.setupProfile(
                            fullName = authDraft.fullName.trim(),
                            phone = "",
                            role = "student",
                            massarNumber = authDraft.massarNumber,
                            birthDate = "",
                            email = authDraft.email,
                            establishment = "",
                            mustChangePassword = false,
                            pendingQrToken = authDraft.pendingClassQrToken,
                            onComplete = { ok ->
                                if (ok) {
                                    AuthSessionDraft.clear(activityContext)
                                    authDraft = AuthSessionDraft.load(activityContext)
                                }
                            }
                        )
                    } else if (canAutoSetupTeacher) {
                        autoSetupAttempted = true
                        viewModel.setupProfile(
                            fullName = authDraft.fullName.trim(),
                            phone = authDraft.phoneNumber.trim(),
                            role = "teacher",
                            massarNumber = "",
                            birthDate = "",
                            email = authDraft.email.ifBlank { currentUser.email.orEmpty() },
                            establishment = authDraft.establishment.trim(),
                            mustChangePassword = false,
                            pendingQrToken = "",
                            onComplete = { ok ->
                                if (ok) {
                                    AuthSessionDraft.clear(activityContext)
                                    authDraft = AuthSessionDraft.load(activityContext)
                                }
                            }
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator()
                        if (canAutoEnrollByQr) {
                            Text(
                                text = stringResource(id = R.string.qr_joining_progress),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                val fallbackMassar = studentRoster?.massarNumber?.takeIf { it.isNotBlank() } ?: authDraft.massarNumber
                val resolvedDefaultEmail = authDraft.email.ifBlank {
                    currentUser.email.orEmpty().takeUnless { it.endsWith("@student.bioterre.svt") }.orEmpty()
                }.ifBlank {
                    if (authDraft.role == "student" || studentRoster != null) {
                        AuthSessionDraft.defaultStudentTaalimEmail(fallbackMassar)
                    } else ""
                }
                AccountSetupScreen(
                    defaultName = authDraft.fullName.ifBlank { currentUser.displayName.orEmpty() },
                    defaultRole = authDraft.role,
                    defaultMassar = authDraft.massarNumber,
                    defaultEmail = resolvedDefaultEmail,
                    defaultPhone = authDraft.phoneNumber,
                    defaultEstablishment = authDraft.establishment,
                    studentRoster = studentRoster,
                    hasPendingQrToken = authDraft.pendingClassQrToken.isNotBlank(),
                    currentLanguage = currentLanguage,
                    onLanguageSelected = onLanguageSelected,
                    onSubmit = handleAccountSetupSubmit,
                    onSignOut = onSignOut,
                    isSubmitting = isSettingUpProfile,
                    serverError = onboardingLocalError ?: actionError
                )
            }
        }

        is ProfileGateState.Error -> {
            val fallbackMassar = studentRoster?.massarNumber?.takeIf { it.isNotBlank() } ?: authDraft.massarNumber
            val resolvedDefaultEmail = authDraft.email.ifBlank {
                currentUser.email.orEmpty().takeUnless { it.endsWith("@student.bioterre.svt") }.orEmpty()
            }.ifBlank {
                if (authDraft.role == "student" || studentRoster != null) {
                    AuthSessionDraft.defaultStudentTaalimEmail(fallbackMassar)
                } else ""
            }
            AccountSetupScreen(
                defaultName = authDraft.fullName.ifBlank { currentUser.displayName.orEmpty() },
                defaultRole = authDraft.role,
                defaultMassar = authDraft.massarNumber,
                defaultEmail = resolvedDefaultEmail,
                defaultPhone = authDraft.phoneNumber,
                defaultEstablishment = authDraft.establishment,
                studentRoster = studentRoster,
                hasPendingQrToken = authDraft.pendingClassQrToken.isNotBlank(),
                currentLanguage = currentLanguage,
                onLanguageSelected = onLanguageSelected,
                onSubmit = handleAccountSetupSubmit,
                onSignOut = onSignOut,
                isSubmitting = isSettingUpProfile,
                serverError = onboardingLocalError ?: actionError ?: state.message
            )
        }

        is ProfileGateState.Ready -> {
            val profile = state.profile
            if (profile.role == "student") {
                if (!DeviceSessionManager.isDeviceAllowedForStudent(profile, viewModel.currentDeviceId)) {
                    DeviceLockedBlockedScreen(
                        boundDeviceModel = profile.boundDeviceModel,
                        currentLanguage = currentLanguage,
                        onLanguageSelected = onLanguageSelected,
                        onSignOut = onSignOut
                    )
                } else {
                    LaunchedEffect(profile.userId, profile.boundDeviceId) {
                        if (profile.boundDeviceId.isBlank()) {
                            viewModel.claimStudentDeviceIfNeeded()
                        }
                    }
                    val enrollment = studentEnrollment
                    when {
                        !profile.acceptedTerms -> {
                            TermsAcceptanceScreen(
                                studentName = profile.fullName,
                                currentLanguage = currentLanguage,
                                onLanguageSelected = onLanguageSelected,
                                isSubmitting = isAcceptingTerms,
                                errorMessage = actionError,
                                onAccept = {
                                    viewModel.clearError()
                                    viewModel.acceptTermsOfUse()
                                },
                                onSignOut = onSignOut
                            )
                        }

                    enrollment == null -> {
                        val pendingToken = authDraft.pendingClassQrToken
                        if (pendingToken.isNotBlank() && !autoJoinAttempted) {
                            LaunchedEffect(currentUser.uid, pendingToken) {
                                autoJoinAttempted = true
                                viewModel.joinClassByQrToken(
                                    qrToken = pendingToken,
                                    studentName = profile.fullName,
                                    studentPhone = profile.phoneNumber,
                                    invalidCodeMessage = invalidCodeMessage,
                                    massarNumber = profile.massarNumber,
                                    onResult = {
                                        AuthSessionDraft.savePendingClassQrToken(activityContext, "")
                                        authDraft = AuthSessionDraft.load(activityContext)
                                    }
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text(
                                        text = stringResource(id = R.string.qr_joining_progress),
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            QrJoinClassScreen(
                                currentLanguage = currentLanguage,
                                onLanguageSelected = onLanguageSelected,
                                studentRoster = studentRoster,
                                errorMessage = actionError,
                                isJoining = isJoiningClass,
                                onJoinCode = { token ->
                                    viewModel.clearError()
                                    viewModel.joinClassByQrToken(
                                        qrToken = token,
                                        studentName = profile.fullName,
                                        studentPhone = profile.phoneNumber,
                                        invalidCodeMessage = invalidCodeMessage,
                                        massarNumber = profile.massarNumber,
                                        onResult = {}
                                    )
                                },
                                onSignOut = onSignOut
                            )
                        }
                    }

                    enrollment.status != "approved" -> {
                        PendingApprovalScreen(
                            enrollment = enrollment,
                            currentLanguage = currentLanguage,
                            onLanguageSelected = onLanguageSelected,
                            onChangeClassCode = { viewModel.resetRejectedEnrollment() },
                            onSignOut = onSignOut
                        )
                    }

                    else -> {
                        MainDashboardScreen(
                            profile = profile,
                            currentUserId = currentUser.uid,
                            viewModel = viewModel,
                            currentLanguage = currentLanguage,
                            onLanguageSelected = onLanguageSelected,
                            currentTheme = currentTheme,
                            onThemeChanged = onThemeChanged,
                            onSignOut = onSignOut
                        )
                    }
                }
                }
            } else {
                val canAccessTeacherDashboard =
                    profile.teacherStatus == "approved" || isVerifiedOwnerAdmin
                if (!canAccessTeacherDashboard) {
                    LaunchedEffect(statusMessage) {
                        if (!statusMessage.isNullOrBlank()) {
                            Toast.makeText(localizedUiContext, statusMessage, Toast.LENGTH_LONG).show()
                            viewModel.clearStatusMessage()
                        }
                    }
                    LaunchedEffect(actionError) {
                        if (!actionError.isNullOrBlank()) {
                            Toast.makeText(localizedUiContext, actionError, Toast.LENGTH_LONG).show()
                            viewModel.clearError()
                        }
                    }
                    TeacherPendingApprovalScreen(
                        profile = profile,
                        isOwnerAdminEmail = isOwnerAdminEmail,
                        isEmailVerified = currentUser.isEmailVerified,
                        currentLanguage = currentLanguage,
                        onLanguageSelected = onLanguageSelected,
                        onSendAdminVerification = {
                            viewModel.sendOwnerAdminVerificationEmail(localizedUiContext)
                        },
                        onRefreshStatus = {
                            viewModel.refreshAuthAndProfile(localizedUiContext)
                        },
                        onSignOut = onSignOut
                    )
                } else {
                    MainDashboardScreen(
                        profile = profile,
                        currentUserId = currentUser.uid,
                        viewModel = viewModel,
                        currentLanguage = currentLanguage,
                        onLanguageSelected = onLanguageSelected,
                        currentTheme = currentTheme,
                        onThemeChanged = onThemeChanged,
                        onSignOut = onSignOut
                    )
                }
            }
        }
    }
}
