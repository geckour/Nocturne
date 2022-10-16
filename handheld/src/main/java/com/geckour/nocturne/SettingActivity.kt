package com.geckour.nocturne

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import coil.Coil
import coil.request.ImageRequest
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.io.ByteArrayOutputStream

class SettingActivity : AppCompatActivity() {

    companion object {

        private const val DATA_PATH_BACKGROUND_IMAGE = "/data/background_image"
    }

    private lateinit var sharedPreferences: SharedPreferences

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@registerForActivityResult

        lifecycleScope.launchWhenResumed {
            (Coil.imageLoader(this@SettingActivity).execute(
                ImageRequest.Builder(this@SettingActivity)
                    .data(uri)
                    .allowHardware(false)
                    .build()
            ).drawable as BitmapDrawable?)?.bitmap?.let {
                Wearable.getDataClient(this@SettingActivity).putDataItem(
                    PutDataMapRequest.create(DATA_PATH_BACKGROUND_IMAGE).apply {
                        dataMap.putAsset(
                            "value",
                            Asset.createFromBytes(
                                ByteArrayOutputStream()
                                    .apply { it.compress(Bitmap.CompressFormat.PNG, 100, this) }
                                    .toByteArray()
                            )
                        )
                    }.asPutDataRequest()
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = colorResource(id = R.color.colorBackground))
            ) {
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    painter = painterResource(id = R.drawable.wave),
                    contentDescription = "Wave image",
                    contentScale = ContentScale.FillWidth
                )
                LazyColumn(Modifier.fillMaxSize()) {
//                    item {
//                        SettingItem(title = "Set background image") {
//                            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
//                        }
//                    }
//                    item {
//                        SettingItem(title = "Clear background image") {
//                            Wearable.getDataClient(this@SettingActivity).putDataItem(PutDataMapRequest.create(DATA_PATH_BACKGROUND_IMAGE).apply {
//                                dataMap.putAsset("value", Asset.createFromBytes(byteArrayOf()))
//                            }.asPutDataRequest())
//                        }
//                    }
                }
            }
        }
    }

    @Composable
    private fun SettingItem(title: String, onClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = rememberRipple(),
                    interactionSource = MutableInteractionSource(),
                    onClick = onClick
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                text = title,
                color = colorResource(id = R.color.colorText),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}