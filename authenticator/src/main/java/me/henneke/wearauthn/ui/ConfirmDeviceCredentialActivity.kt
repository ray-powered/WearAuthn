package me.henneke.wearauthn.ui

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import android.support.wearable.activity.WearableActivity
import android.widget.Button
import me.henneke.wearauthn.R

const val EXTRA_CONFIRM_DEVICE_CREDENTIAL_RECEIVER =
    "me.henneke.wearauthn.common.EXTRA_CONFIRM_DEVICE_CREDENTIAL_RECEIVER"

private const val REQUEST_CONFIRM_CREDENTIAL = 5

class ConfirmDeviceCredentialActivity : WearableActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_device_credential)

        findViewById<Button>(R.id.continueButton)?.setOnClickListener {
            launchDeviceCredentialConfirmation()
        }

        findViewById<Button>(R.id.cancelButton)?.setOnClickListener {
            returnResult(Activity.RESULT_CANCELED)
        }
    }

    private fun launchDeviceCredentialConfirmation() {
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        val title = getString(R.string.confirm_device_credential_title)
        val description = getString(R.string.confirm_device_credential_message)
        val launchIntent = keyguardManager?.createConfirmDeviceCredentialIntent(title, description)
        if (launchIntent == null) {
            returnResult(Activity.RESULT_CANCELED)
            return
        }
        startActivityForResult(launchIntent, REQUEST_CONFIRM_CREDENTIAL)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIRM_CREDENTIAL)
            returnResult(resultCode)
    }

    private fun returnResult(resultCode: Int) {
        intent.getParcelableExtra<ResultReceiver>(EXTRA_CONFIRM_DEVICE_CREDENTIAL_RECEIVER)
            ?.send(resultCode, Bundle())
        finish()
    }
}
