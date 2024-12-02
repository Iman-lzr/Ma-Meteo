package com.example.meteo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.meteo.Retrofit.RetrofitClient
import com.example.meteo.Retrofit.WeatherResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val apiKey = "daec2a125bd010f6f6d3f74976eb59d6"

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val localisationTextView = findViewById<TextView>(R.id.localisation)
        val weatherImage = findViewById<ImageView>(R.id.weatherImage)
        val temperatureTextView = findViewById<TextView>(R.id.temperature)
        val weatherConditionTextView = findViewById<TextView>(R.id.txt)
        val txtTem = findViewById<TextView>(R.id.txt_tem)
        val dateTextView = findViewById<TextView>(R.id.date)

        dateTextView.text = getCurrentDate().toString()

        swipeRefreshLayout = findViewById(R.id.main)
        swipeRefreshLayout.setOnRefreshListener {
            fetchWeatherData(
                localisationTextView,
                weatherImage,
                temperatureTextView,
                weatherConditionTextView,
                txtTem
            )
        }

        fetchWeatherData(localisationTextView, weatherImage, temperatureTextView, weatherConditionTextView, txtTem)
    }

    private fun fetchWeatherData(
        localisation: TextView,
        weatherImage: ImageView,
        temperature: TextView,
        condition: TextView,
        txtTem: TextView
    ) {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }


        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
            !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        ) {
            Toast.makeText(this, "Activez les services de localisation.", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
            swipeRefreshLayout.isRefreshing = false
            return
        }


        val location: Location? = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (location != null) {
            val latitude = location.latitude
            val longitude = location.longitude

            RetrofitClient.instance.getWeather(latitude, longitude, apiKey).enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                    if (response.isSuccessful) {
                        val weatherData = response.body()
                        weatherData?.let {
                            localisation.text = it.name
                            temperature.text = "${it.main.temp.toInt()}°C"
                            txtTem.text = "${it.main.temp.toInt()}°C"
                            val weather = it.weather[0].main

                            when {
                                weather.contains("Rain", true) -> {
                                    condition.text = "Cold Day"
                                    weatherImage.setImageResource(R.drawable.orage)
                                }
                                it.main.temp > 25 -> {
                                    condition.text = "Sunny Day"
                                    weatherImage.setImageResource(R.drawable.sunny)
                                }
                                else -> {
                                    condition.text = "Windy Day"
                                    weatherImage.setImageResource(R.drawable.nuageux)
                                }
                            }
                        }
                    }
                    swipeRefreshLayout.isRefreshing = false
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    t.printStackTrace()
                    Toast.makeText(this@MainActivity, "Erreur de connexion", Toast.LENGTH_LONG).show()
                    swipeRefreshLayout.isRefreshing = false
                }
            })
        } else {
            localisation.text = "Impossible de récupérer la localisation."
            swipeRefreshLayout.isRefreshing = false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchWeatherData(
                    findViewById(R.id.localisation),
                    findViewById(R.id.weatherImage),
                    findViewById(R.id.temperature),
                    findViewById(R.id.txt),
                    findViewById(R.id.txt_tem)
                )
            } else {
                Toast.makeText(this, "Permission refusée", Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }
}
