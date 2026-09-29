package com.example.gymcontrol

import com.example.gymcontrol.data.repository.FakeGymRepository
import com.example.gymcontrol.data.repository.GymRepository

object GymApp {
    val repository: GymRepository = FakeGymRepository()
}
