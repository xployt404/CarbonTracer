package com.example.carbontracerrevised

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class HelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.help_activity_layout)

        val closeButton = findViewById<Button>(R.id.closeButton)
        closeButton.setOnClickListener {
            finish() // Close the activity
        }
    }
}
