import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.mythos.mythos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mythos.mythos"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // --- START: Read API key from local.properties ---
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties") // Correctly points to root
        var apiKey = "" // Default to empty string if not found
        if (localPropertiesFile.exists()) {
            try {
                FileInputStream(localPropertiesFile).use { fis ->
                    localProperties.load(fis)
                    apiKey = localProperties.getProperty("GEMINI_API_KEY", "") // Provide default
                }
            } catch (e: Exception) {
                // Log an error or handle it as appropriate for your build
                println("Warning: Could not load GEMINI_API_KEY from local.properties: ${e.message}")
            }
        } else {
            println("Warning: local.properties file not found at ${localPropertiesFile.absolutePath}")
        }
        // Make the API key available as a BuildConfig field
        // Ensure the value is properly quoted for the generated Java/Kotlin string
        buildConfigField("String", "GEMINI_API_KEY", "\"$apiKey\"")
        // --- END: Read API key from local.properties ---
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

    buildFeatures {
        buildConfig = true // Ensure buildConfig is enabled
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.1.0"))
    // Add the dependency for the Firebase AI Logic library (if you are using Firebase Vertex AI for Gemini)
    // implementation("com.google.firebase:firebase-ai") // Uncomment if using Firebase Vertex AI

    // For Google AI Gemini SDK (which you are using based on previous code)
    implementation("com.google.ai.client.generativeai:generativeai:0.3.0") // Check for the latest version
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3") // For coroutines
}
