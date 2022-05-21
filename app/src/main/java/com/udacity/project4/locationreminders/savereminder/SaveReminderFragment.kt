package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
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
    private val GEOFENCE_RADIUS_IN_METERS: Float = 100f
    private val GEOFENCE_EXPIRATION_IN_MILLISECONDS: Long = 1000
    private val ACTION_GEOFENCE_EVENT = "RemindersActivity.geofence.event"

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent =
            Intent(requireActivity().applicationContext, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(
            requireActivity(), 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            val newReminder = ReminderDataItem(
                title = title,
                description = description,
                latitude = latitude,
                longitude = longitude,
                location = location
            )

            // save reminder
            if (_viewModel.validateAndSaveReminder(newReminder))
            // TODO remove
                addGeofence(newReminder)
            else
                Log.w(TAG, "Error saving new reminder data")

//            TODO: use the user entered reminder details to:
//             1) add a geofencing request
//             2) save the reminder to the local db
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    private fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    }

    private fun buildGeoFence(reminder: ReminderDataItem): Geofence {
        return Geofence.Builder()
            .setRequestId(reminder.id)
            .setCircularRegion(
                reminder.latitude!!,
                reminder.longitude!!,
                GEOFENCE_RADIUS_IN_METERS
            )
            .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()
    }

    private fun buildGeoFencingRequest(geoFence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geoFence)
            .build()
    }

    private fun addGeofence(reminder: ReminderDataItem) {
        if (reminder.validateLatLon()) {
            // create geofence
            val geoFence = buildGeoFence(reminder)

            // create request
            val geofencingRequest = buildGeoFencingRequest(geoFence)

            // get client
            val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(
                requireActivity()
            )

            if (!checkLocationPermission()
            ) {
                Toast.makeText(
                    this@SaveReminderFragment.requireActivity(),
                    R.string.error_location_permission_not_added,
                    Toast.LENGTH_SHORT
                ).show()
                return
            } else {
                geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
                    addOnSuccessListener {
                        Toast.makeText(
                            this@SaveReminderFragment.requireActivity(), R.string.geofences_added,
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        Log.e("Add Geofence", reminder.id)
                        _viewModel.validateAndSaveReminder(reminder)
                    }
                    addOnFailureListener {
                        Toast.makeText(
                            this@SaveReminderFragment.requireActivity(),
                            R.string.geofences_not_added,
                            Toast.LENGTH_SHORT
                        ).show()
                        if ((it.message != null)) {
                            Log.w(TAG, it.message!!)
                        }
                    }
                }

            }
        }

    }


}
