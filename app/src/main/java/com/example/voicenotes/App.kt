package com.example.voicenotes

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application класс — точка входа приложения.
 * @HiltAndroidApp инициализирует Hilt DI.
 */
@HiltAndroidApp
class App : Application()
