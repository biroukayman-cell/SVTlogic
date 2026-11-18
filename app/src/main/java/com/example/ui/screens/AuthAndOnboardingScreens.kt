package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Patterns
import android.util.Size
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.core.AppLanguage
import com.example.core.AuthSessionDraft
import com.example.core.QrCodeUtils
import com.example.core.QrImageAnalyzer
import com.example.core.QrScanResult
import com.example.data.model.Enrollment
import com.example.data.model.RosterStudent
import com.example.data.model.UserProfile
import com.example.ui.components.LanguageSwitcherRow
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.Executors

fun signOutUser(
    context: Context,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        AuthSessionDraft.clear(context)
        Firebase.auth.signOut()
        onSignOutComplete()
    }
}

private fun mapFirebaseAuthError(context: Context, e: Exception): String {
    if (e is FirebaseAuthUserCollisionException) {
        return context.getString(R.string.auth_error_email_in_use)
    }
    if (e is FirebaseAuthWeakPasswordException) {
        return context.getString(R.string.auth_error_weak_password)
    }
    if (e is FirebaseAuthInvalidUserException || e is FirebaseAuthInvalidCredentialsException) {
        val code = (e as? FirebaseAuthException)?.errorCode.orEmpty()
        if (code == "ERROR_INVALID_EMAIL") {
            return context.getString(R.string.auth_error_email_invalid)
        }
        return context.getString(R.string.auth_error_invalid_credentials)
    }
    if (e is FirebaseAuthException) {
        val msg = e.message.orEmpty()
        if (e.errorCode == "ERROR_OPERATION_NOT_ALLOWED" ||
            e.errorCode == "CONFIGURATION_NOT_FOUND" ||
            msg.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
            msg.contains("PASSWORD_LOGIN_DISABLED", ignoreCase = true)
        ) {
            return context.getString(R.string.auth_error_email_disabled)
        }
    }
    val rawMsg = e.localizedMessage ?: e.message.orEmpty()
    if (rawMsg.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
        rawMsg.contains("PASSWORD_LOGIN_DISABLED", ignoreCase = true)
    ) {
        return context.getString(R.string.auth_error_email_disabled)
    }
    return rawMsg.ifBlank { context.getString(R.string.auth_error_generic) }
}

