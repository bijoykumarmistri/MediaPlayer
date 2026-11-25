package com.mediaplayer.distribution

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Launch
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import com.mediaplayer.util.AppReleaseInfo
import com.mediaplayer.util.AppUpdateManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object DistributionPlatform {
    private const val TAG = "PlayStoreDistribution"

    const val IS_PLAY_STORE: Boolean = true
    const val DEVELOPER_NAME: String = "Bijoy Kumar Mistri"
    const val DEVELOPER_SUBTITLE: String = "By Bijoy Kumar Mistri • India 🇮🇳"
    const val DEVELOPER_GITHUB_PROFILE_URL: String = "https://github.com/bijoykumarmistri"
    const val SOURCE_REPO_URL: String = "https://github.com/bijoykumarmistri/MediaPlayer"
    const val PRIVACY_POLICY_URL: String =
        "https://bijoykumarmistri.github.io/MediaPlayer/privacy_policy.html"

    // Optional one-time consumable In-App Product IDs (no subscriptions).
    // Configure these in Play Console > Monetize > Products > In-app products.
    val TIP_PRODUCT_IDS: List<String> = listOf(
        "tip_small",
        "tip_medium",
        "tip_large",
        "tip_generous"
    )

    val shareUrl: String
        get() = AppUpdateManager.PLAY_STORE_WEB_URL

    val primaryActionLabel: String
        get() = "GitHub Profile"

    val updateBadgeText: String
        get() = "Official Release • Google Play Store"

    val updatePrimaryButtonLabel: String
        get() = "Update on Google Play"

    val showSecondaryReleaseButton: Boolean
        get() = false

    fun openPrimaryAction(context: Context) {
        AppUpdateManager.openUrl(context, DEVELOPER_GITHUB_PROFILE_URL)
    }

    suspend fun checkForUpdates(context: Context, isManual: Boolean): Result<AppReleaseInfo?> {
        val currentVersion = AppUpdateManager.getCurrentVersionName(context)
        return suspendCancellableCoroutine { cont ->
            try {
                val playUpdateManager = AppUpdateManagerFactory.create(context.applicationContext)
                playUpdateManager.appUpdateInfo
                    .addOnSuccessListener { appUpdateInfo ->
                        val updateAvailable =
                            appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        if (!isManual && !updateAvailable) {
                            if (cont.isActive) cont.resume(Result.success(null))
                            return@addOnSuccessListener
                        }
                        val info = AppReleaseInfo(
                            tagName = if (updateAvailable) "Play Store Update" else "v$currentVersion",
                            releaseTitle = if (updateAvailable) {
                                "BKM Player Update Available"
                            } else {
                                "App is Up to Date"
                            },
                            releaseNotes = if (updateAvailable) {
                                "A newer version of BKM Player is ready on Google Play with the latest fixes and improvements."
                            } else {
                                "You are running the latest version of BKM Player."
                            },
                            releaseUrl = AppUpdateManager.PLAY_STORE_WEB_URL,
                            downloadUrl = AppUpdateManager.PLAY_STORE_MARKET_URI,
                            isUpdateAvailable = updateAvailable,
                            currentVersionName = currentVersion
                        )
                        if (cont.isActive) cont.resume(Result.success(info))
                    }
                    .addOnFailureListener { error ->
                        Log.w(TAG, "Play In-App Update check unavailable", error)
                        if (isManual) {
                            // Gracefully report current status when sideloaded or Play Store unavailable
                            val fallback = AppReleaseInfo(
                                tagName = "v$currentVersion",
                                releaseTitle = "Check Google Play",
                                releaseNotes = "Open Google Play to check for the latest release.",
                                releaseUrl = AppUpdateManager.PLAY_STORE_WEB_URL,
                                downloadUrl = AppUpdateManager.PLAY_STORE_MARKET_URI,
                                isUpdateAvailable = false,
                                currentVersionName = currentVersion
                            )
                            if (cont.isActive) cont.resume(Result.success(fallback))
                        } else {
                            if (cont.isActive) cont.resume(Result.success(null))
                        }
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to query Play AppUpdateManager", e)
                if (cont.isActive) {
                    cont.resume(if (isManual) Result.failure(e) else Result.success(null))
                }
            }
        }
    }

    fun openUpdateDestination(context: Context, releaseInfo: AppReleaseInfo) {
        AppUpdateManager.openPlayStore(context)
    }

    @Composable
    fun SupportAndDeveloperCards() {
        val context = LocalContext.current
        var tipProducts by remember { mutableStateOf<List<ProductDetails>>(emptyList()) }
        var selectedProduct by remember { mutableStateOf<ProductDetails?>(null) }
        var isPurchasing by remember { mutableStateOf(false) }
        var billingClientRef by remember { mutableStateOf<BillingClient?>(null) }

        DisposableEffect(context) {
            val appContext = context.applicationContext
            var client: BillingClient? = null

            fun consumePurchaseIfNeeded(purchase: Purchase) {
                val activeClient = client ?: return
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    val consumeParams = ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    activeClient.consumeAsync(consumeParams) { result, _ ->
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            Log.d(TAG, "Consumed tip purchase")
                        }
                    }
                }
            }

            val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
                isPurchasing = false
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        purchases?.forEach { purchase ->
                            when (purchase.purchaseState) {
                                Purchase.PurchaseState.PURCHASED -> {
                                    consumePurchaseIfNeeded(purchase)
                                    Toast.makeText(
                                        appContext,
                                        "Thank you for supporting BKM Player!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                Purchase.PurchaseState.PENDING -> {
                                    Toast.makeText(
                                        appContext,
                                        "Payment is pending in Google Play.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                else -> Unit
                            }
                        }
                    }
                    BillingClient.BillingResponseCode.USER_CANCELED -> Unit
                    else -> {
                        Toast.makeText(
                            appContext,
                            "Google Play purchase could not be completed.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            val createdClient = BillingClient.newBuilder(appContext)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases(
                    PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .build()
                )
                .enableAutoServiceReconnection()
                .build()
            client = createdClient
            billingClientRef = createdClient

            createdClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Play Billing setup skipped: ${billingResult.responseCode}")
                        return
                    }

                    // Consume any unfinished one-time tip purchases from prior sessions
                    createdClient.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    ) { queryResult, existingPurchases ->
                        if (queryResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            existingPurchases.forEach { consumePurchaseIfNeeded(it) }
                        }
                    }

                    val productList = TIP_PRODUCT_IDS.map { id ->
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(id)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    }
                    val params = QueryProductDetailsParams.newBuilder()
                        .setProductList(productList)
                        .build()

                    createdClient.queryProductDetailsAsync(params) { result, detailsResult ->
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            val sorted = detailsResult.productDetailsList.sortedBy {
                                it.oneTimePurchaseOfferDetails?.priceAmountMicros ?: Long.MAX_VALUE
                            }
                            tipProducts = sorted
                            if (selectedProduct == null && sorted.isNotEmpty()) {
                                selectedProduct = sorted.first()
                            }
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // Handled automatically by enableAutoServiceReconnection()
                }
            })

            onDispose {
                billingClientRef = null
                runCatching { createdClient.endConnection() }
            }
        }

        // Only display Google Play Tip Jar when one-time IAP products are configured in Play Console.
        // No subscriptions are used; the app remains 100% free.
        if (tipProducts.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Support Development",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Optional one-time tip via Google Play • App stays 100% free",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tipProducts.forEach { product ->
                            val isSelected = selectedProduct?.productId == product.productId
                            val priceLabel = product.oneTimePurchaseOfferDetails?.formattedPrice
                                ?: product.name
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        },
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedProduct = product }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = priceLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val activity = context.findActivity()
                            val client = billingClientRef
                            val product = selectedProduct ?: tipProducts.firstOrNull()
                            if (activity != null && client != null && product != null && !isPurchasing) {
                                isPurchasing = true
                                val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(product)
                                    .build()
                                val flowParams = BillingFlowParams.newBuilder()
                                    .setProductDetailsParamsList(listOf(productParams))
                                    .build()
                                val launchResult = client.launchBillingFlow(activity, flowParams)
                                if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                                    isPurchasing = false
                                    Toast.makeText(
                                        context,
                                        "Unable to open Google Play Billing.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        enabled = !isPurchasing && selectedProduct != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        if (isPurchasing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Opening Google Play...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val price = selectedProduct?.oneTimePurchaseOfferDetails?.formattedPrice
                            val label = if (price != null) "Send $price Tip via Google Play" else "Send Tip via Google Play"
                            Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Developer, GitHub Profile & Policy Links Card (100% Play Store Compliant)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Developer & Open Source",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "github.com/bijoykumarmistri",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { AppUpdateManager.openUrl(context, DEVELOPER_GITHUB_PROFILE_URL) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Launch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GitHub Profile", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { AppUpdateManager.openPlayStore(context) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rate App", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { AppUpdateManager.openUrl(context, PRIVACY_POLICY_URL) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PrivacyTip,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Privacy Policy", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
