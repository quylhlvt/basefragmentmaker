package com.example.basefragment

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp                     // QUAN TRỌNG NHẤT – KHÔNG ĐƯỢC THIẾU
class MyApplication : Application()