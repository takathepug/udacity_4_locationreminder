package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

class SelectLocationFragment : OnMapReadyCallback, GoogleMap.OnMapClickListener, BaseFragment() {
    private val TAG: String = javaClass.simpleName

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater, R.layout.fragment_select_location, container, false
            )

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        // add the map setup implementation
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        // save location button starts invisible
        binding.buttonSave.visibility = View.GONE
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onMapReady(p0: GoogleMap?) {
        p0?.let {
            googleMap = p0

            googleMap.setOnMapClickListener(this)

            ensurePermissionCenterCameraOnCurrentLocation()

            mapStyle()

            addPoiClickListener(googleMap)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            Log.e(TAG, "onActivityResult $REQUEST_TURN_DEVICE_LOCATION_ON ")
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    Log.e("StartGeofence", " exception.startResolutionForResult", exception)
                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        REQUEST_TURN_DEVICE_LOCATION_ON,
                        null,
                        0,
                        0,
                        0,
                        null
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.e(
                        "StartGeofence",
                        "Error getting location settings resolution: ${sendEx.message}", sendEx
                    )
                }
            } else {
                Log.e(
                    "StartGeofence",
                    "Error getting location settings resolution: showing snackbar"
                )

                Snackbar.make(
                    binding.constraintLayout,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    Log.e("StartGeofence", "setAction oK")
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }

        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                googleMap.isMyLocationEnabled = true
            }
        }
    }

    private fun mapStyle() {
        try {
            val loadedMap = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireActivity(),
                    R.raw.map_style
                )
            )

            Log.i("TEST", "SelectLocationFragment.mapStyle: $loadedMap")

        } catch (e: Resources.NotFoundException) {
            Log.e("TEST", "SelectLocationFragment.mapStyle ${e.message}")
        }
    }

    override fun onMapClick(p0: LatLng?) {
        binding.buttonSave.visibility = View.VISIBLE

        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        val addresses = p0?.let { geocoder.getFromLocation(it.latitude, p0.longitude, 1) }

        // text for place id unless and address is retrieved
        var placeId = getString(R.string.custom_location_used)
        if (addresses != null && addresses.isNotEmpty()) {
            placeId = addresses[0].getAddressLine(0)
        }

        val poi = PointOfInterest(p0, null, placeId)

        binding.buttonSave.setOnClickListener {
            _viewModel.selectedPOI.value = poi
            findNavController().popBackStack()
        }

        // move center of screen and zoom in
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(p0, 18f)
        googleMap.moveCamera(cameraUpdate)

        val poiMarker = googleMap.addMarker(p0?.let {
            MarkerOptions().position(it).title(poi.name)
        })

        poiMarker.showInfoWindow()
    }

    private fun addPoiClickListener(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            Log.d(TAG, "Adding POI: $poi")

            binding.buttonSave.visibility = View.VISIBLE

            val poiMarker = map.addMarker(
                MarkerOptions().position(poi.latLng).title(poi.name)
            )

            poiMarker.showInfoWindow()

            binding.buttonSave.setOnClickListener {
                _viewModel.selectedPOI.value = poi
                findNavController().popBackStack()
            }
        }
    }

    // permissions
    @TargetApi(29)
    private fun ensurePermissionCenterCameraOnCurrentLocation() {
        // https://developer.android.com/training/permissions/requesting
        when {
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // enable my-location layer
                googleMap.isMyLocationEnabled = true
                checkLocationServiceCenterCameraOnCurrentLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    R.string.location_required_error,
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(android.R.string.ok) {
                        requestPermissions(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
                        )
                    }.show()
            }
            else -> {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
                )
            }
        }
    }

    private fun checkLocationServiceCenterCameraOnCurrentLocation() {
        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
        // build location settings request
        val locationSettingsRequest: LocationSettingsRequest = LocationSettingsRequest.Builder().addLocationRequest(
            LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_LOW_POWER
            }
        ).build()

        val locationSettingsResponseTask: Task<LocationSettingsResponse> =
            LocationServices.getSettingsClient(requireActivity()).checkLocationSettings(locationSettingsRequest);

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Show the dialog by calling startIntentSenderForResult and check the result
                // in onActivityResult().
                startIntentSenderForResult(
                    exception.resolution.intentSender,
                    REQUEST_TURN_DEVICE_LOCATION_ON, null, 0, 0, 0, null
                )
            } else {
                // some other error, inform user and repeat request
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkLocationServiceCenterCameraOnCurrentLocation()
                }.show()
            }
        }

        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                checkPermissionCenterCameraOnCurrentLocation()
            } else {
                Log.e(TAG, "Error in LocationSettingsResponseTask")
            }
        }
    }

    private fun checkPermissionCenterCameraOnCurrentLocation() {

        try {
            // check permissions
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // get last location and zoom on it
                fusedLocationProviderClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            googleMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(location.latitude, location.longitude),
                                    16f
                                )
                            )
                        } else {
                            Log.e(TAG, "Could not obtain location")
                        }
                    }.addOnFailureListener {
                        Log.e(TAG, "Could not obtain location", it)
                    }
            } else {
                // warn user that location is required
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    R.string.error_location_permission_not_added,
                    Snackbar.LENGTH_INDEFINITE
                ).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        Log.d(TAG, "Request code received: $requestCode")

        when (requestCode) {
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE -> {
                // Check if location permissions are granted and if so enable the
                // location data layer.
                if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    ensurePermissionCenterCameraOnCurrentLocation()
                } else {
                    // Show messages to telling the user why your app actually requires the location permission.
                    // In case they previously chose "Deny & don't ask again",
                    // tell your users where to manually enable the location permission.
                    Snackbar.make(
                        requireActivity().findViewById(android.R.id.content),
                        R.string.error_location_permission_not_added,
                        Snackbar.LENGTH_INDEFINITE
                    )
                        .setAction(R.string.settings) {
                            startActivity(Intent().apply {
                                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        }.show()
                }
            }
        }
    }

    // options menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
