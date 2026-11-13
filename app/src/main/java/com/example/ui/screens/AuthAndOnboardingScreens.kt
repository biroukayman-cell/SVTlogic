package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.core.AppLanguage
import com.example.core.QrCodeUtils
import com.example.core.QrImageAnalyzer
import com.example.data.model.Enrollment
import com.example.ui.components.LanguageSwitcherRow
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

private suspend fun signInWithGoogleBrowserInternal(activity: Activity) {
    val provider = OAuthProvider.newBuilder("google.com").apply {
        addCustomParameter("prompt", "select_account")
    }.build()
    Firebase.auth.startActivityForSignInWithProvider(activity, provider).await()
}

fun onGoogleBrowserSignInClicked(
    context: Context,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope
) {
    scope.launch {
        try {
            val activity = context as? Activity
                ?: throw IllegalStateException("Activity context required for sign-in")
            signInWithGoogleBrowserInternal(activity)
            if (Firebase.auth.currentUser != null) {
                onAuthSuccess()
            } else {
                onAuthError(context.getString(R.string.auth_error_generic))
            }
        } catch (e: Exception) {
            Log.e("Auth", "Google browser OAuth sign-in failed", e)
            onAuthError("${e.javaClass.simpleName}: ${e.localizedMessage ?: e.message ?: "Sign in failed"}")
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: (String) -> Unit = {}
) {
    val clientId = try {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) context.getString(resId) else ""
    } catch (_: Exception) {
        ""
    }
    if (clientId.isBlank()) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val primaryRequest = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val activity = context as? Activity
                ?: throw IllegalStateException("Activity context required for sign-in")

            val result = try {
                credentialManager.getCredential(activity, primaryRequest)
            } catch (cancel: GetCredentialCancellationException) {
                throw cancel
            } catch (primaryErr: GetCredentialException) {
                Log.w(
                    "Auth",
                    "GetSignInWithGoogleOption failed (${primaryErr.javaClass.simpleName}: ${primaryErr.errorMessage ?: primaryErr.message}), trying GetGoogleIdOption fallback"
                )
                val fallbackOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(false)
                    .build()
                val fallbackRequest = GetCredentialRequest.Builder().addCredentialOption(fallbackOption).build()
                try {
                    credentialManager.getCredential(activity, fallbackRequest)
                } catch (cancel: GetCredentialCancellationException) {
                    throw cancel
                } catch (fallbackErr: GetCredentialException) {
                    Log.w(
                        "Auth",
                        "GetGoogleIdOption failed (${fallbackErr.javaClass.simpleName}: ${fallbackErr.errorMessage ?: fallbackErr.message}), falling back to browser OAuth"
                    )
                    signInWithGoogleBrowserInternal(activity)
                    if (Firebase.auth.currentUser != null) {
                        onAuthSuccess()
                    } else {
                        onAuthError("${fallbackErr.javaClass.simpleName}: ${fallbackErr.errorMessage ?: fallbackErr.message ?: "Sign in failed"}")
                    }
                    return@launch
                }
            }

            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type: ${credential.type}")
            }
        } catch (e: GetCredentialCancellationException) {
            val detail = e.errorMessage?.toString()?.takeIf { it.isNotBlank() }
                ?: e.message?.takeIf { it.isNotBlank() }
                ?: e.type
            Log.w("Auth", "Google Sign-In cancelled or interrupted: $detail", e)
            onAuthCancelled(detail)
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError("${e.javaClass.simpleName}: ${e.localizedMessage ?: e.message ?: "Sign in failed"}")
        }
    }
}

