package com.app.vrcamapp

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.StrictMode
import androidx.appcompat.app.AppCompatActivity
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import android.view.View
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import androidx.activity.enableEdgeToEdge
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView

class VideoPlayerActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var playerView1: PlayerView
    private lateinit var playerView2: PlayerView

    private var player1: ExoPlayer? = null
    private var player2: ExoPlayer? = null

    private lateinit var sensorManager: SensorManager
    private lateinit var rotationVector: Sensor

    private lateinit var udpSocket: DatagramSocket
    private lateinit var serverAddress: InetAddress
    private lateinit var serverIp: String
    private lateinit var serverPort: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Retrieve IP and port values from intent extras
        serverIp = intent.getStringExtra("ipAddress")!!
        serverPort = intent.getStringExtra("port") ?: "5005" // Default port

        // Keep the screen on
        window.addFlags(FLAG_KEEP_SCREEN_ON)
        // Set the activity to full screen and hide the notification bar
        // Hide the action bar
        supportActionBar?.hide()
        // Hide the notification bar (status bar)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)


        setContentView(R.layout.activity_video_player)

        playerView1 = findViewById(R.id.playerView1)
        playerView2 = findViewById(R.id.playerView2)

        // Disable user interaction on PlayerViews
        playerView1.useController = false
        playerView2.useController = false

        val rtspUrl = intent.getStringExtra("rtspUrl")

        // Log.d("VideoPlayerActivity", "RTSP URL: $rtspUrl")
        Log.d("VideoPlayerActivity", "IP Address: $serverIp")
        Log.d("VideoPlayerActivity", "Port: $serverPort")

        if (!rtspUrl.isNullOrEmpty()) {
            initializePlayer1(rtspUrl)
            initializePlayer2(rtspUrl)

        }


        // Initialize sensor manager and sensor
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)!!

        // Initialize UDP socket
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())

        try {
            udpSocket = DatagramSocket()
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
            return
        }

        // Start sending data
        startSending()
    }



    @androidx.annotation.OptIn(UnstableApi::class)
    @OptIn(UnstableApi::class)
    private fun initializePlayer1(rtspUrl: String) {
        val rtspMediaSourceFactory = RtspMediaSource.Factory()
        val mediaItem = MediaItem.fromUri(rtspUrl)
        val rtspMediaSource = rtspMediaSourceFactory.createMediaSource(mediaItem)

        player1 = ExoPlayer.Builder(this).build().apply {
            setMediaSource(rtspMediaSource)
            prepare()
            playWhenReady = true
        }

        playerView1.player = player1

        player1?.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                // Handle player error
            }
        })
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    @OptIn(UnstableApi::class)
    private fun initializePlayer2(rtspUrl: String) {
        val rtspMediaSourceFactory = RtspMediaSource.Factory()
        val mediaItem = MediaItem.fromUri(rtspUrl)
        val rtspMediaSource = rtspMediaSourceFactory.createMediaSource(mediaItem)

        player2 = ExoPlayer.Builder(this).build().apply {
            setMediaSource(rtspMediaSource)
            prepare()
            playWhenReady = true
        }

        playerView2.player = player2

        player2?.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                // Handle player error
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release resources
        player1?.release()
        player2?.release()
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor == rotationVector) {
            val rotationMatrix = FloatArray(9)
            val orientationAngles = FloatArray(3)
            android.hardware.SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            android.hardware.SensorManager.getOrientation(rotationMatrix, orientationAngles)

            // Convert radians to degrees
            var pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
            var roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
            var yaw = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()

            // Adjust to range from 0 to 360
            pitch = (pitch + 360) % 360
            roll = (roll + 360) % 360
            yaw = (yaw + 360) % 360

            sendData(pitch, roll, yaw)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Not used
    }

    private fun startSending() {
        Thread {
            while (true) {
                // No need to do anything here, as data is sent in onSensorChanged
                try {
                    Thread.sleep(50) // Adjust the delay between each sending iteration as needed
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    private fun sendData(pitch: Float, roll: Float, yaw: Float) {
        // Create data to send
        val data = "Orientation:\nOrientation:\nPitch: $pitch\nRoll: $roll\nYaw: $yaw"

        try {
            // Create UDP packet
            serverAddress = InetAddress.getByName(serverIp)
            val buffer = data.toByteArray()
            val port = serverPort.toInt() // Convert serverPort string to integer
            val packet = DatagramPacket(buffer, buffer.size, serverAddress, port)

            // Send UDP packet
            udpSocket.send(packet)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}
