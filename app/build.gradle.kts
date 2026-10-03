import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import java.util.TimeZone

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// 本地构建自动递增：分钟级时间戳（自 2026-01-01 起），保证每次打包 versionCode 单调递增；
// CI 仍可通过 -PversionCode/-PversionName 显式覆盖
val autoVersionCode = (System.currentTimeMillis() / 60000 - 29453760L).toInt() // 2026-01-01T00:00:00Z 的分钟数
val autoVersionName = "2.0." + SimpleDateFormat("yyyyMMdd.HHmm", Locale.US)
    .apply { timeZone = TimeZone.getTimeZone("Asia/Shanghai") }
    .format(Date())

val versionCodeProp = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: autoVersionCode
val versionNameProp = (project.findProperty("versionName") as String?) ?: autoVersionName

android {
    namespace = "com.hvlg.guide"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hvlg.guide"
        minSdk = 24
        targetSdk = 35
        versionCode = versionCodeProp
        versionName = versionNameProp
    }

    signingConfigs {
        val keystoreFile = localProps.getProperty("KEYSTORE_FILE")
        // CI 未配置签名 secrets 时会写出一个 0 字节的 keystore，此时跳过签名（构建验证用未签名包）
        if (keystoreFile != null && File(keystoreFile).length() > 0) {
            create("release") {
                storeFile = file(keystoreFile)
                storePassword = localProps.getProperty("KEYSTORE_PASSWORD")
                keyAlias = localProps.getProperty("KEY_ALIAS")
                keyPassword = localProps.getProperty("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (signingConfigs.findByName("release") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
