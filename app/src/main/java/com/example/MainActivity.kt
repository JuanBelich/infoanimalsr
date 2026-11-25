package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import coil.compose.AsyncImage
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.InfoAnimalPrimary
import com.example.ui.theme.InfoAnimalSecondary
import com.example.ui.theme.InfoAnimalDestructive
import com.example.ui.theme.InfoAnimalAccent
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContainer()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val viewModel: MainViewModel = viewModel()
    val user by viewModel.currentUser.collectAsState()
    val isInitializing by viewModel.isInitializing.collectAsState()
    val notifications by viewModel.getNotifications().collectAsState(initial = emptyList())
    val unreadNotifsCount = remember(notifications) { notifications.count { !it.leida } }

    // Dialog state for Emergencias
    var showEmergencyDialog by remember { mutableStateOf(false) }
    val veterinarias by viewModel.veterinarias.collectAsState()
    val guardiaVets = remember(veterinarias) {
        veterinarias.filter { it.clasificacion == "Guardia 24hs" }
    }

    if (isInitializing) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_app_logo),
                    contentDescription = "InfoAnimal & Al Recreo Logo",
                    modifier = Modifier
                        .height(90.dp)
                        .fillMaxWidth(0.75f),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(20.dp))
                CircularProgressIndicator(color = InfoAnimalPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Cargando InfoAnimal...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InfoAnimalPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        return
    }

    // Modal Drawer for mobile navigation links
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_logo),
                        contentDescription = "Logo InfoAnimal & Al Recreo",
                        modifier = Modifier
                            .height(65.dp)
                            .fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Uniendo familias mendocinas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Nav Links
                DrawerNavLink("Inicio", Icons.Default.Home) {
                    scope.launch { drawerState.close() }
                    navController.navigate("home")
                }
                DrawerNavLink("Publicaciones", Icons.Default.Pets) {
                    scope.launch { drawerState.close() }
                    navController.navigate("feed")
                }
                DrawerNavLink("Campañas ONGs", Icons.Default.Campaign) {
                    scope.launch { drawerState.close() }
                    navController.navigate("campanias")
                }
                DrawerNavLink("Portal Noticias", Icons.Default.Newspaper) {
                    scope.launch { drawerState.close() }
                    navController.navigate("noticias")
                }
                DrawerNavLink("Veterinarias", Icons.Default.LocalHospital) {
                    scope.launch { drawerState.close() }
                    navController.navigate("veterinarias")
                }
                /*
                // --- Red Social: Comunidad Vecinal (Comentado) ---
                DrawerNavLink("Comunidad Vecinal", Icons.Default.People) {
                    scope.launch { drawerState.close() }
                    navController.navigate("comunidad")
                }
                */
                DrawerNavLink("Colaboradores", Icons.Default.Star) {
                    scope.launch { drawerState.close() }
                    navController.navigate("colaboradores")
                }

                if (user != null) {
                    DrawerNavLink("Usuarios Bloqueados", Icons.Default.Block) {
                        scope.launch { drawerState.close() }
                        navController.navigate("bloqueados")
                    }
                }

                if (user != null && (user!!.rol == "admin" || user!!.rol == "ong")) {
                    DrawerNavLink("Panel Admin", Icons.Default.AdminPanelSettings) {
                        scope.launch { drawerState.close() }
                        navController.navigate("admin")
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Collaborative Footer inside NavMenu
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Desarrollado en colaboración por NYX CentrarDiv y Al Recreo.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        text = "© 2026 InfoAnimalsR • es-AR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                // Sticky Header with Blur/Transparency styling
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    ),
                    modifier = Modifier.border(
                        BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.clickable { navController.navigate("home") }
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_app_icon),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                text = "InfoAnimal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = InfoAnimalPrimary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menú")
                        }
                    },
                    actions = {
                        // Notifications Bell with Badge
                        if (user != null) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifsCount > 0) {
                                        Badge(
                                            containerColor = InfoAnimalDestructive,
                                            contentColor = Color.White
                                        ) {
                                            Text("$unreadNotifsCount", fontSize = 9.sp)
                                        }
                                    }
                                },
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                IconButton(
                                    onClick = { navController.navigate("notificaciones") },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notificaciones",
                                        tint = InfoAnimalPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        // Profile Dropdown Session controls
                        if (user != null) {
                            var accountMenuExpanded by remember { mutableStateOf(false) }
                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(InfoAnimalSecondary.copy(alpha = 0.2f))
                                        .clickable { accountMenuExpanded = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val userAvatarUrl = user?.fotoPerfil
                                    if (!userAvatarUrl.isNullOrBlank() && userAvatarUrl.startsWith("http")) {
                                        AsyncImage(
                                            model = userAvatarUrl,
                                            contentDescription = "Mi Cuenta",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Mi Cuenta",
                                            tint = InfoAnimalSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = accountMenuExpanded,
                                    onDismissRequest = { accountMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                        text = { Text("Mi Perfil") },
                                        onClick = {
                                            accountMenuExpanded = false
                                            navController.navigate("perfil")
                                        }
                                    )
                                    if (user!!.rol == "admin" || user!!.rol == "ong") {
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                                            text = { Text("Administración") },
                                            onClick = {
                                                accountMenuExpanded = false
                                                navController.navigate("admin")
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Default.Logout, contentDescription = null) },
                                        text = { Text("Cerrar Sesión") },
                                        onClick = {
                                            accountMenuExpanded = false
                                            viewModel.logout {
                                                navController.navigate("home") {
                                                    popUpTo(navController.graph.id) { inclusive = true }
                                                    launchSingleTop = true
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = { navController.navigate("login") },
                                colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Entrar", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                )
            },
            // FLOATING EMERGENCY BUTTON
            floatingActionButton = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(InfoAnimalDestructive)
                        .clickable { showEmergencyDialog = true }
                        .testTag("emergency_fab"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalHospital,
                        contentDescription = "Emergencias",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            // Routing NavHost with clean transition Animations
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable("home") {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToFeed = { navController.navigate("feed") },
                        onNavigateToPostDetail = { id -> navController.navigate("post_detail/$id") },
                        onNavigateToLogin = { navController.navigate("login") },
                        onNavigateToSignup = { navController.navigate("signup") },
                        onNavigateToCreatePost = { navController.navigate("create_post") },
                        onNavigateToUserProfile = { userId -> navController.navigate("perfil/$userId") }
                    )
                }

                composable("feed") {
                    FeedScreen(
                        viewModel = viewModel,
                        onNavigateToPostDetail = { id -> navController.navigate("post_detail/$id") },
                        onNavigateToCreatePost = { navController.navigate("create_post") },
                        onNavigateToLogin = { navController.navigate("login") },
                        onNavigateToSignup = { navController.navigate("signup") },
                        onNavigateToEditPost = { id -> navController.navigate("edit_post/$id") },
                        onNavigateToUserProfile = { userId -> navController.navigate("perfil/$userId") }
                    )
                }

                composable(
                    route = "post_detail/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: ""
                    PostDetailScreen(
                        postId = id,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToEditPost = { editId ->
                            navController.navigate("create_post?editId=$editId")
                        },
                        onNavigateToUserProfile = { userId -> navController.navigate("perfil/$userId") }
                    )
                }

                composable(
                    route = "create_post?editId={editId}",
                    arguments = listOf(navArgument("editId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    })
                ) { backStackEntry ->
                    val editId = backStackEntry.arguments?.getString("editId")
                    CreatePostScreen(
                        editId = editId,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onSaveFinished = { finalId ->
                            if (editId != null) {
                                navController.popBackStack()
                            } else {
                                navController.popBackStack()
                                navController.navigate("post_detail/$finalId")
                            }
                        }
                    )
                }

                composable("create_post") {
                    CreatePostScreen(
                        editId = null,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onSaveFinished = { finalId ->
                            navController.popBackStack()
                            navController.navigate("post_detail/$finalId")
                        }
                    )
                }

                composable("veterinarias") {
                    VeterinariasScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("campanias") {
                    CampañasScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("noticias") {
                    NoticiasScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("perfil") {
                    PerfilScreen(
                        profileUserId = null,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onLogout = {
                            navController.navigate("home") {
                                popUpTo(navController.graph.id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(
                    route = "perfil/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id")
                    PerfilScreen(
                        profileUserId = id,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onLogout = {
                            navController.navigate("home") {
                                popUpTo(navController.graph.id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable("login") {
                    LoginScreen(
                        viewModel = viewModel,
                        onNavigateToSignup = { navController.navigate("signup") },
                        onNavigateToRecover = { navController.navigate("recuperar") },
                        onSuccessRedirect = {
                            navController.navigate("home") {
                                popUpTo(navController.graph.id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable("signup") {
                    SignupScreen(
                        viewModel = viewModel,
                        onNavigateToLogin = { navController.navigate("login") },
                        onSuccessRedirect = {
                            navController.navigate("home") {
                                popUpTo(navController.graph.id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable("recuperar") {
                    RecoverPasswordScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("admin") {
                    AdminScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                /*
                // --- Red Social: Comunidad Vecinal Screen (Comentado) ---
                composable("comunidad") {
                    ComunidadScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToUserProfile = { userId -> navController.navigate("perfil/$userId") }
                    )
                }
                */

                composable("bloqueados") {
                    BloqueadosScreen(
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

                composable("notificaciones") {
                    NotificacionesScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    // --- GLOBAL VETERINARY EMERGENCY DIALOG ---
    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            confirmButton = {
                TextButton(onClick = { showEmergencyDialog = false }) {
                    Text("Entendido / Cerrar", color = InfoAnimalPrimary, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Dangerous,
                        contentDescription = "Emergencias Veterinaria Guardia 24hs",
                        tint = InfoAnimalDestructive,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Emergencia 24hs 🚨",
                        color = InfoAnimalDestructive,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Estás por ver veterinarias de Guardia Activa 24hs en la región del Sur de Mendoza. Llamá antes de asistir para asegurar atención inmediata.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (guardiaVets.isEmpty()) {
                        Text(
                            text = "No se encontraron veterinarias de Guardia 24 Horas registradas en este distrito.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.heightIn(max = 280.dp)
                        ) {
                            items(guardiaVets) { vet ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = InfoAnimalDestructive.copy(alpha = 0.08f)
                                    ),
                                    border = BorderStroke(1.dp, InfoAnimalDestructive.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = vet.nombre,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = InfoAnimalDestructive
                                        )
                                        Text(
                                            text = "Dirección: ${vet.direccion} (${vet.distrito})",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            text = "Atención 24 Horas",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = InfoAnimalPrimary,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    triggerWhatsApp(
                                                        context,
                                                        vet.telefono,
                                                        "EMERGENCIA VETERINARIA: Hola! Me comunico con urgencia por una mascota en riesgo que acabo de ver en InfoAnimalsR."
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = InfoAnimalAccent
                                                ),
                                                contentPadding = PaddingValues(
                                                    horizontal = 8.dp,
                                                    vertical = 4.dp
                                                ),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Chat,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "WhatsApp",
                                                    color = Color.White,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Button(
                                                onClick = { triggerCall(context, vet.telefono) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = InfoAnimalSecondary
                                                ),
                                                contentPadding = PaddingValues(
                                                    horizontal = 8.dp,
                                                    vertical = 4.dp
                                                ),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Phone,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "Llamar Urgente",
                                                    color = Color.White,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun DrawerNavLink(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(imageVector = icon, contentDescription = null) },
        label = { Text(label, fontWeight = FontWeight.Bold) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}
