plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.iot_home_automation"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.iot_home_automation"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.google.firebase:firebase-database:21.0.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation ("com.google.firebase:firebase-database:20.2.2")

    // Firebase Authentication (if adding user auth later)
    implementation ("com.google.firebase:firebase-auth:22.1.1")

    // Firebase Cloud Messaging (for notifications)
    implementation ("com.google.firebase:firebase-messaging:23.2.1")

    // Google Play Services Location (for sunset detection)
    implementation ("com.google.android.gms:play-services-location:21.0.1")
}