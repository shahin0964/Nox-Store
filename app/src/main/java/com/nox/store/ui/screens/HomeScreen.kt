package com.nox.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nox.store.data.model.AppItem
import com.nox.store.data.model.DownloadState
import com.nox.store.data.model.StoreNotice
import com.nox.store.ui.components.AppCardGridItem
import com.nox.store.ui.components.AppCardRow
import com.nox.store.ui.components.CategoryChips
import com.nox.store.ui.components.HeroBanner
import com.nox.store.ui.components.SectionHeader
import com.nox.store.ui.components.StoreSearchBar
import com.nox.store.ui.theme.NoxUpdateBlue

@Composable
fun HomeScreen(
    apps: List<AppItem>,
    downloadStates: Map<String, DownloadState>,
    onAppClick: (AppItem) -> Unit,
    onAppAction: (AppItem) -> Unit,
    onNavigateToUpdates: () -> Unit,
    onProfileClick: () -> Unit = {},
    adminNotice: StoreNotice? = null,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Communication", "Productivity", "Personalization", "Development", "Tools")

    val updatesCount = apps.count { it.hasUpdate }

    val filteredApps = apps.filter { app ->
        val matchesCategory = selectedCategory == "All" || app.category.equals(selectedCategory, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
                app.name.contains(searchQuery, ignoreCase = true) ||
                app.summary.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true) ||
                app.developer.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    val featuredApp = apps.firstOrNull { it.isFeatured } ?: apps.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // Top Search Bar (Pinned / Still at top)
        StoreSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onProfileClick = onProfileClick
        )

        // Category Filter Chips (Pinned / Still at top)
        CategoryChips(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )

        // Scrollable content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Broadcast Notice Banner from Admin Panel
            if (adminNotice != null && adminNotice.message.isNotBlank() && searchQuery.isBlank()) {
                item {
                    val (emoji, accentColor, gradientColors) = when (adminNotice.type.uppercase()) {
                        "WARNING" -> Triple(
                            "⚠️",
                            Color(0xFFFFB300),
                            listOf(Color(0xFF472C00), Color(0xFF281900))
                        )
                        "UPDATE" -> Triple(
                            "🚀",
                            Color(0xFF00E5FF),
                            listOf(Color(0xFF0E344D), Color(0xFF102033))
                        )
                        else -> Triple(
                            "📢",
                            colorScheme.primary,
                            listOf(Color(0xFF2E1A5E), Color(0xFF1B163B))
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(gradientColors))
                            .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = emoji, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = adminNotice.title.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = adminNotice.message,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Updates Available Callout Banner
            if (updatesCount > 0 && searchQuery.isBlank()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(NoxUpdateBlue.copy(alpha = 0.14f))
                            .border(1.dp, NoxUpdateBlue.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .clickable { onNavigateToUpdates() }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = NoxUpdateBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$updatesCount Updates Available",
                                    color = colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap to update your apps to latest versions",
                                    color = NoxUpdateBlue,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Hero Showcase Banner (when not searching)
            if (searchQuery.isBlank() && selectedCategory == "All" && featuredApp != null) {
                item {
                    Box(modifier = Modifier.padding(16.dp)) {
                        HeroBanner(
                            app = featuredApp,
                            onClick = { onAppClick(featuredApp) },
                            onActionClick = { onAppAction(featuredApp) }
                        )
                    }
                }
            }

            // Featured Apps Horizontal Row (when not searching)
            if (searchQuery.isBlank() && selectedCategory == "All") {
                item {
                    SectionHeader(title = "Featured Nox Apps")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(apps.take(6)) { app ->
                            AppCardGridItem(
                                app = app,
                                onClick = { onAppClick(app) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // Main App List Section
            item {
                val title = if (searchQuery.isNotBlank()) {
                    "Search Results (${filteredApps.size})"
                } else if (selectedCategory != "All") {
                    "$selectedCategory Apps"
                } else {
                    "Top Charts & Tools"
                }
                SectionHeader(title = title)
            }

            if (filteredApps.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No apps found matching '$searchQuery'",
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(filteredApps) { app ->
                    val state = downloadStates[app.packageName] ?: DownloadState.Idle
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        AppCardRow(
                            app = app,
                            downloadState = state,
                            onClick = { onAppClick(app) },
                            onActionClick = { onAppAction(app) }
                        )
                    }
                }
            }
        }
    }
}
