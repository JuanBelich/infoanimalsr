package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.InfoAnimalTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InfoAnimalTheme {
                MainApp(viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentUser by viewModel.currentUser.collectAsState()

    val bottomNavItems = listOf(
        NavigationItem("feed", "Inicio", Icons.Default.Home, Icons.Outlined.Home),
        NavigationItem("veterinarias", "Veterinarias", Icons.Default.LocalHospital, Icons.Outlined.LocalHospital),
        NavigationItem("campaigns", "Campañas", Icons.Default.VolunteerActivism, Icons.Outlined.VolunteerActivism),
        NavigationItem("news", "Noticias", Icons.Default.Article, Icons.Outlined.Article),
        NavigationItem("colaboradores", "Refugios", Icons.Default.Groups, Icons.Outlined.Groups),
        NavigationItem("perfil", "Perfil", Icons.Default.Person, Icons.Outlined.Person)
    )

    val showBottomBar = currentRoute in listOf(
        "feed", "veterinarias", "campaigns", "news", "colaboradores", "perfil/{userId}"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val isSelected = when (item.route) {
                            "perfil" -> currentRoute?.startsWith("perfil") == true
                            else -> currentRoute == item.route
                        }
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title) },
                            selected = isSelected,
                            onClick = {
                                val target = if (item.route == "perfil") {
                                    if (currentUser != null) "perfil/${currentUser!!.id}" else "auth"
                                } else {
                                    item.route
                                }
                                navController.navigate(target) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "feed",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("feed") {
                FeedScreen(
                    viewModel = viewModel,
                    onNavigateToPostDetail = { postId -> navController.navigate("post_detail/$postId") },
                    onNavigateToCreatePost = {
                        if (currentUser != null) {
                            navController.navigate("create_post")
                        } else {
                            navController.navigate("auth")
                        }
                    },
                    onNavigateToProfile = { userId -> navController.navigate("perfil/$userId") }
                )
            }

            composable("veterinarias") {
                VeterinariasScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("campaigns") {
                CampaignsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("news") {
                NewsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("colaboradores") {
                ColaboradoresScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "post_detail/{postId}",
                arguments = listOf(navArgument("postId") { type = NavType.StringType })
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId") ?: ""
                PostDetailScreen(
                    postId = postId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToProfile = { userId -> navController.navigate("perfil/$userId") }
                )
            }

            composable("create_post") {
                CreateEditPostScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "perfil/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                PerfilScreen(
                    userId = userId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAuth = {
                        navController.navigate("auth") {
                            popUpTo("feed") { inclusive = false }
                        }
                    }
                )
            }

            composable("auth") {
                AuthScreen(
                    viewModel = viewModel,
                    onAuthSuccess = {
                        navController.navigate("feed") {
                            popUpTo("feed") { inclusive = true }
                        }
                    },
                    onContinueAsGuest = {
                        navController.navigate("feed") {
                            popUpTo("feed") { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

data class NavigationItem(
    val route: String,
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)
