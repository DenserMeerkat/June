package com.denser.june.presentation.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.json.JSONArray

import com.denser.june.BuildConfig
import com.denser.june.core.R
import com.denser.june.presentation.components.AnnouncementCard
import com.denser.june.presentation.utils.AnnouncementEntry
import com.denser.june.presentation.utils.AnnouncementActionType
import com.denser.june.presentation.utils.AnnouncementAction

data class VersionEntry(
    val version: String,
    val changes: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogBottomSheet(
    setShowSheet: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val changelogs = remember {
        try {
            val jsonString = context.assets.open("changelog.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<VersionEntry>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val version = obj.getString("version")
                val changesArray = obj.getJSONArray("changes")
                val changesList = mutableListOf<String>()
                for (j in 0 until changesArray.length()) {
                    val element = changesArray.get(j)
                    if (element is String) {
                        changesList.add(element)
                    } else if (element is org.json.JSONObject) {
                        val text = element.getString("text")
                        val flavors = if (element.has("flavors") && !element.isNull("flavors")) {
                            val arr = element.getJSONArray("flavors")
                            val fList = mutableListOf<String>()
                            for (k in 0 until arr.length()) {
                                fList.add(arr.getString(k))
                            }
                            fList
                        } else {
                            null
                        }
                        if (flavors == null || flavors.contains(BuildConfig.FLAVOR)) {
                            changesList.add(text)
                        }
                    }
                }
                if (changesList.isNotEmpty()) {
                    list.add(VersionEntry(version, changesList))
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    val announcement = remember {
        try {
            val jsonString = context.assets.open("announcements.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            if (jsonArray.length() > 0) {
                val obj = jsonArray.getJSONObject(0)
                val actionObj = obj.optJSONObject("action")
                val action = if (actionObj != null) {
                    val typeStr = actionObj.getString("type")
                    val type = try {
                        AnnouncementActionType.valueOf(typeStr.uppercase())
                    } catch (e: Exception) {
                        AnnouncementActionType.WEB_URL
                    }
                    AnnouncementAction(
                        text = actionObj.getString("text"),
                        type = type,
                        target = actionObj.getString("target")
                    )
                } else null

                AnnouncementEntry(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    message = obj.getString("message"),
                    icon = obj.optString("icon", null),
                    action = action
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    val uriHandler = LocalUriHandler.current

    ModalBottomSheet(
        onDismissRequest = { setShowSheet(false) },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.changelog),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(top = 16.dp, bottom = 16.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                if (announcement != null) {
                    item {
                        AnnouncementCard(
                            announcement = announcement,
                            onActionClick = {
                                announcement.action?.let { action ->
                                    when (action.type) {
                                        AnnouncementActionType.WEB_URL -> {
                                            uriHandler.openUri(action.target)
                                        }
                                        else -> {
                                            uriHandler.openUri(action.target)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                itemsIndexed(changelogs) { _, versionEntry ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = versionEntry.version,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            versionEntry.changes.forEachIndexed { index, change ->
                                val shape = when {
                                    versionEntry.changes.size == 1 -> RoundedCornerShape(16.dp)
                                    index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                    index == versionEntry.changes.size - 1 -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                                    else -> RectangleShape
                                }

                                ListItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(shape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                    headlineContent = {
                                        Text(
                                            text = change,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                )

                                if (index < versionEntry.changes.size - 1) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        thickness = 1.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
