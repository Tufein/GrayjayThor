package com.futo.platformplayer.thor

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.futo.platformplayer.R
import com.futo.platformplayer.Settings
import com.futo.platformplayer.activities.MainActivity
import com.futo.platformplayer.logging.Logger
import com.futo.platformplayer.stores.FragmentedStorage

/** Place the complete Grayjay interface on the Thor's lower display. */
class ThorLauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // MainActivity normally initializes storage. The launcher must read saved screen settings first.
        if (!FragmentedStorage.isInitialized) FragmentedStorage.initialize(filesDir)
        val main = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val layout = if (Settings.instance.thor.enabled) ThorDisplaySession.layout(this) else null
        try {
            if (layout != null) {
                startActivity(main, ActivityOptions.makeBasic().setLaunchDisplayId(layout.controls).toBundle())
            } else {
                startActivity(main)
            }
        } catch (e: RuntimeException) {
            Logger.w("ThorLauncher", "Cannot launch controls on the secondary display", e)
            Toast.makeText(this, R.string.thor_output_unavailable, Toast.LENGTH_LONG).show()
            startActivity(main)
        }
        finish()
    }
}
