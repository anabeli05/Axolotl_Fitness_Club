package com.example.gymcontrol.ui.manual

import com.example.gymcontrol.R
import com.example.gymcontrol.data.model.UserRole

/** Pasos de cada manual. Cada paso apunta a un elemento que existe de verdad en la pantalla de ese rol. */
object ManualPasos {

    fun de(rol: UserRole): List<ManualPaso> = when (rol) {
        UserRole.CLIENTE -> cliente
        UserRole.INSTRUCTOR -> instructor
        UserRole.RECEPCION -> recepcion
        UserRole.ENCARGADO -> encargado
    }

    private val cliente: List<ManualPaso> by lazy {
        listOf(
            ManualPaso(null, R.string.manual_cliente_1_t, R.string.manual_cliente_1_d, "Inicio"),
            ManualPaso("barra_idioma", R.string.manual_cliente_2_t, R.string.manual_cliente_2_d, "Inicio", tocar = true),
            ManualPaso("barra_tema", R.string.manual_cliente_3_t, R.string.manual_cliente_3_d, "Inicio", tocar = true),
            ManualPaso("barra_manual", R.string.manual_cliente_4_t, R.string.manual_cliente_4_d, "Inicio", tocar = true),
            ManualPaso("barra_logout", R.string.manual_cliente_5_t, R.string.manual_cliente_5_d, "Inicio", tocar = true),
            ManualPaso("barra_tabs", R.string.manual_cliente_6_t, R.string.manual_cliente_6_d, "Inicio"),
            ManualPaso("home_facial", R.string.manual_cliente_7_t, R.string.manual_cliente_7_d, "Inicio"),
            ManualPaso("home_chart", R.string.manual_cliente_8_t, R.string.manual_cliente_8_d, "Inicio"),
            ManualPaso("home_dias", R.string.manual_cliente_9_t, R.string.manual_cliente_9_d, "Inicio", tocar = true),
            ManualPaso("ases_card", R.string.manual_cliente_10_t, R.string.manual_cliente_10_d, "Asesorías"),
            ManualPaso("ases_accion", R.string.manual_cliente_11_t, R.string.manual_cliente_11_d, "Asesorías", tocar = true),
            ManualPaso("pag_plan", R.string.manual_cliente_12_t, R.string.manual_cliente_12_d, "Pagos", tocar = true),
            ManualPaso("pag_estado", R.string.manual_cliente_13_t, R.string.manual_cliente_13_d, "Pagos"),
            ManualPaso("perf_foto", R.string.manual_cliente_14_t, R.string.manual_cliente_14_d, "Perfil"),
            ManualPaso("perf_estado", R.string.manual_cliente_15_t, R.string.manual_cliente_15_d, "Perfil"),
            ManualPaso("perf_editar", R.string.manual_cliente_16_t, R.string.manual_cliente_16_d, "Perfil", tocar = true),
            ManualPaso("perf_contacto", R.string.manual_cliente_17_t, R.string.manual_cliente_17_d, "Perfil", tocar = true),
            ManualPaso("perf_pass", R.string.manual_cliente_18_t, R.string.manual_cliente_18_d, "Perfil", tocar = true),
            ManualPaso("perf_datos", R.string.manual_cliente_19_t, R.string.manual_cliente_19_d, "Perfil")
        )
    }

