package project.moms.attractions.presentation.partWithMap

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import project.moms.attractions.data.api.LandmarksApiService
import project.moms.attractions.model.Element
import project.moms.attractions.model.LandmarkResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapViewModel(
    private val apiService: LandmarksApiService
) : ViewModel() {

    val landmarkData: MutableLiveData<List<Element>> by lazy {
        MutableLiveData<List<Element>>()
    }

    fun fetchLandmarks() {
        // Создание тела запроса с использованием переданных координат и выполнение запроса
        val requestBody = createRequestBody()
        val call = apiService.getLandmarks(requestBody)

        call.enqueue(object : Callback<LandmarkResponse> {
            override fun onResponse(
                call: Call<LandmarkResponse>,
                response: Response<LandmarkResponse>
            ) {
                if (response.isSuccessful) {
                    val landmarkResponse = response.body()
                    landmarkResponse?.elements?.let {
                        landmarkData.value = it // передаем данные в LiveData
                    }
                } else {
                    // Handle error
                    Log.e("MainActivity", "Failed to fetch landmarks: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<LandmarkResponse>, t: Throwable) {
                // Handle network error
                Log.e("MainActivity", "Failed to fetch landmarks: ${t.message}")
            }
        })
    }

    private fun createRequestBody(): RequestBody {
        val query = "[out:json];node[\"tourism\"=\"museum\"](55.55,37.35,55.95,37.85);out;"
        return RequestBody.create("application/x-www-form-urlencoded"
            .toMediaTypeOrNull(), "data=$query")
    }

}