package me.henneke.wearauthn.ui

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.ResultReceiver
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.ButtonDefaults
import me.henneke.wearauthn.R
import me.henneke.wearauthn.ui.theme.WearAuthnTheme

const val EXTRA_CONFIRM_DEVICE_CREDENTIAL_RECEIVER =
    "me.henneke.wearauthn.common.EXTRA_CONFIRM_DEVICE_CREDENTIAL_RECEIVER"

class ConfirmDeviceCredentialActivity : ComponentActivity() {

    private val receivers = mutableListOf<ResultReceiver>()
    private var credentialPromptLaunched = false

    private val credentialResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            returnResult(result.resultCode)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addReceiver(intent)
        setContent {
            WearAuthnTheme {
                BackHandler { returnResult(Activity.RESULT_CANCELED) }
                WearListScreen(title = stringResource(R.string.prompt_device_credential_title)) {
                    item {
                        WearBodyItem(text = stringResource(R.string.prompt_device_credential_message))
                    }
                    item {
                        WearButton(
                            label = stringResource(R.string.generic_continue),
                            onClick = ::launchCredentialPrompt,
                            enabled = !credentialPromptLaunched,
                        )
                    }
                    item {
                        WearButton(
                            label = stringResource(R.string.generic_cancel),
                            onClick = { returnResult(Activity.RESULT_CANCELED) },
                            colors = ButtonDefaults.filledTonalButtonColors(),
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        addReceiver(intent)
    }

    private fun addReceiver(source: Intent) {
        source.resultReceiverExtra(EXTRA_CONFIRM_DEVICE_CREDENTIAL_RECEIVER)?.let(receivers::add)
    }

    @Suppress("DEPRECATION")
    private fun launchCredentialPrompt() {
        if (credentialPromptLaunched) return
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        val launchIntent = keyguardManager?.createConfirmDeviceCredentialIntent(
            getString(R.string.prompt_device_credential_system_title),
            getString(R.string.prompt_device_credential_system_message),
        )
        if (launchIntent == null) {
            returnResult(Activity.RESULT_CANCELED)
            return
        }
        credentialPromptLaunched = true
        credentialResult.launch(launchIntent)
    }

    private fun returnResult(resultCode: Int) {
        receivers.distinct().forEach { it.send(resultCode, Bundle.EMPTY) }
        receivers.clear()
        setResult(resultCode)
        finish()
    }
}

@Suppress("DEPRECATION")
private fun Intent.resultReceiverExtra(key: String): ResultReceiver? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, ResultReceiver::class.java)
    } else {
        getParcelableExtra(key)
    }
