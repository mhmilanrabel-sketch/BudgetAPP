package com.example.moneymanager

import android.app.Application
import android.util.Log
import net.sqlcipher.database.SQLiteDatabase

class MoneyManagerApplication : Application() {

    companion object {
        private const val TAG = "MoneyManagerApp"

        private const val POI_XML_INPUT_FACTORY  = "org.apache.poi.javax.xml.stream.XMLInputFactory"
        private const val POI_XML_OUTPUT_FACTORY = "org.apache.poi.javax.xml.stream.XMLOutputFactory"
        private const val POI_XML_EVENT_FACTORY  = "org.apache.poi.javax.xml.stream.XMLEventFactory"

        private const val AALTO_INPUT_FACTORY  = "com.fasterxml.aalto.stax.InputFactoryImpl"
        private const val AALTO_OUTPUT_FACTORY = "com.fasterxml.aalto.stax.OutputFactoryImpl"
        private const val AALTO_EVENT_FACTORY  = "com.fasterxml.aalto.stax.EventFactoryImpl"
    }

    override fun onCreate() {
        super.onCreate()
        initSqlCipher()
        configurePoiSystemProperties()
    }

    private fun initSqlCipher() {
        try {
            SQLiteDatabase.loadLibs(this)
            Log.i(TAG, "SQLCipher native library loaded.")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to load SQLCipher native library", t)
            throw IllegalStateException("SQLCipher could not be initialised.", t)
        }
    }

    private fun configurePoiSystemProperties() {
        try {
            System.setProperty(POI_XML_INPUT_FACTORY,  AALTO_INPUT_FACTORY)
            System.setProperty(POI_XML_OUTPUT_FACTORY, AALTO_OUTPUT_FACTORY)
            System.setProperty(POI_XML_EVENT_FACTORY,  AALTO_EVENT_FACTORY)
            Log.i(TAG, "Apache POI system properties configured.")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to configure POI system properties", t)
        }
    }
}