@Composable
fun AuthScreen(
    activityContext: Context,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onAuthSuccess: () -> Unit,
    onEnterGuestMode: () -> Unit = {}
) {
    val localizedContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedAuthRole by rememberSaveable { mutableStateOf<String?>(null) } // null, "student", "student_qr", "teacher"
    var isTeacherRegister by rememberSaveable { mutableStateOf(false) }

    var massarInput by rememberSaveable { mutableStateOf("") }
    var studentPassword by rememberSaveable { mutableStateOf("") }

    var qrTokenInput by rememberSaveable { mutableStateOf("") }
    var qrFullName by rememberSaveable { mutableStateOf("") }
    var qrMassarInput by rememberSaveable { mutableStateOf("") }
    var qrPassword by rememberSaveable { mutableStateOf("") }
    var showInlineQrScanner by rememberSaveable { mutableStateOf(false) }
    var showLegalDialog by rememberSaveable { mutableStateOf(false) }
    var showPrivacyDialog by rememberSaveable { mutableStateOf(false) }
    var showTermsDialog by rememberSaveable { mutableStateOf(false) }

    var teacherFullName by rememberSaveable { mutableStateOf("") }
    var teacherPhone by rememberSaveable { mutableStateOf("") }
    var teacherEstablishment by rememberSaveable { mutableStateOf("") }
    var teacherEmail by rememberSaveable { mutableStateOf("") }
    var teacherPassword by rememberSaveable { mutableStateOf("") }
    var teacherConfirmPassword by rememberSaveable { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showStudentForgotDialog by rememberSaveable { mutableStateOf(false) }
    var showTeacherForgotDialog by rememberSaveable { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(localizedContext, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val applyQrScanResult: (QrScanResult) -> Unit = { result ->
        if (result.classToken.isNotBlank()) {
            qrTokenInput = result.classToken
            if (!result.massarNumber.isNullOrBlank()) {
                qrMassarInput = result.massarNumber
            }
            QrCodeUtils.copyTextToClipboard(localizedContext, "Code Classe", result.classToken)
            Toast.makeText(
                localizedContext,
                localizedContext.getString(R.string.qr_code_copied, result.classToken),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) showInlineQrScanner = true
    }

    val galleryQrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val decoded = decodeQrFromUri(localizedContext, uri)
                if (decoded != null && decoded.classToken.isNotBlank()) {
                    errorMessage = null
                    applyQrScanResult(decoded)
                } else {
                    errorMessage = localizedContext.getString(R.string.qr_not_found_in_image)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser != null) {
            onAuthSuccess()
        }
    }

    if (selectedAuthRole != null) {
        BackHandler {
            selectedAuthRole = null
            errorMessage = null
            passwordVisible = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
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
                horizontalArrangement = if (selectedAuthRole != null) Arrangement.SpaceBetween else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedAuthRole != null) {
                    TextButton(
                        onClick = {
                            selectedAuthRole = null
                            errorMessage = null
                            passwordVisible = false
                        },
                        modifier = Modifier.testTag("change_role_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(id = R.string.auth_change_role))
                    }
                }
                LanguageSwitcherRow(
                    currentLanguage = currentLanguage,
                    onLanguageSelected = onLanguageSelected
                )
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_svt_logo),
                    contentDescription = stringResource(id = R.string.app_name),
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(22.dp))
                )

                Text(
                    text = stringResource(id = R.string.auth_welcome_title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(id = R.string.app_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )

                AnimatedVisibility(visible = selectedAuthRole == null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_tab_lab),
                            contentDescription = stringResource(id = R.string.app_subtitle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(148.dp)
                                .clip(MaterialTheme.shapes.large)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                    shape = MaterialTheme.shapes.large
                                ),
                            contentScale = ContentScale.Crop
                        )

                        Text(
                            text = stringResource(id = R.string.auth_welcome_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = stringResource(id = R.string.auth_role_question),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 6.dp)
                        )

                        RoleGatewayCard(
                            title = stringResource(id = R.string.auth_qr_quick_title),
                            subtitle = stringResource(id = R.string.auth_qr_quick_sub),
                            icon = Icons.Filled.QrCodeScanner,
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            onClick = {
                                errorMessage = null
                                selectedAuthRole = "student_qr"
                            },
                            modifier = Modifier.testTag("choose_role_student_qr_button")
                        )

                        RoleGatewayCard(
                            title = stringResource(id = R.string.auth_role_student_btn),
                            subtitle = stringResource(id = R.string.auth_role_student_sub),
                            icon = Icons.Filled.School,
                            accentColor = MaterialTheme.colorScheme.primary,
                            onClick = {
                                errorMessage = null
                                selectedAuthRole = "student"
                            },
                            modifier = Modifier.testTag("choose_role_student_button")
                        )

                        RoleGatewayCard(
                            title = stringResource(id = R.string.auth_role_teacher_btn),
                            subtitle = stringResource(id = R.string.auth_role_teacher_sub),
                            icon = Icons.Filled.Science,
                            accentColor = MaterialTheme.colorScheme.secondary,
                            onClick = {
                                errorMessage = null
                                selectedAuthRole = "teacher"
                            },
                            modifier = Modifier.testTag("choose_role_teacher_button")
                        )

                        RoleGatewayCard(
                            title = stringResource(id = R.string.auth_guest_mode_btn),
                            subtitle = stringResource(id = R.string.auth_guest_mode_sub),
                            icon = Icons.Filled.Visibility,
                            accentColor = MaterialTheme.colorScheme.outline,
                            onClick = onEnterGuestMode,
                            modifier = Modifier.testTag("choose_guest_mode_button")
                        )
                    }
                }

                AnimatedVisibility(visible = selectedAuthRole == "student") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.auth_student_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = stringResource(id = R.string.auth_student_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        OutlinedTextField(
                            value = massarInput,
                            onValueChange = { massarInput = it.uppercase(Locale.US) },
                            label = { Text(stringResource(id = R.string.field_massar)) },
                            placeholder = { Text(stringResource(id = R.string.field_massar_hint)) },
                            leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                autoCorrectEnabled = false
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("student_massar_input")
                        )

                        OutlinedTextField(
                            value = studentPassword,
                            onValueChange = { studentPassword = it },
                            label = { Text(stringResource(id = R.string.field_password)) },
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = stringResource(id = R.string.field_password)
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("student_password_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showStudentForgotDialog = true },
                                modifier = Modifier.testTag("student_forgot_password_button")
                            ) {
                                Text(stringResource(id = R.string.password_forgot_button))
                            }
                        }

                        AuthErrorCard(errorMessage = errorMessage)

                        Button(
                            onClick = {
                                val cleanMassar = AuthSessionDraft.normalizeMassar(massarInput)
                                when {
                                    cleanMassar.length < 4 -> {
                                        errorMessage = localizedContext.getString(R.string.error_massar_required)
                                    }
                                    studentPassword.length < 6 -> {
                                        errorMessage = localizedContext.getString(R.string.auth_error_weak_password)
                                    }
                                    else -> {
                                        errorMessage = null
                                        isLoading = true
                                        coroutineScope.launch {
                                            try {
                                                val synthEmail = AuthSessionDraft.massarToAuthEmail(cleanMassar)
                                                AuthSessionDraft.save(
                                                    context = activityContext,
                                                    role = "student",
                                                    massarNumber = cleanMassar
                                                )
                                                Firebase.auth.signInWithEmailAndPassword(synthEmail, studentPassword).await()
                                                isLoading = false
                                                onAuthSuccess()
                                            } catch (e: Exception) {
                                                isLoading = false
                                                Log.e("Auth", "Student Massar sign-in failed", e)
                                                errorMessage = mapFirebaseAuthError(localizedContext, e)
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("student_login_submit_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = stringResource(id = R.string.auth_sign_in_btn),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                errorMessage = null
                                selectedAuthRole = "student_qr"
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(id = R.string.auth_qr_quick_title))
                        }
                    }
                }

                AnimatedVisibility(visible = selectedAuthRole == "student_qr") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.auth_qr_quick_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = stringResource(id = R.string.auth_qr_quick_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (hasCameraPermission) {
                                        showInlineQrScanner = true
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("auth_qr_scan_button")
                            ) {
                                Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(id = R.string.auth_qr_scan_first))
                            }
                            OutlinedButton(
                                onClick = {
                                    galleryQrLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("auth_qr_gallery_button")
                            ) {
                                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(id = R.string.qr_pick_from_gallery))
                            }
                        }

                        OutlinedTextField(
                            value = qrTokenInput,
                            onValueChange = { qrTokenInput = it },
                            label = { Text(stringResource(id = R.string.qr_manual_label)) },
                            placeholder = { Text(stringResource(id = R.string.qr_manual_hint)) },
                            leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_qr_token_input")
                        )

                        OutlinedTextField(
                            value = qrFullName,
                            onValueChange = { qrFullName = it },
                            label = { Text(stringResource(id = R.string.field_full_name)) },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_qr_fullname_input")
                        )

                        OutlinedTextField(
                            value = qrMassarInput,
                            onValueChange = { qrMassarInput = it.uppercase(Locale.US) },
                            label = { Text(stringResource(id = R.string.field_massar_optional)) },
                            placeholder = { Text(stringResource(id = R.string.field_massar_hint)) },
                            leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                autoCorrectEnabled = false
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_qr_massar_input")
                        )

                        OutlinedTextField(
                            value = qrPassword,
                            onValueChange = { qrPassword = it },
                            label = { Text(stringResource(id = R.string.field_password)) },
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = stringResource(id = R.string.field_password)
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_qr_password_input")
                        )

                        AuthErrorCard(errorMessage = errorMessage)

                        Button(
                            onClick = {
                                val cleanToken = QrCodeUtils.extractTokenFromScan(qrTokenInput)
                                val cleanName = qrFullName.trim()
                                val enteredMassar = AuthSessionDraft.normalizeMassar(qrMassarInput)
                                val finalMassar = if (enteredMassar.length >= 4) {
                                    enteredMassar
                                } else {
                                    "QR" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase(Locale.US)
                                }
                                when {
                                    cleanToken.length < 3 -> {
                                        errorMessage = localizedContext.getString(R.string.qr_token_required_error)
                                    }
                                    cleanName.length < 2 -> {
                                        errorMessage = localizedContext.getString(R.string.error_name_required)
                                    }
                                    qrPassword.length < 6 -> {
                                        errorMessage = localizedContext.getString(R.string.auth_error_weak_password)
                                    }
                                    else -> {
                                        errorMessage = null
                                        isLoading = true
                                        coroutineScope.launch {
                                            try {
                                                val synthEmail = AuthSessionDraft.massarToAuthEmail(finalMassar)
                                                AuthSessionDraft.save(
                                                    context = activityContext,
                                                    role = "student",
                                                    massarNumber = finalMassar,
                                                    fullName = cleanName,
                                                    pendingClassQrToken = cleanToken
                                                )
                                                try {
                                                    Firebase.auth.createUserWithEmailAndPassword(synthEmail, qrPassword).await()
                                                } catch (collision: FirebaseAuthUserCollisionException) {
                                                    if (enteredMassar.length >= 4) {
                                                        Firebase.auth.signInWithEmailAndPassword(synthEmail, qrPassword).await()
                                                    } else {
                                                        throw collision
                                                    }
                                                }
                                                isLoading = false
                                                onAuthSuccess()
                                            } catch (e: Exception) {
                                                isLoading = false
                                                Log.e("Auth", "Direct QR registration failed", e)
                                                errorMessage = mapFirebaseAuthError(localizedContext, e)
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("auth_qr_submit_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = stringResource(id = R.string.auth_qr_join_submit),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = selectedAuthRole == "teacher") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.auth_teacher_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterChip(
                                selected = !isTeacherRegister,
                                onClick = {
                                    isTeacherRegister = false
                                    errorMessage = null
                                },
                                label = { Text(stringResource(id = R.string.auth_mode_login)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("teacher_mode_login_chip")
                            )
                            FilterChip(
                                selected = isTeacherRegister,
                                onClick = {
                                    isTeacherRegister = true
                                    errorMessage = null
                                },
                                label = { Text(stringResource(id = R.string.auth_mode_register)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("teacher_mode_register_chip")
                            )
                        }

                        if (isTeacherRegister) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(
                                    text = stringResource(id = R.string.teacher_approval_register_notice),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }

                            OutlinedTextField(
                                value = teacherFullName,
                                onValueChange = { teacherFullName = it },
                                label = { Text(stringResource(id = R.string.field_full_name_required)) },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("teacher_fullname_input")
                            )

                            OutlinedTextField(
                                value = teacherPhone,
                                onValueChange = { teacherPhone = it },
                                label = { Text(stringResource(id = R.string.field_phone_required)) },
                                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("teacher_phone_input")
                            )

                            OutlinedTextField(
                                value = teacherEstablishment,
                                onValueChange = { teacherEstablishment = it },
                                label = { Text(stringResource(id = R.string.field_establishment_required)) },
                                placeholder = { Text(stringResource(id = R.string.field_establishment_hint)) },
                                leadingIcon = { Icon(Icons.Filled.Business, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("teacher_establishment_input")
                            )
                        }

                        OutlinedTextField(
                            value = teacherEmail,
                            onValueChange = { teacherEmail = it },
                            label = { Text(stringResource(id = R.string.field_email_pro)) },
                            placeholder = { Text(stringResource(id = R.string.field_email_pro_hint)) },
                            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("teacher_email_input")
                        )

                        OutlinedTextField(
                            value = teacherPassword,
                            onValueChange = { teacherPassword = it },
                            label = { Text(stringResource(id = R.string.field_password)) },
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = stringResource(id = R.string.field_password)
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("teacher_password_input")
                        )

                        if (isTeacherRegister) {
                            OutlinedTextField(
                                value = teacherConfirmPassword,
                                onValueChange = { teacherConfirmPassword = it },
                                label = { Text(stringResource(id = R.string.field_password_confirm)) },
                                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = stringResource(id = R.string.field_password_confirm)
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("teacher_confirm_password_input")
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { showTeacherForgotDialog = true },
                                    modifier = Modifier.testTag("teacher_forgot_password_button")
                                ) {
                                    Text(stringResource(id = R.string.password_forgot_button))
                                }
                            }
                        }

                        AuthErrorCard(errorMessage = errorMessage)

                        Button(
                            onClick = {
                                val cleanEmail = teacherEmail.trim()
                                val cleanName = teacherFullName.trim()
                                val cleanPhone = teacherPhone.trim()
                                val cleanEstablishment = teacherEstablishment.trim()
                                when {
                                    isTeacherRegister && cleanName.length < 2 -> {
                                        errorMessage = localizedContext.getString(R.string.error_name_required)
                                    }
                                    isTeacherRegister && cleanPhone.length < 6 -> {
                                        errorMessage = localizedContext.getString(R.string.error_phone_required)
                                    }
                                    isTeacherRegister && cleanEstablishment.length < 2 -> {
                                        errorMessage = localizedContext.getString(R.string.error_establishment_required)
                                    }
                                    cleanEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() -> {
                                        errorMessage = localizedContext.getString(R.string.auth_error_email_invalid)
                                    }
                                    teacherPassword.length < 6 -> {
                                        errorMessage = localizedContext.getString(R.string.auth_error_weak_password)
                                    }
                                    isTeacherRegister && teacherPassword != teacherConfirmPassword -> {
                                        errorMessage = localizedContext.getString(R.string.auth_error_password_mismatch)
                                    }
                                    else -> {
                                        errorMessage = null
                                        isLoading = true
                                        coroutineScope.launch {
                                            try {
                                                AuthSessionDraft.save(
                                                    context = activityContext,
                                                    role = "teacher",
                                                    email = cleanEmail,
                                                    fullName = cleanName,
                                                    phoneNumber = cleanPhone,
                                                    establishment = cleanEstablishment
                                                )
                                                if (isTeacherRegister) {
                                                    Firebase.auth.createUserWithEmailAndPassword(cleanEmail, teacherPassword).await()
                                                } else {
                                                    Firebase.auth.signInWithEmailAndPassword(cleanEmail, teacherPassword).await()
                                                }
                                                isLoading = false
                                                onAuthSuccess()
                                            } catch (e: Exception) {
                                                isLoading = false
                                                Log.e("Auth", "Teacher email auth failed", e)
                                                errorMessage = mapFirebaseAuthError(localizedContext, e)
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("teacher_auth_submit_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = stringResource(
                                        id = if (isTeacherRegister) R.string.auth_register_btn else R.string.auth_sign_in_btn
                                    ),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                TextButton(
                    onClick = { showLegalDialog = true },
                    modifier = Modifier.testTag("auth_legal_notice_button")
                ) {
                    Text(
                        text = stringResource(id = R.string.legal_notice_footer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                TextButton(
                    onClick = { showPrivacyDialog = true },
                    modifier = Modifier.testTag("auth_privacy_policy_button")
                ) {
                    Text(
                        text = stringResource(id = R.string.privacy_policy_button),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
                TextButton(
                    onClick = { showTermsDialog = true },
                    modifier = Modifier.testTag("auth_terms_of_use_button")
                ) {
                    Text(
                        text = stringResource(id = R.string.terms_of_use_button),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showInlineQrScanner && hasCameraPermission) {
        Dialog(
            onDismissRequest = { showInlineQrScanner = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            QrCameraScannerView(
                onResultScanned = { result ->
                    showInlineQrScanner = false
                    applyQrScanResult(result)
                },
                onClose = { showInlineQrScanner = false }
            )
        }
    }

    if (showLegalDialog) {
        AlertDialog(
            onDismissRequest = { showLegalDialog = false },
            title = { Text(stringResource(id = R.string.legal_notice_title)) },
            text = { Text(stringResource(id = R.string.legal_notice_body)) },
            confirmButton = {
                Button(onClick = { showLegalDialog = false }) {
                    Text(stringResource(id = R.string.close))
                }
            }
        )
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    }

    if (showTermsDialog) {
        TermsOfUseDialog(onDismiss = { showTermsDialog = false })
    }

    if (showStudentForgotDialog) {
        StudentForgotPasswordDialog(
            initialMassar = massarInput,
            onDismiss = { showStudentForgotDialog = false }
        )
    }

    if (showTeacherForgotDialog) {
        TeacherForgotPasswordDialog(
            initialEmail = teacherEmail,
            onDismiss = { showTeacherForgotDialog = false }
        )
    }
}

@Composable
private fun RoleGatewayCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .border(
                width = 1.5.dp,
                color = accentColor.copy(alpha = 0.45f),
                shape = MaterialTheme.shapes.large
            )
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AuthErrorCard(errorMessage: String?) {
    if (!errorMessage.isNullOrBlank()) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSetupScreen(
    defaultName: String,
    defaultRole: String,
    defaultMassar: String,
    defaultEmail: String,
    defaultPhone: String = "",
    defaultEstablishment: String = "",
    studentRoster: RosterStudent?,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onSubmit: (
        fullName: String,
        phone: String,
        role: String,
        massarNumber: String,
        birthDate: String,
        email: String,
        establishment: String,
        newPassword: String?
    ) -> Unit,
    onSignOut: () -> Unit,
    isSubmitting: Boolean = false,
    serverError: String? = null
) {
    val resolvedRole = if (studentRoster != null) "student" else defaultRole
    val initialName = studentRoster?.fullName?.takeIf { it.isNotBlank() } ?: defaultName
    val resolvedMassar = studentRoster?.massarNumber?.takeIf { it.isNotBlank() } ?: defaultMassar

    var fullName by remember(initialName) { mutableStateOf(initialName) }
    var birthDate by remember { mutableStateOf("") }
    var phone by remember(defaultPhone) { mutableStateOf(defaultPhone) }
    var establishment by remember(defaultEstablishment) { mutableStateOf(defaultEstablishment) }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val nameRequiredErr = stringResource(id = R.string.error_name_required)
    val phoneRequiredErr = stringResource(id = R.string.error_phone_required)
    val establishmentRequiredErr = stringResource(id = R.string.error_establishment_required)
    val birthDateRequiredErr = stringResource(id = R.string.error_birthdate_required)
    val weakPassErr = stringResource(id = R.string.auth_error_weak_password)
    val mismatchPassErr = stringResource(id = R.string.auth_error_password_mismatch)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.setup_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(
                        id = if (resolvedRole == "teacher") R.string.setup_subtitle_teacher
                        else R.string.setup_subtitle_student
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (resolvedRole == "teacher") Icons.Filled.Science else Icons.Filled.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = stringResource(
                                    id = if (resolvedRole == "teacher") R.string.role_teacher else R.string.role_student
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            val identityText = if (resolvedRole == "teacher") defaultEmail else resolvedMassar
                            if (identityText.isNotBlank()) {
                                Text(
                                    text = identityText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
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

                if (resolvedRole == "student") {
                    OutlinedTextField(
                        value = birthDate,
                        onValueChange = { birthDate = it },
                        label = { Text(stringResource(id = R.string.field_birth_date)) },
                        placeholder = { Text(stringResource(id = R.string.field_birth_date_hint)) },
                        leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                        trailingIcon = {
                            IconButton(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.testTag("setup_birthdate_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarMonth,
                                    contentDescription = stringResource(id = R.string.field_birth_date)
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_birthdate_input")
                    )
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = {
                        Text(
                            stringResource(
                                id = if (resolvedRole == "teacher") R.string.field_phone_required
                                else R.string.field_phone
                            )
                        )
                    },
                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_phone_input")
                )

                if (resolvedRole == "teacher") {
                    OutlinedTextField(
                        value = establishment,
                        onValueChange = { establishment = it },
                        label = { Text(stringResource(id = R.string.field_establishment_required)) },
                        placeholder = { Text(stringResource(id = R.string.field_establishment_hint)) },
                        leadingIcon = { Icon(Icons.Filled.Business, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_establishment_input")
                    )
                }

                if (resolvedRole == "student") {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text(stringResource(id = R.string.field_new_password_optional)) },
                        placeholder = { Text(stringResource(id = R.string.field_new_password_hint)) },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_new_password_input")
                    )

                    if (newPassword.isNotBlank()) {
                        OutlinedTextField(
                            value = confirmNewPassword,
                            onValueChange = { confirmNewPassword = it },
                            label = { Text(stringResource(id = R.string.field_password_confirm)) },
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_confirm_password_input")
                        )
                    }
                }

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
                            resolvedRole == "teacher" && phone.trim().length < 6 -> {
                                validationError = phoneRequiredErr
                            }
                            resolvedRole == "teacher" && establishment.trim().length < 2 -> {
                                validationError = establishmentRequiredErr
                            }
                            resolvedRole == "student" && birthDate.trim().length < 6 -> {
                                validationError = birthDateRequiredErr
                            }
                            newPassword.isNotBlank() && newPassword.length < 6 -> {
                                validationError = weakPassErr
                            }
                            newPassword.isNotBlank() && newPassword != confirmNewPassword -> {
                                validationError = mismatchPassErr
                            }
                            else -> {
                                validationError = null
                                onSubmit(
                                    fullName.trim(),
                                    phone.trim(),
                                    resolvedRole,
                                    resolvedMassar,
                                    birthDate.trim(),
                                    defaultEmail.trim(),
                                    establishment.trim(),
                                    newPassword.takeIf { it.isNotBlank() }
                                )
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("setup_submit_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(id = R.string.setup_continue))
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            birthDate = sdf.format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(id = R.string.setup_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun QrJoinClassScreen(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    studentRoster: RosterStudent?,
    errorMessage: String?,
    isJoining: Boolean = false,
    onJoinCode: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showScanner by rememberSaveable { mutableStateOf(false) }
    var manualCode by remember(studentRoster?.qrToken) {
        mutableStateOf(studentRoster?.qrToken.orEmpty())
    }
    var galleryError by remember { mutableStateOf<String?>(null) }
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

    val galleryQrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val decoded = decodeQrFromUri(context, uri)
                if (decoded != null && decoded.classToken.isNotBlank()) {
                    galleryError = null
                    manualCode = decoded.classToken
                    QrCodeUtils.copyTextToClipboard(context, "Code Classe", decoded.classToken)
                    Toast.makeText(
                        context,
                        context.getString(R.string.qr_code_copied, decoded.classToken),
                        Toast.LENGTH_SHORT
                    ).show()
                    onJoinCode(decoded.classToken)
                } else {
                    galleryError = context.getString(R.string.qr_not_found_in_image)
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        if (showScanner && hasCameraPermission) {
            BackHandler { showScanner = false }
            QrCameraScannerView(
                onResultScanned = { result ->
                    showScanner = false
                    if (result.classToken.isNotBlank()) {
                        manualCode = result.classToken
                        QrCodeUtils.copyTextToClipboard(context, "Code Classe", result.classToken)
                        Toast.makeText(
                            context,
                            context.getString(R.string.qr_code_copied, result.classToken),
                            Toast.LENGTH_SHORT
                        ).show()
                        onJoinCode(result.classToken)
                    }
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

                    if (studentRoster != null && studentRoster.qrToken.isNotBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.qr_assigned_hint, studentRoster.className),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Button(
                                    onClick = { onJoinCode(studentRoster.qrToken) },
                                    enabled = !isJoining,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("qr_quick_join_button")
                                ) {
                                    Text(stringResource(id = R.string.qr_assigned_join_now, studentRoster.className))
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (hasCameraPermission) {
                                showScanner = true
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        enabled = !isJoining,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("qr_scan_button")
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                        Spacer(Modifier.size(10.dp))
                        Text(stringResource(id = R.string.qr_scan_button))
                    }

                    OutlinedButton(
                        onClick = {
                            galleryQrLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isJoining,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("qr_gallery_button")
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(id = R.string.qr_pick_from_gallery))
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

                    val shownErr = galleryError ?: errorMessage
                    if (!shownErr.isNullOrBlank()) {
                        Text(
                            text = shownErr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val token = QrCodeUtils.extractTokenFromScan(manualCode)
                            if (token.isNotBlank()) onJoinCode(token)
                        },
                        enabled = manualCode.isNotBlank() && !isJoining,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("qr_manual_submit_button")
                    ) {
                        if (isJoining) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(id = R.string.qr_join_submit))
                        }
                    }
                }
            }
        }
    }
}

private suspend fun decodeQrFromUri(context: Context, uri: Uri): QrScanResult? =
    withContext(Dispatchers.IO) {
        runCatching {
            val bmp: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val src = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            QrCodeUtils.decodeQrFromBitmap(bmp)
        }.getOrNull()
    }

@SuppressLint("ClickableViewAccessibility")
@Composable
private fun QrCameraScannerView(
    onResultScanned: (QrScanResult) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    var galleryError by remember { mutableStateOf<String?>(null) }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val decoded = decodeQrFromUri(context, uri)
                if (decoded != null && decoded.classToken.isNotBlank()) {
                    onResultScanned(decoded)
                } else {
                    galleryError = context.getString(R.string.qr_not_found_in_image)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { boundCamera?.cameraControl?.enableTorch(false) }
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val resolutionSelector = ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    Size(1280, 720),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                )
                            )
                            .build()
                        val analysis = ImageAnalysis.Builder()
                            .setResolutionSelector(resolutionSelector)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor, QrImageAnalyzer { result ->
                                    ContextCompat.getMainExecutor(ctx).execute {
                                        onResultScanned(result)
                                    }
                                })
                            }
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                        boundCamera = camera
                        previewView.setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_UP) {
                                val point = previewView.meteringPointFactory.createPoint(event.x, event.y)
                                val action = FocusMeteringAction.Builder(point).build()
                                camera.cameraControl.startFocusAndMetering(action)
                            }
                            true
                        }
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

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (boundCamera?.cameraInfo?.hasFlashUnit() == true) {
                IconButton(
                    onClick = {
                        val next = !torchEnabled
                        torchEnabled = next
                        boundCamera?.cameraControl?.enableTorch(next)
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = if (torchEnabled) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                        contentDescription = stringResource(id = R.string.qr_torch_toggle),
                        tint = Color.White
                    )
                }
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier
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

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(bottom = 24.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!galleryError.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = galleryError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            OutlinedButton(
                onClick = {
                    galleryPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.65f),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(id = R.string.qr_pick_from_gallery))
            }
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
        color = Color.Transparent
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

@Composable
fun TermsAcceptanceScreen(
    studentName: String,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    isSubmitting: Boolean,
    errorMessage: String?,
    onAccept: () -> Unit,
    onSignOut: () -> Unit
) {
    var acceptedChecked by rememberSaveable { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageSwitcherRow(
                    currentLanguage = currentLanguage,
                    onLanguageSelected = onLanguageSelected
                )
                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("terms_sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = stringResource(id = R.string.auth_sign_out)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = stringResource(id = R.string.terms_acceptance_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(
                        id = R.string.terms_acceptance_subtitle,
                        studentName.ifBlank { stringResource(id = R.string.role_student) }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 340.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.terms_of_use_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(id = R.string.terms_of_use_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(
                    onClick = { showPrivacyDialog = true },
                    modifier = Modifier.testTag("terms_view_privacy_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(id = R.string.terms_acceptance_privacy_link))
                }

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (acceptedChecked) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .border(
                            width = 1.dp,
                            color = if (acceptedChecked) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = MaterialTheme.shapes.medium
                        )
                        .minimumInteractiveComponentSize()
                        .toggleable(
                            value = acceptedChecked,
                            enabled = !isSubmitting,
                            role = Role.Checkbox,
                            onValueChange = { acceptedChecked = it }
                        )
                        .testTag("terms_accept_checkbox")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = acceptedChecked,
                            onCheckedChange = null
                        )
                        Text(
                            text = stringResource(id = R.string.terms_acceptance_checkbox),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                AuthErrorCard(errorMessage = errorMessage)

                Button(
                    onClick = onAccept,
                    enabled = acceptedChecked && !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("terms_accept_submit_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(id = R.string.terms_acceptance_continue),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Text(
                text = stringResource(id = R.string.legal_notice_footer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    }
}

@Composable
fun DeviceLockedBlockedScreen(
    boundDeviceModel: String,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onSignOut: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageSwitcherRow(
                    currentLanguage = currentLanguage,
                    onLanguageSelected = onLanguageSelected
                )
                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("device_locked_top_sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = stringResource(id = R.string.auth_sign_out)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Text(
                    text = stringResource(id = R.string.device_lock_blocked_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(
                        id = R.string.device_lock_blocked_body,
                        boundDeviceModel.ifBlank { "Android" }
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(id = R.string.device_lock_teacher_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("device_locked_sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.auth_sign_out))
                }
            }

            Text(
                text = stringResource(id = R.string.legal_notice_footer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TeacherPendingApprovalScreen(
    profile: UserProfile,
    isOwnerAdminEmail: Boolean,
    isEmailVerified: Boolean,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onSendAdminVerification: () -> Unit,
    onRefreshStatus: () -> Unit,
    onSignOut: () -> Unit
) {
    val isRejected = profile.teacherStatus == "rejected"

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("teacher_pending_approval_screen"),
        color = Color.Transparent
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageSwitcherRow(
                    currentLanguage = currentLanguage,
                    onLanguageSelected = onLanguageSelected
                )
                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier.testTag("teacher_pending_sign_out_icon")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = stringResource(id = R.string.auth_sign_out)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRejected) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.secondaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isOwnerAdminEmail && !isEmailVerified) {
                            Icons.Filled.MarkEmailRead
                        } else {
                            Icons.Filled.HourglassTop
                        },
                        contentDescription = null,
                        tint = if (isRejected) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Text(
                    text = stringResource(
                        id = when {
                            isOwnerAdminEmail && !isEmailVerified -> R.string.admin_verify_email_title
                            isRejected -> R.string.teacher_approval_rejected_title
                            else -> R.string.teacher_approval_pending_title
                        }
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = when {
                        isOwnerAdminEmail && !isEmailVerified -> stringResource(id = R.string.admin_verify_email_body)
                        isRejected -> stringResource(id = R.string.teacher_approval_rejected_body)
                        else -> stringResource(
                            id = R.string.teacher_approval_pending_body,
                            profile.fullName,
                            profile.establishment.ifBlank { "-" }
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = profile.fullName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (profile.email.isNotBlank()) {
                            Text(profile.email, style = MaterialTheme.typography.bodySmall)
                        }
                        if (profile.phoneNumber.isNotBlank()) {
                            Text(profile.phoneNumber, style = MaterialTheme.typography.bodySmall)
                        }
                        if (profile.establishment.isNotBlank()) {
                            Text(
                                text = "${stringResource(id = R.string.field_establishment_label)} : ${profile.establishment}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (isOwnerAdminEmail && !isEmailVerified) {
                    Button(
                        onClick = onSendAdminVerification,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admin_send_verification_button")
                    ) {
                        Icon(Icons.Filled.MarkEmailRead, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(id = R.string.admin_send_verification_btn))
                    }
                }

                OutlinedButton(
                    onClick = onRefreshStatus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("teacher_refresh_approval_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.teacher_refresh_status_btn))
                }

                TextButton(
                    onClick = onSignOut,
                    modifier = Modifier.testTag("teacher_pending_sign_out_button")
                ) {
                    Text(stringResource(id = R.string.auth_sign_out))
                }
            }

            Text(
                text = stringResource(id = R.string.legal_notice_footer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StudentForgotPasswordDialog(
    initialMassar: String,
    onDismiss: () -> Unit
) {
    val cleanMassar = AuthSessionDraft.normalizeMassar(initialMassar)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.password_forgot_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(id = R.string.password_forgot_student_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (cleanMassar.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.password_forgot_student_massar_tip, cleanMassar),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("student_forgot_close_button")
            ) {
                Text(stringResource(id = R.string.close))
            }
        }
    )
}

@Composable
private fun TeacherForgotPasswordDialog(
    initialEmail: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var emailInput by remember { mutableStateOf(initialEmail.trim()) }
    var isSending by remember { mutableStateOf(false) }
    var sentToEmail by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val invalidEmailErr = stringResource(id = R.string.auth_error_email_invalid)

    AlertDialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        title = { Text(stringResource(id = R.string.password_forgot_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(id = R.string.password_forgot_teacher_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text(stringResource(id = R.string.field_email_pro)) },
                    placeholder = { Text(stringResource(id = R.string.field_email_pro_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("teacher_forgot_email_input")
                )
                if (!sentToEmail.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.password_forgot_email_sent, sentToEmail!!),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                if (!errorMsg.isNullOrBlank()) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = emailInput.trim()
                    if (clean.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(clean).matches()) {
                        errorMsg = invalidEmailErr
                        return@Button
                    }
                    errorMsg = null
                    isSending = true
                    scope.launch {
                        try {
                            Firebase.auth.sendPasswordResetEmail(clean).await()
                            sentToEmail = clean
                            isSending = false
                        } catch (e: Exception) {
                            isSending = false
                            errorMsg = mapFirebaseAuthError(context, e)
                        }
                    }
                },
                enabled = !isSending,
                modifier = Modifier.testTag("teacher_forgot_send_button")
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(id = R.string.password_forgot_send_link))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSending) {
                Text(stringResource(id = R.string.close))
            }
        }
    )
}


