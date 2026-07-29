package cn.anitabi.navigator.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.anitabi.navigator.core.model.Anime
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.data.repository.PilgrimageWarning
import cn.anitabi.navigator.ui.theme.Ink
import cn.anitabi.navigator.ui.theme.Moss
import cn.anitabi.navigator.ui.theme.MutedInk
import cn.anitabi.navigator.ui.theme.Paper
import cn.anitabi.navigator.ui.theme.Sand
import cn.anitabi.navigator.ui.theme.Vermilion
import cn.anitabi.navigator.ui.planner.PlannerRoute
import cn.anitabi.navigator.ui.planner.PlannerViewModel
import cn.anitabi.navigator.navigation.NavigationViewModel
import cn.anitabi.navigator.ui.navigation.NavigationRoute
import cn.anitabi.navigator.ui.about.AboutScreen
import coil3.compose.AsyncImage

@Composable
fun SearchRoute(
    viewModel: SearchViewModel,
    plannerViewModel: PlannerViewModel,
    navigationViewModel: NavigationViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigationState by navigationViewModel.state.collectAsStateWithLifecycle()
    val navigationPlanId = navigationState.plan?.id
    val showNavigation = state.navigationOpen ||
        (navigationState.isRunning && navigationPlanId != state.hiddenNavigationTourId)
    BackHandler(
        enabled = !showNavigation && (state.aboutOpen || state.selectionOpen),
        onBack = when {
            state.aboutOpen -> viewModel::closeAbout
            state.plannerOpen -> viewModel::closePlanner
            else -> viewModel::backToResults
        },
    )

    if (showNavigation) {
        NavigationRoute(viewModel = navigationViewModel, onBack = viewModel::closeNavigation)
    } else if (state.aboutOpen) {
        AboutScreen(onBack = viewModel::closeAbout)
    } else if (state.plannerOpen) {
        PlannerRoute(
            viewModel = plannerViewModel,
            onBack = viewModel::closePlanner,
            onStartNavigation = { plan ->
                navigationViewModel.start(plan)
                viewModel.openNavigation()
            },
        )
    } else if (!state.selectionOpen) {
        SearchScreen(
            state = state,
            onQueryChange = viewModel::updateQuery,
            onSearch = viewModel::search,
            onAnimeToggle = viewModel::toggleAnime,
            onOpenSelection = viewModel::openSelection,
            onOpenAbout = viewModel::openAbout,
        )
    } else {
        PilgrimageSelectionScreen(
            state = state,
            onBack = viewModel::backToResults,
            onTogglePoint = viewModel::togglePoint,
            onBoundsChanged = viewModel::updateVisibleBounds,
            onSelectVisible = viewModel::selectVisiblePoints,
            onClearSelection = viewModel::clearSelection,
            onShowList = viewModel::setShowList,
            onPlan = {
                state.combinedPilgrimageData?.let { data ->
                    val points = data.points.filter { it.id in state.selectedPointIds }
                    plannerViewModel.configure(data.anime, points)
                    viewModel.openPlanner()
                }
            },
        )
    }
}

@Composable
private fun SearchScreen(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onAnimeToggle: (Anime) -> Unit,
    onOpenSelection: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchHero()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("动漫名称") },
                    placeholder = { Text("例如：吹响吧！上低音号") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "清空")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Vermilion,
                        focusedLabelColor = Vermilion,
                        unfocusedContainerColor = Color.White.copy(alpha = 0.64f),
                        focusedContainerColor = Color.White.copy(alpha = 0.84f),
                    ),
                )
                Button(
                    onClick = onSearch,
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink),
                ) {
                    Text("搜索 Bangumi")
                }
                TextButton(
                    onClick = onOpenAbout,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("关于、隐私与数据来源", modifier = Modifier.padding(start = 6.dp))
                }
                StatusMessage(state.errorMessage)
            }

            if (state.selectedAnimes.isNotEmpty()) {
                SelectedAnimeStrip(
                    selectedAnimes = state.selectedAnimes,
                    onAnimeToggle = onAnimeToggle,
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> LoadingState("正在翻阅作品目录…")
                    state.searchResults.isEmpty() -> EmptySearchState(hasQuery = state.query.isNotBlank())
                    else -> AnimeResults(
                        results = state.searchResults,
                        selectedAnimeIds = state.selectedAnimeData.keys,
                        loadingAnimeIds = state.loadingAnimeIds,
                        onAnimeToggle = onAnimeToggle,
                    )
                }
            }
            if (state.selectedAnimes.isNotEmpty()) {
                AnimeSelectionFooter(
                    animeCount = state.selectedAnimes.size,
                    pointCount = state.combinedPilgrimageData?.points?.size.orZero(),
                    onOpenSelection = onOpenSelection,
                )
            }
        }
    }
}

