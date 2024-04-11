package project.moms.attractions.presentation.partWithMap

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import project.moms.attractions.R
import project.moms.attractions.data.api.NetworkApi
import project.moms.attractions.databinding.FragmentMapBinding
import project.moms.attractions.model.Element

class FragmentMap : Fragment() {
    private var _binding : FragmentMapBinding? = null
    private val binding : FragmentMapBinding
        get() {return _binding!!}
    private val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map ->
        if (map.values.all { it }) {
            showMyLocation()
        } else {
            Toast.makeText(requireContext(), "permission is not Granted", Toast.LENGTH_SHORT).show()
        }
    }

    private lateinit var mapView: MapView
    private lateinit var mapObjects: MapObjectCollection
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var viewModel: MapViewModel

    private var saveLatitude = 0.0
    private var saveLongitude = 0.0
    private var saveZoom = 0.0f
    private var routeStartLocation = Point(0.0, 0.0)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMapBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.mapView)
        mapObjects = mapView.map.mapObjects.addCollection()

        fusedClient = LocationServices.getFusedLocationProviderClient(requireContext())

        viewModel = MapViewModel(NetworkApi.apiService)
        viewModel.landmarkData.observe(viewLifecycleOwner, Observer { landmarks ->
            landmarks?.let {
                addMarkers(it)
            }
        })

        viewModel.fetchLandmarks()

        if (savedInstanceState != null) {
            saveLatitude = savedInstanceState.getDouble(SAVE_LATITUDE)
            saveLongitude = savedInstanceState.getDouble(SAVE_LONGITUDE)
            saveZoom = savedInstanceState.getFloat(SAVE_ZOOM)
            routeStartLocation = Point(saveLatitude, saveLongitude)
            cameraSavePosition()
        }

        binding.locationButton.setOnClickListener {
            showMyLocation()
        }

        binding.enlargeButton.setOnClickListener { changeZoom(1f) }
        binding.reduceButton.setOnClickListener { changeZoom(-1f) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putDouble(SAVE_LATITUDE, saveLatitude)
        outState.putDouble(SAVE_LONGITUDE, saveLongitude)
        outState.putFloat(SAVE_ZOOM, saveZoom)
    }

    private fun cameraSavePosition() {
        mapView.mapWindow.map.move(
            CameraPosition(
                Point(saveLatitude, saveLongitude),
                saveZoom,
                0f,
                0f
            )
        )
    }

    private fun changeZoom(delta: Float) {
        val currentZoom = mapView.map.cameraPosition.zoom
        mapView.map.move(
            CameraPosition(mapView.map.cameraPosition.target, currentZoom + delta, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 1f), null
        )
    }

    private fun checkPermissions() {
            val isAllGranted = REQUEST_PERMISSIONS.all { permission ->
                ContextCompat.checkSelfPermission(requireContext(), permission) ==
                        PackageManager.PERMISSION_GRANTED
            }
            if (isAllGranted) {
                Toast.makeText(requireContext(), "permission is Granted", Toast.LENGTH_SHORT).show()
            } else {
                launcher.launch(REQUEST_PERMISSIONS)
            }
        }

    private fun showMyLocation() {
        val permissionsToRequest = REQUEST_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) !=
                    PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isEmpty()) {
            fusedClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val point = Point(it.latitude, it.longitude)
                    val namePlace = "My location"

                    // Записываем значения в наши константы для сохранения положения карты
                    saveLatitude = it.latitude
                    saveLongitude = it.longitude
                    saveZoom = 11.0f


                    mapView.map.move(
                        CameraPosition(
                            point,
                            11.0f,
                            0.0f,
                            0.0f
                        ), Animation(Animation.Type.SMOOTH, 3f), null
                    )

                    val currentLocation = Element(
                        type = "currentLocation",
                        id = 0L,
                        lat = it.latitude,
                        lon = it.longitude,
                        tags = mapOf()
                    )
                    addPlaceMark(point, namePlace, currentLocation)
                }
            }
        } else {
            launcher.launch(permissionsToRequest)
        }
    }

    private fun addMarkers(landmarks: List<Element>) {
        for (landmark in landmarks) {
            addPlaceMarkIfValid(landmark)
        }
        setupMarkerTapListener()
    }

    private fun addPlaceMarkIfValid(element: Element) {
        val name = element.tags["name:en"] ?: "Unknown"
        if (name != "Unknown") {
            val latitude = element.lat
            val longitude = element.lon
            val markerName = name

            val point = Point(latitude, longitude)
            addPlaceMark(point, markerName, element)
        }
    }

    private fun setupMarkerTapListener() {
        mapObjects.addTapListener { mapObject, _ ->
            if (mapObject is PlacemarkMapObject) {
                val userData = mapObject.userData as? Element
                userData?.let { sendMarker(it) }
                true
            } else {
                false
            }
        }
    }

    private fun sendMarker(item: Element) {
        val bundle = Bundle().apply {
            putParcelable(FragmentFullScreenItem.KEY_MARKER, item)
        }
        findNavController().navigate(R.id.action_fragmentMap_to_fragmentFullScreenItem, bundle)
    }

    private fun addPlaceMark(point: Point, namePlace: String, element: Element) {
        val placeMark = mapObjects.addPlacemark(point)
        placeMark.opacity = 0.5f

        val bitmap = getBitmapFromVectorDrawable(requireContext(), R.drawable.marker)
        val icon = ImageProvider.fromBitmap(bitmap)
        placeMark.setIcon(icon)

        placeMark.userData = element
    }

    private fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, drawableId)
        drawable?.let {
            val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth,
                drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)

            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }
        return null
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        MapKitFactory.getInstance().onStart()
        checkPermissions()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val REQUEST_PERMISSIONS: Array<String> = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }.toTypedArray()

        private const val SAVE_LATITUDE = "latitude"
        private const val SAVE_LONGITUDE = "longitude"
        private const val SAVE_ZOOM = "zoom"
    }
}