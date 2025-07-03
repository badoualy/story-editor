[![Release](https://jitpack.io/v/badoualy/story-editor.svg)](https://jitpack.io/#badoualy/story-editor)

# Story Editor

Instagram-like story editor to add content to your pictures.

Note: this is still a WIP

<img src="https://github.com/badoualy/story-editor/blob/main/ART/preview.gif" width="300">

Setup
----------------

First, add jitpack in your build.gradle at the end of repositories:

 ```gradle
repositories {
    // ...
    maven { url "https://jitpack.io" }
}
```

Then, add the library dependency:

```gradle
implementation 'com.github.badoualy:story-editor:0.1.0'
```

Usage
----------------

(See MainActivity sample)

In your manifest:

```
android:windowSoftInputMode="adjustResize"
```

In your activity (important to handle keyboard correctly):

```kotlin
WindowCompat.setDecorFitsSystemWindows(window, false)
```

Then:

```kotlin
val elements = remember { mutableStateListOf() }
StoryEditor(
  state = rememberStoryEditorState(),
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
      "https://i.ytimg.com/vi/h78qlOYCXJQ/maxresdefault.jpg",
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.aspectRatio(9f / 16f)
    )
  }
) {
    elements.forEach { element ->
      Element(element = element, modifier = Modifier) {
        TextElement(
          element = element,
        )
      }
    }
}

@Composable
private fun rememberStoryEditorState(): StoryEditorState {
  return remember {
    StoryEditorState(
      elementsBoundsFraction = Rect(0.01f, 0.1f, 0.99f, 0.99f),
      editMode = true,
      debug = true, // draws hitbox red box
      screenshotMode = StoryEditorState.ScreenshotMode.FULL
    )
  }
}

```

Screenshot
----------------

You can take a screenshot of the editor's content via `editorState.takeScreenshot()`.

* Specify a screenshot mode when creating your `StoryEditorState`.

Current restrictions:

* Make sure you background doesn't have any hardware bitmap, or you'll get the exception:
  `Software rendering doesn't support hardware bitmaps`. If you're using Coil/Glide/... you can
  disable hardware bitmap when creating the request.
* If your background is clipped to a given shape, the bitmap will also be clipped.

Screenshot mode:

* `DISABLED`: Screenshot support is disabled
* `FULL`: Screenshot support is enabled, and the screenshot will contain background + content
* `FULL_NOT_CLIPPED`: Same as `FULL`, but the screenshot won't be clipped to the `StoryEditor`'s
  shape. This is useful when you specify a shape for the background, and you don't want the
  screenshot to be clipped.
* `CONTENT`: Screenshot support is enabled, and the screenshot will contain only the content without
  the background

Elements
----------------

By default, only a `TextElement` is provided, but you can easily add your own components to the
editor. Check TextElement implementation to do so.
