package com.example.gymcontrol.data.repository

import com.example.gymcontrol.data.model.*

class FakeGymRepository : GymRepository {
    override fun users() = listOf(
        User(1, "Ana López", "ana@email.com", "3121111111", "M-1001", UserRole.CLIENTE, Status.ACTIVO, "12/01/2026"),
        User(2, "Carlos Ruiz", "carlos@email.com", "3122222222", "M-1002", UserRole.CLIENTE, Status.INACTIVO, "03/02/2026"),
        User(3, "Laura Pérez", "recepcion@gym.com", "3123333333", null, UserRole.RECEPCION),
        User(4, "Marco Díaz", "marco@gym.com", "3124444444", null, UserRole.INSTRUCTOR, instructorCost = 350.0, maxClients = 12),
        User(5, "Dueño Gym", "admin@gym.com", "3125555555", null, UserRole.ENCARGADO)
    )

    override fun instructors() = listOf(
        Instructor(1, "Marco Díaz", cost = 350.0, maxClients = 12, activeClients = 8),
        Instructor(2, "Sofía Torres", cost = 420.0, maxClients = 10, activeClients = 7),
        Instructor(3, "Diego Ramos", cost = 300.0, maxClients = 15, activeClients = 12)
    )

    override fun requests() = listOf(
        AdvisoryRequest(1, "M-1001", "Ana López", "Marco Díaz", 350.0, "08/09/2026"),
        AdvisoryRequest(2, "M-1007", "Luis Gómez", "Sofía Torres", 420.0, "08/09/2026")
    )

    override fun instructorClients() = listOf(
        AdvisoryClient(1, "Ana López", "Lun, Mié, Vie", "Ganar fuerza"),
        AdvisoryClient(2, "Luis Gómez", "Mar, Jue, Sáb", "Bajar grasa"),
        AdvisoryClient(3, "Mariana Silva", "Lun a Vie", "Acondicionamiento")
    )

    override fun services() = listOf(
        Service(1, "Mensualidad", "30 días", 500.0),
        Service(2, "Trimestre", "90 días", 1350.0),
        Service(3, "Visita", "1 día", 80.0)
    )

    override fun expenses() = listOf(
        Expense(1, "Recibo de luz", "Servicios", 4500.0, "03/09/2026"),
        Expense(2, "Mantenimiento caminadora", "Mantenimiento", 1800.0, "05/09/2026")
    )

    override fun reports() = listOf(
        MonthlyReport("Enero 2026", 42000.0, 12800.0, 15100.0),
        MonthlyReport("Febrero 2026", 45500.0, 14000.0, 16200.0),
        MonthlyReport("Marzo 2026", 47200.0, 14900.0, 15800.0)
    )

    override fun occupancy() = listOf(
        HourOccupancy("06:00", 10),
        HourOccupancy("08:00", 22),
        HourOccupancy("10:00", 13),
        HourOccupancy("12:00", 9),
        HourOccupancy("14:00", 14),
        HourOccupancy("16:00", 25),
        HourOccupancy("18:00", 38),
        HourOccupancy("20:00", 31)
    )
}
