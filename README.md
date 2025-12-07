# Render Engine

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0.21-purple.svg)](https://kotlinlang.org/)
[![LWJGL](https://img.shields.io/badge/LWJGL-3.3.6-orange.svg)](https://www.lwjgl.org/)

A lightweight, Kotlin-based 2D rendering library built on OpenGL and LWJGL.

## Overview

Render Engine is a comprehensive 2D graphics library designed for game development and interactive applications. It provides a simple yet powerful API for handling rendering, input, camera management, and resource loading.

### Key Features

- **Window Management** - OpenGL context and GLFW window integration
- **2D Rendering** - Sprite and geometry rendering with texture atlas support
- **Text Rendering** - TrueType and OpenType font support with Unicode
- **Input System** - Keyboard and mouse event handling
- **Camera System** - 2D camera with zoom, interpolation, and coordinate transformation
- **Resource Packs** - Minecraft-style resource pack system for easy modding
- **Interpolation** - Smooth rendering between physics ticks

## Quick Start

```kotlin
class MyGame : RenderEngineCore("My Game", -1, -1, 1280, 720, 60) {
    override fun onEnable() {
        keyboard.onKeyPress(GLFW_KEY_ESCAPE) { window.close() }
    }

    override fun tick() {
        camera.tick()
    }

    override fun onRender() {
        renderEngine.flush()
    }
}

fun main() {
    check(glfwInit()) { "Failed to initialize GLFW" }
    MyGame().enable()
}
```

## Documentation

**[📚 View Full Documentation](https://terraminer.github.io/render-engine/)**


## Project Structure

```
ua/terra/renderengine/
├── RenderEngineCore.kt    # Core engine class
├── camera/                # Camera management
├── input/                 # Keyboard and mouse input
├── resource/              # Resource pack system
├── render/                # Interpolation and rendering
├── shader/                # Shader management
├── text/                  # Font rendering
├── texture/               # Texture management and atlases
├── util/                  # Utilities (Cooldown, Point, Color)
└── window/                # Window and metrics
```

## Dependencies

- Kotlin 2.0.21+
- LWJGL 3.3.6 (OpenGL, GLFW, STB)
- fastutil 8.5.12
- JOML 1.10.5
- Gson 2.11.0

## Building

```bash
./gradlew build
```

## License

MIT License - see [LICENSE](LICENSE) file for details

## Links

- [GitHub Repository](https://github.com/TerraMiner/render-engine)
- [Documentation](https://terraminer.github.io/render-engine/)
- [Issue Tracker](https://github.com/TerraMiner/render-engine/issues)

---

Made with ❤️ using Kotlin and LWJGL

