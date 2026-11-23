package app.fireplace

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log

/**
 * Invisible activity behind every scene tile. Each tile is an activity-alias carrying a
 * "scene_id" meta-data value; this reads it, opens the scene and closes straight away.
 */
class SceneActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val sceneId = intent.component?.let { component ->
                packageManager.getActivityInfo(component, PackageManager.GET_META_DATA)
                    .metaData?.getString("scene_id")
            }
            val config = Scenes.load(this)
            val scene = config.scenes.firstOrNull { it.id == sceneId }
            if (scene != null) Scenes.play(this, config, scene)
            else Log.w("Fireplace", "No scene for tile $sceneId")
        } catch (e: Exception) {
            Log.e("Fireplace", "Could not open scene", e)
        }
        finish()
    }
}
