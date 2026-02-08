package com.example.location.domain.entity

sealed class MotionState {
    data object Stationary : MotionState()
    data object Moving : MotionState()
}
