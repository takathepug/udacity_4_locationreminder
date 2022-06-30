package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.validateLatLon
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    private val TAG: String = javaClass.simpleName

    companion object {
        private val GEOFENCE_RADIUS_IN_METERS: Float = 100f
        val ACTION_GEOFENCE_EVENT = "RemindersActivity.geofence.event"
        private val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
        private val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        private val REQUEST_TURN_DEVICE_LOCATION_ON = 29
        private val ANDROID_Q_OR_HIGHER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofencePendingIntent: PendingIntent

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        val intent =
            Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        geofencePendingIntent = PendingIntent.getBroadcast(
            requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(
                    SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment()
                )
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val selectedPoi = _viewModel.selectedPOI.value
            val location = selectedPoi?.name
            val latitude = selectedPoi?.latLng?.latitude
            val longitude = selectedPoi?.latLng?.longitude

            val newReminder = ReminderDataItem(
                title = title,
                description = description,
                latitude = latitude,
                longitude = longitude,
                location = location
            )

            addGeofenceAndSaveReminder(newReminder)

            //  check if both the required permissions (foreground and background) have been granted
            //ensureForegroundAndBackgroundLocationPermissions()

            // use the user entered reminder details to:
            // 1) add a geofencing request
            // 2) save the reminder to the local db
            /*if (_viewModel.validateEnteredData(newReminder))
                addGeofenceAndSaveReminder(newReminder)
            else
                Log.i(TAG, "Reminder data contains errors: $newReminder")*/
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    @TargetApi(29)
    private fun addGeofenceAndSaveReminder(newReminder: ReminderDataItem) {
        // Check if reminder has all data
        if(!_viewModel.validateEnteredData(newReminder))
            return

        // Check if all the required permissions have been granted (foreground and background).
        // If there is any ungranted permission, request it properly
        ensureForegroundAndBackgroundLocationPermissions()

        // If all the required permissions have been granted, then we should proceed to check
        // if the device location is on. If the device location is not on, show the location
        // settings dialog and ask the user to enable it.
        if(foregroundAndBackgroundLocationPermissionApproved())
        {
            ensureLocationServiceAddGeofenceAndSaveReminder(newReminder)
        } else {
            Log.d(TAG, "The required location permissions condition are not met: " +
                    "Foreground: ${isForegroundLocationApproved()}," +
                    "Background: ${isBackgroundLocationApproved()}")
            _viewModel.onInvalidLocationPermissions()

            return
        }
    }



    private fun buildGeoFence(reminder: ReminderDataItem): Geofence {
        // build geofence around reminder location

        return Geofence.Builder()
            .setRequestId(reminder.id)
            .setCircularRegion(
                reminder.latitude!!,
                reminder.longitude!!,
                GEOFENCE_RADIUS_IN_METERS
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or
                    Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()
    }

    private fun buildGeoFencingRequest(geoFence: Geofence): GeofencingRequest {
        // build geofence request from geofence
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geoFence)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(reminder: ReminderDataItem) {
        if (reminder.validateLatLon()) {
            // create geofence
            val geoFence = buildGeoFence(reminder)

            // create request
            val geofencingRequest = buildGeoFencingRequest(geoFence)

            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
                addOnSuccessListener {
                    Log.i("Adding Geofence", reminder.id)
                    _viewModel.onGeofenceAdded(reminder)
                }
                addOnFailureListener {
                    if ((it.message != null)) {
                        Log.w(TAG, it.message!!)
                    }
                    _viewModel.onGeofenceAddError()
                }

            }
        } else {
            // error in latitude and/or longitude
            Log.e(TAG, "Location is not valid")
            _viewModel.onLatLonError()
        }

    }

    // permissions
    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        // checks both foreground and background permissions
        val isFgLocationApproved = isForegroundLocationApproved()
        val isBgLocationApproved = isBackgroundLocationApproved()

        return isFgLocationApproved && isBgLocationApproved
    }

    private fun isForegroundLocationApproved(): Boolean {
        return (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ))
    }

    private fun isBackgroundLocationApproved(): Boolean {
        val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        val backgroundLocationApproved =
            if (runningQOrLater) {

                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            } else {
                true
            }

        return backgroundLocationApproved
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun ensureForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            Log.d(TAG, "Required location permissions granted")
            return
        } else {
            var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            var requestCode = REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE

            if (ANDROID_Q_OR_HIGHER) {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                requestCode = REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }

            requestPermissions(
                permissionsArray,
                requestCode
            )
        }
    }

    private fun ensureLocationServiceAddGeofenceAndSaveReminder(newReminder: ReminderDataItem) {

        val locationSettingsRequest: LocationSettingsRequest =
            LocationSettingsRequest.Builder().addLocationRequest(LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_LOW_POWER
            }
            ).build()

        val locationSettingsResponseTask: Task<LocationSettingsResponse> =
            LocationServices.getSettingsClient(requireActivity())
                .checkLocationSettings(locationSettingsRequest);

        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                addGeofence(newReminder)
            } else {
                Log.e(TAG, "Error in LocationSettingsResponseTask", it.exception)
            }
        }

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Show the dialog by calling startIntentSenderForResult and check the result
                // in onActivityResult().
                startIntentSenderForResult(
                    exception.resolution.intentSender,
                    REQUEST_TURN_DEVICE_LOCATION_ON,
                    null, 0, 0, 0, null
                )
            } else {
                // some other error, inform user and repeat request
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    ensureLocationServiceAddGeofenceAndSaveReminder(newReminder)
                }.show()
            }
        }
    }

}
