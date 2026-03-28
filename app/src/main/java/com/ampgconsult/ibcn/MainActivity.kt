package com.ampgconsult.ibcn

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ampgconsult.ibcn.data.devops.BackendState
import com.ampgconsult.ibcn.ui.screens.*
import com.ampgconsult.ibcn.ui.theme.IBCNTheme
import com.ampgconsult.ibcn.ui.viewmodels.AuthViewModel
import com.ampgconsult.ibcn.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IBCNTheme {
                val mainViewModel: MainViewModel = hiltViewModel()
                val backendState by mainViewModel.backendState.collectAsState()
                val context = LocalContext.current

                // Non-blocking status feedback
                LaunchedEffect(backendState) {
                    when (backendState) {
                        BackendState.READY -> {
                            Toast.makeText(context, "Backend ready ✅", Toast.LENGTH_SHORT).show()
                        }
                        BackendState.FAILED -> {
                            Toast.makeText(context, "Backend setup failed. Some features may be limited.", Toast.LENGTH_LONG).show()
                        }
                        else -> {}
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Login screen and navigation are always visible immediately
                    AppNavigation()

                    // Optional: Small non-intrusive indicator for background setup
                    if (backendState == BackendState.INITIALIZING) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 32.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Setting up backend...", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthViewModel.AuthState.Idle) {
            navController.navigate("auth") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val startDestination = when (authState) {
        is AuthViewModel.AuthState.Authenticated -> "dashboard"
        else -> "splash"
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("splash") {
            SplashScreen(onSplashFinished = {
                navController.navigate("onboarding") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        composable("onboarding") {
            OnboardingScreen(onGetStarted = {
                navController.navigate("auth")
            })
        }
        composable("auth") {
            AuthScreen(onAuthSuccess = { isNewUser ->
                if (isNewUser) {
                    navController.navigate("username_selection") {
                        popUpTo("auth") { inclusive = true }
                    }
                } else {
                    navController.navigate("dashboard") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            })
        }
        composable("username_selection") {
            UsernameSelectionScreen(onUsernameCreated = {
                navController.navigate("dashboard") {
                    popUpTo("username_selection") { inclusive = true }
                }
            })
        }
        composable("dashboard") {
            DashboardScreen(
                onLaunchAIBuilder = { navController.navigate("ai_builder") },
                onLaunchAICoding = { navController.navigate("ai_coding") },
                onSelectProject = { id ->
                    navController.navigate("workspace/$id")
                },
                onViewAnalytics = { navController.navigate("analytics") },
                onViewNotifications = { navController.navigate("notifications") },
                onViewMarketplace = { navController.navigate("marketplace") },
                onViewProfile = { navController.navigate("profile") },
                onViewLaunchpad = { navController.navigate("launchpad") },
                onLaunchStartup = { navController.navigate("business_launch") }
            )
        }
        composable("edit_profile") {
            EditProfileScreen(onBack = { navController.popBackStack() })
        }
        composable("ai_builder") {
            AIBuilderScreen(onBack = { navController.popBackStack() })
        }
        composable("ai_coding") {
            AICodingScreen(onBack = { navController.popBackStack() })
        }
        composable("ai_video_studio") {
            AIVideoStudioScreen(onBack = { navController.popBackStack() })
        }
        composable("ai_asset_generator") {
            AIAssetGeneratorScreen(
                onBack = { navController.popBackStack() },
                onViewMarketplace = { navController.navigate("marketplace") }
            )
        }
        composable(
            route = "workspace/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            ProjectWorkspaceScreen(
                projectId = projectId,
                projectName = "Project",
                onBack = { navController.popBackStack() },
                onNavigateToHabits = { navController.navigate("habit_tracker/$projectId") },
                onNavigateToDevLab = { navController.navigate("dev_lab/$projectId") }
            )
        }
        composable(
            route = "dev_lab/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            DevLabScreen(
                projectId = projectId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "habit_tracker/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            HabitTrackerScreen(
                projectId = projectId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("analytics") {
            AnalyticsScreen(onBack = { navController.popBackStack() })
        }
        composable("notifications") {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }
        composable("marketplace") {
            MarketplaceScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate("marketplace_detail/$id") },
                onNavigateToPublish = { navController.navigate("marketplace_publish") },
                onNavigateToVideoStudio = { navController.navigate("ai_video_studio") }
            )
        }
        composable(
            route = "marketplace_detail/{assetId}",
            arguments = listOf(navArgument("assetId") { type = NavType.StringType })
        ) { backStackEntry ->
            val assetId = backStackEntry.arguments?.getString("assetId") ?: ""
            AssetDetailScreen(
                assetId = assetId,
                onBack = { navController.popBackStack() },
                onNavigateToChat = { chatId -> navController.navigate("chat/$chatId") }
            )
        }
        composable("marketplace_publish") {
            PublishAssetScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "chat/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatScreen(
                chatId = chatId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("profile") {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onEditProfile = { navController.navigate("edit_profile") }
            )
        }
        composable("launchpad") {
            StartupLaunchpadScreen(onBack = { navController.popBackStack() })
        }
        composable("business_launch") {
            BusinessLaunchScreen(
                onBack = { navController.popBackStack() },
                onViewProject = { id -> navController.navigate("workspace/$id") }
            )
        }
    }
}
