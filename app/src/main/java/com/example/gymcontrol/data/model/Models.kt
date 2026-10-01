package com.example.gymcontrol.data.model

enum class UserRole { CLIENTE, RECEPCION, INSTRUCTOR, ENCARGADO }
enum class Status { ACTIVO, INACTIVO }
enum class RequestStatus { PENDIENTE, APROBADA, RECHAZADA }

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String = "",
    val membershipNumber: String? = null,
    val role: UserRole,
    val status: Status = Status.ACTIVO,
    val registrationDate: String = "",
    val instructorCost: Double? = null,
    val maxClients: Int? = null
)

data class Instructor(
    val id: Int,
    val name: String,
    val profilePhoto: String = "",
    val cost: Double,
    val maxClients: Int,
    val activeClients: Int = 0
)

data class AdvisoryRequest(
    val id: Int,
    val membershipNumber: String,
    val clientName: String,
    val instructorName: String,
    val cost: Double,
    val requestDate: String,
    val status: RequestStatus = RequestStatus.PENDIENTE
)

data class AdvisoryClient(
    val id: Int,
    val clientName: String,
    val days: String,
    val goal: String,
    val active: Boolean = true
)

data class Service(
    val id: Int,
    val name: String,
    val duration: String,
    val price: Double
)

data class Expense(
    val id: Int,
    val concept: String,
    val category: String,
    val amount: Double,
    val date: String
)

data class MonthlyReport(
    val period: String,
    val services: Double,
    val advisories: Double,
    val expenses: Double
) {
    val total: Double get() = services + advisories - expenses
}

data class HourOccupancy(
    val hour: String,
    val people: Int
)
