package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.provider.Settings
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
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

class SelectLocationFragment : OnMapReadyCallback, BaseFragment() {
    private val TAG: String = javaClass.simpleName

    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

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
        binding.buttonSave.visibility = View.INVISIBLE
    }

    private fun onLocationSelected(poi: PointOfInterest) {
        // When the user confirms on the selected location, send back the selected location
        // details to the view model and navigate back to the previous fragment to save the
        // reminder and add the geofence
        val latLng = poi.latLng
        _viewModel.reminderSelectedLocationStr.value = poi.name
        _viewModel.latitude.value = latLng.latitude
        _viewModel.longitude.value = latLng.longitude
        findNavController().popBackStack()
    }


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

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onMapReady(p0: GoogleMap?) {
        p0?.let {
            googleMap = p0

            // get permission as in https://classroom.udacity.com/nanodegrees/nd940/parts/cd0638/modules/8b797530-97b5-414f-9942-36a96cabeb5b/lessons/60b582bc-211b-4a9d-9bb8-f0ae348eedf4/concepts/b8180ae1-34e5-4cb4-92bd-9dbb930c3f25
            foregroundAndBackgroundLocationPermission()

            // set map to current position
            // permissions are set in the previous function call
            fusedLocationProviderClient.lastLocation.addOnSuccessListener(requireActivity()) { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    val zoomLevel = 15f
                    googleMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            userLatLng,
                            zoomLevel
                        )
                    )
                }
            }

            mapStyle()
            addMapClickListener(googleMap)
            addPoi(googleMap)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.d(TAG,"Request code received: $requestCode")

        if (grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED)
        ) {
            Snackbar.make(
                binding.constraintLayout,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            ).setAction(R.string.settings) {
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.show()
        } else {
            Log.e("onRequestPermissionsRes","onRequestPermissionsResult else called")
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            Log.e(TAG,"onActivityResult REQUEST_TURN_DEVICE_LOCATION_ON ")
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
                    Log.e("StartGeofence"," exception.startResolutionForResult")
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
                    Log.e("StartGeofence","Error getting location settings resolution: ${sendEx.message}")
                }
            } else {
                Log.e("StartGeofence","Error getting location settings resolution: showing snackbar")
                Snackbar.make(
                    binding.constraintLayout,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    Log.e("StartGeofence","setAction oK")
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

    private fun addMapClickListener(map: GoogleMap) {
        map.setOnMapClickListener { latLng ->
            binding.buttonSave.visibility = View.VISIBLE
            binding.buttonSave.setOnClickListener {
                _viewModel.latitude.value = latLng.latitude
                _viewModel.longitude.value = latLng.longitude
                _viewModel.reminderSelectedLocationStr.value = getString(R.string.custom_location_used)
                findNavController().popBackStack()
            }

            // move center of screen and zoom in
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 12f)
            map.moveCamera(cameraUpdate)

            val poiMarker = map.addMarker(MarkerOptions().position(latLng))
            poiMarker.showInfoWindow()
        }
    }

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermission() {
        Log.e("foregroundBackground","foregroundAndBackgroundLocationPermissionApproved is ${foregroundAndBackgroundLocationPermissionApproved()}")
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
        }
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        Log.e("runningQOrLater","runningQOrLater is $runningQOrLater")
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else ->
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        Log.e("resultCode","resultCode is $resultCode")
        ActivityCompat.requestPermissions(
            requireActivity(),
            permissionsArray,
            resultCode
        )
    }

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved =
            (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ))
        val backgroundLocationApproved =
            if (runningQOrLater) {

                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundLocationApproved
    }

    private fun addPoi(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            binding.buttonSave.visibility = View.VISIBLE
            binding.buttonSave.setOnClickListener {
                onLocationSelected(poi)
            }

            val poiMarker = map.addMarker(
                MarkerOptions().position(poi.latLng).title(poi.name)
            )

            poiMarker.showInfoWindow()
        }
    }

}
