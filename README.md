# WebPane

A lightweight browser plugin for JetBrains IDEs. Browse the web, test front-end code, and use DevTools — all without leaving your IDE.

为 JetBrains IDE 提供轻量级内置浏览器面板，在不离开 IDE 的情况下浏览网页、测试前端代码、使用开发者工具。

## Features

- **Integrated Browser** — JCEF-powered Chromium panel docked on the right side of the editor
- **DevTools (F12)** — Built-in developer tools for debugging web pages
- **Zoom & Navigation** — Forward/backward, zoom in/out, reset zoom
- **HTML Quick Open** — Gutter icon on `<html>` tags to preview HTML files instantly
- **Memory Monitor** — Alerts when IDE memory usage is high, preventing crashes
- **Theme Aware** — Icons and UI adapt to your IDE theme (light/dark)

## Installation

### From JetBrains Marketplace (recommended)

1. Open your JetBrains IDE
2. Go to **Settings → Plugins → Marketplace**
3. Search for **WebPane**
4. Click **Install** and restart the IDE

### Manual Installation

1. Download `webpane-1.0.0.zip` from [Releases](https://github.com/ApolleoOnce/WebPane/releases)
2. Go to **Settings → Plugins → ️ → Install Plugin from Disk...**
3. Select the downloaded ZIP file
4. Restart the IDE

## Usage

### Opening WebPane

- **Menu**: Tools → Open WebPane
- **Toolbar**: Click the WebPane icon in the right-side tool window bar

### Browsing

1. Enter a URL in the address bar and press **Enter**
2. Use the **Tools** dropdown for navigation, zoom, and DevTools

### Preview HTML Files

Click the browser icon in the gutter next to the `<html>` tag in any `.html` file to open it directly in WebPane.

### DevTools

Click **Tools → F12** to toggle the developer tools panel at the bottom of the IDE.

## Build from Source

### Requirements

- JDK 21+
- Maven 3.8+
- JetBrains IDE (PyCharm / IntelliJ IDEA 2025.x or 2026.x) with JCEF enabled

### Steps

```bash
git clone https://github.com/ApolleoOnce/WebPane.git
cd WebPane
```

Edit `pom.xml` and set `<ide.home>` to your IDE installation path:

```xml
<ide.home>/path/to/your/PyCharm</ide.home>
```

Then build:

```bash
mvn clean package
```

The plugin ZIP will be generated at `target/webpane-1.0.0.zip`.

## License

This project is licensed under the [MIT License](LICENSE).
