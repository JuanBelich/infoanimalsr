package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Helper date formatter
fun formatTimestamp(time: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(time))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: MainViewModel,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToCreatePost: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val posts by viewModel.filteredPosts.collectAsState()
    val selectedDistrito by viewModel.selectedDistrito.collectAsState()
    val selectedTipo by viewModel.selectedTipo.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val distritos = listOf("Todos", "San Rafael", "General Alvear", "Malargüe")
    val tipos = listOf(
        "Todos" to "Todos",
        "perdida" to "Perdidos",
        "encontrada" to "Encontrados",
        "avistamiento" to "Avistamientos",
        "en_transito" to "En Tránsito"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Pets,
                            contentDescription = "Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "InfoAnimal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshAll() },
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentUser != null) {
                FloatingActionButton(
                    onClick = onNavigateToCreatePost,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("create_post_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Crear Reporte")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Buscador
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Buscar por mascota, zona, descripción...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Filtro Distritos
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(distritos) { distrito ->
                    FilterChip(
                        selected = selectedDistrito == distrito,
                        onClick = { viewModel.setDistrito(distrito) },
                        label = { Text(distrito) },
                        leadingIcon = if (selectedDistrito == distrito) {
                            { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // Filtro Tipo
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tipos) { (key, label) ->
                    FilterChip(
                        selected = selectedTipo == key,
                        onClick = { viewModel.setTipo(key) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lista de Publicaciones
            if (posts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Pets,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No hay publicaciones disponibles con los filtros actuales.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            onClick = { onNavigateToPostDetail(post.id) },
                            onUserClick = { onNavigateToProfile(post.userId) },
                            onLikeClick = { viewModel.toggleLikePost(post.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostCard(
    post: PostEntity,
    onClick: () -> Unit,
    onUserClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    val tipoColor = when (post.tipo.lowercase()) {
        "perdida" -> StatusLost
        "encontrada" -> StatusFound
        "en_transito" -> StatusTransit
        "avistamiento" -> StatusSighting
        else -> MaterialTheme.colorScheme.primary
    }

    val tipoLabel = when (post.tipo.lowercase()) {
        "perdida" -> "Perdido"
        "encontrada" -> "Encontrado"
        "en_transito" -> "En Tránsito"
        "avistamiento" -> "Avistamiento"
        else -> post.tipo.replaceFirstChar { it.uppercase() }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Usuario y Tipo Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onUserClick() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.userNombreCompleto.take(1).ifBlank { "U" },
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = post.userNombreCompleto.ifBlank { "Vecino" },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${post.distrito} • ${formatTimestamp(post.created)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = tipoColor.copy(alpha = 0.15f),
                    contentColor = tipoColor
                ) {
                    Text(
                        text = tipoLabel,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Fotos (si tiene)
            val fotos = post.fotosJson.split(",").filter { it.isNotBlank() }
            if (fotos.isNotEmpty()) {
                AsyncImage(
                    model = fotos.first(),
                    contentDescription = "Foto de ${post.nombreMascota}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // Contenido
            Column(modifier = Modifier.padding(12.dp)) {
                if (post.nombreMascota.isNotBlank()) {
                    Text(
                        text = "Mascota: ${post.nombreMascota}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = post.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                if (post.rasgos.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Rasgos: ${post.rasgos}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Footer: Zona, Likes, etc.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (post.zona.isNotBlank()) "${post.distrito} (${post.zona})" else post.distrito,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onLikeClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "Me gusta",
                                tint = if (post.likesCount > 0) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "${post.likesCount}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val posts by viewModel.posts.collectAsState()
    val post = posts.find { it.id == postId }
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current

    var comments by remember { mutableStateOf<List<CommentEntity>>(emptyList()) }
    var newCommentText by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(postId) {
        comments = viewModel.repository.getCommentsForPost(postId)
    }

    if (post == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Detalle") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Publicación no encontrada.")
            }
        }
        return
    }

    val isAuthor = currentUser?.id == post.userId
    val isAdmin = currentUser?.rol == "admin" || currentUser?.rol == "super_admin"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reporte de Mascota") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (isAuthor || isAdmin) {
                        IconButton(
                            onClick = {
                                viewModel.deletePost(post.id) { success ->
                                    if (success) {
                                        Toast.makeText(context, "Publicación eliminada", Toast.LENGTH_SHORT).show()
                                        onNavigateBack()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (currentUser != null && !isAuthor) {
                        IconButton(onClick = { showReportDialog = true }) {
                            Icon(Icons.Default.Report, contentDescription = "Reportar")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Usuario Autor
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToProfile(post.userId) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.userNombreCompleto.take(1).ifBlank { "U" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = post.userNombreCompleto.ifBlank { "Vecino" },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Publicado el ${formatTimestamp(post.created)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Fotos
            val fotos = post.fotosJson.split(",").filter { it.isNotBlank() }
            if (fotos.isNotEmpty()) {
                item {
                    AsyncImage(
                        model = fotos.first(),
                        contentDescription = "Foto de ${post.nombreMascota}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Datos principales
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (post.nombreMascota.isNotBlank()) post.nombreMascota else "Mascota sin nombre",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tipo: ${post.tipo.uppercase()} • Especie: ${post.especie.uppercase()} • Tamaño: ${post.tamano}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = post.descripcion,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (post.rasgos.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Rasgos particulares: ${post.rasgos}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ubicación: ${post.distrito} ${if (post.zona.isNotBlank()) "(${post.zona})" else ""}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Botón de Llamada / Contacto
            if (post.telefonoVisible && post.telefonoContacto.isNotBlank()) {
                item {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${post.telefonoContacto}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Llamar al ${post.telefonoContacto}")
                    }
                }
            }

            // Sección de Comentarios
            item {
                Text(
                    text = "Comentarios (${comments.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Input para nuevo comentario
            if (currentUser != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCommentText,
                            onValueChange = { newCommentText = it },
                            placeholder = { Text("Escribe un comentario...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newCommentText.isNotBlank()) {
                                    val txt = newCommentText
                                    newCommentText = ""
                                    viewModel.addComment(post.id, txt) { success ->
                                        if (success) {
                                            coroutineScope.launch {
                                                comments = viewModel.repository.getCommentsForPost(post.id)
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = newCommentText.isNotBlank()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            items(comments) { comment ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = comment.userNombreCompleto.ifBlank { "Vecino" },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (currentUser?.id == comment.userId || isAdmin) {
                                IconButton(
                                    onClick = {
                                        viewModel.deleteComment(comment.id) {
                                            coroutineScope.launch {
                                                comments = viewModel.repository.getCommentsForPost(post.id)
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Eliminar", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = comment.texto, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Reportar Publicación") },
            text = {
                Column {
                    Text("Indica el motivo del reporte:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        placeholder = { Text("Spam, contenido inapropiado, etc.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reportReason.isNotBlank()) {
                            viewModel.reportPost(post.id, reportReason) {
                                Toast.makeText(context, "Reporte enviado", Toast.LENGTH_SHORT).show()
                                showReportDialog = false
                                reportReason = ""
                            }
                        }
                    }
                ) {
                    Text("Enviar")
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
fun CreateEditPostScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current

    var tipo by remember { mutableStateOf("perdida") }
    var especie by remember { mutableStateOf("perro") }
    var tamano by remember { mutableStateOf("mediano") }
    var nombreMascota by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var rasgos by remember { mutableStateOf("") }
    var distrito by remember { mutableStateOf("San Rafael") }
    var zona by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf(currentUser?.telefono ?: "") }
    var telefonoVisible by remember { mutableStateOf(true) }
    var fotoUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val distritos = listOf("San Rafael", "General Alvear", "Malargüe")
    val tipos = listOf("perdida" to "Perdida", "encontrada" to "Encontrada", "avistamiento" to "Avistamiento", "en_transito" to "En Tránsito")
    val especies = listOf("perro" to "Perro", "gato" to "Gato", "otros" to "Otros")
    val tamanos = listOf("pequeño" to "Pequeño", "mediano" to "Mediano", "grande" to "Grande")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Reporte") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Tipo de Reporte", fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tipos) { (key, label) ->
                    FilterChip(
                        selected = tipo == key,
                        onClick = { tipo = key },
                        label = { Text(label) }
                    )
                }
            }

            Text("Especie", fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(especies) { (key, label) ->
                    FilterChip(
                        selected = especie == key,
                        onClick = { especie = key },
                        label = { Text(label) }
                    )
                }
            }

            Text("Tamaño", fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tamanos) { (key, label) ->
                    FilterChip(
                        selected = tamano == key,
                        onClick = { tamano = key },
                        label = { Text(label) }
                    )
                }
            }

            OutlinedTextField(
                value = nombreMascota,
                onValueChange = { nombreMascota = it },
                label = { Text("Nombre de la mascota (si se conoce)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción *") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = rasgos,
                onValueChange = { rasgos = it },
                label = { Text("Rasgos particulares (color de collar, manchas, etc.)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Distrito", fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(distritos) { d ->
                    FilterChip(
                        selected = distrito == d,
                        onClick = { distrito = d },
                        label = { Text(d) }
                    )
                }
            }

            OutlinedTextField(
                value = zona,
                onValueChange = { zona = it },
                label = { Text("Barrio o Zona (ej. Barrio Unimev, Centro)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = telefono,
                onValueChange = { telefono = it },
                label = { Text("Teléfono de contacto") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = telefonoVisible, onCheckedChange = { telefonoVisible = it })
                Spacer(modifier = Modifier.width(8.dp))
                Text("Hacer visible mi teléfono para llamadas directas")
            }

            OutlinedTextField(
                value = fotoUrl,
                onValueChange = { fotoUrl = it },
                label = { Text("URL de Foto (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (descripcion.isBlank()) {
                        Toast.makeText(context, "Por favor describe a la mascota", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    viewModel.createPost(
                        tipo = tipo,
                        especie = especie,
                        tamano = tamano,
                        nombre = nombreMascota,
                        desc = descripcion,
                        rasgos = rasgos,
                        distrito = distrito,
                        zona = zona,
                        telefono = telefono,
                        visible = telefonoVisible,
                        fotos = fotoUrl,
                        lat = -34.6177,
                        lng = -68.3301
                    ) { success ->
                        isLoading = false
                        if (success) {
                            Toast.makeText(context, "Publicación creada con éxito", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        } else {
                            Toast.makeText(context, "Error al crear la publicación", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Publicar Reporte")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeterinariasScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val veterinarias by viewModel.veterinarias.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("Todas") }

    val filteredVets = when (selectedFilter) {
        "24hs" -> veterinarias.filter { it.clasificacion.contains("24", ignoreCase = true) }
        "Comercio" -> veterinarias.filter { !it.clasificacion.contains("24", ignoreCase = true) }
        else -> veterinarias
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Veterinarias y Guardias") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedFilter == "Todas",
                    onClick = { selectedFilter = "Todas" },
                    label = { Text("Todas") }
                )
                FilterChip(
                    selected = selectedFilter == "24hs",
                    onClick = { selectedFilter = "24hs" },
                    label = { Text("Guardia 24hs") }
                )
                FilterChip(
                    selected = selectedFilter == "Comercio",
                    onClick = { selectedFilter = "Comercio" },
                    label = { Text("Horario Comercio") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredVets, key = { it.id }) { vet ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = vet.nombre,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (vet.clasificacion.contains("24", ignoreCase = true)) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = vet.clasificacion,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = if (vet.clasificacion.contains("24", ignoreCase = true)) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "📍 ${vet.direccion} (${vet.distrito})", style = MaterialTheme.typography.bodyMedium)
                            if (vet.horarios.isNotBlank()) {
                                Text(text = "⏰ ${vet.horarios}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }
                            if (vet.staff.isNotBlank()) {
                                Text(text = "👨‍⚕️ ${vet.staff}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${vet.telefono}"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Llamar (${vet.telefono})")
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
fun CampaignsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val campaigns by viewModel.campaigns.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campañas Solidarias") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(campaigns, key = { it.id }) { campaign ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = campaign.titulo,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Organiza: ${campaign.ongNombre}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = campaign.descripcion, style = MaterialTheme.typography.bodyMedium)
                        if (campaign.contenidoDetallado.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = campaign.contenidoDetallado,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Vigencia: ${campaign.fechaInicio} al ${campaign.fechaFin}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            IconButton(onClick = { viewModel.toggleLikeCampaign(campaign.id) }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.Red)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${campaign.likesCount}")
                                }
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
fun NewsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val news by viewModel.news.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Noticias y Consejos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(news, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = item.titular,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${item.autorNombre} • ${formatTimestamp(item.created)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = item.contenido, style = MaterialTheme.typography.bodyMedium)
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
    val colaboradores by viewModel.colaboradores.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Refugios y Colaboradores") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(colaboradores, key = { it.id }) { colab ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = colab.nombre,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = colab.descripcion, style = MaterialTheme.typography.bodyMedium)
                        if (colab.direccion.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "📍 ${colab.direccion}", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (colab.telefono.isNotBlank()) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${colab.telefono}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Llamar")
                                }
                            }
                            if (colab.enlace.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(colab.enlace))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Instagram / Web")
                                }
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
fun PerfilScreen(
    userId: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAuth: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isMyProfile = currentUser?.id == userId
    val context = LocalContext.current
    var showEditProfile by remember { mutableStateOf(false) }

    var editNombre by remember { mutableStateOf(currentUser?.nombre ?: "") }
    var editTelefono by remember { mutableStateOf(currentUser?.telefono ?: "") }
    var editZona by remember { mutableStateOf(currentUser?.zonaPrincipal ?: "San Rafael") }
    var editBio by remember { mutableStateOf(currentUser?.biografia ?: "") }

    val name = if (isMyProfile) currentUser?.nombre ?: "Usuario" else "Vecino Colaborador"
    val email = if (isMyProfile) currentUser?.email ?: "" else "Email protegido"
    val rol = if (isMyProfile) currentUser?.rol ?: "user" else "user"
    val zona = if (isMyProfile) currentUser?.zonaPrincipal ?: "San Rafael" else "Sur de Mendoza"
    val bio = if (isMyProfile) currentUser?.biografia ?: "" else ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil de Usuario") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (isMyProfile && currentUser != null) {
                        IconButton(onClick = { showEditProfile = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                        IconButton(
                            onClick = {
                                viewModel.logout()
                                onNavigateToAuth()
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                contentColor = MaterialTheme.colorScheme.secondary
            ) {
                Text(
                    text = "Rol: ${rol.uppercase()}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "📍 Zona Principal: $zona", style = MaterialTheme.typography.bodyLarge)
                    if (bio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "📝 Biografía:\n$bio", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (!isMyProfile) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.reportProfile(userId, "Reporte de perfil") {
                            Toast.makeText(context, "Usuario reportado a moderación", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Report, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reportar Usuario")
                }
            }
        }
    }

    if (showEditProfile) {
        AlertDialog(
            onDismissRequest = { showEditProfile = false },
            title = { Text("Editar Perfil") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editNombre,
                        onValueChange = { editNombre = it },
                        label = { Text("Nombre Completo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editTelefono,
                        onValueChange = { editTelefono = it },
                        label = { Text("Teléfono") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editZona,
                        onValueChange = { editZona = it },
                        label = { Text("Zona Principal") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Biografía") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfile(
                            nombre = editNombre,
                            telefono = editTelefono,
                            zona = editZona,
                            bio = editBio,
                            foto = "",
                            portada = ""
                        ) {
                            Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                            showEditProfile = false
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfile = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: MainViewModel,
    onAuthSuccess: () -> Unit,
    onContinueAsGuest: () -> Unit
) {
    var isRegister by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Pets,
                    contentDescription = "Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )

                Text(
                    text = if (isRegister) "Crear Cuenta en InfoAnimal" else "Bienvenido a InfoAnimal",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Comunidad solidaria para mascotas perdidas y encontradas en Mendoza",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isRegister) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre y Apellido") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Teléfono (para llamadas)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo Electrónico") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Por favor completa todos los campos"
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        if (isRegister) {
                            viewModel.signup(email, name, phone, password, {
                                isLoading = false
                                onAuthSuccess()
                            }, { err ->
                                isLoading = false
                                errorMessage = err
                            })
                        } else {
                            viewModel.login(email, password, {
                                isLoading = false
                                onAuthSuccess()
                            }, { err ->
                                isLoading = false
                                errorMessage = err
                            })
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text(if (isRegister) "Registrarse" else "Iniciar Sesión")
                    }
                }

                TextButton(onClick = { isRegister = !isRegister }) {
                    Text(if (isRegister) "¿Ya tienes cuenta? Inicia sesión" else "¿No tienes cuenta? Regístrate aquí")
                }

                Divider()

                OutlinedButton(
                    onClick = onContinueAsGuest,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Continuar como Invitado")
                }
            }
        }
    }
}
