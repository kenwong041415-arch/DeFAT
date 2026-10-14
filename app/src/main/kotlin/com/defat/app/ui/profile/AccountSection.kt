package com.defat.app.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.defat.app.R
import com.defat.app.ui.component.SectionCard
import com.defat.core.domain.sync.SyncStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** What the 我 tab's account block can do. Grouped so ProfileScreen takes one parameter. */
data class AccountActions(
    val onSignOut: () -> Unit,
    val onExport: () -> Unit,
    val onDeleteAccount: (password: String) -> Unit,
    val onDismissDeleteError: () -> Unit,
)

@Composable
fun AccountSection(
    state: AccountUiState,
    exportFailed: Boolean,
    actions: AccountActions,
    modifier: Modifier = Modifier,
) {
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    SectionCard(title = stringResource(R.string.account_title), modifier = modifier) {
        if (state.email != null) {
            Text(
                stringResource(R.string.account_signed_in_as, state.email),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        SyncStatusLines(state.sync)

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = actions.onExport, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.account_export))
        }
        if (exportFailed) {
            Text(
                stringResource(R.string.account_export_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        OutlinedButton(onClick = { showSignOutDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.account_sign_out))
        }
        TextButton(onClick = { showDeleteDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.account_delete), color = MaterialTheme.colorScheme.error)
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.account_sign_out_title)) },
            // Says plainly that nothing is deleted (plan D7).
            text = { Text(stringResource(R.string.account_sign_out_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        actions.onSignOut()
                    },
                ) { Text(stringResource(R.string.account_sign_out)) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (showDeleteDialog) {
        DeleteAccountDialog(
            delete = state.delete,
            onConfirm = actions.onDeleteAccount,
            onDismiss = {
                showDeleteDialog = false
                actions.onDismissDeleteError()
            },
        )
    }
}

@Composable
private fun SyncStatusLines(status: SyncStatus) {
    // A property of a class from another module cannot be smart-cast, so read it once.
    val lastSyncAt = status.lastSyncAt
    val text = when {
        status.running -> stringResource(R.string.sync_status_running)
        lastSyncAt == null -> stringResource(R.string.sync_status_never)
        else -> stringResource(R.string.sync_status_last, formatSyncTime(lastSyncAt))
    }
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (status.lastFailed && !status.running) {
        Text(
            stringResource(R.string.sync_status_retry),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

private val timeOnly: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateAndTime: DateTimeFormatter = DateTimeFormatter.ofPattern("d/M HH:mm")

/** 14:32 for today, 3/10 14:32 for an earlier day. */
internal fun formatSyncTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault(), today: LocalDate = LocalDate.now(zone)): String {
    val moment = Instant.ofEpochMilli(epochMillis).atZone(zone)
    return if (moment.toLocalDate() == today) moment.format(timeOnly) else moment.format(dateAndTime)
}

@Composable
private fun DeleteAccountDialog(
    delete: DeleteUiState,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    val busy = delete == DeleteUiState.Busy
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.account_delete_title)) },
        text = {
            Column {
                Text(stringResource(R.string.account_delete_body), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.account_delete_password_why),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.account_delete_password_label)) },
                    singleLine = true,
                    enabled = !busy,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (delete is DeleteUiState.Failed) {
                    Text(
                        stringResource(deleteErrorRes(delete.error)),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password) }, enabled = !busy && password.isNotEmpty()) {
                Text(stringResource(R.string.account_delete_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

private fun deleteErrorRes(error: DeleteError): Int = when (error) {
    DeleteError.WRONG_PASSWORD -> R.string.account_delete_error_password
    DeleteError.TOO_MANY_REQUESTS -> R.string.auth_error_too_many
    DeleteError.NETWORK -> R.string.auth_error_network
    DeleteError.CLOUD -> R.string.account_delete_error_cloud
    DeleteError.LOCAL -> R.string.account_delete_error_local
    DeleteError.UNKNOWN -> R.string.auth_error_unknown
}
