package com.qba.feedflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qba.feedflow.R
import com.qba.feedflow.data.ALL_CHANNELS
import com.qba.feedflow.data.Liked_ITEMS
import com.qba.feedflow.data.RssViewModel
import com.qba.feedflow.data.testchannel
import com.qba.feedflow.data.uiState
import com.qba.feedflow.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Tab(modifier: Modifier = Modifier,
        viewModel:RssViewModel = viewModel()) {
    var showAdd by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    val uistate = viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        modifier = modifier,
                        text = stringResource(R.string.tab_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                actions = {
                    IconButton(onClick = {
                        showAdd = !showAdd
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加订阅源"
                        )
                    }
                    IconButton(onClick = {
                        showDelete = !showDelete
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除订阅源"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column{
            if (showAdd){
                AddChannelBar(rssUrl = uistate.value.newChannel,
                    onUrlChange = {viewModel.updateNewChannel(it)},
                    onSubmit = {viewModel.submitNewChannel()},
                    onCancel = {showAdd = false
                               viewModel.updateNewChannel("")
                               },
                    error = uistate.value.addChannelFailed,
                    modifier = Modifier.padding(innerPadding))
            }
            ChannelList(
                channelList = uistate.value.channels.map { it -> it.name },
                modifier = Modifier.padding(innerPadding),
                onChannelClick = {viewModel.selectChannel(it)},
                onChannelDelete = {viewModel.deleteChannel(it)},
                showDelete = showDelete,
                selectedChannel = uistate.value.selectedChannel
            )
        }
    }
}

@Composable
fun ChannelList(channelList: List<String> = emptyList(),
                onChannelClick: (String) -> Unit = {},
                onChannelDelete: (String) -> Unit = {},
                modifier: Modifier = Modifier,
                showDelete: Boolean = false,
                selectedChannel: String = ALL_CHANNELS) {
    LazyColumn(modifier = modifier.padding(dimensionResource(R.dimen.padding_medium))) {
        item {
            if(selectedChannel == ALL_CHANNELS)
                ChannelEntry(ALL_CHANNELS,  selected = true, onChannelClick = onChannelClick)
            else
                ChannelEntry(ALL_CHANNELS,  selected = false, onChannelClick = onChannelClick)
        }
        item {
            if(selectedChannel == Liked_ITEMS)
                ChannelEntry(Liked_ITEMS,  selected = true,
                    onChannelDelete = onChannelDelete,
                    onChannelClick = onChannelClick)
            else
                ChannelEntry(Liked_ITEMS,  selected = false,
                    onChannelDelete = onChannelDelete,
                    onChannelClick = onChannelClick)
        }
        items(channelList) { item ->
            if(item == selectedChannel){
                ChannelEntry(item,
                    onChannelClick=onChannelClick,
                    onChannelDelete = onChannelDelete,
                    showDelete = showDelete,
                    selected = true)
            }else{
                ChannelEntry(item,
                    onChannelClick=onChannelClick,
                    onChannelDelete = onChannelDelete,
                    showDelete = showDelete)
            }

        }
    }
}

@Composable
fun ChannelEntry(
    channel: String,
    modifier: Modifier = Modifier,
    onChannelClick: (String) -> Unit = {},
    onChannelDelete: (String) -> Unit = {},
    showDelete: Boolean = false,
    selected: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(all = dimensionResource(R.dimen.padding_small))
    ) {
        Row(modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = channel,
                style = if (selected) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable {onChannelClick(channel)}
            )
            Spacer(modifier = Modifier.weight(1f))
            if (showDelete){
                IconButton(onClick = {onChannelDelete(channel)}){
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除订阅源: " + channel
                    )
                }
            }
        }
    }
}

@Composable
fun AddChannelBar(
    rssUrl: String,
    onUrlChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    error: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = dimensionResource(R.dimen.padding_small))
    ) {
        OutlinedTextField(
            value = rssUrl,
            onValueChange = { onUrlChange(it) },
            label = { Text(text = if (error) stringResource(R.string.faileAdd) else stringResource(R.string.add_channel)) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    onSubmit()
                }
            ),
            isError = error,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(){
            Button(onClick = {onSubmit()}, Modifier.weight(1f)) {
                Text(text = stringResource(R.string.add))
            }
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.padding_small)))
            Button(onClick = {
                onCancel()
            }, Modifier.weight(1f)) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MoreTabPreview() {
    AppTheme(darkTheme = false) {
        Tab()
    }
}

@Preview(showBackground = true)
@Composable
fun AddChannelBarPreview() {
    AppTheme(darkTheme = false) {
        AddChannelBar(rssUrl = "", onUrlChange = {},
            onSubmit = {}, onCancel = {})
    }
}

@Preview(showBackground = true)
@Composable
fun AddList() {
    AppTheme(darkTheme = false) {
        ChannelList(testchannel, showDelete = true)
    }
}
