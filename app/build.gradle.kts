plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")

}

android {
    namespace = "com.game.bubblepop"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.game.bubblepop"
        minSdk = 24
        targetSdk = 35
        versionCode = 12
        versionName = "2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation("com.google.android.ump:user-messaging-platform:3.1.0")
    implementation ("com.google.firebase:firebase-analytics:17.4.1")
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    //implementation ("com.google.android.gms:play-services-ads-lite:24.0.0")
    implementation(libs.firebase.common.ktx)
    //implementation("com.google.android.gms:play-services-ads:24.2.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.foundation.android)
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.play.services.ads.lite)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}