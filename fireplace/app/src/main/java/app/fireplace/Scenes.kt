package app.fireplace

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import org.json.JSONObject

data class Scene(val id: String, val title: String, val subtitle: String, val url: String)

data class Config(val players: List<String>, val scenes: List<Scene>)

object Scenes {
    private const val TAG = "Fireplace"

    fun load(context: Context): Config {
        val json = context.assets.open("scenes.json").bufferedReader().use { JSONObject(it.readText()) }
        val players = json.optJSONArray("players")?.let { a -> List(a.length()) { a.getString(it) } }.orEmpty()
        val list = json.getJSONArray("scenes")
        val scenes = List(list.length()) { i ->
            val s = list.getJSONObject(i)
            Scene(s.getString("id"), s.getString("title"), s.optString("subtitle"), s.getString("url"))
        }
        return Config(players, scenes)
    }

    /**
     * Hands the scene's YouTube link to the first installed player in the configured order
     * (SmartTube first, then the official YouTube app), then to anything that opens the link.
     */
    fun play(context: Context, config: Config, scene: Scene) {
        val uri = Uri.parse(scene.url)
        for (pkg in config.players) {
            if (start(context, Intent(Intent.ACTION_VIEW, uri).setPackage(pkg))) {
                Log.i(TAG, "Opened ${scene.id} in $pkg")
                return
            }
        }
        if (start(context, Intent(Intent.ACTION_VIEW, uri))) return
        Toast.makeText(context, "Install SmartTube or YouTube to play ${scene.title}", Toast.LENGTH_LONG).show()
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }
}
