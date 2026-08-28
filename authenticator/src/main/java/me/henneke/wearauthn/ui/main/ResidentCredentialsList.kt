package me.henneke.wearauthn.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import me.henneke.wearauthn.R
import me.henneke.wearauthn.fido.context.AuthenticatorContext
import me.henneke.wearauthn.fido.context.WebAuthnCredential
import me.henneke.wearauthn.ui.theme.WearAuthnTheme

@ExperimentalUnsignedTypes
class ResidentCredentialsList : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WearAuthnTheme {
                val listState = rememberScalingLazyListState()
                var credentialsMap by remember {
                    mutableStateOf(AuthenticatorContext.getAllResidentCredentials(this@ResidentCredentialsList))
                }
                var selectedCredential by remember { mutableStateOf<Pair<String, WebAuthnCredential>?>(null) }
                var showDeleteConfirmDialog by remember { mutableStateOf(false) }

                ScreenScaffold(scrollState = listState) {
                    ScalingLazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        state = listState,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            ListHeader {
                                Text(
                                    text = if (credentialsMap.isEmpty()) {
                                        stringResource(R.string.credential_management_title_no_credentials)
                                    } else {
                                        stringResource(R.string.credential_management_title)
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        for ((rpId, credentials) in credentialsMap) {
                            if (credentials.isEmpty()) continue

                            item {
                                ListHeader {
                                    Text(
                                        text = rpId,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            items(credentials) { credential ->
                                credential.unlockUserInfoIfNecessary()
                                val twoLineInfo = credential.getTwoLineInfo(1)
                                Chip(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        selectedCredential = Pair(rpId, credential)
                                    },
                                    label = { Text(twoLineInfo.first) },
                                    secondaryLabel = twoLineInfo.second?.let { { Text(it) } },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_btn_key),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    colors = ChipDefaults.chipColors()
                                )
                            }
                        }
                    }
                }

                // Credential details dialog
                selectedCredential?.let { (rpId, credential) ->
                    val info = credential.getFormattedInfo() ?: ""
                    val dialogListState = rememberScalingLazyListState()
                    ScreenScaffold(scrollState = dialogListState) {
                        ScalingLazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            state = dialogListState,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item {
                                Text(
                                    text = rpId,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = info,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { showDeleteConfirmDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.button_delete))
                                }
                            }
                            item {
                                FilledTonalButton(
                                    onClick = { selectedCredential = null },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.confirm_lock_cancel))
                                }
                            }
                        }
                    }

                    // Delete confirmation dialog
                    if (showDeleteConfirmDialog) {
                        val confirmListState = rememberScalingLazyListState()
                        ScreenScaffold(scrollState = confirmListState) {
                            ScalingLazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                state = confirmListState,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                item {
                                    Text(
                                        text = stringResource(
                                            R.string.prompt_delete_resident_credential_message,
                                            credential.getTwoLineInfo(1).first
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            AuthenticatorContext.deleteResidentCredential(
                                                this@ResidentCredentialsList,
                                                credential
                                            )
                                            credentialsMap = AuthenticatorContext.getAllResidentCredentials(this@ResidentCredentialsList)
                                            showDeleteConfirmDialog = false
                                            selectedCredential = null
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(stringResource(R.string.button_delete))
                                    }
                                }
                                item {
                                    FilledTonalButton(
                                        onClick = { showDeleteConfirmDialog = false },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(stringResource(R.string.confirm_lock_cancel))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}