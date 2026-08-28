package me.henneke.wearauthn.ui

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
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
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import me.henneke.wearauthn.R
import me.henneke.wearauthn.ui.theme.WearAuthnTheme

const val EXTRA_CONFIRM_DEVICE_CREDENTIAL_RECEIVER =
    "me.henneke.wearauthn.common.EXTRA_CONFIRM_DEVICE_CREDENTIAL_RECEIVER"

private const val REQUEST_CONFIRM_CREDENTIAL = 5

class ConfirmDeviceCredentialActivity : ComponentActivity() {

    private var hasLaunched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            hasLaunched = savedInstanceState.getBoolean("hasLaunched", false)
        }

        setContent {
            WearAuthnTheme {
                val listState = rememberScalingLazyListState()
                ScreenScaffold(scrollState = listState) {
                    ScalingLazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        state = listState,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_launcher_outline),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.confirm_lock_title),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.confirm_lock_message),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { launchCredentialConfirmation() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.confirm_lock_continue))
                            }
                        }
                        item {
                            FilledTonalButton(
                                onClick = { returnResult(Activity.RESULT_CANCELED) },
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("hasLaunched", hasLaunched)
    }

    private fun launchCredentialConfirmation() {
        if (hasLaunched) return
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        val launchIntent = keyguardManager?.createConfirmDeviceCredentialIntent(null, null)
        if (launchIntent == null) {
            returnResult(Activity.RESULT_CANCELED)
            return
        }
        hasLaunched = true
        startActivityForResult(launchIntent, REQUEST_CONFIRM_CREDENTIAL)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIRM_CREDENTIAL) {
            returnResult(resultCode)
        }
    }

    private fun returnResult(resultCode: Int) {
        intent.getParcelableExtra<ResultReceiver>(EXTRA_CONFIRM_DEVICE_CREDENTIAL_RECEIVER)
            ?.send(resultCode, Bundle())
        finish()
    }
}