fun signOutUser(
    context: Context,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    val credentialManager = CredentialManager.create(context)
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@Composable
fun AuthScreen(
    activityContext: Context,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onAuthSuccess: () -> Unit
) {
    val localizedContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(activityContext) { CredentialManager.create(activityContext) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser != null) {
            onAuthSuccess()
            return@LaunchedEffect
        }
        try {
            val pendingTask = Firebase.auth.pendingAuthResult
            if (pendingTask != null) {
                isLoading = true
                pendingTask.await()
                isLoading = false
                if (Firebase.auth.currentUser != null) {
                    onAuthSuccess()
                }
            }
        } catch (e: Exception) {
            isLoading = false
            errorMessage = "${e.javaClass.simpleName}: ${e.localizedMessage ?: e.message}"
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                LanguageSwitcherRow(
                    currentLanguage = currentLanguage,
                    onLanguageSelected = onLanguageSelected
                )
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_svt_logo),
                    contentDescription = stringResource(id = R.string.app_name),
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(24.dp))
                )

                Text(
                    text = stringResource(id = R.string.auth_welcome_title),
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(id = R.string.app_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )

                Image(
                    painter = painterResource(id = R.drawable.img_svt_hero),
                    contentDescription = stringResource(id = R.string.app_subtitle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(MaterialTheme.shapes.large),
                    contentScale = ContentScale.Crop
                )

                Text(
                    text = stringResource(id = R.string.auth_welcome_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!errorMessage.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        onGoogleSignInClicked(
                            context = activityContext,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = coroutineScope,
                            onAuthCancelled = { detail ->
                                isLoading = false
                                errorMessage = localizedContext.getString(R.string.auth_cancelled_hint, detail)
                            }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.Science, contentDescription = null)
                        Spacer(Modifier.size(10.dp))
                        Text(
                            text = stringResource(id = R.string.auth_google_sign_in),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        onGoogleBrowserSignInClicked(
                            context = activityContext,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = coroutineScope
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("google_browser_sign_in_button")
                ) {
                    Text(
                        text = stringResource(id = R.string.auth_google_browser_sign_in),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun AccountSetupScreen(
    defaultName: String,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onSubmit: (fullName: String, phone: String, role: String) -> Unit,
    onSignOut: () -> Unit,
    serverError: String? = null
) {
    var fullName by remember(defaultName) { mutableStateOf(defaultName) }
    var phone by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("student") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val nameRequiredErr = stringResource(id = R.string.error_name_required)
    val phoneRequiredErr = stringResource(id = R.string.error_phone_required)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageSwitcherRow(
                    currentLanguage = currentLanguage,
                    onLanguageSelected = onLanguageSelected
                )
                IconButton(onClick = onSignOut) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = stringResource(id = R.string.auth_sign_out)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier.widthIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.setup_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(id = R.string.setup_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(id = R.string.setup_role_label),
                    style = MaterialTheme.typography.titleSmall
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RoleChoiceCard(
                        title = stringResource(id = R.string.role_student),
                        icon = Icons.Filled.School,
                        selected = selectedRole == "student",
                        onClick = { selectedRole = "student" },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("role_student_card")
                    )
                    RoleChoiceCard(
                        title = stringResource(id = R.string.role_teacher),
                        icon = Icons.Filled.Science,
                        selected = selectedRole == "teacher",
                        onClick = { selectedRole = "teacher" },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("role_teacher_card")
                    )
                }

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text(stringResource(id = R.string.field_full_name)) },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_fullname_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(id = R.string.field_phone)) },
                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_phone_input")
                )

                val displayedError = validationError ?: serverError
                if (!displayedError.isNullOrBlank()) {
                    Text(
                        text = displayedError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        when {
                            fullName.trim().length < 2 -> validationError = nameRequiredErr
                            selectedRole == "student" && phone.trim().length < 6 -> validationError = phoneRequiredErr
                            else -> {
                                validationError = null
                                onSubmit(fullName.trim(), phone.trim(), selectedRole)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("setup_submit_button")
                ) {
                    Text(stringResource(id = R.string.setup_continue))
                }
            }
        }
    }
}

@Composable
private fun RoleChoiceCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.medium
            )
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

@Composable
fun QrJoinClassScreen(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    errorMessage: String?,
    onJoinCode: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var showScanner by remember { mutableStateOf(false) }
    var manualCode by remember { mutableStateOf("") }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) showScanner = true
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (showScanner && hasCameraPermission) {
            BackHandler { showScanner = false }
            QrCameraScannerView(
                onCodeScanned = { scanned ->
                    showScanner = false
                    onJoinCode(scanned)
                },
                onClose = { showScanner = false }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LanguageSwitcherRow(
                        currentLanguage = currentLanguage,
                        onLanguageSelected = onLanguageSelected
                    )
                    IconButton(onClick = onSignOut) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(id = R.string.auth_sign_out)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Column(
                    modifier = Modifier.widthIn(max = 520.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_svt_classroom),
                        contentDescription = stringResource(id = R.string.qr_join_title),
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Text(
                        text = stringResource(id = R.string.qr_join_title),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(id = R.string.qr_join_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            if (hasCameraPermission) {
                                showScanner = true
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("qr_scan_button")
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                        Spacer(Modifier.size(10.dp))
                        Text(stringResource(id = R.string.qr_scan_button))
                    }

                    if (!hasCameraPermission) {
                        Text(
                            text = stringResource(id = R.string.qr_camera_permission_needed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = manualCode,
                        onValueChange = { manualCode = it },
                        label = { Text(stringResource(id = R.string.qr_manual_label)) },
                        placeholder = { Text(stringResource(id = R.string.qr_manual_hint)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qr_manual_code_input")
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val token = QrCodeUtils.extractTokenFromScan(manualCode)
                            if (token.isNotBlank()) onJoinCode(token)
                        },
                        enabled = manualCode.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("qr_manual_submit_button")
                    ) {
                        Text(stringResource(id = R.string.qr_join_submit))
                    }
                }
            }
        }
    }
}

@Composable
private fun QrCameraScannerView(
    onCodeScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor, QrImageAnalyzer { token ->
                                    ContextCompat.getMainExecutor(ctx).execute {
                                        onCodeScanned(token)
                                    }
                                })
                            }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                    } catch (e: Exception) {
                        Log.e("QrScanner", "Camera binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(240.dp)
                .border(3.dp, Color(0xFF7AD9AD), RoundedCornerShape(24.dp))
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(id = R.string.qr_close_scanner),
                tint = Color.White
            )
        }
    }
}

@Composable
fun PendingApprovalScreen(
    enrollment: Enrollment,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onChangeClassCode: () -> Unit,
    onSignOut: () -> Unit
) {
    val isRejected = enrollment.status == "rejected"

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageSwitcherRow(
                    currentLanguage = currentLanguage,
                    onLanguageSelected = onLanguageSelected
                )
                IconButton(onClick = onSignOut) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = stringResource(id = R.string.auth_sign_out)
                    )
                }
            }

            Column(
                modifier = Modifier.widthIn(max = 480.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRejected) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.HourglassTop,
                        contentDescription = null,
                        tint = if (isRejected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Text(
                    text = stringResource(
                        id = if (isRejected) R.string.rejected_title else R.string.pending_title
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isRejected) {
                        stringResource(id = R.string.rejected_desc, enrollment.className)
                    } else {
                        stringResource(id = R.string.pending_desc, enrollment.className, enrollment.teacherName)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = onChangeClassCode,
                    modifier = Modifier.testTag("change_class_code_button")
                ) {
                    Text(stringResource(id = R.string.change_class_code))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
