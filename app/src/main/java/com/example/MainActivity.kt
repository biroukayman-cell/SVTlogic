package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.AppLanguage
import com.example.core.LanguagePreferences
import com.example.core.NetworkConnectivityObserver
import com.example.core.NotificationHelper
import com.example.core.configureAppCheck
import com.example.core.rememberLocalizedContext
import com.example.data.ai.SvtAiRepository
import com.example.data.offline.OfflineStorageManager
import com.example.data.repository.SvtRepository
import com.example.ui.screens.AccountSetupScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.screens.PendingApprovalScreen
import com.example.ui.screens.QrJoinClassScreen
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

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Step 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase operations
        configureAppCheck(this, intent)
        NotificationHelper.ensureChannel(this)
        enableEdgeToEdge()

        setContent {
            val activityContext = this
            var currentLanguage by remember {
                mutableStateOf(LanguagePreferences.loadLanguage(activityContext))
            }
            val localizedContext = rememberLocalizedContext(activityContext, currentLanguage)

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalLayoutDirection provides currentLanguage.layoutDirection
            ) {
                BioTerreTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
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
                                }
                            )
                        }
                    }
                }
            }
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
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(
            activityContext = activityContext,
            currentLanguage = currentLanguage,
            onLanguageSelected = onLanguageSelected,
            onAuthSuccess = {}
        )
    } else {
        key(user.uid) {
            AuthenticatedSvtFlow(
                activityContext = activityContext,
                currentUser = user,
                currentLanguage = currentLanguage,
                onLanguageSelected = onLanguageSelected
            )
        }
    }
}

@Composable
private fun AuthenticatedSvtFlow(
    activityContext: Context,
    currentUser: FirebaseUser,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit
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
                    offlineStorage = OfflineStorageManager(app, currentUser.uid),
                    connectivityObserver = NetworkConnectivityObserver(app),
                    currentUserId = currentUser.uid
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val studentEnrollment by viewModel.studentEnrollment.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    val invalidCodeMessage = LocalContext.current.getString(R.string.qr_invalid_code)

    val onSignOut: () -> Unit = {
        signOutUser(activityContext, scope)
    }

    when (val state = profileState) {
        ProfileGateState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        ProfileGateState.NeedsSetup -> {
            AccountSetupScreen(
                defaultName = currentUser.displayName.orEmpty(),
                currentLanguage = currentLanguage,
                onLanguageSelected = onLanguageSelected,
                onSubmit = { fullName, phone, role ->
                    viewModel.clearError()
                    viewModel.setupProfile(fullName, phone, role)
                },
                onSignOut = onSignOut,
                serverError = actionError
            )
        }

        is ProfileGateState.Error -> {
            AccountSetupScreen(
                defaultName = currentUser.displayName.orEmpty(),
                currentLanguage = currentLanguage,
                onLanguageSelected = onLanguageSelected,
                onSubmit = { fullName, phone, role ->
                    viewModel.clearError()
                    viewModel.setupProfile(fullName, phone, role)
                },
                onSignOut = onSignOut,
                serverError = actionError ?: state.message
            )
        }

        is ProfileGateState.Ready -> {
            val profile = state.profile
            if (profile.role == "student") {
                val enrollment = studentEnrollment
                when {
                    enrollment == null -> {
                        QrJoinClassScreen(
                            currentLanguage = currentLanguage,
                            onLanguageSelected = onLanguageSelected,
                            errorMessage = actionError,
                            onJoinCode = { token ->
                                viewModel.clearError()
                                viewModel.joinClassByQrToken(
                                    qrToken = token,
                                    studentName = profile.fullName,
                                    studentPhone = profile.phoneNumber,
                                    invalidCodeMessage = invalidCodeMessage,
                                    onResult = {}
                                )
                            },
                            onSignOut = onSignOut
                        )
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
                            onSignOut = onSignOut
                        )
                    }
                }
            } else {
                MainDashboardScreen(
                    profile = profile,
                    currentUserId = currentUser.uid,
                    viewModel = viewModel,
                    currentLanguage = currentLanguage,
                    onLanguageSelected = onLanguageSelected,
                    onSignOut = onSignOut
                )
            }
        }
    }
}
