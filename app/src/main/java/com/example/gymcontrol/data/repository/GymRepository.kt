package com.example.gymcontrol.data.repository

import com.example.gymcontrol.data.model.*

interface GymRepository {
    fun users(): List<User>
    fun instructors(): List<Instructor>
    fun requests(): List<AdvisoryRequest>
    fun instructorClients(): List<AdvisoryClient>
    fun services(): List<Service>
    fun expenses(): List<Expense>
    fun reports(): List<MonthlyReport>
    fun occupancy(): List<HourOccupancy>
}
