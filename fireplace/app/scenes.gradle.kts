// Generates the launcher tiles from config/scenes.json:
//   - an AndroidManifest.xml with one launcher entry (activity-alias) per scene
//   - a banner image per tile, drawn here so no image editing is needed
//   - a copy of scenes.json in the app's assets, which the menu reads at runtime
// Runs while Gradle configures the project, so every build picks up config changes.

import groovy.json.JsonSlurper
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.GradientPaint
import java.awt.RadialGradientPaint
import java.awt.RenderingHints
import java.awt.geom.GeneralPath
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

System.setProperty("java.awt.headless", "true")

val configFile = rootProject.file("config/scenes.json")
val outDir = layout.buildDirectory.dir("generated/scenes").get().asFile
val manifestOut = File(outDir, "manifest/AndroidManifest.xml")
val resOut = File(outDir, "res")
val assetsOut = File(outDir, "assets")

data class Scene(val id: String, val title: String, val subtitle: String, val color: Color, val tile: Boolean)

fun fail(msg: String): Nothing = throw GradleException("config/scenes.json: $msg")

@Suppress("UNCHECKED_CAST")
fun readScenes(): List<Scene> {
    val root = try {
        JsonSlurper().parse(configFile) as Map<String, Any?>
    } catch (e: Exception) {
        fail("not valid JSON (${e.message})")
    }
    val raw = root["scenes"] as? List<Map<String, Any?>> ?: fail("missing \"scenes\" list")
    if (raw.isEmpty()) fail("\"scenes\" is empty")
    val seen = mutableSetOf<String>()
    return raw.mapIndexed { i, s ->
        val id = s["id"] as? String ?: fail("scene #${i + 1} has no \"id\"")
        if (!Regex("[a-z][a-z0-9_]*").matches(id)) fail("id \"$id\" must be lowercase letters, digits and _")
        if (!seen.add(id)) fail("id \"$id\" is used twice")
        val title = s["title"] as? String ?: fail("scene \"$id\" has no \"title\"")
        val url = s["url"] as? String ?: fail("scene \"$id\" has no \"url\"")
        if (!url.startsWith("http")) fail("scene \"$id\" url must start with http")
        val color = try {
            Color.decode((s["color"] as? String) ?: "#C2410C")
        } catch (e: Exception) {
            fail("scene \"$id\" color must look like #C2410C")
        }
        Scene(id, title, (s["subtitle"] as? String) ?: "", color, (s["tile"] as? Boolean) ?: true)
    }
}

fun xml(s: String) = s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")

fun blend(a: Color, b: Color, t: Float) = Color(
    (a.red + (b.red - a.red) * t).toInt(),
    (a.green + (b.green - a.green) * t).toInt(),
    (a.blue + (b.blue - a.blue) * t).toInt(),
)

fun flame(cx: Float, base: Float, w: Float, h: Float): GeneralPath = GeneralPath().apply {
    moveTo(cx, base)
    curveTo(cx - w, base, cx - w * 0.9f, base - h * 0.55f, cx - w * 0.15f, base - h * 0.75f)
    curveTo(cx - w * 0.3f, base - h * 0.9f, cx + w * 0.05f, base - h * 0.95f, cx, base - h)
    curveTo(cx + w * 0.55f, base - h * 0.7f, cx + w, base - h * 0.35f, cx + w * 0.85f, base - h * 0.15f)
    curveTo(cx + w * 0.75f, base, cx + w * 0.3f, base, cx, base)
    closePath()
}

