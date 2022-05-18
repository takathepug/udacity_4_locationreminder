package com.udacity.project4.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.map

class AuthenticationViewModel(): ViewModel() {

    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED
    }

    val authenticationState = FirebaseUserLiveData().map { user ->
        if (user != null) {
            AuthenticationState.AUTHENTICATED
        } else {
            AuthenticationState.UNAUTHENTICATED
        }
    }





/*
    private val _user = MutableLiveData<FirebaseUser>()
    private val _lastSigned = MutableLiveData<GoogleSignInAccount>()

    val user: LiveData<FirebaseUser>
        get() = _user

    init {
        _user.value = firebaseAuth.currentUser
        getLastSigned()
    }

    private fun getLastSigned() {
        val account = GoogleSignIn.getLastSignedInAccount(app.applicationContext)
        _lastSigned.value = account
    }

 */
}