    private val instructor: List<ManualPaso> by lazy {
        listOf(
            ManualPaso(null, R.string.manual_instructor_1_t, R.string.manual_instructor_1_d, "Mis clientes"),
            ManualPaso("barra_idioma", R.string.manual_instructor_2_t, R.string.manual_instructor_2_d, "Mis clientes", tocar = true),
            ManualPaso("barra_tema", R.string.manual_instructor_3_t, R.string.manual_instructor_3_d, "Mis clientes", tocar = true),
            ManualPaso("barra_manual", R.string.manual_instructor_4_t, R.string.manual_instructor_4_d, "Mis clientes", tocar = true),
            ManualPaso("barra_logout", R.string.manual_instructor_5_t, R.string.manual_instructor_5_d, "Mis clientes", tocar = true),
            ManualPaso("barra_tabs", R.string.manual_instructor_6_t, R.string.manual_instructor_6_d, "Mis clientes"),
            ManualPaso("inst_card", R.string.manual_instructor_7_t, R.string.manual_instructor_7_d, "Mis clientes"),
            ManualPaso("inst_editar", R.string.manual_instructor_8_t, R.string.manual_instructor_8_d, "Mis clientes", tocar = true),
            ManualPaso("perf_foto", R.string.manual_instructor_9_t, R.string.manual_instructor_9_d, "Perfil"),
            ManualPaso("perf_estado", R.string.manual_instructor_10_t, R.string.manual_instructor_10_d, "Perfil"),
            ManualPaso("perf_editar", R.string.manual_instructor_11_t, R.string.manual_instructor_11_d, "Perfil", tocar = true),
            ManualPaso("perf_datos", R.string.manual_instructor_12_t, R.string.manual_instructor_12_d, "Perfil"),
            ManualPaso("inst_cupo", R.string.manual_instructor_13_t, R.string.manual_instructor_13_d, "Perfil")
        )
    }

    private val recepcion: List<ManualPaso> by lazy {
        listOf(
            ManualPaso(null, R.string.manual_recepcion_1_t, R.string.manual_recepcion_1_d, "Home"),
            ManualPaso("barra_menu", R.string.manual_recepcion_2_t, R.string.manual_recepcion_2_d, "Home", tocar = true),
            ManualPaso("lateral_secciones", R.string.manual_recepcion_3_t, R.string.manual_recepcion_3_d, "Home", menuLateral = true, tocar = true),
            ManualPaso("lateral_idioma", R.string.manual_recepcion_4_t, R.string.manual_recepcion_4_d, "Home", menuLateral = true, tocar = true),
            ManualPaso("lateral_tema", R.string.manual_recepcion_5_t, R.string.manual_recepcion_5_d, "Home", menuLateral = true, tocar = true),
            ManualPaso("lateral_logout", R.string.manual_recepcion_6_t, R.string.manual_recepcion_6_d, "Home", menuLateral = true, tocar = true),
            ManualPaso("barra_manual", R.string.manual_recepcion_7_t, R.string.manual_recepcion_7_d, "Home", tocar = true),
            ManualPaso("rec_aforo", R.string.manual_recepcion_8_t, R.string.manual_recepcion_8_d, "Home"),
            ManualPaso("rec_camara", R.string.manual_recepcion_9_t, R.string.manual_recepcion_9_d, "Home"),
            ManualPaso("rec_info", R.string.manual_recepcion_10_t, R.string.manual_recepcion_10_d, "Home"),
            ManualPaso("cli_buscar", R.string.manual_recepcion_11_t, R.string.manual_recepcion_11_d, "Clientes", tocar = true),
            ManualPaso("cli_nuevo", R.string.manual_recepcion_12_t, R.string.manual_recepcion_12_d, "Clientes", tocar = true),
            ManualPaso("cli_foto", R.string.manual_recepcion_13_t, R.string.manual_recepcion_13_d, "Clientes", tocar = true),
            ManualPaso("cli_estado", R.string.manual_recepcion_14_t, R.string.manual_recepcion_14_d, "Clientes"),
            ManualPaso("cli_editar", R.string.manual_recepcion_15_t, R.string.manual_recepcion_15_d, "Clientes", tocar = true),
            ManualPaso("cli_accion", R.string.manual_recepcion_16_t, R.string.manual_recepcion_16_d, "Clientes", tocar = true),
            ManualPaso("vis_precio", R.string.manual_recepcion_17_t, R.string.manual_recepcion_17_d, "Visitas"),
            ManualPaso("vis_form", R.string.manual_recepcion_18_t, R.string.manual_recepcion_18_d, "Visitas"),
            ManualPaso("vis_cobrar", R.string.manual_recepcion_19_t, R.string.manual_recepcion_19_d, "Visitas", tocar = true),
            ManualPaso("vis_hoy", R.string.manual_recepcion_20_t, R.string.manual_recepcion_20_d, "Visitas"),
            ManualPaso("sol_contador", R.string.manual_recepcion_21_t, R.string.manual_recepcion_21_d, "Solicitudes"),
            ManualPaso("sol_card", R.string.manual_recepcion_22_t, R.string.manual_recepcion_22_d, "Solicitudes"),
            ManualPaso("sol_acciones", R.string.manual_recepcion_23_t, R.string.manual_recepcion_23_d, "Solicitudes", tocar = true),
            ManualPaso("pay_titulo", R.string.manual_recepcion_24_t, R.string.manual_recepcion_24_d, "Pagos"),
            ManualPaso("pay_comprobante", R.string.manual_recepcion_25_t, R.string.manual_recepcion_25_d, "Pagos", tocar = true),
            ManualPaso("pay_botones", R.string.manual_recepcion_26_t, R.string.manual_recepcion_26_d, "Pagos", tocar = true),
            ManualPaso("perf_editar", R.string.manual_recepcion_27_t, R.string.manual_recepcion_27_d, "Perfil", tocar = true),
            ManualPaso("perf_pass", R.string.manual_recepcion_28_t, R.string.manual_recepcion_28_d, "Perfil", tocar = true),
            ManualPaso("perf_datos", R.string.manual_recepcion_29_t, R.string.manual_recepcion_29_d, "Perfil")
        )
    }

