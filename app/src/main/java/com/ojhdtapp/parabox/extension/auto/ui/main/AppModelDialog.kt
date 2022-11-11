package com.ojhdtapp.parabox.extension.auto.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ojhdtapp.parabox.extension.auto.R
import com.ojhdtapp.parabox.extension.auto.domain.model.AppModel

@Composable
fun AppModelDialog(
    modifier: Modifier = Modifier,
    showDialog: Boolean,
    appModelList: List<AppModel>,
    onValueChange: (target: AppModel, value: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.done))
                }
            },
            title = {
                Text(text = stringResource(R.string.app_list))
            },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(0.dp, 480.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        if (appModelList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = stringResource(R.string.no_notification_received))
                            }
                        }
                    }
                    items(items = appModelList, key = { it.id }) {
                        Row(
                            modifier = modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = it.appName,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Checkbox(
                                checked = it.disabled,
                                onCheckedChange = { value -> onValueChange(it, value) })
                        }
                    }
                }
            }
        )
    }
}