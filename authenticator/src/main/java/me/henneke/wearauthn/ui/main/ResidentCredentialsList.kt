package me.henneke.wearauthn.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Text
import me.henneke.wearauthn.R
import me.henneke.wearauthn.fido.context.AuthenticatorContext
import me.henneke.wearauthn.fido.context.WebAuthnCredential
import me.henneke.wearauthn.ui.WearBodyItem
import me.henneke.wearauthn.ui.WearButton
import me.henneke.wearauthn.ui.WearListScreen
import me.henneke.wearauthn.ui.WearSection
import me.henneke.wearauthn.ui.theme.WearAuthnTheme

@ExperimentalUnsignedTypes
class ResidentCredentialsList : ComponentActivity() {

    private var groups by mutableStateOf<List<CredentialGroup>>(emptyList())
    private var selected by mutableStateOf<CredentialItem?>(null)
    private var showDeleteConfirmation by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshCredentials()
        setContent {
            WearAuthnTheme {
                val item = selected
                if (item == null) {
                    CredentialList(groups = groups, onSelect = { selected = it })
                } else {
                    BackHandler { selected = null }
                    WearListScreen(title = item.rpId) {
                        item { WearBodyItem(text = item.formattedInfo) }
                        item {
                            WearButton(
                                label = stringResource(R.string.button_delete),
                                onClick = { showDeleteConfirmation = true },
                            )
                        }
                    }
                    DeleteConfirmation(
                        visible = showDeleteConfirmation,
                        item = item,
                        onConfirm = {
                            AuthenticatorContext.deleteResidentCredential(
                                this@ResidentCredentialsList,
                                item.credential,
                            )
                            showDeleteConfirmation = false
                            selected = null
                            refreshCredentials()
                        },
                        onDismiss = { showDeleteConfirmation = false },
                    )
                }
            }
        }
    }

    private fun refreshCredentials() {
        groups = AuthenticatorContext.getAllResidentCredentials(this).mapNotNull { (rpId, credentials) ->
            val items = credentials.mapIndexed { index, credential ->
                credential.unlockUserInfoIfNecessary()
                val info = credential.getTwoLineInfo(index + 1)
                CredentialItem(
                    rpId = rpId,
                    credential = credential,
                    title = info.first.toString(),
                    summary = info.second?.toString(),
                    formattedInfo = credential.getFormattedInfo().toString(),
                )
            }
            if (items.isEmpty()) null else CredentialGroup(rpId, items)
        }
    }
}

private data class CredentialGroup(val rpId: String, val items: List<CredentialItem>)

private data class CredentialItem(
    val rpId: String,
    val credential: WebAuthnCredential,
    val title: String,
    val summary: String?,
    val formattedInfo: String,
)

@Composable
private fun CredentialList(groups: List<CredentialGroup>, onSelect: (CredentialItem) -> Unit) {
    WearListScreen(
        title = stringResource(
            if (groups.isEmpty()) R.string.credential_management_title_no_credentials
            else R.string.credential_management_title,
        ),
    ) {
        groups.forEach { group ->
            item { WearSection(group.rpId) }
            group.items.forEach { item ->
                item(key = item.credential.hashCode()) {
                    WearButton(
                        label = item.title,
                        secondaryLabel = item.summary,
                        onClick = { onSelect(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirmation(
    visible: Boolean,
    item: CredentialItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        confirmButton = { AlertDialogDefaults.ConfirmButton(onClick = onConfirm) },
        dismissButton = { AlertDialogDefaults.DismissButton(onClick = onDismiss) },
        title = { Text(item.rpId) },
        text = {
            Text(stringResource(R.string.prompt_delete_resident_credential_message, item.title))
        },
    )
}
