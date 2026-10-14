package com.defat.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.defat.app.R
import com.defat.app.ui.theme.DefatTheme

@Composable
fun SignInRoute(
    viewModel: AuthViewModel,
    onCreateAccount: () -> Unit,
    onForgotPassword: () -> Unit,
    onCompleted: (AuthCompletion) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CompletionEffect(state.completion, viewModel, onCompleted)
    AuthFormScreen(
        title = stringResource(R.string.auth_sign_in_title),
        actionLabel = stringResource(R.string.auth_sign_in_action),
        state = state,
        showPasswordHint = false,
        onEmailChange = viewModel::setEmail,
        onPasswordChange = viewModel::setPassword,
        onSubmit = viewModel::signIn,
        switchLabel = stringResource(R.string.auth_to_sign_up),
        onSwitch = onCreateAccount,
        onForgotPassword = onForgotPassword,
    )
    ReplaceRecordsDialog(state, viewModel)
}

@Composable
fun SignUpRoute(
    viewModel: AuthViewModel,
    onHaveAccount: () -> Unit,
    onCompleted: (AuthCompletion) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CompletionEffect(state.completion, viewModel, onCompleted)
    AuthFormScreen(
        title = stringResource(R.string.auth_sign_up_title),
        actionLabel = stringResource(R.string.auth_sign_up_action),
        state = state,
        showPasswordHint = true,
        onEmailChange = viewModel::setEmail,
        onPasswordChange = viewModel::setPassword,
        onSubmit = viewModel::signUp,
        switchLabel = stringResource(R.string.auth_to_sign_in),
        onSwitch = onHaveAccount,
        onForgotPassword = null,
    )
    ReplaceRecordsDialog(state, viewModel)
}

@Composable
fun ForgotPasswordRoute(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(stringResource(R.string.auth_forgot_title), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.auth_forgot_body), style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(24.dp))
            EmailField(value = state.email, onValueChange = viewModel::setEmail, imeAction = ImeAction.Done)
            ErrorLine(state.error)
            if (state.resetSent) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.auth_forgot_sent), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = viewModel::sendPasswordReset,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.auth_forgot_send))
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.auth_back_to_sign_in))
            }
        }
    }
}

/**
 * Shown when the app opens signed in but the first sync never completed and
 * there is no profile on the phone (see StartDestinationPolicy). It runs the
 * sync itself; the only controls are retry and sign out.
 */
@Composable
fun FinishSignInRoute(
    viewModel: AuthViewModel,
    onSignOut: () -> Unit,
    onCompleted: (AuthCompletion) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.finishSignIn() }
    CompletionEffect(state.completion, viewModel, onCompleted)
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.auth_finish_title), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.auth_finish_body), style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(24.dp))
            if (state.busy) {
                CircularProgressIndicator()
            } else {
                ErrorLine(state.error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = viewModel::finishSignIn, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.auth_retry))
                }
                TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.account_sign_out))
                }
            }
        }
    }
    ReplaceRecordsDialog(state, viewModel)
}

@Composable
private fun CompletionEffect(
    completion: AuthCompletion?,
    viewModel: AuthViewModel,
    onCompleted: (AuthCompletion) -> Unit,
) {
    LaunchedEffect(completion) {
        if (completion != null) {
            onCompleted(completion)
            viewModel.consumeCompletion()
        }
    }
}

@Composable
fun AuthFormScreen(
    title: String,
    actionLabel: String,
    state: AuthUiState,
    showPasswordHint: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    switchLabel: String,
    onSwitch: () -> Unit,
    onForgotPassword: (() -> Unit)?,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))
            EmailField(value = state.email, onValueChange = onEmailChange, imeAction = ImeAction.Next)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.auth_password)) },
                supportingText = if (showPasswordHint) {
                    { Text(stringResource(R.string.auth_password_hint)) }
                } else {
                    null
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            ErrorLine(state.error)
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (state.busy) {
                    CircularProgressIndicator()
                } else {
                    Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth()) { Text(actionLabel) }
                }
            }
            if (state.busy) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.auth_working),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            if (onForgotPassword != null) {
                TextButton(onClick = onForgotPassword, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.auth_forgot_link))
                }
            }
            TextButton(onClick = onSwitch, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Text(switchLabel)
            }
        }
    }
}

@Composable
private fun EmailField(value: String, onValueChange: (String) -> Unit, imeAction: ImeAction) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.auth_email)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = imeAction),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ErrorLine(error: AuthUiError?) {
    if (error != null) {
        Text(
            text = stringResource(authErrorRes(error)),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

internal fun authErrorRes(error: AuthUiError): Int = when (error) {
    AuthUiError.INVALID_EMAIL -> R.string.auth_error_invalid_email
    AuthUiError.WEAK_PASSWORD -> R.string.auth_error_weak_password
    AuthUiError.EMAIL_IN_USE -> R.string.auth_error_email_in_use
    AuthUiError.WRONG_CREDENTIALS -> R.string.auth_error_wrong_credentials
    AuthUiError.TOO_MANY_REQUESTS -> R.string.auth_error_too_many
    AuthUiError.NETWORK -> R.string.auth_error_network
    AuthUiError.UNKNOWN -> R.string.auth_error_unknown
    AuthUiError.SYNC_FAILED -> R.string.auth_error_sync
}

/**
 * Case C (phase1.5 plan §6.4). Replace is destructive, so it is the *non-default*
 * action: plain text in the dismiss slot, no accent colour. Cancel takes the
 * confirm slot (the emphasised one), and so does tapping outside the dialog or
 * pressing back.
 */
@Composable
private fun ReplaceRecordsDialog(state: AuthUiState, viewModel: AuthViewModel) {
    val email = state.replacePromptEmail ?: return
    AlertDialog(
        onDismissRequest = viewModel::cancelReplace,
        title = { Text(stringResource(R.string.signin_other_account_title)) },
        text = { Text(stringResource(R.string.signin_other_account_body, email)) },
        confirmButton = {
            Button(onClick = viewModel::cancelReplace) {
                Text(stringResource(R.string.signin_other_account_cancel))
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::confirmReplace) {
                Text(
                    stringResource(R.string.signin_other_account_replace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Preview(name = "Light", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AuthFormPreview() {
    DefatTheme {
        AuthFormScreen(
            title = stringResource(R.string.auth_sign_in_title),
            actionLabel = stringResource(R.string.auth_sign_in_action),
            state = AuthUiState(email = "a@example.com", error = AuthUiError.WRONG_CREDENTIALS),
            showPasswordHint = false,
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {},
            switchLabel = stringResource(R.string.auth_to_sign_up),
            onSwitch = {},
            onForgotPassword = {},
        )
    }
}
