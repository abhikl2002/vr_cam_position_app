package com.app.vrcamapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY: Long = 2000 // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        // Load fade-out animation
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        // Apply fade-out animation to the logo ImageView
        val logoImageView = findViewById<ImageView>(R.id.logoImageView)
        logoImageView.startAnimation(fadeInAnimation)

        // Start the MainActivity after a delay
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }, SPLASH_DELAY)
    }
}
