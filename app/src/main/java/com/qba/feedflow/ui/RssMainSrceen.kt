package com.qba.feedflow.ui

import android.R.attr.action
import android.content.Intent
import android.net.Uri
import android.text.Html
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qba.feedflow.R
import com.qba.feedflow.data.RssItem
import com.qba.feedflow.data.dateToString
import com.qba.feedflow.data.testData
import com.qba.feedflow.ui.theme.AppTheme
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.qba.feedflow.data.RssViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssAPP(viewModel:RssViewModel = viewModel(), modifier: Modifier = Modifier) {
    val uistate = viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){}
    val context = LocalContext.current
    Scaffold(
        modifier = modifier,
        // 右下角浮动按钮
        floatingActionButton = {
            FloatingActionButton(onClick = {viewModel.selectTab()},
                containerColor = MaterialTheme.colorScheme.secondary) {
                Icon(Icons.Default.Create, contentDescription = "ai")
            }
        },

        // 顶部应用栏
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        modifier = modifier,
                        text = "FeedFlow",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.selectTab()
                    }) {
                        Icon(Icons.Default.Menu, contentDescription = "菜单")
                    }
                }
            )
        }

    ) { innerPadding ->
        // 内容区域
        Box(modifier = Modifier.padding(innerPadding)) {
            NewsList(newsitems = uistate.value.nowItems,
                selectedItem = uistate.value.selectedItem,
                onTitleClick = {
                    openWeb(url = it, context = context, launcher = launcher)
                },
                onDescriptionClick = {
                    viewModel.selectItem(it)
                },
                onLikeClick = {

                },
                modifier = Modifier.fillMaxSize().padding(dimensionResource(R.dimen.padding_medium)))
        }
    }
    if (uistate.value.selectedTab) {
        ModalBottomSheet(
            onDismissRequest = {
                // 点击外部或返回时隐藏底部抽屉
                viewModel.selectTab()
            },
            sheetState = sheetState,

        ) {
            Tab(viewModel = viewModel)
        }
    }
}


@Composable
fun NewsList(modifier: Modifier = Modifier, newsitems: List<RssItem>,
             selectedItem: Long = -1L,
             onTitleClick: (String) -> Unit,
             onDescriptionClick: (Long) -> Unit,
             onLikeClick: () -> Unit) {
        LazyColumn(modifier = modifier) {
            items(newsitems) { item ->
                if (item.id == selectedItem){
                    DetailedCard(item = item,
                        onTitleClick = onTitleClick,
                        onLikeClick = onLikeClick,
                        onDescriptionClick = {onDescriptionClick(-1L)})
                }else{
                    NewsCard(item = item,
                        onTitleClick = onTitleClick,
                        onDescriptionClick = onDescriptionClick)
                }
            }
        }
}

@Composable
fun DetailedCard(
    modifier: Modifier = Modifier,
    item: RssItem,
    onTitleClick: (String) -> Unit = {},
    onLikeClick: () -> Unit = {},
    onDescriptionClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.margin_small),
                vertical = dimensionResource(R.dimen.margin_small)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.card_elevation)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(dimensionResource(R.dimen.margin_medium))
        ) {
            // Channel name
            Text(
                text = item.channel,
                style = MaterialTheme.typography.labelSmall,
            )

            // Title
            Text(
                modifier = Modifier.clickable { onTitleClick(item.link) },
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
            )

            // Optional description
            item.description.let {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_small)))
                Text(
                    modifier = Modifier.clickable { onDescriptionClick() },
                    text = Html.fromHtml(it, Html.FROM_HTML_MODE_COMPACT).toString().trim(),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Publish date (bottom-right aligned)
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_small)))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = dateToString(item.pubDate),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Row (){
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onLikeClick,
                    ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Like")
                }
                IconButton(onClick = {
                    val sendIntent = Intent().apply {
                        val shareText = """
                                        📢  ${item.title}
                                        📰  ${item.channel}
                                        📅  ${dateToString(item.pubDate)}
                                        🔗  ${item.link}
                                    """.trimIndent()
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }

                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                }) {
                    Icon(Icons.Filled.Share, contentDescription = "Share")
                }
            }
        }
    }
}


@Composable
fun NewsCard(
    modifier: Modifier = Modifier,
    item: RssItem,
    onDescriptionClick: (Long) -> Unit = {},
    onTitleClick: (String) -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.margin_small),
                vertical = dimensionResource(R.dimen.margin_small)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.card_elevation)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(dimensionResource(R.dimen.margin_medium))
        ) {
            // Channel name
            Text(
                text = item.channel,
                style = MaterialTheme.typography.labelSmall,
            )

            // Title
            Text(
                modifier = Modifier.clickable { onTitleClick(item.link) },
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
            )

            item.description.let {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_small)))
                Text(
                    modifier = Modifier.clickable { onDescriptionClick(item.id) },
                    text = Html.fromHtml(it, Html.FROM_HTML_MODE_COMPACT).toString().trim(),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Publish date (bottom-right aligned)
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_small)))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = dateToString(item.pubDate),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

fun openWeb(url: String, context: android.content.Context, launcher: androidx.activity.compose.ManagedActivityResultLauncher<Intent, androidx.activity.result.ActivityResult>){
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        launcher.launch(intent)
    } catch (e: Exception) {
        // 可选：显示错误提示
        android.widget.Toast.makeText(context, "Cannot open URL", android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Preview(showBackground = true)
@Composable
fun NewsCardPreview() {
    AppTheme(darkTheme = true) {
        DetailedCard(
            item = RssItem(
                1,
                "huggingface",
                "ScreenSuite - The Most Comprehensive Evaluation Suite for GUI Agents!",
                "h",
                "Over the past few weeks, we’ve been working tirelessly on making GUI agents more open, accessible and easy to integrate. Along the way, we created the largest benchmarking suite for GUI agents performances "
            )
            , onLikeClick = {},
            onTitleClick = {},
            onDescriptionClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NewsListPreview() {
    AppTheme(darkTheme = true) {
        RssAPP()
    }
}