package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.PostEntity
import com.example.ui.theme.InfoAnimalAccent
import com.example.ui.theme.InfoAnimalDestructive
import com.example.ui.theme.InfoAnimalPrimary
import com.example.ui.theme.InfoAnimalSecondary

@Composable
fun StatusBadge(
    tipo: String,
    estado: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, text) = when {
        estado.equals("resuelta", ignoreCase = true) || estado.equals("resuelto", ignoreCase = true) || estado.equals("encontrado", ignoreCase = true) -> Triple(
            Color(0xFFDCFCE7),
            Color(0xFF15803D),
            "¡RESUELTO! ✅"
        )
        tipo == "perdida" -> Triple(
            InfoAnimalDestructive.copy(alpha = 0.15f),
            InfoAnimalDestructive,
            "Perdido 🔴"
        )
        tipo == "encontrada" -> Triple(
            InfoAnimalAccent.copy(alpha = 0.15f),
            InfoAnimalAccent,
            "Encontrado 🟢"
        )
        tipo == "avistamiento" -> Triple(
            InfoAnimalPrimary.copy(alpha = 0.15f),
            InfoAnimalPrimary,
            "Avistado 🟡"
        )
        tipo == "adopcion" || tipo == "adopción" -> Triple(
            Color(0xFF8B5CF6).copy(alpha = 0.15f),
            Color(0xFF8B5CF6),
            "En Adopción 🟣"
        )
        else -> Triple(
            InfoAnimalSecondary.copy(alpha = 0.15f),
            InfoAnimalSecondary,
            "En Tránsito 🔵"
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun OngBadge(
    size: String = "sm",
    modifier: Modifier = Modifier
) {
    val paddingHorizontal = if (size == "lg") 12.dp else 8.dp
    val paddingVertical = if (size == "lg") 6.dp else 3.dp
    val fontSize = if (size == "lg") 12.sp else 10.sp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(InfoAnimalAccent.copy(alpha = 0.15f))
            .border(1.dp, InfoAnimalAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = paddingHorizontal, vertical = paddingVertical),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Verificado",
                tint = InfoAnimalAccent,
                modifier = Modifier.size(if (size == "lg") 16.dp else 12.dp)
            )
            Text(
                text = "ONG Verificada",
                color = InfoAnimalAccent,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapComponent(
    posts: List<PostEntity>,
    centerLat: Double = -34.6150,
    centerLng: Double = -68.3300,
    showRadius: Boolean = false,
    radiusMeters: Double = 200.0,
    modifier: Modifier = Modifier,
    onPostClick: (String) -> Unit = {}
) {
    val cleanCenterLat = if (centerLat == 0.0) -34.6150 else centerLat
    val cleanCenterLng = if (centerLng == 0.0) -68.3300 else centerLng

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = WebViewClient()
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onMarkerClick(postId: String) {
                                post { onPostClick(postId) }
                            }
                        }, "AndroidMapInterface")

                        // Generate leaflet HTML page
                        val markersJS = posts.filter { it.ubicacionLat != 0.0 && it.ubicacionLng != 0.0 }
                            .joinToString("\n") { post ->
                                val popupText = "<b>${if (post.nombreMascota.isEmpty()) "Sin nombre" else post.nombreMascota}</b><br/>${post.tipo.substring(0, 1).uppercase() + post.tipo.substring(1)} - ${post.distrito}"
                                """
                                L.marker([${post.ubicacionLat}, ${post.ubicacionLng}])
                                 .addTo(map)
                                 .bindPopup("$popupText")
                                 .on('click', function() {
                                     AndroidMapInterface.onMarkerClick("${post.id}");
                                 });
                                """.trimIndent()
                            }

                        val radiusJS = if (showRadius) {
                            """
                            L.circle([$cleanCenterLat, $cleanCenterLng], {
                                color: '#4C87F2',
                                fillColor: '#4C87F2',
                                fillOpacity: 0.15,
                                radius: $radiusMeters
                            }).addTo(map);
                            """.trimIndent()
                        } else ""

                        val html = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.7.1/dist/leaflet.css" />
                                <script src="https://unpkg.com/leaflet@1.7.1/dist/leaflet.js"></script>
                                <style>
                                    body { margin: 0; padding: 0; }
                                    #map { width: 100vw; height: 100vh; }
                                </style>
                            </head>
                            <body>
                                <div id="map"></div>
                                <script>
                                    var map = L.map('map', { zoomControl: false }).setView([$cleanCenterLat, $cleanCenterLng], 14);
                                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                        maxZoom: 19
                                    }).addTo(map);
                                    L.control.zoom({ position: 'bottomright' }).addTo(map);
                                    
                                    // Add specific items
                                    L.marker([$cleanCenterLat, $cleanCenterLng], {
                                        icon: L.divIcon({
                                            className: 'custom-div-icon',
                                            html: "<div style='background-color:#FF9100; width:12px; height:12px; border-radius:50%; border:2px solid white; box-shadow: 0 0 4px rgba(0,0,0,0.5);'></div>",
                                            iconSize: [12, 12],
                                            iconAnchor: [6, 6]
                                        })
                                    }).addTo(map).bindPopup("Ubicación referencial");

                                    $markersJS
                                    $radiusJS
                                </script>
                            </body>
                            </html>
                        """.trimIndent()

                        loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "UTF-8", null)
                    }
                }
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LocationPicker(
    centerLat: Double = -34.6150,
    centerLng: Double = -68.3300,
    modifier: Modifier = Modifier,
    onLocationChange: (Double, Double) -> Unit
) {
    val cleanCenterLat = if (centerLat == 0.0) -34.6150 else centerLat
    val cleanCenterLng = if (centerLng == 0.0) -68.3300 else centerLng

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = WebViewClient()
                            addJavascriptInterface(object {
                                @JavascriptInterface
                                fun onLocationPicked(lat: Double, lng: Double) {
                                    post { onLocationChange(lat, lng) }
                                }
                            }, "AndroidLocationPickerInterface")

                            val html = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                                    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.7.1/dist/leaflet.css" />
                                    <script src="https://unpkg.com/leaflet@1.7.1/dist/leaflet.js"></script>
                                    <style>
                                        body { margin: 0; padding: 0; }
                                        #map { width: 100vw; height: 100vh; }
                                    </style>
                                </head>
                                <body>
                                    <div id="map"></div>
                                    <script>
                                        var map = L.map('map', { zoomControl: false }).setView([$cleanCenterLat, $cleanCenterLng], 14);
                                        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                            maxZoom: 19
                                        }).addTo(map);
                                        
                                        var marker = L.marker([$cleanCenterLat, $cleanCenterLng], {draggable: true}).addTo(map);
                                        
                                        function updateLatLng(lat, lng) {
                                            AndroidLocationPickerInterface.onLocationPicked(lat, lng);
                                        }

                                        marker.on('dragend', function (e) {
                                            var coords = e.target.getLatLng();
                                            updateLatLng(coords.lat, coords.lng);
                                        });

                                        map.on('click', function(e) {
                                            marker.setLatLng(e.latlng);
                                            updateLatLng(e.latlng.lat, e.latlng.lng);
                                        });
                                    </script>
                                </body>
                                </html>
                            """.trimIndent()

                            loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "UTF-8", null)
                        }
                    }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PinDrop,
                    contentDescription = "Ubicación",
                    tint = InfoAnimalPrimary
                )
                Text(
                    text = "Hacé click en el mapa o arrastrá el pin para marcar la ubicación exacta de la mascota.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
