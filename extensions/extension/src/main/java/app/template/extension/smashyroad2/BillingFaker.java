package app.template.extension.smashyroad2;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Fakes Google Play Billing purchases for Smashy Road 2 using pure reflection.
 *
 * No compile-time dependency on billingclient — all classes are loaded
 * reflectively at runtime from the game's APK.
 *
 * When the game calls BillingClient.launchBillingFlow(), this intercepts
 * and immediately reports a successful purchase to the game's
 * PurchasesUpdatedListener, without ever showing Google Play UI.
 *
 * This is purchase piracy: it grants paid items without payment.
 * The code itself does no network I/O, no data exfiltration.
 */
public final class BillingFaker {

    private static volatile Object sListener; // PurchasesUpdatedListener

    private BillingFaker() {
    }

    /**
     * Called from the launchBillingFlow hook.
     * Params are Object to avoid compile-time billing dependencies.
     * Returns a BillingResult (as Object).
     */
    public static Object fakeLaunch(Object client, Activity activity, Object params) {
        try {
            Object listener = sListener;
            if (listener == null) {
                listener = extractListener(client);
            }

            Object okResult = newOkResult();
            if (listener == null) {
                return okResult;
            }

            String productId = extractProductId(params);
            if (productId == null) productId = "fake_product";

            String json = "{\"productId\":\"" + productId + "\","
                    + "\"purchaseToken\":\"fake_token_" + System.currentTimeMillis() + "\","
                    + "\"purchaseState\":1,"
                    + "\"purchaseTime\":" + System.currentTimeMillis() + ","
                    + "\"orderId\":\"fake_order_" + System.currentTimeMillis() + "\"}";

            // new Purchase(json, "fake_signature")
            Class<?> purchaseClass = Class.forName("com.android.billingclient.api.Purchase");
            Constructor<?> purchaseCtor = purchaseClass.getConstructor(String.class, String.class);
            Object fakePurchase = purchaseCtor.newInstance(json, "fake_signature");

            List<Object> purchases = new ArrayList<>();
            purchases.add(fakePurchase);

            final Object finalListener = listener;
            final Object finalResult = okResult;
            final List<Object> finalPurchases = purchases;

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        Method m = finalListener.getClass().getMethod("onPurchasesUpdated",
                                Class.forName("com.android.billingclient.api.BillingResult"),
                                List.class);
                        m.invoke(finalListener, finalResult, finalPurchases);
                    } catch (Throwable ignored) {
                    }
                }
            });

            return okResult;
        } catch (Throwable t) {
            try {
                return newOkResult();
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    /**
     * Called from the Builder.setListener hook.
     */
    public static void setListener(Object listener) {
        sListener = listener;
    }

    private static Object newOkResult() throws Exception {
        Class<?> resultClass = Class.forName("com.android.billingclient.api.BillingResult");
        Class<?> builderClass = Class.forName("com.android.billingclient.api.BillingResult$Builder");
        Method newBuilder = resultClass.getMethod("newBuilder");
        Object builder = newBuilder.invoke(null);
        // BillingResponseCode.OK = 0
        Method setCode = builderClass.getMethod("setResponseCode", int.class);
        setCode.invoke(builder, 0);
        Method build = builderClass.getMethod("build");
        return build.invoke(builder);
    }

    private static Object extractListener(Object client) {
        try {
            for (String fieldName : new String[]{"zzb", "listener", "mListener"}) {
                try {
                    Field f = client.getClass().getDeclaredField(fieldName);
                    f.setAccessible(true);
                    Object val = f.get(client);
                    if (val != null && val.getClass().getName().contains("PurchasesUpdatedListener")) {
                        sListener = val;
                        return val;
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String extractProductId(Object params) {
        try {
            // Try getProductDetailsParamsList() (Billing v5+)
            try {
                Method m = params.getClass().getMethod("getProductDetailsParamsList");
                List<?> list = (List<?>) m.invoke(params);
                if (list != null && !list.isEmpty()) {
                    // ProductDetailsParams -> getProductDetails() -> getProductId()
                    Object pdp = list.get(0);
                    for (Method gm : pdp.getClass().getMethods()) {
                        if (gm.getName().equals("zza") && gm.getParameterCount() == 0) {
                            try {
                                Object pd = gm.invoke(pdp);
                                if (pd != null) {
                                    Method gpid = pd.getClass().getMethod("getProductId");
                                    return (String) gpid.invoke(pd);
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
            // Fallback: getSku() (older API)
            try {
                Method m = params.getClass().getMethod("getSku");
                return (String) m.invoke(params);
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
