package com.app.meditec.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.app.meditec.R
import com.app.meditec.ui.maps.MapsActivity
import com.app.meditec.utils.PermissionUtils

class StartUpActivity : AppCompatActivity() {
    private lateinit var mGetStartedButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_startup)
        mGetStartedButton = findViewById(R.id.get_started_btn)
        if (PermissionUtils.isGoogleServicesAvailable(this))
            mGetStartedButton.setOnClickListener {
                startActivity(Intent(this, MapsActivity::class.java))
            }
    }

    companion object {
        private const val TAG = "StartUpActivity"
        const val ERROR_DIALOG_REQUEST = 401
    }
}