package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.core.AvatarStorageHelper
import com.example.core.BiometricAuthHelper
import com.example.core.BiometricAvailability
import com.example.core.PinVerifyResult
import com.example.core.SecurityLockManager
import com.example.core.findFragmentActivity
import kotlinx.coroutines.delay

@Composable
fun ProfileAvatarImage(
    userId: String,
    avatarUrl: String?,
    fullName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resolvedBitmap by produceState<Bitmap?>(initialValue = null, userId, avatarUrl) {
        value = AvatarStorageHelper.resolveLocalAvatarBitmap(context, userId, avatarUrl)
    }

    when {
        resolvedBitmap != null -> {
            Image(
                bitmap = resolvedBitmap!!.asImageBitmap(),
                contentDescription = fullName,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        }

        AvatarStorageHelper.isRemoteHttpUrl(avatarUrl) -> {
            AsyncImage(
                model = avatarUrl,
                contentDescription = fullName,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        }

        else -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = fullName,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}

@Composable
fun QuizQuestionImageBanner(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    if (imageUrl.isBlank()) return
    val isDataUri = AvatarStorageHelper.isDataUri(imageUrl)
    val decodedBitmap by produceState<Bitmap?>(initialValue = null, imageUrl) {
        value = if (isDataUri) AvatarStorageHelper.decodeDataUriBitmap(imageUrl) else null
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        when {
            isDataUri && decodedBitmap != null -> {
                Image(
                    bitmap = decodedBitmap!!.asImageBitmap(),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
            AvatarStorageHelper.isRemoteHttpUrl(imageUrl) -> {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }
    }
}

@Composable
fun GuestReadOnlyBanner(
    onExitGuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = modifier
            .fillMaxWidth()
            .testTag("guest_readonly_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Visibility,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.guest_mode_banner_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = stringResource(id = R.string.guest_mode_banner_desc),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
                )
            }
            FilledTonalButton(
                onClick = onExitGuest,
                modifier = Modifier.testTag("guest_exit_button")
            ) {
                Text(stringResource(id = R.string.guest_exit_button))
            }
        }
    }
}

@Composable
fun SecurityPinLockScreen(
    userId: String,
    userName: String,
    onUnlocked: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lockoutSeconds by remember {
        mutableIntStateOf(SecurityLockManager.getRemainingLockoutSeconds(context, userId))
    }
    val canBiometricUnlock = remember(userId, context) {
        BiometricAuthHelper.getAvailability(context) == BiometricAvailability.AVAILABLE &&
            BiometricAuthHelper.isBiometricEnabledForUser(context, userId)
    }

    fun triggerBiometricUnlock() {
        val fragmentActivity = context.findFragmentActivity() ?: return
        BiometricAuthHelper.authenticate(
            activity = fragmentActivity,
            title = context.getString(R.string.biometric_prompt_title),
            subtitle = context.getString(R.string.biometric_prompt_unlock_subtitle),
            negativeButtonText = context.getString(R.string.biometric_prompt_cancel_pin),
            onSuccess = {
                errorMessage = null
                onUnlocked()
            },
            onError = { err ->
                if (!err.isNullOrBlank()) {
                    errorMessage = err
                }
            }
        )
    }

    LaunchedEffect(userId, canBiometricUnlock) {
        if (canBiometricUnlock && lockoutSeconds == 0) {
            triggerBiometricUnlock()
        }
    }

    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000L)
            lockoutSeconds = SecurityLockManager.getRemainingLockoutSeconds(context, userId)
            if (lockoutSeconds == 0) {
                errorMessage = null
            }
        }
    }

    fun submitPin() {
        if (lockoutSeconds > 0 || enteredPin.length < SecurityLockManager.MIN_PIN_LENGTH) return
        when (val res = SecurityLockManager.verifyPin(context, userId, enteredPin)) {
            PinVerifyResult.Success, PinVerifyResult.NotConfigured -> {
                errorMessage = null
                onUnlocked()
            }
            is PinVerifyResult.Invalid -> {
                enteredPin = ""
                errorMessage = context.getString(R.string.security_pin_wrong_attempts, res.attemptsRemaining)
            }
            is PinVerifyResult.LockedOut -> {
                enteredPin = ""
                lockoutSeconds = res.remainingSeconds
                errorMessage = context.getString(R.string.security_pin_locked_seconds, res.remainingSeconds)
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("security_pin_lock_screen"),
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
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onSignOut,
                    modifier = Modifier.testTag("pin_lock_sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(id = R.string.auth_sign_out))
                }
            }

            Column(
                modifier = Modifier.widthIn(max = 400.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Text(
                    text = stringResource(id = R.string.security_lock_screen_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (userName.isNotBlank()) {
                        stringResource(id = R.string.security_lock_screen_subtitle_user, userName)
                    } else {
                        stringResource(id = R.string.security_lock_screen_subtitle)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // PIN dot indicator (supports 4 to 6 digits)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    repeat(SecurityLockManager.MAX_PIN_LENGTH) { idx ->
                        val filled = idx < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (filled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                val shownError = if (lockoutSeconds > 0) {
                    stringResource(id = R.string.security_pin_locked_seconds, lockoutSeconds)
                } else errorMessage
                if (!shownError.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = shownError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        )
                    }
                }

                // Numeric keypad
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    rows.forEach { rowDigits ->
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            rowDigits.forEach { digit ->
                                FilledTonalButton(
                                    onClick = {
                                        if (lockoutSeconds == 0 && enteredPin.length < SecurityLockManager.MAX_PIN_LENGTH) {
                                            errorMessage = null
                                            enteredPin += digit
                                        }
                                    },
                                    enabled = lockoutSeconds == 0,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .size(72.dp, 58.dp)
                                        .testTag("pin_key_$digit")
                                ) {
                                    Text(
                                        text = digit,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (enteredPin.isNotEmpty()) {
                                    enteredPin = enteredPin.dropLast(1)
                                }
                            },
                            enabled = lockoutSeconds == 0 && enteredPin.isNotEmpty(),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .size(72.dp, 58.dp)
                                .testTag("pin_key_backspace")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = stringResource(id = R.string.svt_dna_clear)
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                if (lockoutSeconds == 0 && enteredPin.length < SecurityLockManager.MAX_PIN_LENGTH) {
                                    errorMessage = null
                                    enteredPin += "0"
                                }
                            },
                            enabled = lockoutSeconds == 0,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .size(72.dp, 58.dp)
                                .testTag("pin_key_0")
                        ) {
                            Text(
                                text = "0",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { submitPin() },
                            enabled = lockoutSeconds == 0 && enteredPin.length >= SecurityLockManager.MIN_PIN_LENGTH,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .size(72.dp, 58.dp)
                                .testTag("pin_key_submit")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = stringResource(id = R.string.security_pin_unlock)
                            )
                        }
                    }

                    if (canBiometricUnlock) {
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { triggerBiometricUnlock() },
                            enabled = lockoutSeconds == 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("pin_lock_biometric_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fingerprint,
                                contentDescription = stringResource(id = R.string.biometric_unlock_button),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(id = R.string.biometric_unlock_button))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun SetupSecurityPinDialog(
    hasExistingPin: Boolean,
    userId: String,
    onSaved: (enabled: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                stringResource(
                    id = if (hasExistingPin) R.string.security_pin_manage_title
                    else R.string.security_pin_setup_title
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.security_pin_dialog_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (hasExistingPin) {
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { input ->
                            currentPin = input.filter { it.isDigit() }.take(SecurityLockManager.MAX_PIN_LENGTH)
                        },
                        label = { Text(stringResource(id = R.string.security_pin_current_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pin_current_input")
                    )
                }

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { input ->
                        newPin = input.filter { it.isDigit() }.take(SecurityLockManager.MAX_PIN_LENGTH)
                    },
                    label = { Text(stringResource(id = R.string.security_pin_new_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_new_input")
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { input ->
                        confirmPin = input.filter { it.isDigit() }.take(SecurityLockManager.MAX_PIN_LENGTH)
                    },
                    label = { Text(stringResource(id = R.string.security_pin_confirm_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_confirm_input")
                )

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (hasExistingPin) {
                    OutlinedButton(
                        onClick = {
                            when (val check = SecurityLockManager.verifyPin(context, userId, currentPin)) {
                                PinVerifyResult.Success, PinVerifyResult.NotConfigured -> {
                                    SecurityLockManager.disablePin(context, userId)
                                    onSaved(false)
                                }
                                is PinVerifyResult.Invalid -> {
                                    errorMessage = context.getString(
                                        R.string.security_pin_wrong_attempts,
                                        check.attemptsRemaining
                                    )
                                }
                                is PinVerifyResult.LockedOut -> {
                                    errorMessage = context.getString(
                                        R.string.security_pin_locked_seconds,
                                        check.remainingSeconds
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("pin_disable_button")
                    ) {
                        Text(stringResource(id = R.string.security_pin_disable_btn))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (hasExistingPin) {
                        when (val check = SecurityLockManager.verifyPin(context, userId, currentPin)) {
                            PinVerifyResult.Success, PinVerifyResult.NotConfigured -> Unit
                            is PinVerifyResult.Invalid -> {
                                errorMessage = context.getString(
                                    R.string.security_pin_wrong_attempts,
                                    check.attemptsRemaining
                                )
                                return@Button
                            }
                            is PinVerifyResult.LockedOut -> {
                                errorMessage = context.getString(
                                    R.string.security_pin_locked_seconds,
                                    check.remainingSeconds
                                )
                                return@Button
                            }
                        }
                    }
                    when {
                        !SecurityLockManager.isValidPinFormat(newPin) -> {
                            errorMessage = context.getString(R.string.security_pin_format_error)
                        }
                        newPin != confirmPin -> {
                            errorMessage = context.getString(R.string.security_pin_mismatch_error)
                        }
                        else -> {
                            SecurityLockManager.setPin(context, userId, newPin)
                            onSaved(true)
                        }
                    }
                },
                modifier = Modifier.testTag("pin_save_button")
            ) {
                Text(stringResource(id = R.string.profile_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}
