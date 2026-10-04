package com.example.moneymanager

import android.app.Application
import android.util.Log
import com.example.moneymanager.di.AppContainer
import com.example.moneymanager.di.DefaultAppContainer
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import net.sqlcipher.database.SQLiteDatabase

class MoneyManagerApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Apache POI XML StAX Factories for Android compatibility
        try {
            System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl")
            System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl")
            System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.stax.EventFactoryImpl")
        } catch (t: Throwable) {
            Log.e("MoneyManagerApp", "Failed to set POI system properties", t)
        }

        // 2. Initialize SQLCipher native library
        try {
            SQLiteDatabase.loadLibs(this)
        } catch (t: Throwable) {
            Log.e("MoneyManagerApp", "Failed to load SQLCipher libs", t)
        }

        // 3. Initialize PDFBox Android resource loader
        PDFBoxResourceLoader.init(applicationContext)

        // 4. Initialize Dependency Container
        container = DefaultAppContainer(this)
    }
}
