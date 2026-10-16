package com.example.sol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.sol.core.ui.*
import com.example.sol.feature.auth.ui.LoginScreen
import com.example.sol.feature.auth.ui.ProfileScreen
import com.example.sol.feature.auth.viewmodel.ProfileViewModel
import com.example.sol.feature.auth.viewmodel.SignupViewModel
import com.example.sol.feature.auth.ui.SignupScreen
import com.example.sol.feature.settings.viewmodel.DemoAccountViewModel
import com.example.sol.feature.settings.ui.MyInfoScreen
import com.example.sol.feature.auth.viewmodel.LoginViewModel
import com.example.sol.feature.home.ui.HomeScreen
import com.example.sol.feature.home.viewmodel.HomeViewModel
import com.example.sol.feature.habit.ui.HabitScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SolTheme { SolApp() } }
    }
}

private val destinations = linkedMapOf(
    "meal" to "식사", "drugdictionary" to "의약품 사전",
    "medication" to "복약", "statistics" to "통계",
)

@Composable
fun SolApp() {
    val nav = rememberNavController()
    val account: DemoAccountViewModel = viewModel()
    val signup: SignupViewModel = viewModel()
    val profile: ProfileViewModel = viewModel()
    val home: HomeViewModel = viewModel()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "login"
    val showBottom = route !in listOf("login", "signup", "profile-onboarding", "profile-edit")
    val exitDemo: () -> Unit = {
        account.clear()
        signup.reset()
        profile.load(null)
        home.reset()
        nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true }; launchSingleTop = true }
    }
    // Process recreation does not restore a fake authenticated session.
    LaunchedEffect(route) {
        if (route == "profile-onboarding" && account.email.isEmpty()) exitDemo()
    }
    Scaffold(bottomBar = {
        if (showBottom) SolBottomBar(
            current = if (route in listOf("meal", "medication", "drugdictionary")) "habit" else route,
            onNavigate = { target ->
                if (route != target) nav.navigate(target) {
                    popUpTo("home") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            })
    }) { padding ->
        NavHost(navController = nav, startDestination = "login", modifier = Modifier.padding(padding)) {
            composable("login") {
                val model: LoginViewModel = viewModel()
                LoginScreen(model.state, model::updateEmail, model::updatePassword, model::previewValidation,
                    onExplore = {
                        model.clear()
                        nav.navigate("home") { popUpTo("login") { inclusive = true }; launchSingleTop = true }
                    }, onSignup = {
                        model.clear(); signup.reset(); profile.load(null)
                        nav.navigate("signup") { launchSingleTop = true }
                    })
            }
            composable("signup") {
                SignupScreen(signup, onExit = { nav.popBackStack() }, onProfile = { email, consents ->
                    account.start(email, consents)
                    profile.load(null)
                    signup.reset()
                    nav.navigate("profile-onboarding") { popUpTo("signup") { inclusive = true }; launchSingleTop = true }
                })
            }
            composable("home") {
                HomeScreen(home.state, home.mode, account.profile?.nickname,
                    onOpen = { nav.navigate(it) { launchSingleTop = true } },
                    onReload = home::reload, onRetry = home::retry, onMode = home::selectMode,
                    onExit = exitDemo)
            }
            composable("habit") {
                HabitScreen(completed = home.completedHabits, onToggle = home::toggleHabit,
                    onBack = { nav.popBackStack("home", inclusive = false) })
            }
            composable("profile-onboarding") {
                BackHandler { exitDemo() }
                ProfileScreen(profile, onBack = exitDemo, onSave = {
                    account.save(profile.state)
                    nav.navigate("home") { popUpTo(nav.graph.id) { inclusive = true }; launchSingleTop = true }
                })
            }
            composable("profile-edit") {
                ProfileScreen(profile, onBack = { nav.popBackStack() }, editing = true, onSave = {
                    account.save(profile.state)
                    nav.popBackStack()
                })
            }
            composable("settings") {
                MyInfoScreen(account, onEdit = {
                    profile.load(account.profile)
                    nav.navigate("profile-edit") { launchSingleTop = true }
                }, onLogout = exitDemo, onOpen = { nav.navigate(it) { launchSingleTop = true } })
            }
            destinations.forEach { (route, title) ->
                composable(route) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text(title, style = MaterialTheme.typography.headlineLarge)
                        Text("화면 이동을 확인하는 1주차 안내 화면입니다.")
                        Text("3.1 로그인·초기 설정부터 순서대로 구현합니다. 이 기능의 기록·조회는 아직 제공하지 않습니다.")
                        SolButton("이전 화면으로", { nav.popBackStack() })
                    }
                }
            }
        }
    }
}
