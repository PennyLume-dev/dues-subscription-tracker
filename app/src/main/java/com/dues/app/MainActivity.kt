package com.dues.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dues.app.ui.CatalogScreen
import com.dues.app.ui.DataScreen
import com.dues.app.ui.DetailScreen
import com.dues.app.ui.DuesTheme
import com.dues.app.ui.EditScreen
import com.dues.app.ui.EmailGuideScreen
import com.dues.app.ui.GuideScreen
import com.dues.app.ui.MainScreen
import com.dues.app.ui.NotificationsScreen
import com.dues.app.ui.OnboardingScreen
import com.dues.app.ui.PriceHistoryScreen
import com.dues.app.ui.RegionScreen
import com.dues.app.ui.LegalScreen
import com.dues.app.ui.Bg
import com.dues.app.ui.TagsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        createChannel(this)
        scheduleDaily(this)

        setContent {
            DuesTheme {
                val vm: AppVM = viewModel()
                val nav = rememberNavController()
                val idArg = listOf(navArgument("id") { type = NavType.LongType })
                NavHost(
                    nav,
                    startDestination = if (vm.prefs.onboarded) "main" else "onboarding",
                    modifier = Modifier.fillMaxSize().background(Bg),
                ) {
                    composable("onboarding") {
                        OnboardingScreen(vm) { nav.navigate("main") { popUpTo("onboarding") { inclusive = true } } }
                    }
                    composable("main") { MainScreen(vm, nav) }
                    composable("add") { CatalogScreen(vm, nav) }
                    composable("edit/{id}", idArg) { EditScreen(vm, nav, it.arguments!!.getLong("id")) }
                    composable("detail/{id}", idArg) { DetailScreen(vm, nav, it.arguments!!.getLong("id")) }
                    composable("prices/{id}", idArg) { PriceHistoryScreen(vm, nav, it.arguments!!.getLong("id")) }
                    composable("lists") { androidx.compose.runtime.key("list") { TagsScreen(vm, nav, com.dues.app.data.TagKind.LIST) } }
                    composable("categories") { androidx.compose.runtime.key("category") { TagsScreen(vm, nav, com.dues.app.data.TagKind.CATEGORY) } }
                    composable("payments") { androidx.compose.runtime.key("payment") { TagsScreen(vm, nav, com.dues.app.data.TagKind.PAYMENT) } }
                    composable("notifications") { NotificationsScreen(vm, nav) }
                    composable("data") { DataScreen(vm, nav) }
                    composable("region") { RegionScreen(vm, nav) }
                    composable("legal/{doc}") { LegalScreen(nav, it.arguments!!.getString("doc")!!) }
                    composable("guide") { GuideScreen(vm, nav) }
                    composable("email") { EmailGuideScreen(nav) }
                }
            }
        }
    }
}
