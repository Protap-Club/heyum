package com.pgstorm148.heyum.firebase

import com.pgstorm148.heyum.model.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AuthRepository(private val firebaseAuth: FirebaseAuth) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(firebaseAuth.currentCachedUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        scope.launch {
            firebaseAuth.currentUser.collect { user ->
                _currentUser.value = user
            }
        }
    }
}
