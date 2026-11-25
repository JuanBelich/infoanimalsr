package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.asImageBitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import com.example.data.local.ServiceLocator
import com.example.data.model.*
import com.example.ui.components.OngBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.flowOf
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import java.text.SimpleDateFormat
import java.util.*

// --- Utility: Simple Static Pet Image Generator Based on Species/Avatar ---
@Composable
fun PetPlaceholder(
    especie: String,
    modifier: Modifier = Modifier,
    name: String = ""
) {
    val bgColor = when (especie.lowercase()) {
        "perro" -> Color(0xFFFFDAB9) // Peach
        "gato" -> Color(0xFFE0FFFF)  // Light Cyan
        else -> Color(0xFFE6E6FA)    // Lavender
    }
    val icon = when (especie.lowercase()) {
        "perro" -> Icons.Default.Pets
        "gato" -> Icons.Default.Pets
        else -> Icons.Default.Pets
    }

    Box(
        modifier = modifier
            .background(bgColor)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = "Placeholder Mascota",
                tint = InfoAnimalPrimary,
                modifier = Modifier.size(48.dp)
            )
            if (name.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = InfoAnimalPrimary
                )
            }
        }
    }
}

@Composable
fun MascotaImage(
    fotoString: String?,
    postId: String,
    especie: String,
    nombreMascota: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (fotoString.isNullOrEmpty()) {
        PetPlaceholder(
            especie = especie,
            modifier = modifier,
            name = nombreMascota
        )
    } else {
        val isUrl = fotoString.startsWith("http://") || fotoString.startsWith("https://")
        val isBase64 = !isUrl && (fotoString.length > 100 && !fotoString.contains(".") && (fotoString.contains("/") || fotoString.contains("+") || fotoString.endsWith("=")))

        if (isUrl) {
            AsyncImage(
                model = fotoString,
                contentDescription = "Foto Mascota",
                modifier = modifier,
                contentScale = contentScale
            )
        } else if (isBase64) {
            val bitmap = remember(fotoString) {
                try {
                    val cleanB64 = if (fotoString.contains(",")) fotoString.substringAfter(",") else fotoString
                    val decodedBytes = android.util.Base64.decode(cleanB64, android.util.Base64.DEFAULT)
                    android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                } catch (e: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Foto Mascota",
                    modifier = modifier,
                    contentScale = contentScale
                )
            } else {
                PetPlaceholder(
                    especie = especie,
                    modifier = modifier,
                    name = nombreMascota
                )
            }
        } else {
            val fileUrl = remember(fotoString, postId) {
                try {
                    com.example.data.network.PocketBaseClient.getFileUrl("posts", postId, fotoString)
                } catch (e: Exception) {
                    ""
                }
            }
            if (fileUrl.isNotEmpty()) {
                AsyncImage(
                    model = fileUrl,
                    contentDescription = "Foto Mascota",
                    modifier = modifier,
                    contentScale = contentScale,
                    error = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_menu_gallery)
                )
            } else {
                PetPlaceholder(
                    especie = especie,
                    modifier = modifier,
                    name = nombreMascota
                )
            }
        }
    }
}

// --- Utility: Format Timestamp ---
fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "AR"))
    return sdf.format(Date(timestamp))
}

// --- Utility: Contact Dialer ---
fun triggerWhatsApp(context: android.content.Context, phone: String, message: String) {
    try {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        val uri = Uri.parse("https://wa.me/54$cleanPhone?text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No se pudo abrir WhatsApp.", Toast.LENGTH_SHORT).show()
    }
}

fun triggerCall(context: android.content.Context, phone: String) {
    try {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No se pudo iniciar la llamada.", Toast.LENGTH_SHORT).show()
    }
}

// ============================================
// 1. HOME SCREEN
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToFeed: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onNavigateToCreatePost: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    val posts by viewModel.allPosts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val recentPosts = remember(posts) { posts.take(6) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshPosts() },
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
        // --- HERO VIEWPORT ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 320.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                InfoAnimalPrimary.copy(alpha = 0.14f),
                                InfoAnimalSecondary.copy(alpha = 0.06f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            ) {
                // Hero Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_logo),
                        contentDescription = "InfoAnimal & Al Recreo Logo",
                        modifier = Modifier
                            .height(86.dp)
                            .fillMaxWidth(0.88f),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Ayudamos a reunir familias con sus mascotas",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Plataforma comunitaria del Sur Mendocino para publicar mascotas perdidas, encontradas o avistadas. ¡Unite gratis!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (user == null) {
                            Button(
                                onClick = onNavigateToSignup,
                                colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalPrimary)
                            ) {
                                Text("Registrarse", color = Color.White)
                            }
                            OutlinedButton(
                                onClick = onNavigateToLogin,
                                border = BorderStroke(1.dp, InfoAnimalPrimary)
                            ) {
                                Text("Iniciar Sesión", color = InfoAnimalPrimary)
                            }
                        } else {
                            Button(
                                onClick = onNavigateToFeed,
                                colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalPrimary)
                            ) {
                                Text("Ver Publicaciones", color = Color.White)
                            }
                            Button(
                                onClick = onNavigateToCreatePost,
                                colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalSecondary)
                            ) {
                                Text("Crear Publicación", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // --- RECENT POSTS CAROUSEL ---
        if (recentPosts.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Últimas Publicaciones",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Reportes recientes en tu vecindario",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = onNavigateToFeed) {
                            Text("Ver todas", color = InfoAnimalPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recentPosts) { post ->
                            PostListItemCard(
                                post = post,
                                onClick = { onNavigateToPostDetail(post.id) }
                            )
                        }
                    }
                }
            }
        }

        // --- HOW IT WORKS GRID ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(24.dp)
            ) {
                Text(
                    text = "Cómo funciona la red",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Cuatro pilares de ayuda comunitaria inmediata",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FeatureRow(
                        icon = Icons.Default.Search,
                        title = "Busca y publica rápidamente",
                        description = "Carga datos específicos de rasgos de la mascota, fotos y distrito."
                    )
                    FeatureRow(
                        icon = Icons.Default.LocationOn,
                        title = "Mapa de avistamientos interactivo",
                        description = "Consulta la ubicación referencial donde fue vista por última vez para achicar zonas de búsqueda."
                    )
                    FeatureRow(
                        icon = Icons.Default.People,
                        title = "Comunidad vecinal activa",
                        description = "Los vecinos interactúan directamente compartiendo contactos y consejos clave."
                    )
                    FeatureRow(
                        icon = Icons.Default.Check,
                        title = "Reuniones familiares exitosas",
                        description = "Un sistema que da de baja las alertas una vez resuelto el caso para mantener la claridad."
                    )
                }
            }
        }

        // --- CTA FINAL ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "¡Unite a InfoAnimalsR!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "No esperes a que sea tarde. Crea tu cuenta vecinal hoy y apoya los reportes locales activamente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (user != null) onNavigateToFeed() else onNavigateToSignup()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalPrimary)
                ) {
                    Text(if (user != null) "Explorar Publicaciones" else "Unirse Ahora", color = Color.White)
                }
            }
        }
    }
}
}