@Composable
private fun SearchHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ink)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Column {
            Text(
                text = "巡礼手帖",
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                text = "从一帧画面，走进真实街巷。",
                color = Sand,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 16.dp),
            ) {
                HeroTag("搜索")
                HeroTag("选点")
                HeroTag("一程到底")
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(58.dp)
                .clip(RoundedCornerShape(50))
                .background(Vermilion),
            contentAlignment = Alignment.Center,
        ) {
            Text("巡", color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun HeroTag(text: String) {
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun SelectedAnimeStrip(
    selectedAnimes: List<Anime>,
    onAnimeToggle: (Anime) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 6.dp)) {
        Text(
            text = "已选动画 · 点击标签可移除",
            color = MutedInk,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(selectedAnimes, key = Anime::subjectId) { anime ->
                FilterChip(
                    selected = true,
                    onClick = { onAnimeToggle(anime) },
                    label = { Text(anime.nameCn ?: anime.name, maxLines = 1) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                )
            }
        }
    }
}

@Composable
private fun AnimeResults(
    results: List<Anime>,
    selectedAnimeIds: Set<Long>,
    loadingAnimeIds: Set<Long>,
    onAnimeToggle: (Anime) -> Unit,
) {
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("找到 ${results.size} 部作品", style = MaterialTheme.typography.titleMedium, color = MutedInk)
        }
        items(results, key = Anime::subjectId) { anime ->
            val selected = anime.subjectId in selectedAnimeIds
            val loading = anime.subjectId in loadingAnimeIds
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !loading) { onAnimeToggle(anime) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) Color(0xFFFFE8E2) else Color(0xFFFFFCF7),
                ),
                border = BorderStroke(1.dp, if (selected) Vermilion else Sand),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = anime.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(width = 66.dp, height = 88.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Sand),
                        contentScale = ContentScale.Crop,
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp),
                    ) {
                        Text(
                            text = anime.nameCn ?: anime.name,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 2,
                        )
                        if (anime.nameCn != null) {
                            Text(
                                text = anime.name,
                                color = MutedInk,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Text(
                            text = "Bangumi #${anime.subjectId}",
                            color = Vermilion,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                    when {
                        loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        selected -> Icon(Icons.Rounded.Check, contentDescription = "已选择", tint = Vermilion)
                        else -> Text("选择", color = Moss, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeSelectionFooter(
    animeCount: Int,
    pointCount: Int,
    onOpenSelection: () -> Unit,
) {
    Surface(color = Color(0xFFFFFCF7), shadowElevation = 10.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("已选 $animeCount 部动画", fontWeight = FontWeight.Bold)
                Text("合计 $pointCount 个巡礼点", color = MutedInk, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = onOpenSelection,
                enabled = pointCount > 0,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Vermilion),
            ) {
                Text("查看地图")
            }
        }
    }
}

@Composable
private fun PilgrimageSelectionScreen(
    state: SearchUiState,
    onBack: () -> Unit,
    onTogglePoint: (String) -> Unit,
    onBoundsChanged: (GeoBounds) -> Unit,
    onSelectVisible: () -> Unit,
    onClearSelection: () -> Unit,
    onShowList: (Boolean) -> Unit,
    onPlan: () -> Unit,
) {
    val data = state.combinedPilgrimageData
    Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SelectionToolbar(
                title = data?.anime?.nameCn ?: data?.anime?.name.orEmpty(),
                pointCount = data?.points?.size,
                partialData = data?.warnings?.contains(PilgrimageWarning.PARTIAL_DATA) == true,
                onBack = onBack,
            )
            StatusMessage(state.errorMessage)

            when {
                state.isLoading || data == null -> LoadingState("正在展开巡礼地图…")
                state.showList -> PointList(
                    points = data.points,
                    selectedPointIds = state.selectedPointIds,
                    onTogglePoint = onTogglePoint,
                    modifier = Modifier.weight(1f),
                )
                else -> Box(modifier = Modifier.weight(1f)) {
                    PilgrimageMap(
                        contentKey = state.mapContentKey,
                        points = data.points,
                        selectedPointIds = state.selectedPointIds,
                        onPointToggle = onTogglePoint,
                        onVisibleBoundsChanged = onBoundsChanged,
                        modifier = Modifier.fillMaxSize(),
                    )
                    OutlinedButton(
                        onClick = onSelectVisible,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xEFFFFCF7)),
                        border = BorderStroke(1.dp, Ink),
                        shape = RoundedCornerShape(50),
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("选择当前地图范围", modifier = Modifier.padding(start = 6.dp))
                    }
                    Attribution(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp),
                    )
                }
            }

            SelectionFooter(
                selectedCount = state.selectedPointIds.size,
                showList = state.showList,
                onShowList = onShowList,
                onClear = onClearSelection,
                onPlan = onPlan,
            )
        }
    }
}

