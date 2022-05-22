package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : OnMapReadyCallback, BaseFragment() {
    private val TAG: String = javaClass.simpleName

    private val PERMISSIONS_REQUEST_CODE = 4321

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

        // taking permissions

//        TODO: zoom to the user location
//        TODO: add style to the map
//        TODO: put a marker to location that the user selected


//        TODO: call this function after the user confirms on the selected location
        onLocationSelected()

        return binding.root
    }

    private fun onLocationSelected() {
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // TODO: Change the map type based on the user's selection.
        R.id.normal_map -> {
            true
        }
        R.id.hybrid_map -> {
            true
        }
        R.id.satellite_map -> {
            true
        }
        R.id.terrain_map -> {
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onMapReady(p0: GoogleMap?) {
        p0?.let {
            googleMap = p0
            checkPermissions()
            mapStyle()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkPermissions() {
        // will contain missing permission that will be requested
        val permissionsToRequest : MutableList<String> = mutableListOf()

        val isAccessFineLocGranted =
            PackageManager.PERMISSION_GRANTED ==
                ActivityCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)

        if (!isAccessFineLocGranted)
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)

        val isBackgroundLocGranted =
            PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION)

        if (!isBackgroundLocGranted)
            permissionsToRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

        Log.d(TAG, "ACCESS_FINE_LOCATION granted: $isAccessFineLocGranted")
        Log.d(TAG, "ACCESS_BACKGROUND_LOCATION granted: $isBackgroundLocGranted")

        if (permissionsToRequest.isNotEmpty())
            requestPermissions(permissionsToRequest)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode ==  PERMISSIONS_REQUEST_CODE
        ) {
            Snackbar.make(
                binding.constraintLayout,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_LONG
            )
            .show()
        } else {
            Log.e(TAG,"Request code: $PERMISSIONS_REQUEST_CODE")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestPermissions(permissionsToRequest: MutableList<String>) {

        ActivityCompat.requestPermissions(
            requireActivity(),
            permissionsToRequest.toTypedArray(),
            PERMISSIONS_REQUEST_CODE
        )
    }

    private fun mapStyle() {
        try {
            val loadedMap = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireActivity(),
                    R.raw.map_style
                )
            )

            Log.i(
                "TEST",
                "SelectLocationFragment.mapStyle: $loadedMap"
            )

        } catch (e: Resources.NotFoundException) {
            Log.e(
                "TEST",
                "SelectLocationFragment.mapStyle ${e.message}"
            )
        }
    }

}
