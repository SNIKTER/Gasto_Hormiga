package com.example.gasto_hormiga

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.Locale
import kotlin.math.pow

data class Gasto(
    val id: Int,
    val concepto: String,
    val monto: Double,
    val categoria: String,
    val anio: Int,
    val mes: Int, // 1 a 12
    val dia: Int
) {
    val fechaFormateada: String
        get() = String.format(Locale.getDefault(), "%02d/%02d/%04d", dia, mes, anio)
}

data class UsuarioPerfil(
    val nombre: String = "",
    val edad: String = "",
    val ingresoMensual: Double = 0.0,
    val registrado: Boolean = false
)

class MainViewModel : ViewModel() {

    val categorias = listOf("Comida", "Café", "Transporte", "Antojos", "Otros")
    val nombresMeses = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )
    val mesesCortos = listOf(
        "Ene", "Feb", "Mar", "Abr", "May", "Jun",
        "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    )

    private val _perfil = MutableStateFlow(UsuarioPerfil())
    val perfil: StateFlow<UsuarioPerfil> = _perfil.asStateFlow()

    private val _gastos = MutableStateFlow<List<Gasto>>(emptyList())
    val gastos: StateFlow<List<Gasto>> = _gastos.asStateFlow()

    fun guardarPerfil(nombre: String, edad: String, ingreso: Double) {
        _perfil.value = UsuarioPerfil(
            nombre = nombre.trim(),
            edad = edad.trim(),
            ingresoMensual = ingreso,
            registrado = true
        )
    }

    fun actualizarIngreso(nuevoIngreso: Double) {
        _perfil.value = _perfil.value.copy(ingresoMensual = nuevoIngreso)
    }

    fun agregarGasto(
        concepto: String,
        monto: Double,
        categoria: String,
        anio: Int,
        mes: Int,
        dia: Int
    ) {
        val nuevoGasto = Gasto(
            id = (_gastos.value.maxOfOrNull { it.id } ?: 0) + 1,
            concepto = concepto.trim(),
            monto = monto,
            categoria = categoria,
            anio = anio,
            mes = mes,
            dia = dia
        )
        _gastos.value = listOf(nuevoGasto) + _gastos.value
    }

    fun eliminarGasto(id: Int) {
        _gastos.value = _gastos.value.filterNot { it.id == id }
    }

    fun calcularTotalGastos(): Double = _gastos.value.sumOf { it.monto }

    fun calcularPorcentajeGastado(): Float {
        val ingreso = _perfil.value.ingresoMensual
        if (ingreso <= 0.0) return 0f
        return ((calcularTotalGastos() / ingreso) * 100.0).toFloat().coerceIn(0f, 100f)
    }

    // Días del mes que tienen al menos un gasto registrado (para marcar el calendario)
    fun diasConGastos(anio: Int, mes: Int): Set<Int> {
        return _gastos.value
            .filter { it.anio == anio && it.mes == mes }
            .map { it.dia }
            .toSet()
    }

    // Lista de gastos en una fecha puntual
    fun gastosPorFecha(anio: Int, mes: Int, dia: Int): List<Gasto> {
        return _gastos.value.filter { it.anio == anio && it.mes == mes && it.dia == dia }
    }

    // Gráfica Diaria: últimos 7 días terminando en la fecha seleccionada
    fun obtenerDatosGraficaDiaria(anio: Int, mes: Int, dia: Int): List<Pair<String, Double>> {
        val resultado = mutableListOf<Pair<String, Double>>()
        val cal = Calendar.getInstance().apply {
            set(anio, mes - 1, dia)
            add(Calendar.DAY_OF_MONTH, -6)
        }
        repeat(7) {
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            val d = cal.get(Calendar.DAY_OF_MONTH)
            val totalDia = _gastos.value
                .filter { it.anio == y && it.mes == m && it.dia == d }
                .sumOf { it.monto }
            val etiqueta = String.format(Locale.getDefault(), "%02d/%02d", d, m)
            resultado.add(etiqueta to totalDia)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return resultado
    }

    // Gráfica Mensual: los 12 meses del año seleccionado
    fun obtenerDatosGraficaMensual(anio: Int): List<Pair<String, Double>> {
        return (1..12).map { mesIndex ->
            val totalMes = _gastos.value
                .filter { it.anio == anio && it.mes == mesIndex }
                .sumOf { it.monto }
            mesesCortos[mesIndex - 1] to totalMes
        }
    }

    fun gastosPorCategoria(): Map<String, Double> {
        return categorias.associateWith { cat ->
            _gastos.value.filter { it.categoria == cat }.sumOf { it.monto }
        }
    }

    fun calcularEfectoLatte(monto: Double): Double = monto * 30 * 12

    fun simularPrestamo(monto: Double, tasaAnual: Double, meses: Int): Triple<Double, Double, Double> {
        if (monto <= 0 || tasaAnual <= 0 || meses <= 0) return Triple(0.0, 0.0, 0.0)
        val tasaMensual = (tasaAnual / 100.0) / 12.0
        val factor = (1 + tasaMensual).pow(meses)
        val cuotaMensual = monto * (tasaMensual * factor) / (factor - 1)
        val pagoTotal = cuotaMensual * meses
        val interesesTotales = pagoTotal - monto
        return Triple(cuotaMensual, pagoTotal, interesesTotales)
    }
}