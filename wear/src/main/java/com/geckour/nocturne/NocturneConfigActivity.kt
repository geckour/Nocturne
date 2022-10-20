package com.geckour.nocturne

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.RadioButton
import androidx.wear.compose.material.RadioButtonDefaults
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.SwitchDefaults
import androidx.wear.compose.material.Text
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.editor.EditorSession
import androidx.wear.watchface.style.UserStyleSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NocturneConfigActivity : AppCompatActivity() {

    private var editorSession: EditorSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launchWhenCreated {
            editorSession = EditorSession.createOnWatchEditorSession(this@NocturneConfigActivity)
        }

        setContent {
            val editorSession: EditorSession? by remember { mutableStateOf(editorSession) }
            val coroutineScope = rememberCoroutineScope()

            ScalingLazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = colorResource(id = R.color.background)),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(20.dp),
                autoCentering = null
            ) {
                item {
                    val previewData = editorSession?.complicationsPreviewData?.collectAsState()
                    val dataSourceInfo = editorSession?.complicationsDataSourceInfo?.collectAsState()
                    val slotState = editorSession?.complicationSlotsState?.collectAsState()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        val complicationData1 = previewData?.value?.get(1) ?: dataSourceInfo?.value?.get(1)?.fallbackPreviewData
                        val actualBounds1 = slotState?.value?.get(1)?.bounds
                        ComplicationButton(
                            modifier = Modifier.padding(top = 8.dp),
                            coroutineScope = coroutineScope,
                            id = 1,
                            complicationData = complicationData1,
                            actualBounds = actualBounds1
                        )

                        val complicationData0 = previewData?.value?.get(0) ?: dataSourceInfo?.value?.get(0)?.fallbackPreviewData
                        val actualBounds0 = slotState?.value?.get(0)?.bounds
                        ComplicationButton(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            coroutineScope = coroutineScope,
                            id = 0,
                            complicationData = complicationData0,
                            actualBounds = actualBounds0
                        )

                        val complicationData2 = previewData?.value?.get(2) ?: dataSourceInfo?.value?.get(2)?.fallbackPreviewData
                        val actualBounds2 = slotState?.value?.get(2)?.bounds
                        ComplicationButton(
                            modifier = Modifier.padding(top = 8.dp),
                            coroutineScope = coroutineScope,
                            id = 2,
                            complicationData = complicationData2,
                            actualBounds = actualBounds2
                        )
                    }
                }
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(text = "Circle type:")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val selectedType = (editorSession?.userStyle
                                ?.collectAsState()
                                ?.value
                                ?.get(NocturneFaceService.circleTypeSetting) as UserStyleSetting.LongRangeUserStyleSetting.LongRangeOption?)
                                ?.value
                                ?.let { CircleType.fromOrdinal(it.toInt()) }
                                ?: CircleType.default

                            CircleType.values().forEach {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = it.displayName)
                                    RadioButton(
                                        selected = it == selectedType,
                                        colors = RadioButtonDefaults.colors(
                                            selectedDotColor = colorResource(id = R.color.colorAccent),
                                            selectedRingColor = colorResource(id = R.color.text)
                                        ),
                                        onClick = {
                                            editorSession?.userStyle?.value?.let { userStyle ->
                                                editorSession?.userStyle?.value = userStyle.toMutableUserStyle()
                                                    .apply {
                                                        set(
                                                            NocturneFaceService.circleTypeSetting,
                                                            UserStyleSetting.LongRangeUserStyleSetting.LongRangeOption(it.ordinal.toLong())
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
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val checked =
                            (editorSession?.userStyle
                                ?.collectAsState()
                                ?.value
                                ?.get(NocturneFaceService.fillWaveSetting) as UserStyleSetting.BooleanUserStyleSetting.BooleanOption?)?.value
                                ?: true

                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f), text = "Fill wave"
                        )
                        Switch(
                            modifier = Modifier.padding(4.dp),
                            checked = checked,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colorResource(id = R.color.colorAccent),
                                checkedTrackColor = colorResource(id = R.color.text)
                            ),
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

    @Composable
    fun ComplicationButton(
        modifier: Modifier = Modifier,
        coroutineScope: CoroutineScope,
        id: Int,
        complicationData: ComplicationData?,
        actualBounds: Rect?
    ) {
        actualBounds ?: return

        Box(modifier = modifier.size(28.dp)) {
            Button(
                modifier = Modifier.fillMaxSize(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colorResource(id = android.R.color.transparent),
                    contentColor = colorResource(id = R.color.text)
                ),
                border = ButtonDefaults.buttonBorder(borderStroke = BorderStroke(2.dp, colorResource(id = R.color.colorAccent))),
                onClick = {
                    coroutineScope.launch {
                        editorSession?.openComplicationDataSourceChooser(id)
                    }
                }
            ) {
                when (complicationData?.type) {
                    null,
                    ComplicationType.NO_DATA,
                    ComplicationType.NOT_CONFIGURED,
                    ComplicationType.EMPTY -> {
                        Icon(imageVector = Icons.Rounded.Add, "Add additional function")
                    }
                    else -> {
                        ComplicationDrawable(
                            this@NocturneConfigActivity
                        ).run {
                            bounds = actualBounds
                            setComplicationData(complicationData, false)
                            Image(
                                modifier = Modifier.fillMaxSize(),
                                bitmap = current
                                    .toBitmap(
                                        width = actualBounds.width(),
                                        height = actualBounds.height(),
                                        config = Bitmap.Config.ARGB_8888
                                    )
                                    .asImageBitmap(),
                                contentDescription = "Additional function preview"
                            )
                        }
                    }
                }
            }
        }
    }
}