package com.example.weatherapp.viewmodel

import android.location.Geocoder
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.weatherapp.model.currentweather.CurrentWeather
import com.example.weatherapp.model.daily.DailyData
import com.example.weatherapp.model.forecast.Forecast
import com.example.weatherapp.model.hourly.HourlyData
import com.example.weatherapp.model.location.Location
import com.example.weatherapp.service.WeatherRepository
import com.example.weatherapp.util.Month
import com.example.weatherapp.util.WeatherIcon
import com.example.weatherapp.view.MainActivity
import java.util.Locale

class WeatherViewModel(
) : ViewModel() {
    var locationLiveData: MutableLiveData<Location> = MutableLiveData(null)
    var weatherLiveData: MutableLiveData<CurrentWeather> = MutableLiveData(null)
    var forecastLiveData: MutableLiveData<Forecast> = MutableLiveData(null)
    var hourlyLiveData: MutableLiveData<List<HourlyData>> = MutableLiveData(emptyList())
    var dailyLiveData: MutableLiveData<List<DailyData>> = MutableLiveData(emptyList())

    private fun setLocationData(locationData: MutableLiveData<Location>) {
        locationLiveData = locationData
    }

    private fun setCurrentWeatherData(weatherData: MutableLiveData<CurrentWeather>) {
        weatherLiveData = weatherData
    }

    private fun setForecastData(forecastData: MutableLiveData<Forecast>) {
        forecastLiveData = forecastData
    }

    private fun addHourlyLiveData(hourlyData: HourlyData) {
        val currentList = hourlyLiveData.value ?: mutableListOf()
        val updatedList = currentList.toMutableList()
        updatedList.add(hourlyData)
        hourlyLiveData.value = updatedList
    }

    private fun clearHourlyLiveData() {
        hourlyLiveData.value = emptyList()
    }

    private fun addDailyLiveData(dailyData: DailyData) {
        val currentList = dailyLiveData.value ?: mutableListOf()
        val updatedList = currentList.toMutableList()
        updatedList.add(dailyData)
        dailyLiveData.value = updatedList
    }

    private fun removeDailyLiveData(dailyData: DailyData) {
        val currentList = dailyLiveData.value ?: mutableListOf()
        val updatedList = currentList.toMutableList()
        if (updatedList.contains(dailyData)) {
            updatedList.remove(dailyData)
        }
        dailyLiveData.value = updatedList
    }

    private fun clearDailyLiveData() {
        dailyLiveData.value = emptyList()
    }

    fun callApiUpdateData(query: String) {
        // Update Location after Location API Call

        setLocationData(WeatherRepository.callLocation(query))

        // Update Weather after Weather API Call
        setCurrentWeatherData(WeatherRepository.callWeather(query))

        // Update Forecast after Forecast API Call
        setForecastData(WeatherRepository.callForecast(query))
    }

    fun updateHourlyLiveData() {
        clearHourlyLiveData()
        // create an arraylist of type hourly temp class
        for (i in 0..7) {
            addHourlyLiveData(
                HourlyData(
                    convertUTCtoCSTTime(
                        forecastLiveData.value!!.list[i].dt_txt.split(" ").get(1)
                    ),
                    convertUTCtoCSTDate(forecastLiveData.value!!.list[i].dt_txt),
                    WeatherIcon.fromIcon(forecastLiveData.value!!.list[i].weather.get(0).icon).resourceId,
                    forecastLiveData.value!!.list[i].main.temp_min.toString()
                        .substringBefore(".") + "°F",
                    forecastLiveData.value!!.list[i].main.temp_max.toString()
                        .substringBefore(".") + "°F"
                )
            )
        }
    }

    fun updateDailyLiveData() {
        clearDailyLiveData()
        //val dailyData_list = ArrayList<DailyData>()
        for (item in forecastLiveData.value!!.list) {
            //date for Daily Data data class
            val date = convertUTCtoCSTDate(item.dt_txt)

            //check if date exists
            val existingDate = dailyLiveData.value?.firstOrNull { it.date == date }

            if (existingDate != null) {
                // if yes update min and max temp, add icon
                // checks if min temp is lower than existing
                if (existingDate.minTemp.split("°").get(0).toDouble() > item.main.temp_min) {
                    // checks for max temp is higher than existing max temp
                    if (existingDate.maxTemp.split("°").get(0)
                            .toDouble() < item.main.temp_max
                    ) {
                        removeDailyLiveData(existingDate)
                        addDailyLiveData(
                            DailyData(
                                date,
                                WeatherIcon.fromIcon(item.weather.get(0).icon).resourceId,
                                item.main.temp_min.toString().substringBefore(".") + "°F",
                                item.main.temp_max.toString().substringBefore(".") + "°F"
                            )
                        )
                        //existing max temp is higher
                    } else {
                        removeDailyLiveData(existingDate)
                        addDailyLiveData(
                            DailyData(
                                date,
                                WeatherIcon.fromIcon(item.weather.get(0).icon).resourceId,
                                item.main.temp_min.toString().substringBefore(".") + "°F",
                                existingDate.minTemp
                            )
                        )
                    }
                }
            }
            //if not add date, min temp, max temp, add icon
            else {
                addDailyLiveData(
                    DailyData(
                        date,
                        WeatherIcon.fromIcon(item.weather.get(0).icon).resourceId,
                        item.main.temp_min.toString().substringBefore(".") + "°F",
                        item.main.temp_max.toString().substringBefore(".") + "°F"
                    )
                )
            }
        }
    }


    // Converts UTC Date to CST Date
    private fun convertUTCtoCSTDate(timeVal: String): String {
        val month = timeVal.split(" ")[0].split("-")[1]
        var date = timeVal.split(" ")[0].split("-")[2].toInt()
        val convertedTime = timeVal.split(" ")[1].split(":")[0].toInt().minus(6)
        if (convertedTime < 0) {
            date -= 1
        }
        //Log.v("DATE","DATE: "+ (Month.fromString(month) ?: "Invalid Month"))
        //val monthString = Month.fromString(month).toString()
        return Month.fromString(month).toString() + " " + date.toString()
    }

    // Convert UTC time to CST
    fun convertUTCtoCSTTime(timeVal: String): String {
        var utcTime = timeVal.split(":")[0].toInt().minus(6)

        if (utcTime < 0) {
            utcTime += 24
        }

        return when (utcTime) {
            0 -> "12:00 AM"
            12 -> "12:00 PM"
            in 13..24 -> utcTime.minus(12).toString() + ":00 PM"
            else -> "$utcTime:00 AM"
        }
    }

    fun getCityName(context: MainActivity, lat: Double, long: Double): String {
        // init Geocoder, gets location from latitude and longitude
        val address = Geocoder(context, Locale.getDefault()).getFromLocation(lat, long, 1)
        // Address in the form of: City, State, Country
        //Log.v("ADDRESS", "Address: " + location)
        return if (address != null) {
            address[0].locality + "," + address[0].adminArea + "," + address[0].countryCode
        } else "ERROR"
    }
}
