extension {
    name = "extensions/extension.mpe"
}

android {
    namespace = "app.template.extension"
}

dependencies {
    // Billing client for BillingFaker (Smashy Road 2 free IAP).
    // compileOnly: the classes are provided by the game APK at runtime.
    compileOnly("com.android.billingclient:billing:5.2.1")
}