fun drawFire(g: java.awt.Graphics2D, cx: Float, base: Float, scale: Float) {
    val layers = listOf(
        Triple(Color(0xEA, 0x58, 0x0C), 1.0f, 1.0f),
        Triple(Color(0xF9, 0x73, 0x16), 0.72f, 0.78f),
        Triple(Color(0xFB, 0xBF, 0x24), 0.45f, 0.52f),
        Triple(Color(0xFE, 0xF3, 0xC7), 0.22f, 0.28f),
    )
    for ((c, ws, hs) in layers) {
        g.color = c
        g.fill(flame(cx, base, 38f * scale * ws, 120f * scale * hs))
    }
    g.color = Color(0x3B, 0x22, 0x14)
    g.stroke = BasicStroke(12f * scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    g.drawLine((cx - 55 * scale).toInt(), (base + 6 * scale).toInt(), (cx + 55 * scale).toInt(), (base - 2 * scale).toInt())
    g.drawLine((cx - 50 * scale).toInt(), (base - 2 * scale).toInt(), (cx + 52 * scale).toInt(), (base + 8 * scale).toInt())
}

fun paintBackground(g: java.awt.Graphics2D, w: Int, h: Int, accent: Color, glowX: Float) {
    val dark = Color(0x14, 0x0C, 0x08)
    g.paint = GradientPaint(0f, 0f, dark, 0f, h.toFloat(), blend(dark, accent, 0.45f))
    g.fillRect(0, 0, w, h)
    g.paint = RadialGradientPaint(
        Point2D.Float(glowX, h * 0.95f), w * 0.55f,
        floatArrayOf(0f, 1f),
        arrayOf(Color(accent.red, accent.green, accent.blue, 170), Color(accent.red, accent.green, accent.blue, 0)),
    )
    g.fillRect(0, 0, w, h)
}

fun renderBanner(title: String, subtitle: String, accent: Color, file: File) {
    val w = 640
    val h = 360
    val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    val g = img.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    paintBackground(g, w, h, accent, w * 0.8f)
    drawFire(g, w * 0.8f, h * 0.78f, 1.55f)

    // Title, shrunk until it fits beside the flame.
    var size = 60
    var font = Font(Font.SANS_SERIF, Font.BOLD, size)
    while (g.getFontMetrics(font).stringWidth(title) > w * 0.62 && size > 30) {
        size -= 2
        font = Font(Font.SANS_SERIF, Font.BOLD, size)
    }
    g.font = font
    g.color = Color(0xFF, 0xF7, 0xED)
    g.drawString(title, 36, (h * 0.52).toInt())
    if (subtitle.isNotEmpty()) {
        g.font = Font(Font.SANS_SERIF, Font.PLAIN, 26)
        g.color = Color(0xFF, 0xED, 0xD5, 190)
        g.drawString(subtitle, 38, (h * 0.52).toInt() + 44)
    }
    g.dispose()
    file.parentFile.mkdirs()
    ImageIO.write(img, "png", file)
}

fun renderIcon(file: File) {
    val s = 192
    val img = BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB)
    val g = img.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    paintBackground(g, s, s, Color(0xC2, 0x41, 0x0C), s / 2f)
    drawFire(g, s / 2f, s * 0.8f, 1.1f)
    g.dispose()
    file.parentFile.mkdirs()
    ImageIO.write(img, "png", file)
}

fun manifest(scenes: List<Scene>): String {
    val aliases = scenes.filter { it.tile }.joinToString("\n") { s ->
        """
        <activity-alias
            android:name=".tile.${s.id}"
            android:exported="true"
            android:label="${xml(s.title)}"
            android:banner="@drawable/banner_${s.id}"
            android:icon="@drawable/app_icon"
            android:targetActivity=".SceneActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
            <meta-data android:name="scene_id" android:value="${s.id}" />
        </activity-alias>""".trimEnd()
    }
    return """<?xml version="1.0" encoding="utf-8"?>
<!-- Generated from config/scenes.json by app/scenes.gradle.kts. Do not edit. -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-feature android:name="android.software.leanback" android:required="false" />
    <uses-feature android:name="android.hardware.touchscreen" android:required="false" />

    <application
        android:allowBackup="false"
        android:banner="@drawable/banner_menu"
        android:icon="@drawable/app_icon"
        android:label="Fireplace"
        android:theme="@android:style/Theme.DeviceDefault.NoActionBar.Fullscreen">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="Fireplace">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".SceneActivity"
            android:excludeFromRecents="true"
            android:exported="false"
            android:noHistory="true"
            android:theme="@android:style/Theme.NoDisplay" />
$aliases
    </application>
</manifest>
"""
}

val scenes = readScenes()
outDir.deleteRecursively()
manifestOut.parentFile.mkdirs()
manifestOut.writeText(manifest(scenes))
renderIcon(File(resOut, "drawable-xxxhdpi/app_icon.png"))
renderBanner("Fireplace", "All scenes", Color(0xC2, 0x41, 0x0C), File(resOut, "drawable-xhdpi/banner_menu.png"))
scenes.filter { it.tile }.forEach { s ->
    renderBanner(s.title, s.subtitle, s.color, File(resOut, "drawable-xhdpi/banner_${s.id}.png"))
}
assetsOut.mkdirs()
configFile.copyTo(File(assetsOut, "scenes.json"), overwrite = true)

extra["scenesManifest"] = manifestOut
extra["scenesRes"] = resOut
extra["scenesAssets"] = assetsOut
