package project.moms.attractions.presentation.partWithMap

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import project.moms.attractions.R
import project.moms.attractions.databinding.FragmentMapBinding

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMapBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = view.findViewById(R.id.mapView)
        mapObjects = mapView.map.mapObjects.addCollection()

        fusedClient = LocationServices.getFusedLocationProviderClient(requireContext())

        binding.locationButton.setOnClickListener {
            showMyLocation()
        }
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
        // Проверяем все необходимые разрешения
        val permissionsToRequest = REQUEST_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) !=
                    PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isEmpty()) {
            fusedClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val point = Point(it.latitude, it.longitude)
                    val namePlace = "My location"
                    mapView.map.move(
                        CameraPosition(
                            point,
                            11.0f,
                            0.0f,
                            0.0f
                        ), Animation(Animation.Type.SMOOTH, 3f), null
                    )
                    addPlaceMark(point, namePlace)
                }
            }
        } else {
            launcher.launch(permissionsToRequest)
        }
    }

    private fun addPlaceMark(point: Point, namePlace: String) {
        val placeMark = mapObjects.addPlacemark(point)
        placeMark.opacity = 0.5f

        val bitmap = getBitmapFromVectorDrawable(requireContext(), R.drawable.marker)
        val icon = ImageProvider.fromBitmap(bitmap)
        placeMark.setIcon(icon)

        placeMark.userData = namePlace
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
    }
}