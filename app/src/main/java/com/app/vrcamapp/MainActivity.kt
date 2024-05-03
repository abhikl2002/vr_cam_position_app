package com.app.vrcamapp

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.StrictMode
import android.os.Vibrator
import android.util.Log
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var ipInput: EditText
    private lateinit var portInput: EditText
    private lateinit var sendButton: Button
    private lateinit var sensorValues: TextView
    private lateinit var orientationValues: TextView

    private lateinit var sensorManager: SensorManager
    private lateinit var rotationVector: Sensor

    private lateinit var udpSocket: DatagramSocket
    private lateinit var serverAddress: InetAddress
    private var serverPort: Int = 0

    private var sending: Boolean = false
    private lateinit var sendingThread: Thread
    private lateinit var videoView: VideoView
    private lateinit var urlEditText: EditText
    private lateinit var vibrator: Vibrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        ipInput = findViewById(R.id.ipInput)
        portInput = findViewById(R.id.portInput)
        sendButton = findViewById(R.id.sendButton)
        sensorValues = findViewById(R.id.sensorValues)
        orientationValues = findViewById(R.id.orientationValues)

        // Initialize sensor manager and sensor
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)!!

        // Initialize UDP socket
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())

        try {
            udpSocket = DatagramSocket()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        sendButton.setOnClickListener {
            // Get IP and port from input fields
            val ip = ipInput.text.toString()
            val portText = portInput.text.toString()

            performHapticFeedback() // Perform haptic feedback
            playClickSound()
            if (!sending) {
                startSending()
            } else {
                stopSending()
            }
            if (ip.isEmpty() || portText.isEmpty()) {
                // Handle the case where IP or port is empty (e.g., show a toast message)
                Toast.makeText(this@MainActivity, "Please enter IP address and port", Toast.LENGTH_SHORT).show()

            }

        }

        // Initialize views
        urlEditText = findViewById(R.id.urlEditText)
        val startButton: Button = findViewById(R.id.startButton)

        // Initialize vibrator
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        // Set click listener for startButton
        startButton.setOnClickListener {
            performHapticFeedback() // Perform haptic feedback
            playClickSound()
            val rtspUrl = urlEditText.text.toString().trim()
            startStreaming(rtspUrl)
        }
    }

    private fun startStreaming(rtspUrl: String) {
        // Check if URL is not empty
        if (rtspUrl.isNotEmpty()) {
            val countdownDuration = 10 // Countdown from 10 seconds

            val countDownTimer = object : CountDownTimer((countdownDuration * 1000).toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val secondsRemaining = millisUntilFinished / 1000
                    val message = "The RTSP streaming will start in $secondsRemaining seconds.\nPlace your smartphone in the VR box."
                    // Show the countdown message
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                }

                override fun onFinish() {
                    Log.d("MainActivity", "Count down")
                }
            }

            countDownTimer.start()

            val delayMillis = 21 * 1000L

            // Get the main looper's Handler
            val handler = Handler(Looper.getMainLooper())

            // Post a delayed task to the handler
            handler.postDelayed({
                Toast.makeText(this@MainActivity, "Start streaming", Toast.LENGTH_SHORT).show()
                startStreamingAfterCountdown(rtspUrl)
            }, delayMillis)

        } else {
            Log.e("MainActivity", "RTSP URL is empty")
            Toast.makeText(this@MainActivity, "Please enter a rtsp url", Toast.LENGTH_SHORT).show()
        }
    }




    private fun startStreamingAfterCountdown(rtspUrl: String) {
        // Extract IP address from RTSP URL
        val IP = extractIpAddress(rtspUrl)

        // Get IP address and port
        val ipAddress = ipInput.text.toString().trim()
        val port = portInput.text.toString().trim()

        // Start VideoPlayerActivity with the provided RTSP URL
        val intent = Intent(this, VideoPlayerActivity::class.java)
        intent.putExtra("rtspUrl", rtspUrl)

        if (ipAddress.isNotEmpty()) {
            intent.putExtra("ipAddress", ipAddress)
        } else {
            intent.putExtra("ipAddress", IP)
            Log.d("MainActivity", "ipaddres:$IP")
        }

        if (port.isNotEmpty()) {
            intent.putExtra("port", port)
        } else {
            Log.d("MainActivity", "port: 5005")
        }

        // Pass sensor data as extra to the intent
        val orientationData = orientationValues.text.toString()
        intent.putExtra("sensorData", orientationData)

        startActivity(intent)
        Log.d("MainActivity", "Video playback started")
    }


    private fun extractIpAddress(rtspUrl: String): String {
        val ipAddressPattern = "(?<=rtsp://)(.*?)(?=:\\d)".toRegex()
        val matchResult = ipAddressPattern.find(rtspUrl)
        return matchResult?.value ?: ""
    }

    private fun performHapticFeedback() {
        // Check if device supports vibration
        if (vibrator.hasVibrator()) {
            // Vibrate for 100 milliseconds
            vibrator.vibrate(100)
        }
    }

    private fun playClickSound() {
        var mediaPlayer = MediaPlayer.create(this, R.raw.button_click)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            it.release()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop vibrator and release resources
        vibrator.cancel()
    }

    private fun startSending() {
        // Check if the input field is not empty
        val ip = ipInput.text.toString()
        val portText = portInput.text.toString()
        if (ip.isNotEmpty() || portText.isNotEmpty()) {
            sending = true
            sendButton.text = "Stop Sending"

            sendingThread = Thread {
                while (sending) {
                    sendData()
                    try {
                        Thread.sleep(100) // Adjust the delay between each sending iteration as needed
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }
            sendingThread.start()
        } else {
            // If the input field is empty, do nothing
            // You can add a message or handle the empty input field case here
        }
    }



    private fun stopSending() {
        sending = false
        sendButton.text = "Send UDP"
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
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            // Convert radians to degrees
            var pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
            var roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
            var yaw = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()

            // Adjust to range from 0 to 360
            pitch = (pitch + 360) % 360
            roll = (roll + 360) % 360
            yaw = (yaw + 360) % 360

            // Update TextView to display orientation values
            orientationValues.text = "Orientation:\nPitch: $pitch\nRoll: $roll\nYaw: $yaw"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Not used
    }

    private fun sendData() {
        // Get IP and port from input fields
        val ip = ipInput.text.toString()
        val portText = portInput.text.toString()

        // Check if IP and port are empty
        if (ip.isEmpty() || portText.isEmpty()) {
            // Handle the case where IP or port is empty (e.g., show a toast message)

            runOnUiThread {
                //Toast.makeText(this@MainActivity, "Please enter IP address and port", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Convert port to integer
        val port = portText.toInt()

        // Get orientation values from TextView
        val orientationData = orientationValues.text.toString()

        // Create data to send
        val data = "Orientation:\n$orientationData"

        try {
            // Create UDP packet
            serverAddress = InetAddress.getByName(ip)
            val buffer = data.toByteArray()
            val packet = DatagramPacket(buffer, buffer.size, serverAddress, port)

            // Send UDP packet
            udpSocket.send(packet)
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle the exception (e.g., show a toast message)
            runOnUiThread {
                //Toast.makeText(this@MainActivity, "Failed to send data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
