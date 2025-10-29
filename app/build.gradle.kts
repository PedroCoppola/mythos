import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.serialization") // <-- AÑADE ESTA LÍNEA
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

        // --- START: Leer API key de local.properties ---
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        var apiKey = "" // Valor por defecto
        if (localPropertiesFile.exists()) {
            try {
                FileInputStream(localPropertiesFile).use { fis ->
                    localProperties.load(fis)
                    apiKey = localProperties.getProperty("GEMINI_API_KEY", "")
                }
            } catch (e: Exception) {
                println("Advertencia: No se pudo cargar GEMINI_API_KEY de local.properties: ${e.message}")
            }
        } else {
            println("Advertencia: No se encontró el archivo local.properties en ${localPropertiesFile.absolutePath}")
        }

        // Crea el campo en BuildConfig. Gradle se encarga de que las comillas sean correctas.
// Forma SEGURA, usando la variable que leíste arriba
// Usamos la variable 'apiKey' que ya leíste de local.properties
// Usamos el formato $variable para inyectar el valor.
        buildConfigField("String", "GEMINI_API_KEY", "\"$apiKey\"")    }

    buildFeatures {
        buildConfig = true
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
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
    implementation(libs.mediation.test.suite)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3") // <-- AÑADE ESTA LÍNEA
    // En build.gradle.kts (Module :app)
    implementation("androidx.core:core-splashscreen:1.0.1")




    // --- FIREBASE (LA FORMA CORRECTA Y LIMPIA) ---
    // 1. Importa el BoM (Bill of Materials). Usa una versión reciente.
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))

    // 2. Agrega las librerías de Firebase que necesitas SIN especificar la versión.
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    // implementation("com.google.firebase:firebase-analytics") // Si la llegas a usar

    // --- OTRAS DEPENDENCIAS (GEMINI, ETC.) ---
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0") // Usa la última versión de coroutines

    // --- TEST (Estas están bien) ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")


    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
