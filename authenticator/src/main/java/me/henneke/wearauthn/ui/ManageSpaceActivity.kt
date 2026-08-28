package me.henneke.wearauthn.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.activity.ConfirmationActivity
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import kotlinx.coroutines.*
import me.henneke.wearauthn.R
import me.henneke.wearauthn.fido.context.AuthenticatorContext
import me.henneke.wearauthn.ui.theme.WearAuthnTheme
import kotlin.coroutines.CoroutineContext

const val EXTRA_MANAGE_SPACE_RECEIVER = "me.henneke.wearauthn.common.EXTRA_MANAGE_SPACE_RECEIVER"

@ExperimentalUnsignedTypes
class ManageSpaceActivity : ComponentActivity(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + SupervisorJob()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WearAuthnTheme {
                var step by remember { mutableStateOf(1) }
                val listState = rememberScalingLazyListState()

                Scaffold(
                    positionIndicator = {
                        PositionIndicator(scalingLazyListState = listState)
                    }
                ) {
                    ScalingLazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        state = listState,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.title2,
                                color = MaterialTheme.colors.error,
                                textAlign = TextAlign.Center
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (step == 1) {
                                    stringResource(R.string.prompt_delete_all_data_first_step_message)
                                        .replace("<b>", "").replace("</b>", "")
                                } else {
                                    stringResource(R.string.prompt_delete_all_data_second_step_message)
                                        .replace("\n\n\n\n\n\n\n\n\n\n\n", "\n\n")
                                        .replace("<b>", "").replace("</b>", "")
                                },
                                style = MaterialTheme.typography.body2,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colors.onSurface
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            if (step == 1) {
                                Chip(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { step = 2 },
                                    label = { Text(stringResource(R.string.generic_accept)) },
                                    colors = ChipDefaults.primaryChipColors()
                                )
                            } else {
                                Chip(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { deleteAllData() },
                                    label = { Text(stringResource(R.string.button_delete)) },
                                    colors = ChipDefaults.chipColors(
                                        backgroundColor = MaterialTheme.colors.error,
                                        contentColor = MaterialTheme.colors.onError
                                    )
                                )
                            }
                        }
                        item {
                            CompactChip(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { returnResult(Activity.RESULT_CANCELED) },
                                label = { Text(stringResource(R.string.generic_deny)) },
                                colors = ChipDefaults.secondaryChipColors()
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancelChildren()
    }

    private fun deleteAllData() {
        launch {
            try {
                AuthenticatorContext.deleteAllData(this@ManageSpaceActivity)
                withContext(Dispatchers.Main) {
                    startActivity(
                        Intent(
                            this@ManageSpaceActivity,
                            ConfirmationActivity::class.java
                        ).apply {
                            putExtra(
                                ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                                ConfirmationActivity.SUCCESS_ANIMATION
                            )
                            putExtra(
                                ConfirmationActivity.EXTRA_MESSAGE,
                                getString(R.string.message_deleted_all_data)
                            )
                        }
                    )
                    returnResult(Activity.RESULT_OK)
                }
            } catch (e: CancellationException) {
                returnResult(Activity.RESULT_CANCELED)
            }
        }
    }

    private fun returnResult(resultCode: Int) {
        intent.getParcelableExtra<ResultReceiver>(EXTRA_MANAGE_SPACE_RECEIVER)
            ?.send(resultCode, Bundle())
        finish()
    }
}