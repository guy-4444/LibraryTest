package com.guy.librarytest

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.guy.librarytest.databinding.ActivityMainBinding
import com.guy2.simpleStockGraph.MyCalculator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        MyCalculator.add(4, 6);

        val stockPrices = floatArrayOf(
            100f, 101.5f, 99.8f, 102.2f, 103.1f, 101.9f, 104.0f, 105.6f, 104.8f, 106.3f,
            107.9f, 108.5f, 107.2f, 109.0f, 110.4f, 111.1f, 109.7f, 112.3f, 113.8f, 114.6f,
            113.2f, 115.1f, 116.7f, 117.3f, 118.9f, 117.8f, 119.6f, 121.0f, 120.2f, 122.5f,
            123.9f, 124.4f, 123.1f, 125.8f, 127.2f, 126.0f, 128.6f, 129.9f, 131.5f, 130.3f,
            132.8f, 134.1f, 133.0f, 131.4f, 129.8f, 128.2f, 126.9f, 124.7f, 123.5f, 121.9f
        )

//        binding.grpApple.setData(floatArrayOf(10f, 25f, 15f, 30f, 22f, 35f, 28f, 40f))
        binding.grpApple.setData(stockPrices)


        binding.grpGoogle.apply {
            barWidthRatio = 0.3f
            barCornerRadius = 3f
            setData(stockPrices)
        }

    }
}