package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.util.Patterns
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.core.AppLanguage
import com.example.core.AuthSessionDraft
import com.example.core.QrCodeUtils
import com.example.core.QrImageAnalyzer
import com.example.data.model.Enrollment
import com.example.data.model.RosterStudent
import com.example.ui.components.LanguageSwitcherRow
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
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
    onAuthSuccess: () -> Unit
) {
    val localizedContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedAuthRole by remember { mutableStateOf<String?>(null) } // null -> role question, "student", "teacher"
    var isTeacherRegister by remember { mutableStateOf(false) }

    var massarInput by remember { mutableStateOf("") }
    var studentPassword by remember { mutableStateOf("") }

    var teacherFullName by remember { mutableStateOf("") }
    var teacherEmail by remember { mutableStateOf("") }
    var teacherPassword by remember { mutableStateOf("") }
    var teacherConfirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                            painter = painterResource(id = R.drawable.img_svt_hero),
                            contentDescription = stringResource(id = R.string.app_subtitle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(MaterialTheme.shapes.large),
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
                            OutlinedTextField(
                                value = teacherFullName,
                                onValueChange = { teacherFullName = it },
                                label = { Text(stringResource(id = R.string.field_full_name)) },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("teacher_fullname_input")
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
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("teacher_confirm_password_input")
                            )
                        }

                        AuthErrorCard(errorMessage = errorMessage)

                        Button(
                            onClick = {
                                val cleanEmail = teacherEmail.trim()
                                val cleanName = teacherFullName.trim()
                                when {
                                    isTeacherRegister && cleanName.length < 2 -> {
                                        errorMessage = localizedContext.getString(R.string.error_name_required)
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
                                                    fullName = cleanName
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

            Spacer(Modifier.height(8.dp))
        }
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
            containerColor = accentColor.copy(alpha = 0.08f)
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
        newPassword: String?
    ) -> Unit,
    onSignOut: () -> Unit,
    serverError: String? = null
) {
    val resolvedRole = if (studentRoster != null) "student" else defaultRole
    val initialName = studentRoster?.fullName?.takeIf { it.isNotBlank() } ?: defaultName
    val resolvedMassar = studentRoster?.massarNumber?.takeIf { it.isNotBlank() } ?: defaultMassar

    var fullName by remember(initialName) { mutableStateOf(initialName) }
    var birthDate by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val nameRequiredErr = stringResource(id = R.string.error_name_required)
    val birthDateRequiredErr = stringResource(id = R.string.error_birthdate_required)
    val weakPassErr = stringResource(id = R.string.auth_error_weak_password)
    val mismatchPassErr = stringResource(id = R.string.auth_error_password_mismatch)

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
                    label = { Text(stringResource(id = R.string.field_phone)) },
                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_phone_input")
                )

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
                                    newPassword.takeIf { it.isNotBlank() }
                                )
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
    onJoinCode: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var showScanner by remember { mutableStateOf(false) }
    var manualCode by remember(studentRoster?.qrToken) {
        mutableStateOf(studentRoster?.qrToken.orEmpty())
    }
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
