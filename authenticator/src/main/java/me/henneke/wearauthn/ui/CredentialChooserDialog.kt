package me.henneke.wearauthn.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentDialog
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.henneke.wearauthn.R
import me.henneke.wearauthn.fido.context.WebAuthnCredential
import me.henneke.wearauthn.ui.theme.WearAuthnTheme

private const val CREDENTIAL_CHOOSER_TIMEOUT_MS = 30_000L

@ExperimentalUnsignedTypes
class CredentialChooserDialog(
    private val credentials: Array<WebAuthnCredential>,
    context: Context,
    private val callback: (WebAuthnCredential?) -> Unit,
) : ComponentDialog(context), CoroutineScope {

    override val coroutineContext = Dispatchers.Main + SupervisorJob()
    private var chosenCredential: WebAuthnCredential? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            ComposeView(context).apply {
                setContent {
                    WearAuthnTheme {
                        WearListScreen(title = stringResource(R.string.credential_chooser_title)) {
                            credentials.forEachIndexed { index, credential ->
                                val info = credential.getTwoLineInfo(index + 1)
                                item(key = credential.hashCode()) {
                                    WearButton(
                                        label = info.first.toString(),
                                        secondaryLabel = info.second?.toString(),
                                        onClick = {
                                            if (chosenCredential == null) chosenCredential = credential
                                            dismiss()
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            },
        )
        setOnDismissListener { callback(chosenCredential) }
    }

    override fun onStart() {
        super.onStart()
        launch {
            delay(CREDENTIAL_CHOOSER_TIMEOUT_MS)
            cancel()
        }
    }

    override fun onStop() {
        coroutineContext.cancelChildren()
        super.onStop()
    }
}
