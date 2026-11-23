package app.fireplace

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** The "Fireplace" tile: a remote-friendly list of every scene, including ones without a tile. */
class MainActivity : Activity() {

    private val prefs by lazy { getSharedPreferences("fireplace", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = Scenes.load(this)
        val lastId = prefs.getString("last_scene", null)

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(64), dp(40), dp(64), dp(40))
        }
        list.addView(TextView(this).apply {
            text = "Fireplace"
            setTextColor(Color.parseColor("#FFF7ED"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(8), 0, 0, dp(24))
        })

        var focusTarget: View? = null
        for (scene in config.scenes) {
            val row = sceneRow(scene) {
                prefs.edit().putString("last_scene", scene.id).apply()
                Scenes.play(this, config, scene)
            }
            list.addView(row)
            if (focusTarget == null || scene.id == lastId) focusTarget = row
        }

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#140C08"))
            addView(list, MATCH_PARENT, WRAP_CONTENT)
        })
        focusTarget?.requestFocus()
    }

    private fun sceneRow(scene: Scene, onClick: () -> Unit): View {
        val normal = rounded("#2A1A12")
        val focused = rounded("#C2410C")
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            isFocusable = true
            isClickable = true
            background = normal
            setPadding(dp(28), dp(18), dp(28), dp(18))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(12) }
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(context).apply {
                text = scene.title
                setTextColor(Color.parseColor("#FFF7ED"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
                typeface = Typeface.DEFAULT_BOLD
            })
            if (scene.subtitle.isNotEmpty()) addView(TextView(context).apply {
                text = scene.subtitle
                setTextColor(Color.parseColor("#FED7AA"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            })
            setOnFocusChangeListener { v, hasFocus ->
                v.background = if (hasFocus) focused else normal
                v.animate().scaleX(if (hasFocus) 1.02f else 1f).scaleY(if (hasFocus) 1.02f else 1f).setDuration(120).start()
            }
            setOnClickListener { onClick() }
        }
    }

    private fun rounded(color: String) = GradientDrawable().apply {
        setColor(Color.parseColor(color))
        cornerRadius = dp(14).toFloat()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
