package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.internal.ContextUtils.getActivity
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {
    private val TAG: String = javaClass.simpleName

    private lateinit var binding: ActivityAuthenticationBinding
    private val viewModel by viewModel<AuthenticationViewModel>()

    // Can only use lower 16 bits for requestCode
    private val SIGN_IN_REQUEST_CODE = 1357


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonLogin.setOnClickListener {
            launchSignInFlow()
        }

        // observe
        viewModel.authenticationState.observe(this, Observer { authenticationState ->
            if (authenticationState == AuthenticationViewModel.AuthenticationState.UNAUTHENTICATED) {
                Log.i("onCreate", "User is not Authenticated")

            } else if (authenticationState == AuthenticationViewModel.AuthenticationState.AUTHENTICATED) {
                Log.i("onCreate", "User is Authenticated")
                startRemindersActivity()
            }
        })

        // TODO: a bonus is to customize the sign in flow to look nice using :
        // https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#custom-layout

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SIGN_IN_REQUEST_CODE) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // User successfully signed in
                Log.i(
                    TAG,
                    "Successfully signed in user ${FirebaseAuth.getInstance().currentUser?.displayName}!"
                )
                startRemindersActivity()
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                Log.i(TAG, "Sign in unsuccessful ${response?.error?.errorCode}")

                // display alert dialog
                val builder = AlertDialog.Builder(applicationContext)
                    .setMessage(getString(R.string.error_log_in))
                    .setPositiveButton(android.R.string.ok, null)

                val alertDialog: AlertDialog  = builder.create()

                alertDialog.show()
            }
        }
    }

    private fun launchSignInFlow() {
        val providers: MutableList<AuthUI.IdpConfig> = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        // login button click listener
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            SIGN_IN_REQUEST_CODE
        )
    }

    private fun startRemindersActivity() {
        startActivity(Intent(this, RemindersActivity::class.java))
    }
}

