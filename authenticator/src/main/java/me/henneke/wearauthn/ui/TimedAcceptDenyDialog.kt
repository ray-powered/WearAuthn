package me.henneke.wearauthn.ui

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP
import android.os.PowerManager.FULL_WAKE_LOCK
import androidx.activity.ComponentDialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.delay
import me.henneke.wearauthn.R
import me.henneke.wearauthn.ui.theme.WearAuthnTheme

private const val DEFAULT_TIMEOUT = 5_000L
private const val COUNTDOWN_TICK_MS = 100L

class TimedAcceptDenyDialog(context: Context) : ComponentDialog(context) {

    var messageLineBreaks: List<Int>? = null
        private set

    private var titleText by mutableStateOf<CharSequence?>(null)
    private var messageText by mutableStateOf<CharSequence?>(null)
    private var timeoutMs by mutableLongStateOf(DEFAULT_TIMEOUT)
    private var hasPositiveButton by mutableStateOf(false)
    private var hasNegativeButton by mutableStateOf(false)

    private var wakeOnShow = false
    private var vibrateOnShow = false
    private var positiveButtonListener: DialogInterface.OnClickListener? = null
    private var negativeButtonListener: DialogInterface.OnClickListener? = null
    private var timeoutListener: DialogInterface.OnCancelListener? = null
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
        setContentView(
            ComposeView(context).apply {
                setContent {
                    WearAuthnTheme {
                        WearListScreen(
                            title = titleText?.toString()?.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.app_name),
                        ) {
                            messageText?.let { message ->
                                item {
                                    Text(
                                        text = message.toString(),
                                        onTextLayout = ::captureLineBreaks,
                                    )
                                }
                            }
                            if (hasPositiveButton) {
                                item {
                                    WearButton(
                                        label = stringResource(R.string.generic_accept),
                                        onClick = {
                                            positiveButtonListener?.onClick(
                                                this@TimedAcceptDenyDialog,
                                                DialogInterface.BUTTON_POSITIVE,
                                            )
                                            dismiss()
                                        },
                                    )
                                }
                            }
                            if (hasNegativeButton) {
                                item {
                                    var remaining by remember(timeoutMs) { mutableLongStateOf(timeoutMs) }
                                    LaunchedEffect(timeoutMs) {
                                        remaining = timeoutMs
                                        while (remaining > 0) {
                                            delay(COUNTDOWN_TICK_MS.coerceAtMost(remaining))
                                            remaining -= COUNTDOWN_TICK_MS
                                        }
                                        if (timeoutListener != null) {
                                            timeoutListener?.onCancel(this@TimedAcceptDenyDialog)
                                        } else {
                                            negativeButtonListener?.onClick(
                                                this@TimedAcceptDenyDialog,
                                                DialogInterface.BUTTON_NEGATIVE,
                                            )
                                        }
                                        dismiss()
                                    }
                                    WearButton(
                                        label = stringResource(R.string.generic_deny),
                                        secondaryLabel = stringResource(
                                            R.string.status_timeout_seconds,
                                            ((remaining + 999) / 1_000).coerceAtLeast(0),
                                        ),
                                        colors = ButtonDefaults.filledTonalButtonColors(),
                                        onClick = {
                                            negativeButtonListener?.onClick(
                                                this@TimedAcceptDenyDialog,
                                                DialogInterface.BUTTON_NEGATIVE,
                                            )
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
    }

    override fun onStart() {
        super.onStart()
        if (vibrateOnShow) wink(context)
        if (wakeOnShow) {
            @Suppress("DEPRECATION")
            wakeLock = context.powerManager?.newWakeLock(
                FULL_WAKE_LOCK or ACQUIRE_CAUSES_WAKEUP,
                "WearAuthn:WakeForDialog",
            )?.apply { acquire(timeoutMs) }
        }
    }

    override fun onStop() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        super.onStop()
    }

    fun setMessage(message: CharSequence?) {
        messageText = message
    }

    fun setNegativeButton(listener: DialogInterface.OnClickListener) {
        negativeButtonListener = listener
        hasNegativeButton = true
    }

    fun setPositiveButton(listener: DialogInterface.OnClickListener) {
        positiveButtonListener = listener
        hasPositiveButton = true
    }

    fun setTimeoutListener(listener: DialogInterface.OnCancelListener) {
        timeoutListener = listener
    }

    fun setTimeout(timeout: Long) {
        timeoutMs = timeout
    }

    override fun setTitle(title: CharSequence?) {
        titleText = title
    }

    override fun setTitle(resId: Int) {
        titleText = if (resId == 0) null else context.getText(resId)
    }

    fun setVibrateOnShow(vibrateOnShow: Boolean) {
        this.vibrateOnShow = vibrateOnShow
    }

    fun setWakeOnShow(wakeOnShow: Boolean) {
        this.wakeOnShow = wakeOnShow
    }

    private fun captureLineBreaks(layout: TextLayoutResult) {
        messageLineBreaks = (0 until layout.lineCount - 1).map(layout::getLineEnd)
    }
}
