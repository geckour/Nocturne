package com.geckour.nocturne

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.watchface.editor.EditorSession
import androidx.wear.watchface.style.UserStyleSetting

class NocturneConfigActivity : AppCompatActivity() {

    private var editorSession: EditorSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launchWhenCreated {
            editorSession = EditorSession.createOnWatchEditorSession(this@NocturneConfigActivity)
        }

        setContent {
            val editorSession: EditorSession? by remember { mutableStateOf(editorSession) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = colorResource(id = R.color.background)), contentAlignment = Alignment.Center
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val checked =
                            (editorSession?.userStyle
                                ?.collectAsState()
                                ?.value
                                ?.get(NocturneFaceService.showLongHandSetting) as UserStyleSetting.BooleanUserStyleSetting.BooleanOption?)?.value
                                ?: false

                        Text(text = "Show long hand")
                        Switch(
                            modifier = Modifier.padding(4.dp),
                            checked = checked,
                            onCheckedChange = {
                                editorSession?.userStyle?.value?.let { userStyle ->
                                    editorSession?.userStyle?.value = userStyle.toMutableUserStyle()
                                        .apply {
                                            set(
                                                NocturneFaceService.showLongHandSetting,
                                                UserStyleSetting.BooleanUserStyleSetting.BooleanOption.from(it)
                                            )
                                        }
                                        .toUserStyle()
                                }
                            }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val checked =
                            (editorSession?.userStyle
                                ?.collectAsState()
                                ?.value
                                ?.get(NocturneFaceService.showShortHandSetting) as UserStyleSetting.BooleanUserStyleSetting.BooleanOption?)?.value
                                ?: false

                        Text(text = "Show short hand")
                        Switch(
                            modifier = Modifier.padding(4.dp),
                            checked = checked,
                            onCheckedChange = {
                                editorSession?.userStyle?.value?.let { userStyle ->
                                    editorSession?.userStyle?.value = userStyle.toMutableUserStyle()
                                        .apply {
                                            set(
                                                NocturneFaceService.showShortHandSetting,
                                                UserStyleSetting.BooleanUserStyleSetting.BooleanOption.from(it)
                                            )
                                        }
                                        .toUserStyle()
                                }
                            }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val checked =
                            (editorSession?.userStyle
                                ?.collectAsState()
                                ?.value
                                ?.get(NocturneFaceService.fillWaveSetting) as UserStyleSetting.BooleanUserStyleSetting.BooleanOption?)?.value
                                ?: true

                        Text(text = "Fill wave")
                        Switch(
                            modifier = Modifier.padding(4.dp),
                            checked = checked,
                            onCheckedChange = {
                                editorSession?.userStyle?.value?.let { userStyle ->
                                    editorSession?.userStyle?.value = userStyle.toMutableUserStyle()
                                        .apply {
                                            set(
                                                NocturneFaceService.fillWaveSetting,
                                                UserStyleSetting.BooleanUserStyleSetting.BooleanOption.from(it)
                                            )
                                        }
                                        .toUserStyle()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}