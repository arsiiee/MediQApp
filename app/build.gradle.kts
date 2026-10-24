plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // The API models are @Serializable; without this plugin they have no
    // generated serializer and Retrofit cannot build a request body for them.
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.mediq"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.mediq"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    sourceSets {
        getByName("main") {
            kotlin.srcDir("$buildDir/generated/source/openapi/main/kotlin")
        }
    }
}

// Configuration used to fetch the OpenAPI Generator CLI for Task 0.2 codegen.
val codegenConfig = configurations.create("codegen") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    add("codegen", "org.openapitools:openapi-generator-cli:7.14.0")
}

// --- Task 0.2: Kotlin client codegen from the Ktor OpenAPI spec ----------
// Generates a Retrofit 2 + kotlinx.serialization Kotlin client into a package
// that does not collide with the hand-written `data.api`/`data.model` sources.
tasks.register<JavaExec>("generateKotlinClient") {
    description = "Generates the Kotlin Retrofit client from the Ktor OpenAPI spec"
    group = "codegen"
    val cp: FileCollection = codegenConfig
    classpath = cp
    mainClass.set("org.openapitools.codegen.OpenAPIGenerator")
    args = listOf(
        "generate",
        "-i", "$rootDir/server/src/main/resources/openapi/documentation.yaml",
        "-g", "kotlin",
        "-o", "$buildDir/generated/source/openapi/main",
        "--api-package", "com.example.mediq.data.api.generated.api",
        "--model-package", "com.example.mediq.data.api.generated.model",
        "--package-name", "com.example.mediq.data.api.generated",
        "--library", "jvm-retrofit2",
        "--additional-properties", "serializationLibrary=kotlinx_serialization",
        "--skip-validate-spec",
    )
    outputs.dir("$buildDir/generated/source/openapi/main/kotlin")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    dependsOn("generateKotlinClient")
}
