package app.template.extension.smashyroad2;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Fakes Google Play Billing purchases for Smashy Road 2.
 *
 * When the game calls BillingClient.launchBillingFlow(), this intercepts
 * and immediately reports a successful purchase to the game's
 * PurchasesUpdatedListener, without ever showing Google Play UI.
 *
 * The listener is captured by hooking BillingClient.Builder.setListener().
 * The fake Purchase uses a dummy signature — games that don't verify
 * signatures locally (most Unity games) will accept it.
 *
 * This is purchase piracy: it grants paid items without payment.
 * The code itself does no network I/O, no data exfiltration.
 */
public final class BillingFaker {

    private static volatile PurchasesUpdatedListener sListener;

    private BillingFaker() {
    }

    /**
     * Called from the launchBillingFlow hook. Captures the listener if not
     * already set, then fakes a successful purchase.
     */
    public static BillingResult fakeLaunch(
            BillingClient client, Activity activity, BillingFlowParams params) {
        try {
            // Try to get listener from the client via reflection as fallback
            PurchasesUpdatedListener listener = sListener;
            if (listener == null) {
                listener = extractListener(client);
            }
            if (listener == null) {
                // No listener captured; return OK anyway (flow "launched")
                return newOkResult();
            }

            // Get product ID from params
            String productId = extractProductId(params);
            if (productId == null) productId = "fake_product";

            // Build fake purchase JSON
            String json = "{\"productId\":\"" + productId + "\","
                    + "\"purchaseToken\":\"fake_token_" + System.currentTimeMillis() + "\","
                    + "\"purchaseState\":1,"
                    + "\"purchaseTime\":" + System.currentTimeMillis() + ","
                    + "\"orderId\":\"fake_order_" + System.currentTimeMillis() + "\"}";

            final Purchase fakePurchase = new Purchase(json, "fake_signature");
            final List<Purchase> purchases = new ArrayList<>();
            purchases.add(fakePurchase);
            final BillingResult okResult = newOkResult();
            final PurchasesUpdatedListener finalListener = listener;

            // Call listener on main thread (BillingClient requires it)
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        finalListener.onPurchasesUpdated(okResult, purchases);
                    } catch (Throwable ignored) {
                    }
                }
            });

            return okResult;
        } catch (Throwable t) {
            // On any failure, return OK to avoid crashing the game
            return newOkResult();
        }
    }

    /**
     * Called from the Builder.setListener hook to capture the game's listener.
     */
    public static void setListener(PurchasesUpdatedListener listener) {
        sListener = listener;
    }

    private static BillingResult newOkResult() {
        return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.OK)
                .build();
    }

    private static PurchasesUpdatedListener extractListener(BillingClient client) {
        try {
            // BillingClientImpl stores listener in a field; try common names
            for (String fieldName : new String[]{"zzb", "listener", "mListener"}) {
                try {
                    Field f = client.getClass().getDeclaredField(fieldName);
                    f.setAccessible(true);
                    Object val = f.get(client);
                    if (val instanceof PurchasesUpdatedListener) {
                        sListener = (PurchasesUpdatedListener) val;
                        return sListener;
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String extractProductId(BillingFlowParams params) {
        try {
            // BillingFlowParams has getProductDetailsParamsList() in v5+
            // or getSku() in older versions. Try both via reflection.
            try {
                java.lang.reflect.Method m = params.getClass().getMethod("getProductDetailsParamsList");
                List<?> list = (List<?>) m.invoke(params);
                if (list != null && !list.isEmpty()) {
                    Object p = list.get(0);
                    java.lang.reflect.Method gm = p.getClass().getMethod("zza"); // obfuscated getter
                    Object pd = gm.invoke(p);
                    if (pd != null) {
                        java.lang.reflect.Method gpm = pd.getClass().getMethod("getProductId");
                        return (String) gpm.invoke(pd);
                    }
                }
            } catch (Throwable ignored) {
            }
            // Fallback: try getSku() (deprecated but still present in some versions)
            try {
                java.lang.reflect.Method m = params.getClass().getMethod("getSku");
                return (String) m.invoke(params);
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
