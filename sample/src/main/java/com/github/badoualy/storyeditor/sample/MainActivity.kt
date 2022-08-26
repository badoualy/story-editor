@file:OptIn(ExperimentalMaterial3Api::class)

package com.github.badoualy.storyeditor.sample

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.github.badoualy.storyeditor.StoryEditor
import com.github.badoualy.storyeditor.StoryEditorState
import com.github.badoualy.storyeditor.element.text.StoryTextElement
import com.github.badoualy.storyeditor.element.text.TextElement
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.Black.toArgb()

        setContent {
            MaterialTheme(darkColorScheme()) {
                Surface(
                    color = Color.Black,
                    contentColor = Color.White,
                    modifier = Modifier.systemBarsPadding()
                ) {
                    Box {
                        Content(modifier = Modifier)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .background(
                                    Brush.verticalGradient(
                                        0f to Color.Black.copy(alpha = 0.2f),
                                        0.4f to Color.Black.copy(alpha = 0.2f),
                                        1f to Color.Black.copy(alpha = 0f),
                                    )
                                )
                        )
                        SmallTopAppBar(
                            navigationIcon = {
                                IconButton(onClick = {}) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            },
                            title = { Text("Kyoto - Kiyomizudera") },
                            colors = TopAppBarDefaults.smallTopAppBarColors(
                                containerColor = Color.Transparent,
                                navigationIconContentColor = Color.White,
                                titleContentColor = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Content(
    modifier: Modifier = Modifier
) {
    val elements = remember {
        mutableStateListOf(
            StoryTextElement(
                text = "Kiyomizudera\nKyoto",
                positionFraction = Offset(0.2f, 0.2f)
            )
        )
    }

    val editorState = rememberStoryEditorState()

    Box {
        StoryEditor(
            state = editorState,
            elements = elements.toImmutableList(),
            modifier = modifier.fillMaxSize(),
            onClick = {
                val element = StoryTextElement()
                editorState.focusedElement = element
                elements.add(element)
            },
            onDeleteElement = {
                elements.remove(it)
            },
            background = {
                AsyncImage(
                    ImageRequest.Builder(LocalContext.current)
                        .data("https://www.cercledesvoyages.com/wp-content/webp-express/webp-images/uploads/2020/12/pm_8002_101_101701-43vek4eifa-3364.jpeg.webp")
                        // Very important to avoid hardware bitmap crash when taking screenshot
                        .allowHardware(false)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f)
                )
            },
            shape = RoundedCornerShape(8.dp)
        ) { element ->
            when (element) {
                is StoryTextElement -> {
                    TextElement(
                        element = element,
                    )
                }
            }
        }

        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        Button(
            onClick = {
                coroutineScope.launch {
                    editorState.takeScreenshot(context)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Text("Screenshot")
        }
    }
}

@Composable
private fun rememberStoryEditorState(): StoryEditorState {
    return remember {
        StoryEditorState(
            elementsBoundsFraction = Rect(0.01f, 0.1f, 0.99f, 0.99f),
            editMode = true,
            debug = true,
            screenshotMode = StoryEditorState.ScreenshotMode.CONTENT
        )
    }
}

private suspend fun StoryEditorState.takeScreenshot(context: Context) {
    try {
        val bitmap = takeScreenshot()
        Log.i("StoryEditorSample", "Screenshot success")
        context.openFileOutput("screenshot.jpg", Context.MODE_PRIVATE).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
            bitmap.recycle()
        }
    } catch (e: Exception) {
        Log.e("StoryEditorSample", "Failed to take screenshot", e)
    }
}