@Composable
fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(InfoAnimalPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = InfoAnimalPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PostListItemCard(
    post: PostEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(200.dp)
) {
    Card(
        modifier = modifier
            .height(280.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                val firstFoto = remember(post.fotosJson) {
                    post.fotosJson.split(",").firstOrNull { it.isNotEmpty() }
                }
                MascotaImage(
                    fotoString = firstFoto,
                    postId = post.id,
                    especie = post.especie,
                    nombreMascota = post.nombreMascota,
                    modifier = Modifier
                        .height(140.dp)
                        .fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )

                Column(modifier = Modifier.padding(8.dp)) {
                    StatusBadge(tipo = post.tipo, estado = post.estado)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = post.nombreMascota.ifEmpty { "Mascota sin Nombre" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        minLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = post.descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        minLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Footer (Ubicacion)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = InfoAnimalSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = post.distrito,
                    style = MaterialTheme.typography.labelSmall,
                    color = InfoAnimalSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Custom helper: fixed value height layout modifier
@Composable
fun Modifier.fillValueHeight(value: androidx.compose.ui.unit.Dp) = this.height(value)

// ============================================
// 2. FEED SCREEN
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: MainViewModel,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToCreatePost: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onNavigateToEditPost: ((String) -> Unit)? = null,
    onNavigateToUserProfile: (String) -> Unit = {}
) {
    val user by viewModel.currentUser.collectAsState()
    val posts by viewModel.allPosts.collectAsState()
    val selectedDistrito by viewModel.selectedDistrito.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    // Filter Logic
    val filteredPosts = remember(posts, selectedDistrito, selectedTab) {
        posts.filter { post ->
            val matchesDistrito = selectedDistrito == "Todos" || post.distrito == selectedDistrito
            val matchesTab = when (selectedTab) {
                "Todos" -> post.estado == "activo"
                "Perdidos" -> post.tipo == "perdida" && post.estado == "activo"
                "Encontrados" -> post.tipo == "encontrada" && post.estado == "activo"
                "Avistamientos" -> post.tipo == "avistamiento" && post.estado == "activo"
                "En Tránsito" -> (post.tipo == "en_transito" || post.tipo == "transito") && post.estado == "activo"
                "Adopciones" -> post.tipo == "adopcion" && post.estado == "activo"
                "Resueltos" -> post.estado == "resuelta"
                else -> true
            }
            matchesDistrito && matchesTab
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshPosts() },
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Info
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Publicaciones",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Ayudemos juntos a que cada mascota regrese a su hogar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Distritos Filters Select Trigger
            Row(
                modifier = Modifier
                    .fillModifierWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Distrito:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )

                var dropdownExpanded by remember { mutableStateOf(false) }
                Box {
                    Button(
                        onClick = { dropdownExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = selectedDistrito,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        listOf("Todos", "San Rafael", "General Alvear", "Malargüe").forEach { dist ->
                            DropdownMenuItem(
                                text = { Text(dist) },
                                onClick = {
                                    viewModel.setDistritoFilter(dist)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (user != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = onNavigateToCreatePost,
                        colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("pub_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Publicar",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Publicar",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontal Tab Scrollable Items
            val tabs = listOf("Todos", "Perdidos", "Encontrados", "Avistamientos", "En Tránsito", "Adopciones", "Resueltos")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tabs) { tab ->
                    val isSelected = selectedTab == tab
                    val tabBg = if (isSelected) InfoAnimalPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    val tabTextCol = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(tabBg)
                            .clickable { viewModel.setTabFilter(tab) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tab,
                            color = tabTextCol,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // List of PostCards
            if (filteredPosts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No se encontraron publicaciones",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Probá cambiando los filtros o cargá un nuevo caso voluntario.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredPosts) { post ->
                        PostItemFeedCard(
                            post = post,
                            isAuthenticated = user != null,
                            onCardClick = { onNavigateToPostDetail(post.id) },
                            onLikeClick = { viewModel.toggleLike(post.id) },
                            user = user,
                            viewModel = viewModel,
                            onNavigateToEditPost = onNavigateToEditPost,
                            onNavigateToUserProfile = onNavigateToUserProfile
                        )
                    }
                }
            }
        }

        // --- AUTH FLOATING PAYWALL IF NOT SIGNED-IN ---
        if (user == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = InfoAnimalSecondary),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "¡Contactá con los dueños!",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Iniciá sesión para ver teléfonos e interactuar con la comunidad.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onNavigateToLogin,
                            colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalPrimary)
                        ) {
                            Text("Entrar", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostItemFeedCard(
    post: PostEntity,
    isAuthenticated: Boolean,
    onCardClick: () -> Unit,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier,
    user: UserEntity? = null,
    viewModel: MainViewModel? = null,
    onNavigateToEditPost: ((String) -> Unit)? = null,
    onNavigateToUserProfile: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val isAuthorOrAdmin = remember(user, post.userId) {
        user != null && (user.id == post.userId || user.rol == "admin" || user.rol == "super_admin" || user.rol == "superadmin")
    }
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(430.dp)
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Box {
                    val firstFoto = remember(post.fotosJson) {
                        post.fotosJson.split(",").firstOrNull { it.isNotEmpty() }
                    }
                    MascotaImage(
                        fotoString = firstFoto,
                        postId = post.id,
                        especie = post.especie,
                        nombreMascota = post.nombreMascota,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentScale = ContentScale.Crop
                    )

                    // Top badges Overlay
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusBadge(tipo = post.tipo, estado = post.estado)
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    // Author Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (onNavigateToUserProfile != null) {
                                        Modifier.clickable { onNavigateToUserProfile(post.userId) }
                                    } else Modifier
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(InfoAnimalSecondary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!post.userFotoPerfil.isNullOrBlank() && post.userFotoPerfil.startsWith("http")) {
                                    AsyncImage(
                                        model = post.userFotoPerfil,
                                        contentDescription = "Avatar de ${post.userNombreCompleto}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = InfoAnimalSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                            Column {
                                Text(
                                    text = post.userNombreCompleto,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatTimestamp(post.created),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (isAuthorOrAdmin && viewModel != null) {
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Opciones de publicación",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    if (onNavigateToEditPost != null) {
                                        DropdownMenuItem(
                                            text = { Text("Editar publicación") },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                            onClick = {
                                                menuExpanded = false
                                                onNavigateToEditPost(post.id)
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Text(if (post.estado == "activo") "Marcar como Resuelta" else "Marcar como Activa")
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (post.estado == "activo") Icons.Default.CheckCircle else Icons.Default.Undo,
                                                contentDescription = null,
                                                tint = InfoAnimalAccent
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            if (post.estado == "activo") {
                                                android.app.AlertDialog.Builder(context)
                                                    .setTitle("¿Marcar como Resuelta?")
                                                    .setMessage("¿Reencontró a su familia esta mascota? La publicación se actualizará como RESUELTA.")
                                                    .setPositiveButton("Sí, resuelto") { _, _ ->
                                                        viewModel.markPostAsResolved(post.id)
                                                    }
                                                    .setNegativeButton("Cancelar", null)
                                                    .show()
                                            } else {
                                                viewModel.togglePostEstado(post.id)
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Eliminar publicación", color = InfoAnimalDestructive) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = InfoAnimalDestructive
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            android.app.AlertDialog.Builder(context)
                                                .setTitle("¿Eliminar publicación?")
                                                .setMessage("¿Estás seguro de que deseas eliminar esta publicación? Esta acción no se puede deshacer.")
                                                .setPositiveButton("Eliminar") { _, _ ->
                                                    viewModel.deletePost(post.id)
                                                }
                                                .setNegativeButton("Cancelar", null)
                                                .show()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = post.nombreMascota.ifEmpty { "Mascota sin Nombre" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        minLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Species - Size
                    Text(
                        text = "${post.especie.uppercase()} • Tamaño ${post.tamano}",
                        style = MaterialTheme.typography.labelSmall,
                        color = InfoAnimalPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = post.descripcion,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        minLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Bottom section: Location box & Footer actions
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                // Location Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Ubicación",
                            tint = InfoAnimalSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${post.distrito}, ${post.zona}",
                            style = MaterialTheme.typography.bodySmall,
                            color = InfoAnimalSecondary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Footer counts and direct quick actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (isAuthenticated) onLikeClick() },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ThumbUp, contentDescription = "Me Gusta", tint = InfoAnimalPrimary, modifier = Modifier.size(16.dp))
                            Text(text = "${post.likesCount}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(onClick = onCardClick) {
                        Text("Ver Detalle", color = InfoAnimalPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Custom helper: fixed width layout modifier
@Composable
fun Modifier.fillModifierWidth() = this.fillMaxWidth()

// ============================================
// 3. POST DETAIL SCREEN
// ============================================
@Composable
fun PostDetailScreen(
    postId: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditPost: (String) -> Unit,
    onNavigateToUserProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    val posts by viewModel.allPosts.collectAsState()
    val post = remember(posts, postId) { posts.find { it.id == postId } }

    val comments by (post?.let { viewModel.getComments(it.id) } ?: flowOf<List<CommentEntity>>(emptyList())).collectAsState(initial = emptyList())
    var commentText by remember { mutableStateOf("") }

    // Resolve author phone
    var authorPhone by remember(post) { mutableStateOf(post?.telefonoContacto ?: "2604112233") }

    if (post == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Publicación no encontrada.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Toolbar header with back button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                }
                Text(
                    text = "Detalle del Reporte",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Shared App / Web paridad URL generator
                    IconButton(onClick = {
                        val appUrl = "https://ais-pre-3fywf4tdmcd6axkku6ocl3-341410052535.us-east5.run.app"
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Mascota Reportada en InfoAnimals")
                            putExtra(android.content.Intent.EXTRA_TEXT, "¡Ayuda a difundir! Mascota: ${post.nombreMascota.ifEmpty { "sin nombre" }} (${post.especie}) - Zona: ${post.zona ?: ""}, ${post.distrito}. Ver en InfoAnimals: $appUrl/post/${post.id}")
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir Reporte"))
                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Compartir", tint = InfoAnimalPrimary)
                    }

                    // Edit / Status / Delete Buttons if Owner, Admin, Super Admin, or ONG
                    if (user != null && (user!!.id == post.userId || user!!.rol == "admin" || user!!.rol == "super_admin" || user!!.rol == "superadmin" || user!!.rol == "ong")) {
                        IconButton(onClick = { onNavigateToEditPost(post.id) }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = InfoAnimalSecondary)
                        }
                        IconButton(onClick = {
                            if (post.estado == "activo") {
                                android.app.AlertDialog.Builder(context)
                                    .setTitle("¿Marcar como Resuelto?")
                                    .setMessage("¿Reencontró a su familia esta mascota? La publicación se actualizará como RESUELTA.")
                                    .setPositiveButton("Sí, resuelto") { _, _ -> viewModel.markPostAsResolved(post.id) }
                                    .setNegativeButton("Cancelar", null)
                                    .show()
                            } else {
                                viewModel.togglePostEstado(post.id)
                            }
                        }) {
                            Icon(
                                imageVector = if (post.estado == "activo") Icons.Default.CheckCircle else Icons.Default.Undo,
                                contentDescription = "Cambiar Estado",
                                tint = InfoAnimalAccent
                            )
                        }
                        IconButton(onClick = {
                            android.app.AlertDialog.Builder(context)
                                .setTitle("¿Eliminar publicación?")
                                .setMessage("¿Estás seguro de que deseas eliminar esta publicación? Esta acción no se puede deshacer.")
                                .setPositiveButton("Eliminar") { _, _ ->
                                    viewModel.deletePost(post.id)
                                    onNavigateBack()
                                }
                                .setNegativeButton("Cancelar", null)
                                .show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = InfoAnimalDestructive
                            )
                        }
                    }
                }
            }
        }

        // Pet Hero Image
        item {
            val fotosList = remember(post.fotosJson) {
                post.fotosJson.split(",").filter { it.isNotEmpty() }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                if (fotosList.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(fotosList) { fotoStr ->
                            MascotaImage(
                                fotoString = fotoStr,
                                postId = post.id,
                                especie = post.especie,
                                nombreMascota = post.nombreMascota,
                                modifier = Modifier
                                    .fillParentMaxWidth()
                                    .fillMaxHeight(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                } else {
                    PetPlaceholder(
                        especie = post.especie,
                        modifier = Modifier.fillMaxSize(),
                        name = post.nombreMascota
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    StatusBadge(tipo = post.tipo, estado = post.estado)
                }
            }
        }

        // Details Panel layout split
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = post.nombreMascota.ifEmpty { "Mascota sin Nombre" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "${post.especie.uppercase()} • Tamaño ${post.tamano}",
                    style = MaterialTheme.typography.titleSmall,
                    color = InfoAnimalPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Detail Box Content
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Descripción del Caso:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = post.descripcion,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (post.rasgos.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Rasgos Particulares:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = post.rasgos,
                                style = MaterialTheme.typography.bodyMedium,
                                color = InfoAnimalSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Distrito & Zona
                Text(
                    text = "Ubicación:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${post.distrito} - ${post.zona}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // --- AUTHOR INFO CARD (Perfil social comentado) ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(InfoAnimalSecondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!post.userFotoPerfil.isNullOrBlank() && post.userFotoPerfil.startsWith("http")) {
                                AsyncImage(
                                    model = post.userFotoPerfil,
                                    contentDescription = "Foto de ${post.userNombreCompleto}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = InfoAnimalSecondary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Text(
                                text = "Publicado por",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = post.userNombreCompleto,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { onNavigateToUserProfile(post.userId) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = InfoAnimalPrimary),
                        border = BorderStroke(1.dp, InfoAnimalPrimary),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Ver perfil", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // --- CONTACT INFO & WHATSAPP BUTTONS ---
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Contacto:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (user == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(InfoAnimalDestructive.copy(alpha = 0.1f))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Iniciá sesión para ver los datos de contacto del dueño.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InfoAnimalDestructive,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (!post.telefonoVisible) {
                    Text(
                        text = "Teléfono marcado como no visible.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Show contact buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { triggerWhatsApp(context, authorPhone, "Hola! Vengo de ver tu publicación de ${post.nombreMascota} en InfoAnimalsR.") },
                            colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp", color = Color.White)
                        }

                        Button(
                            onClick = { triggerCall(context, authorPhone) },
                            colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalSecondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Llamar", color = Color.White)
                        }
                    }
                }
            }
        }

        /*
        // --- Red Social: Sección de Comentarios (Comentado) ---
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Comentarios (${comments.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (user == null) {
                    Text(
                        text = "Debes iniciar sesión para comentar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("Escribí un aporte vecinal guiado...") },
                            modifier = Modifier.weight(1f),
                            maxLines = 3
                        )
                        IconButton(
                            onClick = {
                                if (commentText.isNotEmpty()) {
                                    viewModel.insertComment(post.id, commentText)
                                    commentText = ""
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = InfoAnimalPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Enviar", tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    comments.forEach { comment ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.clickable { onNavigateToUserProfile(comment.userId) }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(InfoAnimalSecondary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!comment.userFotoPerfil.isNullOrBlank() && comment.userFotoPerfil.startsWith("http")) {
                                                AsyncImage(
                                                    model = comment.userFotoPerfil,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape)
                                                )
                                            } else {
                                                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = InfoAnimalSecondary, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                        Text(
                                            text = comment.userNombreCompleto,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        if (comment.userIsOng) {
                                            OngBadge()
                                        }
                                    }

                                    // Delete commenter button if owner / admin / super_admin
                                    if (user != null && (user!!.id == comment.userId || user!!.rol == "admin" || user!!.rol == "super_admin" || user!!.rol == "superadmin")) {
                                        IconButton(
                                            onClick = {
                                                if (android.app.AlertDialog.Builder(context)
                                                        .setTitle("¿Eliminar Comentario?")
                                                        .setMessage("Esta acción es irreversible.")
                                                        .setPositiveButton("Sí, eliminar") { _, _ -> viewModel.deleteComment(comment.id) }
                                                        .setNegativeButton("Cancelar", null)
                                                        .show() != null
                                                ) { /* alert active */ }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = InfoAnimalDestructive, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                Text(
                                    text = formatTimestamp(comment.created),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = comment.texto,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
        */
    }
}

// ============================================
// 4. CREATE / EDIT POST SCREEN (CON HELPER GEOCODER & FOTOS)
// ============================================

// Helper: Convierte Uri a Base64
fun uriToBase64(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        if (bytes != null) {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

// Helper: Geocodifica para Mendoza, Argentina
suspend fun geocodeAddress(distrito: String, zona: String): Pair<Double, Double>? {
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val queryStr = if (zona.isNotEmpty()) "$zona, $distrito, Mendoza, Argentina" else "$distrito, Mendoza, Argentina"
            val query = java.net.URLEncoder.encode(queryStr, "UTF-8")
            val urlStr = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=$query"
            val conn = java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection
            conn.setRequestProperty("User-Agent", "InfoAnimalsR-MobileApp/1.0")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            if (text.startsWith("[")) {
                val jsonArray = org.json.JSONArray(text)
                if (jsonArray.length() > 0) {
                    val obj = jsonArray.getJSONObject(0)
                    val lat = obj.getDouble("lat")
                    val lon = obj.getDouble("lon")
                    return@withContext Pair(lat, lon)
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

@Composable
fun CreatePostScreen(
    editId: String?,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onSaveFinished: (String) -> Unit
) {
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    val posts by viewModel.allPosts.collectAsState()
    val postToEdit = remember(posts, editId) { posts.find { it.id == editId } }

    var tipo by remember { mutableStateOf(postToEdit?.tipo ?: "perdida") }
    var especie by remember { mutableStateOf(postToEdit?.especie ?: "perro") }
    var tamano by remember { mutableStateOf(postToEdit?.tamano ?: "mediano") }
    var nombre by remember { mutableStateOf(postToEdit?.nombreMascota ?: "") }
    var descripcion by remember { mutableStateOf(postToEdit?.descripcion ?: "") }
    var rasgos by remember { mutableStateOf(postToEdit?.rasgos ?: "") }
    var distrito by remember { mutableStateOf(postToEdit?.distrito ?: "San Rafael") }
    var zona by remember { mutableStateOf(postToEdit?.zona ?: "") }
    var telefonoVisible by remember { mutableStateOf(postToEdit?.telefonoVisible ?: true) }
    var telefonoContacto by remember { mutableStateOf(postToEdit?.telefonoContacto ?: user?.telefono ?: "") }

    val initialPhotos = remember(postToEdit) {
        postToEdit?.fotosJson?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    }
    var selectedPhotosBase64 by remember { mutableStateOf<List<String>>(initialPhotos) }

    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (user == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Debes iniciar sesión para publicar.")
        }
        return
    }

    // Photo picker setup
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val remainLimit = 10 - selectedPhotosBase64.size
            if (remainLimit <= 0) {
                Toast.makeText(context, "Límite máximo de 10 fotos alcanzado", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            val base64s = uris.take(remainLimit).mapNotNull { uri ->
                uriToBase64(context, uri)
            }
            selectedPhotosBase64 = selectedPhotosBase64 + base64s
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                }
                Text(
                    text = if (editId != null) "Editar Publicación" else "Cargar Mascota",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Form Fields
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Tipo Select
                Text("Tipo de Reporte:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("perdida", "encontrada", "avistamiento").forEach { t ->
                            val isSel = tipo == t
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) InfoAnimalPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { tipo = t }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = t.substring(0, 1).uppercase() + t.substring(1),
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("transito", "adopcion").forEach { t ->
                            val valueToSet = if (t == "transito") "en_transito" else "adopcion"
                            val isSel = tipo == valueToSet
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) InfoAnimalPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { tipo = valueToSet }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (t == "transito") "Tránsito" else "Adopción",
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Especie Select
                Text("Especie:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("perro", "gato", "otros").forEach { e ->
                        val isSel = especie == e
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) InfoAnimalSecondary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { especie = e }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = e.substring(0, 1).uppercase() + e.substring(1),
                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                              )
                        }
                    }
                }

                // Tamaño Select
                Text("Tamaño:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("pequeño", "mediano", "grande").forEach { sz ->
                        val isSel = tamano == sz
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) InfoAnimalAccent else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { tamano = sz }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = sz.substring(0, 1).uppercase() + sz.substring(1),
                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Inputs
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre (Opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción del Caso *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                OutlinedTextField(
                    value = rasgos,
                    onValueChange = { rasgos = it },
                    label = { Text("Rasgos Distintivos (Señas particulares)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Distrito & Zona Dropdown Selector
                Text("Distrito mendocino *", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                var distExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { distExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(distrito, color = MaterialTheme.colorScheme.onSurface)
                    }

                    DropdownMenu(expanded = distExpanded, onDismissRequest = { distExpanded = false }) {
                        listOf("San Rafael", "General Alvear", "Malargüe").forEach { d ->
                            DropdownMenuItem(text = { Text(d) }, onClick = {
                                distrito = d
                                distExpanded = false
                            })
                        }
                    }
                }

                OutlinedTextField(
                    value = zona,
                    onValueChange = { zona = it },
                    label = { Text("Zona o Barrio Específico") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Fotos Selector Gallery Preview Row
                Text("Fotos (Máximo 10):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = "Añadir Foto", tint = InfoAnimalPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Añadir", style = MaterialTheme.typography.labelSmall, color = InfoAnimalPrimary)
                            }
                        }
                    }

                    items(selectedPhotosBase64) { b64 ->
                        val bitmap = remember(b64) {
                            try {
                                val cleanB64 = if (b64.contains(",")) b64.substringAfter(",") else b64
                                val decodedBytes = android.util.Base64.decode(cleanB64, android.util.Base64.DEFAULT)
                                android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        Box(modifier = Modifier.size(90.dp)) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Previsualización",
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(Color.LightGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Photo, contentDescription = null)
                                }
                            }

                            IconButton(
                                onClick = {
                                    selectedPhotosBase64 = selectedPhotosBase64 - b64
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Eliminar",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Phone Contact Information
                OutlinedTextField(
                    value = telefonoContacto,
                    onValueChange = { telefonoContacto = it },
                    label = { Text("Teléfono de contacto (Opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                // Phone visibility
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Mostrar botones de contacto directo", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = telefonoVisible, onCheckedChange = { telefonoVisible = it })
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (descripcion.isEmpty()) {
                            Toast.makeText(context, "Por favor completa la descripción del caso (Campo obligatorio).", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (distrito.isEmpty()) {
                            Toast.makeText(context, "Por favor selecciona un distrito (Campo obligatorio).", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isSaving = true
                        viewModel.savePost(
                            id = editId,
                            tipo = tipo,
                            especie = especie,
                            tamano = tamano,
                            nombre = nombre,
                            descripcion = descripcion,
                            rasgos = rasgos,
                            distrito = distrito,
                            zona = zona,
                            fotos = selectedPhotosBase64.joinToString(","),
                            lat = 0.0,
                            lng = 0.0,
                            telefonoVisible = telefonoVisible,
                            telefonoContacto = telefonoContacto
                        ) { finalId, errorMsg ->
                            isSaving = false
                            if (finalId != null) {
                                viewModel.refreshPosts()
                                Toast.makeText(context, "¡Tu publicación ha sido creada exitosamente!", Toast.LENGTH_SHORT).show()
                                onSaveFinished(finalId)
                            } else {
                                Toast.makeText(context, errorMsg ?: "No pudimos conectar con el servidor. Verifica tu conexión a internet e intenta de nuevo.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardando...")
                    } else {
                        Text("Guardar Reporte", color = Color.White)
                    }
                }
            }
        }
    }
}

// ============================================
// 5. VETERINARIAS SCREEN
// ============================================
@Composable
fun VeterinariasScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    val veterinarias by viewModel.veterinarias.collectAsState()

    var activeTab by remember { mutableStateOf("Todas") }

    // Dialog state
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingVet by remember { mutableStateOf<com.example.data.model.VeterinariaEntity?>(null) }
    var vetNombre by remember { mutableStateOf("") }
    var vetDireccion by remember { mutableStateOf("") }
    var vetTelefono by remember { mutableStateOf("") }
    var vetStaff by remember { mutableStateOf("") }
    var vetHorarios by remember { mutableStateOf("") }
    var vetDistrito by remember { mutableStateOf("San Rafael") }
    var vetClasificacion by remember { mutableStateOf("Horario Comercio") }
    var vetActiva by remember { mutableStateOf(true) }

    val isAdmin = user != null && (user!!.rol == "admin" || user!!.rol == "super_admin")

    val filteredVets = remember(veterinarias, activeTab, isAdmin) {
        val baseList = if (isAdmin) veterinarias else veterinarias.filter { it.activa }
        when (activeTab) {
            "Todas" -> baseList
            "Guardia 24hs" -> baseList.filter { it.clasificacion == "Guardia 24hs" }
            else -> baseList.filter { it.clasificacion == "Horario Comercio" }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                }
                Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
                    Text("Guía de Veterinarias", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Directorio local del sur de Mendoza", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isAdmin) {
                    Button(
                        onClick = {
                            editingVet = null
                            vetNombre = ""
                            vetDireccion = ""
                            vetTelefono = ""
                            vetStaff = ""
                            vetHorarios = ""
                            vetDistrito = "San Rafael"
                            vetClasificacion = "Horario Comercio"
                            vetActiva = true
                            showAddEditDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalPrimary)
                    ) {
                        Text("Crear", color = Color.White)
                    }
                }
            }
        }

        // Horizontal tabs options
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Todas", "Guardia 24hs", "Horario Comercio").forEach { tab ->
                    val isSel = activeTab == tab
                    val bg = if (isSel) {
                        if (tab == "Guardia 24hs") InfoAnimalDestructive else InfoAnimalSecondary
                    } else MaterialTheme.colorScheme.surfaceVariant

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .clickable { activeTab = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // List of Veterinarias Cards
        items(filteredVets) { vet ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo Box
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (vet.clasificacion == "Guardia 24hs") InfoAnimalDestructive.copy(alpha = 0.15f) else InfoAnimalSecondary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (vet.clasificacion == "Guardia 24hs") Icons.Default.LocalHospital else Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = if (vet.clasificacion == "Guardia 24hs") InfoAnimalDestructive else InfoAnimalSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = vet.nombre,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            if (isAdmin) {
                                IconButton(onClick = {
                                    editingVet = vet
                                    vetNombre = vet.nombre
                                    vetDireccion = vet.direccion
                                    vetTelefono = vet.telefono
                                    vetStaff = vet.staff
                                    vetHorarios = vet.horarios
                                    vetDistrito = vet.distrito
                                    vetClasificacion = vet.clasificacion
                                    vetActiva = vet.activa
                                    showAddEditDialog = true
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = InfoAnimalSecondary, size = 18.dp)
                                }
                                IconButton(onClick = {
                                    android.app.AlertDialog.Builder(context)
                                        .setTitle("¿Eliminar Veterinaria?")
                                        .setMessage("¿Estás seguro que deseas eliminar esta veterinaria?")
                                        .setPositiveButton("Eliminar") { _, _ -> viewModel.deleteVeterinaria(vet.id) }
                                        .setNegativeButton("Cancelar", null)
                                        .show()
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = InfoAnimalDestructive, size = 18.dp)
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Badge Clasificacion
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (vet.clasificacion == "Guardia 24hs") InfoAnimalDestructive.copy(alpha = 0.1f) else InfoAnimalSecondary.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = vet.clasificacion,
                                    color = if (vet.clasificacion == "Guardia 24hs") InfoAnimalDestructive else InfoAnimalSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (!vet.activa) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(InfoAnimalDestructive.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "INACTIVA",
                                        color = InfoAnimalDestructive,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Dirección: ${vet.direccion} (${vet.distrito})", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Horarios: ${vet.horarios}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (vet.staff.isNotEmpty()) {
                            Text(text = "Staff: ${vet.staff}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { triggerWhatsApp(context, vet.telefono, "Hola! Vengo de InfoAnimalsR. ¿Cuentan con atención en este momento?") }) {
                                Icon(imageVector = Icons.Default.Chat, contentDescription = "WhatsApp", tint = InfoAnimalAccent)
                            }
                            IconButton(onClick = { triggerCall(context, vet.telefono) }) {
                                Icon(imageVector = Icons.Default.Phone, contentDescription = "Llamar", tint = InfoAnimalSecondary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = { Text(if (editingVet == null) "Cargar Veterinaria" else "Editar Veterinaria") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = vetNombre,
                        onValueChange = { vetNombre = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = vetDireccion,
                        onValueChange = { vetDireccion = it },
                        label = { Text("Dirección") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = vetTelefono,
                        onValueChange = { vetTelefono = it },
                        label = { Text("Teléfono") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = vetHorarios,
                        onValueChange = { vetHorarios = it },
                        label = { Text("Horarios") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = vetStaff,
                        onValueChange = { vetStaff = it },
                        label = { Text("Staff / Profesionales") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Distrito Dropdown
                    Text("Distrito:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    var distMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        Button(
                            onClick = { distMenuExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(vetDistrito, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(expanded = distMenuExpanded, onDismissRequest = { distMenuExpanded = false }) {
                            listOf("San Rafael", "General Alvear", "Malargüe").forEach { d ->
                                DropdownMenuItem(
                                    text = { Text(d) },
                                    onClick = {
                                        vetDistrito = d
                                        distMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Clasificacion Dropdown
                    Text("Clasificación:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    var classMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        Button(
                            onClick = { classMenuExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(vetClasificacion, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(expanded = classMenuExpanded, onDismissRequest = { classMenuExpanded = false }) {
                            listOf("Horario Comercio", "Guardia 24hs").forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c) },
                                    onClick = {
                                        vetClasificacion = c
                                        classMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Activa Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Visible (Activa):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Switch(checked = vetActiva, onCheckedChange = { vetActiva = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (vetNombre.isEmpty() || vetDireccion.isEmpty() || vetTelefono.isEmpty()) {
                            Toast.makeText(context, "Por favor, completa Nombre, Dirección y Teléfono.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.saveVeterinaria(
                            id = editingVet?.id,
                            nombre = vetNombre,
                            direccion = vetDireccion,
                            telefono = vetTelefono,
                            staff = vetStaff,
                            horarios = vetHorarios,
                            distrito = vetDistrito,
                            clasificacion = vetClasificacion,
                            lat = -34.61, // default coords
                            lng = -68.33,
                            logo = "",
                            activa = vetActiva
                        ) {
                            showAddEditDialog = false
                            Toast.makeText(context, "Veterinaria guardada exitosamente!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalPrimary)
                ) {
                    Text("Guardar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEditDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// ============================================
// 6. CAMPAÑAS SCREEN
// ============================================
@Composable
fun CampañasScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    val campaigns by viewModel.campaigns.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var currentCampDetail by remember { mutableStateOf<CampaignEntity?>(null) }

    // Dialog inputs
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var detailText by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                }
                Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
                    Text("Campañas de Recolección", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Apoya los esfuerzos de protectoras locales", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Add campaign if ONG / Admin / Super Admin
                if (user != null && (user!!.rol == "ong" || user!!.rol == "admin" || user!!.rol == "super_admin" || user!!.rol == "superadmin")) {
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalPrimary)
                    ) {
                        Text("Crear", color = Color.White)
                    }
                }
            }
        }

        // Grid listing of campaigns
        items(campaigns) { camp ->
            var supported by remember { mutableStateOf(false) }
            LaunchedEffect(camp.id) {
                supported = viewModel.isCampaignSupported(camp.id)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { currentCampDetail = camp },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = camp.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Por ${camp.ongNombre}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InfoAnimalPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = camp.descripcion, style = MaterialTheme.typography.bodyMedium, maxLines = 3)

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vigencia: ${camp.fechaInicio} al ${camp.fechaFin}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                if (user == null) {
                                    Toast.makeText(context, "Iniciá sesión para apoyar.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.toggleCampaignLike(camp.id)
                                supported = !supported
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (supported) InfoAnimalSecondary else InfoAnimalPrimary)
                        ) {
                            Text(if (supported) "Apoyando" else "Apoyar (${camp.likesCount})", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // --- Campaign Details dialog ---
    currentCampDetail?.let { camp ->
        AlertDialog(
            onDismissRequest = { currentCampDetail = null },
            confirmButton = {
                TextButton(onClick = { currentCampDetail = null }) {
                    Text("Cerrar")
                }
            },
            title = {
                Column {
                    Text(text = camp.titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    OngBadge(size = "lg")
                }
            },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    item {
                        Text(text = "Organización: ${camp.ongNombre}", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = camp.contenidoDetallado, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Período: ${camp.fechaInicio} - ${camp.fechaFin}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        )
    }

    // --- Create Campaign dialog ---
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isEmpty() || desc.isEmpty()) {
                            Toast.makeText(context, "Completa los campos.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.saveCampaign(null, title, desc, detailText, startDate, endDate, "activa", "") {
                            showCreateDialog = false
                            title = ""
                            desc = ""
                            detailText = ""
                            startDate = ""
                            endDate = ""
                            Toast.makeText(context, "Campaña creada exitosamente!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalPrimary)
                ) {
                    Text("Guardar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancelar")
                }
            },
            title = { Text("Nueva Campaña") },
            text = {
                LazyColumn {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Título de Campaña") }
                            )
                            OutlinedTextField(
                                value = desc,
                                onValueChange = { desc = it },
                                label = { Text("Descripción Corta") }
                            )
                            OutlinedTextField(
                                value = detailText,
                                onValueChange = { detailText = it },
                                label = { Text("Contenido Detallado") },
                                minLines = 3
                            )
                            OutlinedTextField(
                                value = startDate,
                                onValueChange = { startDate = it },
                                label = { Text("Fecha Inicio (ej: 18/06/2026)") }
                            )
                            OutlinedTextField(
                                value = endDate,
                                onValueChange = { endDate = it },
                                label = { Text("Fecha Fin (ej: 31/07/2026)") }
                            )
                        }
                    }
                }
            }
        )
    }
}

// ============================================
// 7. NOTICIAS SCREEN
// ============================================
@Composable
fun NoticiasScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    val news by viewModel.news.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var currentNewsDetail by remember { mutableStateOf<NewsEntity?>(null) }

    // Dialog inputs
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                }
                Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
                    Text("Portal de Noticias", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Novedades y logros locales del Sur Mendocino", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Create news if ONG / Admin / Super Admin
                if (user != null && (user!!.rol == "ong" || user!!.rol == "admin" || user!!.rol == "super_brand" || user!!.rol == "super_admin" || user!!.rol == "superadmin")) {
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalPrimary)
                    ) {
                        Text("Escribir", color = Color.White)
                    }
                }
            }
        }

        // List of news items
        items(news) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { currentNewsDetail = item },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = item.titular,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Publicado por ${item.autorNombre}",
                        style = MaterialTheme.typography.labelSmall,
                        color = InfoAnimalPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = item.contenido, style = MaterialTheme.typography.bodyMedium, maxLines = 3)

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { currentNewsDetail = item }) {
                            Text("Leer completa", color = InfoAnimalSecondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // --- News Details dialog ---
    currentNewsDetail?.let { item ->
        AlertDialog(
            onDismissRequest = { currentNewsDetail = null },
            confirmButton = {
                TextButton(onClick = { currentNewsDetail = null }) {
                    Text("Cerrar")
                }
            },
            title = { Text(text = item.titular, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    item {
                        Text(text = "Autor: ${item.autorNombre}", style = MaterialTheme.typography.labelSmall, color = InfoAnimalPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = item.contenido, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        )
    }

    // --- Create News dialog ---
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isEmpty() || content.isEmpty()) {
                            Toast.makeText(context, "Completa los campos.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.saveNews(null, title, content, "", "") {
                            showCreateDialog = false
                            title = ""
                            content = ""
                            Toast.makeText(context, "Noticia publicada!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalPrimary)
                ) {
                    Text("Publicar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancelar")
                }
            },
            title = { Text("Informar Novedad") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Titular de la Noticia") }
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Contenido Completo") },
                        minLines = 4
                    )
                }
            }
        )
    }
}

// ============================================
// 8. PERFIL SCREEN
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    profileUserId: String?,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onLogout: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    val allPosts by viewModel.allPosts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isUploadingAvatar by viewModel.isUploadingAvatar.collectAsState()
    val isUploadingCover by viewModel.isUploadingCover.collectAsState()

    val isSelf = profileUserId == null || profileUserId == user?.id

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadAvatar(it) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadCover(it) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Retrieve target profile info
    var targetUser by remember { mutableStateOf<UserEntity?>(null) }
    var currentTab by remember { mutableStateOf("Publicaciones") }

    // Forms settings edit
    var showSettingsDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editZone by remember { mutableStateOf("") }

    // State profile muro
    var muroText by remember { mutableStateOf("") }

    val activePosts = remember(allPosts, profileUserId, user) {
        val uid = profileUserId ?: user?.id ?: ""
        allPosts.filter { it.userId == uid }
    }

    val statuses by viewModel.statuses.collectAsState()
    val filteredStatuses = remember(statuses, profileUserId, user) {
        val uid = profileUserId ?: user?.id ?: ""
        statuses.filter { it.userId == uid }
    }

    LaunchedEffect(profileUserId, user) {
        val uid = profileUserId ?: user?.id ?: ""
        if (uid.isNotEmpty()) {
            if (uid == user?.id) {
                targetUser = user
            } else {
                try {
                    val response = com.example.data.network.ApiClient.getService().getUserProfile("eq.$uid")
                    if (response.isSuccessful && response.body() != null && response.body()!!.isNotEmpty()) {
                        val su = response.body()!!.first()
                        targetUser = UserEntity(
                            id = su.id,
                            email = su.email ?: "",
                            nombre = su.nombre ?: "",
                            telefono = su.telefono ?: "",
                            zonaPrincipal = su.zonaPrincipal ?: "San Rafael",
                            rol = su.rol ?: "user",
                            fotoPerfil = su.fotoPerfil ?: "avatar_placeholder",
                            fotoPortada = su.fotoPortada ?: "gradient_blue",
                            biografia = su.biografia ?: ""
                        )
                    } else {
                        val matchedPost = allPosts.find { it.userId == uid }
                        val matchedStatus = statuses.find { it.userId == uid }
                        val userName = matchedPost?.userNombreCompleto ?: matchedStatus?.userNombreCompleto ?: "Vecino"
                        val userPhoto = matchedPost?.userFotoPerfil ?: matchedStatus?.userFotoPerfil ?: "avatar_placeholder"
                        targetUser = UserEntity(
                            id = uid,
                            email = "",
                            nombre = userName,
                            telefono = matchedPost?.telefonoContacto ?: "",
                            zonaPrincipal = matchedPost?.zona ?: "San Rafael",
                            rol = "user",
                            fotoPerfil = userPhoto,
                            fotoPortada = "gradient_blue",
                            biografia = ""
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AppScreens", "Error fetching profile", e)
                    val matchedPost = allPosts.find { it.userId == uid }
                    val matchedStatus = statuses.find { it.userId == uid }
                    val userName = matchedPost?.userNombreCompleto ?: matchedStatus?.userNombreCompleto ?: "Vecino"
                    val userPhoto = matchedPost?.userFotoPerfil ?: matchedStatus?.userFotoPerfil ?: "avatar_placeholder"
                    targetUser = UserEntity(
                        id = uid,
                        email = "",
                        nombre = userName,
                        telefono = matchedPost?.telefonoContacto ?: "",
                        zonaPrincipal = matchedPost?.zona ?: "San Rafael",
                        rol = "user",
                        fotoPerfil = userPhoto,
                        fotoPortada = "gradient_blue",
                        biografia = ""
                    )
                }
            }
            targetUser?.let { dbUser ->
                editName = dbUser.nombre
                editBio = dbUser.biografia
                editPhone = dbUser.telefono
                editZone = dbUser.zonaPrincipal
            }
        }
    }

    val displayUser = if (isSelf && user != null) user else targetUser
    val followingUserIds by viewModel.followingUserIds.collectAsState()

    if (displayUser == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = InfoAnimalPrimary)
        }
        return
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshPosts() },
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
        // Toolbar with back button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                }
                Text(
                    text = if (isSelf) "Mi Perfil" else "Perfil Vecino",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                if (isSelf) {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Ajustes", tint = InfoAnimalPrimary)
                    }
                } else {
                    // Profile Report button
                    IconButton(onClick = {
                        val reportPrompt = android.app.AlertDialog.Builder(context)
                        val input = android.widget.EditText(context)
                        input.hint = "Razón del reporte"
                        reportPrompt.setTitle("Reportar Perfil")
                            .setMessage("Escribí el motivo para procesado por la moderación.")
                            .setView(input)
                            .setPositiveButton("Enviar") { _, _ ->
                                val text = input.text.toString()
                                if (text.isNotEmpty()) {
                                    viewModel.reportProfile(displayUser.id, text) {
                                        Toast.makeText(context, "Reporte enviado exitosamente.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }) {
                        Icon(imageVector = Icons.Default.Report, contentDescription = "Reportar", tint = InfoAnimalDestructive)
                    }
                }
            }
        }

        // Cover picture header & Avatar overlay
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Cover Picture Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(InfoAnimalPrimary, InfoAnimalSecondary)
                            )
                        )
                ) {
                    if (displayUser.fotoPortada.startsWith("http")) {
                        AsyncImage(
                            model = displayUser.fotoPortada,
                            contentDescription = "Foto de Portada",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (isUploadingCover) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }

                    if (isSelf) {
                        SmallFloatingActionButton(
                            onClick = { coverPickerLauncher.launch("image/*") },
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            contentColor = InfoAnimalPrimary,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Cambiar Foto de Portada",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Avatar overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp)
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .then(
                            if (isSelf) Modifier.clickable { avatarPickerLauncher.launch("image/*") }
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (displayUser.fotoPerfil.startsWith("http")) {
                        AsyncImage(
                            model = displayUser.fotoPerfil,
                            contentDescription = "Foto de Perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar de Perfil",
                            tint = InfoAnimalPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    if (isUploadingAvatar) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                        }
                    } else if (isSelf) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(InfoAnimalPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Cambiar Foto de Perfil",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // User info details listing
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = displayUser.nombre,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (displayUser.rol == "ong") {
                            OngBadge()
                        }
                    }

                    /*
                    // --- Red Social: Funciones de Seguir / Dejar de seguir (Comentado) ---
                    if (!isSelf && user != null) {
                        val isFollowing = followingUserIds.contains(displayUser.id)
                        if (isFollowing) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.unfollowUser(displayUser.id) {
                                        Toast.makeText(context, "Dejaste de seguir a ${displayUser.nombre}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = InfoAnimalPrimary),
                                border = BorderStroke(1.dp, InfoAnimalPrimary),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Siguiendo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.followUser(displayUser.id) {
                                        Toast.makeText(context, "Ahora sigues a ${displayUser.nombre}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalPrimary),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Seguir", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                    */
                }

                if (displayUser.biografia.isNotEmpty()) {
                    Text(
                        text = displayUser.biografia,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = InfoAnimalSecondary, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Zona: ${displayUser.zonaPrincipal.ifEmpty { "Sin definir" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InfoAnimalSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (displayUser.telefono.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = InfoAnimalAccent, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Teléfono: ${displayUser.telefono}",
                            style = MaterialTheme.typography.bodySmall,
                            color = InfoAnimalAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Header de Publicaciones del Usuario
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    tint = InfoAnimalPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isSelf) "Mis Publicaciones de Mascotas" else "Publicaciones del Vecino",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = InfoAnimalPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = CircleShape,
                    color = InfoAnimalPrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${activePosts.size}",
                        color = InfoAnimalPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Display contents
        if (activePosts.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aún no hay publicaciones activas de mascotas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(activePosts) { post ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    PostListItemCard(
                        post = post,
                        onClick = {}
                    )
                }
            }
        }
    }
}

    // --- Profile settings Dialog ---
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            showSettingsDialog = false
                            viewModel.logout {
                                onLogout?.invoke()
                            }
                        },
                        border = BorderStroke(1.dp, InfoAnimalDestructive)
                    ) {
                        Text("Cerrar Sesión", color = InfoAnimalDestructive)
                    }
                    Button(
                        onClick = {
                            viewModel.updateProfile(editName, editBio, editPhone, editZone, {
                                showSettingsDialog = false
                                Toast.makeText(context, "Perfil guardado!", Toast.LENGTH_SHORT).show()
                            }, { err ->
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalPrimary)
                    ) {
                        Text("Guardar", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Cancelar")
                }
            },
            title = { Text("Ajustes de Perfil") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Nombre Completo") })
                    OutlinedTextField(value = editBio, onValueChange = { editBio = it }, label = { Text("Biografía") }, minLines = 2)
                    OutlinedTextField(value = editPhone, onValueChange = { editPhone = it }, label = { Text("Teléfono de Contacto") })
                    OutlinedTextField(value = editZone, onValueChange = { editZone = it }, label = { Text("Distrito Predeterminado") })
                }
            }
        )
    }
}

@Composable
fun Icon(imageVector: ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    Icon(imageVector = imageVector, contentDescription = contentDescription, modifier = Modifier.size(size), tint = tint)
}

// ============================================
// 9. ADMIN PANEL SCREEN
// ============================================
@Composable
fun AdminScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val totalCount by viewModel.totalPostsCount.collectAsState()
    val activeCount by viewModel.activePostsCount.collectAsState()
    val resolvedCount by viewModel.resolvedPostsCount.collectAsState()
    val usersCount by viewModel.totalUsersCount.collectAsState()

    val pendingReports by viewModel.reports.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                }
                Text(
                    text = "Panel de Gestión",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // Statistics Cards Overview
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Estadísticas de la plataforma:", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox(modifier = Modifier.weight(1f), count = totalCount, label = "Totales")
                    StatBox(modifier = Modifier.weight(1f), count = activeCount, label = "Activas")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox(modifier = Modifier.weight(1f), count = resolvedCount, label = "Resueltas")
                    StatBox(modifier = Modifier.weight(1f), count = usersCount, label = "Usuarios")
                }
            }
        }

        // Connection Info Card
        if (com.example.BuildConfig.ENABLE_TEST_ROLE_MAPPING) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = "Database",
                                tint = InfoAnimalPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Base de Datos Externa (PostgreSQL)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = InfoAnimalPrimary
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        
                        val dbHost = try { com.example.BuildConfig.DB_HOST } catch (e: Exception) { "dpg-d8pvjlq8qa3s73ftn780-a" }
                        val dbName = try { com.example.BuildConfig.DB_NAME } catch (e: Exception) { "infoanimalsr" }
                        val dbUser = try { com.example.BuildConfig.DB_USER } catch (e: Exception) { "infoanimalsr_user" }
                        val dbExternalUrl = try { com.example.BuildConfig.DB_EXTERNAL_URL } catch (e: Exception) { "postgresql://infoanimalsr_user:LK1pIHb5FmmEAAUrEhvzBXarIxcvLMi1@dpg-d8pvjlq8qa3s73ftn780-a.oregon-postgres.render.com/infoanimalsr" }
                        
                        Text(
                            text = "Servidor remoto postgres de producción configurado en Render. Sincronización en tiempo real vía REST API.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Column(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(6.dp))
                                .padding(10.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "Host: $dbHost", style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Text(text = "Database: $dbName", style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Text(text = "User: $dbUser", style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Text(text = "URL: $dbExternalUrl", style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // Reports title
        item {
            Text("Reportes Pendientes de la Comunidad:", fontWeight = FontWeight.Bold)
        }

        if (pendingReports.isEmpty()) {
            item {
                Text(
                    text = "¡Excelente! No hay reportes ni denuncias vecinales sin resolver.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // List of reports
        items(pendingReports) { rep ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(InfoAnimalDestructive.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = rep.estado.uppercase(), color = InfoAnimalDestructive, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { viewModel.reviewReport(rep.id, true) }) {
                                Text("Aprobar / Ignorar", color = InfoAnimalAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { viewModel.deletePost(rep.postId) }) {
                                Text("Eliminar Post", color = InfoAnimalDestructive, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Razón: ${rep.razon}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Post ID referencial: ${rep.postId}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun StatBox(modifier: Modifier = Modifier, count: Int, label: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$count", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = InfoAnimalPrimary)
            Text(text = label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ServerConfigDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var urlInput by remember { mutableStateOf(com.example.data.network.PocketBaseClient.baseUrl) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Storage, contentDescription = null, tint = InfoAnimalPrimary) },
        title = { Text("Configuración del Servidor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Ingresa la URL de tu backend de PocketBase (configurado en Hostinger o Render).",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("URL del Servidor PocketBase") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("https://mi-pocketbase.com") }
                )
                Text(
                    text = "URL actual: ${com.example.data.network.PocketBaseClient.baseUrl}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Nota: Al registrarte, la app conectará a este servidor PocketBase. Si usas la URL de vista previa de AI Studio, el registro fallará (retornará un error HTML).",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (urlInput.isBlank() || !urlInput.startsWith("http")) {
                        Toast.makeText(context, "Por favor ingresa una URL válida (http/https)", Toast.LENGTH_SHORT).show()
                    } else {
                        com.example.data.network.PocketBaseClient.updateBaseUrl(context, urlInput.trim())
                        Toast.makeText(context, "Servidor actualizado con éxito", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalPrimary)
            ) {
                Text("Guardar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// ============================================
// 10. AUTHENTICATION MODULES (LOGIN / REGISTER / RECOVER)
// ============================================
@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onNavigateToSignup: () -> Unit,
    onNavigateToRecover: () -> Unit,
    onSuccessRedirect: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showServerConfig by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    if (showServerConfig) {
        ServerConfigDialog(onDismiss = { showServerConfig = false })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = { if (!isLoading) showServerConfig = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            enabled = !isLoading
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Configurar Servidor",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_app_logo),
                    contentDescription = "InfoAnimal & Al Recreo Logo",
                    modifier = Modifier
                        .height(60.dp)
                        .fillMaxWidth(0.9f),
                    contentScale = ContentScale.Fit
                )
                
                Text(
                    text = "Iniciar Sesión",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo Electrónico") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                // Password with visible toggle option
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(
                            onClick = { isPasswordVisible = !isPasswordVisible },
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    }
                )

                TextButton(
                    onClick = onNavigateToRecover,
                    enabled = !isLoading,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("¿Olvidaste tu contraseña?", color = InfoAnimalSecondary)
                }

                Button(
                    onClick = {
                        if (email.trim().isEmpty() || password.trim().isEmpty()) {
                            Toast.makeText(context, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        viewModel.login(email.trim(), password.trim(), {
                            isLoading = false
                            Toast.makeText(context, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                            onSuccessRedirect()
                        }, { err ->
                            isLoading = false
                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        })
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Entrar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("¿No tenés cuenta?")
                    TextButton(onClick = onNavigateToSignup, enabled = !isLoading) {
                        Text("Registrate", color = InfoAnimalPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SignupScreen(
    viewModel: MainViewModel,
    onNavigateToLogin: () -> Unit,
    onSuccessRedirect: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showServerConfig by remember { mutableStateOf(false) }

    if (showServerConfig) {
        ServerConfigDialog(onDismiss = { showServerConfig = false })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = { showServerConfig = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Configurar Servidor",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_logo),
                        contentDescription = "InfoAnimal & Al Recreo Logo",
                        modifier = Modifier
                            .height(60.dp)
                            .fillMaxWidth(0.9f),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Registrar Vecino",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre Completo *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo Electrónico *") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Teléfono de Contacto (Opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }

                item {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña (Min 8 letras) *") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation()
                    )
                }

                item {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirmar Contraseña *") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation()
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                                Toast.makeText(context, "Por favor, completa todos los campos obligatorios.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.signup(email, name, phone, password, confirmPassword, {
                                Toast.makeText(context, "¡Cuenta creada con éxito! Ya puedes iniciar sesión.", Toast.LENGTH_SHORT).show()
                                onSuccessRedirect()
                            }, { err ->
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Crear Cuenta", color = Color.White)
                    }
                }

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("¿Ya tenés cuenta?")
                        TextButton(onClick = onNavigateToLogin) {
                            Text("Entrá", color = InfoAnimalPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecoverPasswordScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Recuperar Contraseña",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Te enviaremos un correo con un link para restaurar tus credenciales vecinales.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo Electrónico") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Button(
                    onClick = {
                        if (email.isEmpty()) {
                            Toast.makeText(context, "Escribe tu email.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        viewModel.requestPasswordReset(email) { success ->
                            isLoading = false
                            if (success) {
                                Toast.makeText(context, "Instrucciones enviadas. Revisa tu casilla.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Se envió la solicitud de recuperación para tu email. Revisa tu casilla o spam.", Toast.LENGTH_LONG).show()
                            }
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text(if (isLoading) "Enviando..." else "Enviar Correo", color = Color.White)
                }

                TextButton(onClick = onNavigateBack) {
                    Text("Volver", color = InfoAnimalSecondary)
                }
            }
        }
    }
}

// ==========================================
// CLIENT MODULES: COMMUNITY & COLLABORATORS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComunidadScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    val statuses by viewModel.statuses.collectAsState()
    val followingUserIds by viewModel.followingUserIds.collectAsState()
    
    var selectedCommunityTab by remember { mutableIntStateOf(0) }
    var stateText by remember { mutableStateOf("") }
    val maxChars = if (user?.rol == "ong") 2000 else 500
    val charsLeft = maxChars - stateText.length
    
    val displayedStatuses = remember(statuses, followingUserIds, selectedCommunityTab) {
        if (selectedCommunityTab == 0) {
            statuses
        } else {
            statuses.filter { it.userId in followingUserIds }
        }
    }
    
    var showReportDialog by remember { mutableStateOf(false) }
    var reportTargetId by remember { mutableStateOf("") }
    var reportReason by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comunidad Vecinal", fontWeight = FontWeight.Bold, color = InfoAnimalPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Write status section (Only logged-in)
            if (user != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Actualizá tu estado a la comunidad:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = InfoAnimalPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = stateText,
                                onValueChange = { if (it.length <= maxChars) stateText = it },
                                placeholder = { Text("¿Qué está pasando con las mascotas en tu barrio?") },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                maxLines = 4
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$charsLeft caracteres restantes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (charsLeft < 50) InfoAnimalDestructive else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = {
                                        if (stateText.isBlank()) {
                                            Toast.makeText(context, "Escribí algo antes de publicar.", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        viewModel.postStatus(stateText) {
                                            stateText = ""
                                            Toast.makeText(context, "Estado publicado con éxito!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalPrimary),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Publicar", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = InfoAnimalSecondary.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "Iniciá sesión para publicar un estado y compartir novedades en tu zona.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Community Tabs: General / Seguidos
            item {
                TabRow(
                    selectedTabIndex = selectedCommunityTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = InfoAnimalPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedCommunityTab == 0,
                        onClick = { selectedCommunityTab = 0 },
                        text = {
                            Text(
                                "General",
                                fontWeight = if (selectedCommunityTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedCommunityTab == 0) InfoAnimalPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    Tab(
                        selected = selectedCommunityTab == 1,
                        onClick = { selectedCommunityTab = 1 },
                        text = {
                            Text(
                                "Seguidos",
                                fontWeight = if (selectedCommunityTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedCommunityTab == 1) InfoAnimalPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            if (displayedStatuses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedCommunityTab == 0) {
                                "No hay estados recientes. ¡Escribí el tuyo para comenzar!"
                            } else {
                                "No hay actualizaciones de personas que sigues. ¡Visitá el perfil de otros vecinos para seguirlos!"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                items(displayedStatuses) { status ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Status Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.clickable { onNavigateToUserProfile(status.userId) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(InfoAnimalSecondary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!status.userFotoPerfil.isNullOrBlank() && status.userFotoPerfil.startsWith("http")) {
                                            AsyncImage(
                                                model = status.userFotoPerfil,
                                                contentDescription = "Avatar de ${status.userNombreCompleto}",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                            )
                                        } else {
                                            Icon(imageVector = Icons.Default.Person, contentDescription = null, size = 18.dp, tint = InfoAnimalSecondary)
                                        }
                                    }
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = status.userNombreCompleto,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            // Handle role visual decorators
                                            if (status.userId == "usr_admin" || status.userId.contains("admin")) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(InfoAnimalDestructive.copy(alpha = 0.12f))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Staff", fontSize = 8.sp, color = InfoAnimalDestructive, fontWeight = FontWeight.Bold)
                                                }
                                            } else if (status.userId == "usr_ong" || status.userId.contains("ong")) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(InfoAnimalPrimary.copy(alpha = 0.12f))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text("ONG", fontSize = 8.sp, color = InfoAnimalPrimary, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Text(
                                            text = formatTimestamp(status.created),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                // Menu dropdown triggers
                                var menuExpanded by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { menuExpanded = true }) {
                                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Opciones")
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        /*
                                        // --- Red Social: Ver Perfil y Seguir Usuario (Comentado) ---
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                            text = { Text("Ver Perfil") },
                                            onClick = {
                                                menuExpanded = false
                                                onNavigateToUserProfile(status.userId)
                                            }
                                        )
                                        if (user != null && user!!.id != status.userId) {
                                            val isFollowing = followingUserIds.contains(status.userId)
                                            DropdownMenuItem(
                                                leadingIcon = { Icon(if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                                text = { Text(if (isFollowing) "Siguiendo (Dejar de seguir)" else "Seguir Usuario") },
                                                onClick = {
                                                    menuExpanded = false
                                                    if (isFollowing) {
                                                        viewModel.unfollowUser(status.userId) {
                                                            Toast.makeText(context, "Dejaste de seguir a ${status.userNombreCompleto}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } else {
                                                        viewModel.followUser(status.userId) {
                                                            Toast.makeText(context, "Ahora sigues a ${status.userNombreCompleto}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                        */
                                        if (user != null && user!!.id != status.userId) {
                                            DropdownMenuItem(
                                                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                                text = { Text("Bloquear Usuario") },
                                                onClick = {
                                                    menuExpanded = false
                                                    viewModel.blockUser(status.userId, status.userNombreCompleto, "") {
                                                        Toast.makeText(context, "Bloqueaste a ${status.userNombreCompleto}. No verás más sus publicaciones.", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            )
                                            DropdownMenuItem(
                                                leadingIcon = { Icon(Icons.Default.Report, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                                text = { Text("Reportar Estado") },
                                                onClick = {
                                                    menuExpanded = false
                                                    reportTargetId = status.id
                                                    showReportDialog = true
                                                }
                                            )
                                        }
                                        if (user != null && (user!!.id == status.userId || user!!.rol == "admin")) {
                                            DropdownMenuItem(
                                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                                text = { Text("Eliminar") },
                                                onClick = {
                                                    menuExpanded = false
                                                    viewModel.deleteStatus(status.id)
                                                    Toast.makeText(context, "Estado eliminado.", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Body
                            Text(
                                text = status.texto,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Custom action controls
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { viewModel.toggleStatusLike(status.id) }
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ThumbUp,
                                        contentDescription = "Me gusta",
                                        size = 14.dp,
                                        tint = InfoAnimalPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "${status.likesCount} me gustas",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = InfoAnimalPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Reportar Contenido", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Contanos por qué este estado infringe las normas de la comunidad InfoAnimals:")
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        placeholder = { Text("Ej: Contenido ofensivo, Spam, etc.") },
                        modifier = Modifier.fillMaxWidth().height(80.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (reportReason.isBlank()) {
                            Toast.makeText(context, "Ingresá una razón.", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        viewModel.reportProfile(reportTargetId, "Reporte Estado: $reportReason") {
                            showReportDialog = false
                            reportReason = ""
                            Toast.makeText(context, "Reporte enviado. Revisaremos el contenido a la brevedad.", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("Reportar", color = InfoAnimalDestructive, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloqueadosScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val blockedUsers by viewModel.blockedUsers.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Usuarios Bloqueados", fontWeight = FontWeight.Bold, color = InfoAnimalPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        if (blockedUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Block, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Text(
                        "No tenés usuarios bloqueados.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(blockedUsers) { block ->
                    Card(
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(InfoAnimalSecondary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = InfoAnimalSecondary, modifier = Modifier.size(20.dp))
                                }
                                Text(
                                    text = block.blockedUserNombre,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Button(
                                onClick = { viewModel.unblockUser(block.blockedUserId) },
                                colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalSecondary),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                            ) {
                                Text("Desbloquear", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
fun ColaboradoresScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    val colaboradores by viewModel.colaboradores.collectAsState()

    var showFormDialog by remember { mutableStateOf(false) }
    var editId by remember { mutableStateOf<String?>(null) }
    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var logo by remember { mutableStateOf("") }
    var enlace by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var instagram by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Directorio Colaboradores", fontWeight = FontWeight.Bold, color = InfoAnimalPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            if (user != null && (user!!.rol == "admin" || user!!.rol == "super_admin" || user!!.rol == "superadmin")) {
                FloatingActionButton(
                    onClick = {
                        editId = null
                        nombre = ""
                        descripcion = ""
                        logo = ""
                        enlace = ""
                        telefono = ""
                        whatsapp = ""
                        instagram = ""
                        direccion = ""
                        showFormDialog = true
                    },
                    containerColor = InfoAnimalPrimary,
                    contentColor = Color.White
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Nuevo Colaborador")
                }
            }
        }
    ) { paddingValues ->
        if (colaboradores.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aún no se registraron colaboradores. ¡Pronto verás marcas de apoyo local aquí!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                items(colaboradores) { col ->
                    Card(
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(InfoAnimalAccent.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Handshake, contentDescription = null, size = 24.dp, tint = InfoAnimalAccent)
                                    }
                                    Column {
                                        Text(text = col.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        if (col.direccion.isNotEmpty()) {
                                            Text(text = col.direccion, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                
                                // Admin dropdown controls
                                if (user != null && (user!!.rol == "admin" || user!!.rol == "super_admin" || user!!.rol == "superadmin")) {
                                    var controlExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        IconButton(onClick = { controlExpanded = true }) {
                                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Editar")
                                        }
                                        DropdownMenu(
                                            expanded = controlExpanded,
                                            onDismissRequest = { controlExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                                text = { Text("Editar") },
                                                onClick = {
                                                    controlExpanded = false
                                                    editId = col.id
                                                    nombre = col.nombre
                                                    descripcion = col.descripcion
                                                    logo = col.logo
                                                    enlace = col.enlace
                                                    telefono = col.telefono
                                                    whatsapp = col.whatsapp
                                                    instagram = col.instagram
                                                    direccion = col.direccion
                                                    showFormDialog = true
                                                }
                                            )
                                            DropdownMenuItem(
                                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = InfoAnimalDestructive) },
                                                text = { Text("Eliminar", color = InfoAnimalDestructive) },
                                                onClick = {
                                                    controlExpanded = false
                                                    viewModel.deleteColaborador(col.id, col.nombre)
                                                    Toast.makeText(context, "Colaborador eliminado.", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = col.descripcion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(12.dp))

                            // Public communication channels
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (col.enlace.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(col.enlace))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "No se pudo abrir el enlace.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = InfoAnimalSecondary.copy(alpha = 0.08f)),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Web, contentDescription = "Enlace Web", size = 18.dp, tint = InfoAnimalSecondary)
                                    }
                                }
                                if (col.whatsapp.isNotEmpty()) {
                                    IconButton(
                                        onClick = { triggerWhatsApp(context, col.whatsapp, "Hola! Me comunico para consultar los servicios compartidos con InfoAnimalsR.") },
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = InfoAnimalAccent.copy(alpha = 0.08f)),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Chat, contentDescription = "WhatsApp", size = 18.dp, tint = InfoAnimalAccent)
                                    }
                                }
                                if (col.telefono.isNotEmpty()) {
                                    IconButton(
                                        onClick = { triggerCall(context, col.telefono) },
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = InfoAnimalPrimary.copy(alpha = 0.08f)),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Phone, contentDescription = "Llamar", size = 18.dp, tint = InfoAnimalPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        AlertDialog(
            onDismissRequest = { showFormDialog = false },
            title = { Text(if (editId == null) "Nuevo Colaborador" else "Editar Colaborador", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre de la marca") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = descripcion, onValueChange = { descripcion = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                    OutlinedTextField(value = direccion, onValueChange = { direccion = it }, label = { Text("Dirección") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = logo, onValueChange = { logo = it }, label = { Text("Logo URL") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = enlace, onValueChange = { enlace = it }, label = { Text("Enlace Web / Redes") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    OutlinedTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = { Text("WhatsApp") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nombre.isBlank() || descripcion.isBlank()) {
                            Toast.makeText(context, "Completá nombre y descripción.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.saveColaborador(editId, nombre, descripcion, logo, enlace, telefono, whatsapp, instagram, direccion) {
                            showFormDialog = false
                            Toast.makeText(context, "Guardado con éxito!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = InfoAnimalPrimary)
                ) {
                    Text("Guardar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFormDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val notifications by viewModel.getNotifications().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones", fontWeight = FontWeight.Bold, color = InfoAnimalPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (notifications.any { !it.leida }) {
                        TextButton(onClick = {
                            viewModel.markNotificationsRead()
                            Toast.makeText(context, "Notificaciones leídas", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Leídas", color = InfoAnimalSecondary, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.NotificationsNone, contentDescription = null, size = 48.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Text(
                        text = "No tenés notificaciones aún.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(notifications) { notif ->
                    val containerBg = if (notif.leida) MaterialTheme.colorScheme.surface else InfoAnimalPrimary.copy(alpha = 0.04f)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(containerBg)
                            .clickable {
                                if (!notif.leida) {
                                    viewModel.markNotificationRead(notif)
                                }
                            }
                            .border(BorderStroke(0.3.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Decorative Notification type icon
                        val iconVector = when (notif.tipo) {
                            "comment" -> Icons.Default.Chat
                            "like" -> Icons.Default.ThumbUp
                            "status_like" -> Icons.Default.Favorite
                            else -> Icons.Default.Notifications
                        }
                        val tintColor = when (notif.tipo) {
                            "comment" -> InfoAnimalSecondary
                            "like" -> InfoAnimalPrimary
                            "status_like" -> InfoAnimalDestructive
                            else -> InfoAnimalAccent
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(tintColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = iconVector, contentDescription = null, size = 18.dp, tint = tintColor)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = notif.mensaje,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (notif.leida) FontWeight.Normal else FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatTimestamp(notif.created),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Notification unread dot
                        if (!notif.leida) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(InfoAnimalPrimary)
                            )
                        }
                    }
                }
            }
        }
    }
}

