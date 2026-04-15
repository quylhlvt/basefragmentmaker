package com.example.basefragment

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.basefragment.core.extention.hideNavigation
import com.example.basefragment.core.helper.SharedPreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private val mainViewModel: ViewModelActivity by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        hideNavigation(true)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        val sharedPrefs = getSharedPreferences("DEFAULT", Context.MODE_PRIVATE)
//        SharedPreferencesManager.sharedPreferences = sharedPrefs
//        SharedPreferencesManager.editor = sharedPrefs.edit()

        // Lấy NavController từ NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Tuỳ chọn: ẩn thanh trạng thái hoặc làm gì đó
        // window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    // QUAN TRỌNG: xử lý nút Back đúng cách
    override fun onBackPressed() {
        if (!navController.popBackStack()) {
            // Không còn gì trong back stack → thoát app
            super.onBackPressed()
        }
    }

    // Nếu bạn dùng Toolbar + NavigationIcon (mũi tên back)
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onResume() {
        super.onResume()
        hideNavigation(true)
    }
}