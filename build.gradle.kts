// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

}
buildscript {
    dependencies {
        // 這行是加 Firebase 的設定外掛
        classpath("com.google.gms:google-services:4.4.4")
    }
}