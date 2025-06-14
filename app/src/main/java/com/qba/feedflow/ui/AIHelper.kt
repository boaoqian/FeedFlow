package com.qba.feedflow.ui

import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qba.feedflow.R
import com.qba.feedflow.data.AITool
import com.qba.feedflow.data.AIstep
import com.qba.feedflow.data.RssViewModel
import com.qba.feedflow.data.dateToString
import com.qba.feedflow.ui.theme.AppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AITab(viewModel: RssViewModel = viewModel(),modifier: Modifier = Modifier){
    var setting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val uistate = viewModel.uiState.collectAsState()
    val context = LocalContext.current
    Scaffold(modifier = Modifier.fillMaxWidth(), topBar = {
        TopAppBar(title = {},
            navigationIcon = {
                IconButton(onClick = { setting = !setting }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            })
    }){ innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.05f))
            if (!setting) {
                AIoutput(ai_res = uistate.value.ai_res, ai_step = uistate.value.ai_step)
                Spacer(modifier.padding(20.dp))
                if (uistate.value.ai_step == AIstep.none)
                    Row(modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium))) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
//                                viewModel.chatAI(AITool.SUMMARY)
                                viewModel.chatAIStream(AITool.SUMMARY)
                            }
                          },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(id = R.string.summary))
                    }

                    if (uistate.value.selectedItem != -1L) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
//                                    viewModel.chatAI(AITool.TRANS)
                                    viewModel.chatAIStream(AITool.TRANS)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = stringResource(id = R.string.trans))
                        }
                    }
                }
                else if(uistate.value.ai_step == AIstep.end)
                    Row(modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium))) {
                        IconButton(onClick = {
                            val sendIntent = Intent().apply {
                                val shareText = uistate.value.ai_res
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
            }else{
                Setting(modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium))
                    ,key = uistate.value.keyinput,
                    onChange = {viewModel.updateNewKey(it)},
                    onSubmit = {viewModel.saveKey()},
                    onCancel = {setting = false})
            }
            Spacer(Modifier.weight(0.8f))
        }

    }
}

@Composable
fun AIoutput(modifier: Modifier  = Modifier, ai_res: String = "", ai_step: AIstep = AIstep.none){
    when (ai_step) {
        AIstep.none -> {
            Text(text = stringResource(R.string.ai_hello),
                style = MaterialTheme.typography.bodyLarge)
        }
        AIstep.wait -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()  // 关键点：Box 扩展为整行
                    .padding(dimensionResource(R.dimen.padding_medium)),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = ai_res,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.ai_processing),
                    style = MaterialTheme.typography.bodyLarge,)
            }
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()  // 关键点：Box 扩展为整行
                    .padding(dimensionResource(R.dimen.padding_medium)),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = ai_res,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun Setting(modifier: Modifier = Modifier,key: String,
            onSubmit:()->Unit,
            onChange:(String)->Unit={},
            onCancel:()->Unit={}){
    Column(modifier) {
        Text(text = stringResource(R.string.set_api_key),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium)))
        OutlinedTextField(
            value = key,
            onValueChange = { onChange(it) },
            label = { Text(text = stringResource(R.string.apikey)) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    onSubmit()
                    onCancel()
                }
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(dimensionResource(R.dimen.margin_medium))
        )
        Row(){
            Button(onClick = {
                onSubmit()
                onCancel()}, Modifier.weight(1f)) {
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

@Preview()
@Composable()
fun p(){
    AppTheme {
        AITab()
    }
}

