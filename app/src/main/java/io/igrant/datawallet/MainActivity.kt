package io.igrant.datawallet

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import io.igrant.data_wallet.activity.InitializeActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val wallet = findViewById<Button>(R.id.wallet)

        wallet.setOnClickListener {
            val intent = Intent(this,InitializeActivity::class.java)
            startActivity(intent)
        }
    }
}