private fun Int?.orZero(): Int = this ?: 0

@Composable
private fun SelectionToolbar(
    title: String,
    pointCount: Int?,
    partialData: Boolean,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ink)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = Color.White)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            Text(
                text = when {
                    pointCount == null -> "读取巡礼点…"
                    partialData -> "$pointCount 个可用地点 · 数据可能不完整"
                    else -> "$pointCount 个巡礼地点"
                },
                color = Sand,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text("选点", color = Color.White, modifier = Modifier.padding(horizontal = 12.dp))
    }
}

@Composable
private fun PointList(
    points: List<PilgrimagePoint>,
    selectedPointIds: Set<String>,
    onTogglePoint: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(points, key = PilgrimagePoint::id) { point ->
            val selected = point.id in selectedPointIds
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTogglePoint(point.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) Color(0xFFFFE8E2) else Color(0xFFFFFCF7),
                ),
                border = BorderStroke(1.dp, if (selected) Vermilion else Sand),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = point.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(78.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Sand),
                        contentScale = ContentScale.Crop,
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                    ) {
                        Text(point.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                        Text(
                            "%.5f, %.5f".format(point.coordinate.latitude, point.coordinate.longitude),
                            color = MutedInk,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                        if (!point.origin.isNullOrBlank()) {
                            Text(
                                text = "截图来源：${point.origin}",
                                color = Vermilion,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier
                                    .padding(top = 7.dp)
                                    .clickable(enabled = !point.originUrl.isNullOrBlank()) {
                                        point.originUrl?.let(uriHandler::openUri)
                                    },
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) Vermilion else Sand),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionFooter(
    selectedCount: Int,
    showList: Boolean,
    onShowList: (Boolean) -> Unit,
    onClear: () -> Unit,
    onPlan: () -> Unit,
) {
    Surface(color = Color(0xFFFFFCF7), shadowElevation = 10.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = showList,
                onClick = { onShowList(!showList) },
                label = { Text(if (showList) "地图" else "列表") },
                leadingIcon = {
                    Icon(
                        if (showList) Icons.Rounded.Map else Icons.AutoMirrored.Rounded.List,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("已选 $selectedCount / ${SearchViewModel.MAX_ROAD_POINTS}", fontWeight = FontWeight.Bold)
                Text("可单点或按当前地图范围批选", color = MutedInk, style = MaterialTheme.typography.bodyMedium)
            }
            AnimatedVisibility(visible = selectedCount > 0) {
                OutlinedButton(onClick = onClear, shape = RoundedCornerShape(50)) {
                    Text("清空")
                }
            }
            Button(
                onClick = onPlan,
                enabled = selectedCount >= 2,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Vermilion),
            ) {
                Text("规划")
            }
        }
    }
}

@Composable
private fun Attribution(modifier: Modifier = Modifier) {
    Text(
        text = "OpenFreeMap · OpenMapTiles · © OpenStreetMap contributors",
        color = Ink,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.88f))
            .padding(horizontal = 5.dp, vertical = 3.dp),
    )
}

@Composable
private fun StatusMessage(message: String?) {
    AnimatedVisibility(visible = message != null) {
        Text(
            text = message.orEmpty(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun LoadingState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = Vermilion)
        Text(message, color = MutedInk, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun EmptySearchState(hasQuery: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 52.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (hasQuery) "还没有搜索结果" else "先找到想去的故事",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (hasQuery) "换一个译名或原名再试试。" else "支持中文译名、日文原名和英文名。",
            color = MutedInk,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