    private val encargado: List<ManualPaso> by lazy {
        listOf(
            ManualPaso(null, R.string.manual_encargado_1_t, R.string.manual_encargado_1_d, "Dashboard"),
            ManualPaso("barra_menu", R.string.manual_encargado_2_t, R.string.manual_encargado_2_d, "Dashboard", tocar = true),
            ManualPaso("lateral_secciones", R.string.manual_encargado_3_t, R.string.manual_encargado_3_d, "Dashboard", menuLateral = true, tocar = true),
            ManualPaso("lateral_idioma", R.string.manual_encargado_4_t, R.string.manual_encargado_4_d, "Dashboard", menuLateral = true, tocar = true),
            ManualPaso("lateral_tema", R.string.manual_encargado_5_t, R.string.manual_encargado_5_d, "Dashboard", menuLateral = true, tocar = true),
            ManualPaso("lateral_logout", R.string.manual_encargado_6_t, R.string.manual_encargado_6_d, "Dashboard", menuLateral = true, tocar = true),
            ManualPaso("barra_manual", R.string.manual_encargado_7_t, R.string.manual_encargado_7_d, "Dashboard", tocar = true),
            ManualPaso("dash_dentro", R.string.manual_encargado_8_t, R.string.manual_encargado_8_d, "Dashboard"),
            ManualPaso("dash_activos", R.string.manual_encargado_9_t, R.string.manual_encargado_9_d, "Dashboard"),
            ManualPaso("dash_inactivos", R.string.manual_encargado_10_t, R.string.manual_encargado_10_d, "Dashboard"),
            ManualPaso("dash_ingresos", R.string.manual_encargado_11_t, R.string.manual_encargado_11_d, "Dashboard"),
            ManualPaso("dash_ocupacion", R.string.manual_encargado_12_t, R.string.manual_encargado_12_d, "Dashboard"),
            ManualPaso("dash_clientes", R.string.manual_encargado_13_t, R.string.manual_encargado_13_d, "Dashboard"),
            ManualPaso("dash_semana", R.string.manual_encargado_14_t, R.string.manual_encargado_14_d, "Dashboard"),
            ManualPaso("cli_buscar", R.string.manual_encargado_15_t, R.string.manual_encargado_15_d, "Clientes", tocar = true),
            ManualPaso("cli_nuevo", R.string.manual_encargado_16_t, R.string.manual_encargado_16_d, "Clientes", tocar = true),
            ManualPaso("cli_foto", R.string.manual_encargado_17_t, R.string.manual_encargado_17_d, "Clientes", tocar = true),
            ManualPaso("cli_estado", R.string.manual_encargado_18_t, R.string.manual_encargado_18_d, "Clientes"),
            ManualPaso("cli_editar", R.string.manual_encargado_19_t, R.string.manual_encargado_19_d, "Clientes", tocar = true),
            ManualPaso("cli_accion", R.string.manual_encargado_20_t, R.string.manual_encargado_20_d, "Clientes", tocar = true),
            ManualPaso("per_nuevo", R.string.manual_encargado_21_t, R.string.manual_encargado_21_d, "Personal", tocar = true),
            ManualPaso("per_foto", R.string.manual_encargado_22_t, R.string.manual_encargado_22_d, "Personal", tocar = true),
            ManualPaso("per_info", R.string.manual_encargado_23_t, R.string.manual_encargado_23_d, "Personal"),
            ManualPaso("per_acciones", R.string.manual_encargado_24_t, R.string.manual_encargado_24_d, "Personal", tocar = true),
            ManualPaso("serv_nuevo", R.string.manual_encargado_25_t, R.string.manual_encargado_25_d, "Servicios", tocar = true),
            ManualPaso("serv_card", R.string.manual_encargado_26_t, R.string.manual_encargado_26_d, "Servicios"),
            ManualPaso("serv_acciones", R.string.manual_encargado_27_t, R.string.manual_encargado_27_d, "Servicios", tocar = true),
            ManualPaso("pay_titulo", R.string.manual_encargado_28_t, R.string.manual_encargado_28_d, "Pagos"),
            ManualPaso("pay_comprobante", R.string.manual_encargado_29_t, R.string.manual_encargado_29_d, "Pagos", tocar = true),
            ManualPaso("pay_botones", R.string.manual_encargado_30_t, R.string.manual_encargado_30_d, "Pagos", tocar = true),
            ManualPaso("vis_precio", R.string.manual_encargado_31_t, R.string.manual_encargado_31_d, "Visitas"),
            ManualPaso("vis_form", R.string.manual_encargado_32_t, R.string.manual_encargado_32_d, "Visitas"),
            ManualPaso("vis_cobrar", R.string.manual_encargado_33_t, R.string.manual_encargado_33_d, "Visitas", tocar = true),
            ManualPaso("vis_hoy", R.string.manual_encargado_34_t, R.string.manual_encargado_34_d, "Visitas"),
            ManualPaso("gas_nuevo", R.string.manual_encargado_35_t, R.string.manual_encargado_35_d, "Gastos", tocar = true),
            ManualPaso("gas_card", R.string.manual_encargado_36_t, R.string.manual_encargado_36_d, "Gastos"),
            ManualPaso("gas_acciones", R.string.manual_encargado_37_t, R.string.manual_encargado_37_d, "Gastos", tocar = true),
            ManualPaso("rep_solicitudes", R.string.manual_encargado_38_t, R.string.manual_encargado_38_d, "Reportes"),
            ManualPaso("rep_mensual", R.string.manual_encargado_39_t, R.string.manual_encargado_39_d, "Reportes"),
            ManualPaso("rep_cancel", R.string.manual_encargado_40_t, R.string.manual_encargado_40_d, "Reportes"),
            ManualPaso("avi_nuevo", R.string.manual_encargado_41_t, R.string.manual_encargado_41_d, "Avisos", tocar = true),
            ManualPaso("avi_card", R.string.manual_encargado_42_t, R.string.manual_encargado_42_d, "Avisos"),
            ManualPaso("avi_enviar", R.string.manual_encargado_43_t, R.string.manual_encargado_43_d, "Avisos", tocar = true),
            ManualPaso("perf_foto", R.string.manual_encargado_44_t, R.string.manual_encargado_44_d, "Perfil", tocar = true),
            ManualPaso("perf_editar", R.string.manual_encargado_45_t, R.string.manual_encargado_45_d, "Perfil", tocar = true),
            ManualPaso("perf_pass", R.string.manual_encargado_46_t, R.string.manual_encargado_46_d, "Perfil", tocar = true),
            ManualPaso("perf_datos", R.string.manual_encargado_47_t, R.string.manual_encargado_47_d, "Perfil")
        )
    }

}
