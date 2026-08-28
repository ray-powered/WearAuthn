package me.henneke.wearauthn.companion

import android.app.Activity
import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.*

class BillingManager private constructor(private val application: Application) :
    BillingClientStateListener, PurchasesUpdatedListener {

    private val _isComplicationUnlockedLiveData = MutableLiveData(true)
    val isComplicationUnlockedLiveData: LiveData<Boolean?>
        get() = _isComplicationUnlockedLiveData

    private val _productDetailsLiveData = mapOf(
        WearAuthnInAppProduct.Complication to MutableLiveData<ProductDetails>()
    )
    val productDetailsLiveData: Map<WearAuthnInAppProduct, LiveData<ProductDetails>>
        get() = _productDetailsLiveData

    private val _isBillingReady = MutableLiveData(true)
    val isBillingReady: LiveData<Boolean>
        get() = _isBillingReady

    fun connect() {
        _isComplicationUnlockedLiveData.postValue(true)
        _isBillingReady.postValue(true)
    }

    fun disconnect() {}

    fun updatePurchases() {
        _isComplicationUnlockedLiveData.postValue(true)
    }

    fun launchBillingFlow(activity: Activity, product: WearAuthnInAppProduct) {
        _isComplicationUnlockedLiveData.postValue(true)
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        _isComplicationUnlockedLiveData.postValue(true)
    }

    override fun onBillingServiceDisconnected() {}

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        _isComplicationUnlockedLiveData.postValue(true)
    }

    enum class WearAuthnInAppProduct(val sku: String) {
        Complication("complication_unlock")
    }

    companion object {
        @Volatile
        private var INSTANCE: BillingManager? = null

        fun getInstance(application: Application): BillingManager {
            return INSTANCE ?: synchronized(this) {
                val instance = BillingManager(application)
                INSTANCE = instance
                instance
            }
        }
    }
}