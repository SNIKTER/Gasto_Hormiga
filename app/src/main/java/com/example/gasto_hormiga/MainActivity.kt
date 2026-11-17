package com.example.gasto_hormiga

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

val PurplePrimary = Color(0xFF7B74F5)
val PurpleSoft = Color(0xFFA59FF9)
val PurpleLight = Color(0xFFEBE9FE)
val BackgroundSoft = Color(0xFFF4F6FC)
val TextDark = Color(0xFF2B2D42)
val TextMuted = Color(0xFF7D839C)
val ExpenseRed = Color(0xFFE53935)
val IncomeGreen = Color(0xFF2E7D32)

val PurpleGradient = Brush.horizontalGradient(listOf(PurplePrimary, PurpleSoft))
val PurpleVerticalGradient = Brush.verticalGradient(listOf(PurpleSoft, PurplePrimary))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = PurplePrimary,
                    background = BackgroundSoft,
                    surface = Color.White
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundSoft) {
                    GastoHormigaApp()
                }
            }
        }
    }
}

@Composable
fun GastoHormigaApp(viewModel: MainViewModel = viewModel()) {
    val perfil by viewModel.perfil.collectAsState()
    var tabSeleccionado by remember { mutableIntStateOf(0) }
    var mostrarDialogoGasto by remember { mutableStateOf(false) }

    val hoy = remember { Calendar.getInstance() }
    var fechaDialogo by remember {
        mutableStateOf(
            Triple(
                hoy.get(Calendar.YEAR),
                hoy.get(Calendar.MONTH) + 1,
                hoy.get(Calendar.DAY_OF_MONTH)
            )
        )
    }

    if (!perfil.registrado) {
        OnboardingScreen(viewModel)
    } else {
        Scaffold(
            containerColor = BackgroundSoft,
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 12.dp
                ) {
                    val items = listOf(
                        Triple("Inicio", Icons.Default.Home, 0),
                        Triple("Gráficas", Icons.Default.BarChart, 1),
                        Triple("Préstamos", Icons.Default.AccountBalance, 2),
                        Triple("Perfil", Icons.Default.Person, 3)
                    )
                    items.forEach { (titulo, icono, index) ->
                        NavigationBarItem(
                            selected = tabSeleccionado == index,
                            onClick = { tabSeleccionado = index },
                            icon = { Icon(icono, contentDescription = titulo) },
                            label = { Text(titulo, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PurplePrimary,
                                selectedTextColor = PurplePrimary,
                                indicatorColor = PurpleLight,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                    }
                }
            },
            floatingActionButton = {
                if (tabSeleccionado == 0 || tabSeleccionado == 1) {
                    FloatingActionButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            fechaDialogo = Triple(
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH) + 1,
                                cal.get(Calendar.DAY_OF_MONTH)
                            )
                            mostrarDialogoGasto = true
                        },
                        containerColor = PurplePrimary,
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Nuevo gasto")
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when (tabSeleccionado) {
                    0 -> DashboardScreen(
                        viewModel = viewModel,
                        onAbrirNuevoGasto = {
                            val cal = Calendar.getInstance()
                            fechaDialogo = Triple(
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH) + 1,
                                cal.get(Calendar.DAY_OF_MONTH)
                            )
                            mostrarDialogoGasto = true
                        },
                        onIrAGraficas = { tabSeleccionado = 1 },
                        onIrAPrestamos = { tabSeleccionado = 2 }
                    )
                    1 -> EstadisticasScreen(
                        viewModel = viewModel,
                        onAgregarGastoEnFecha = { anio, mes, dia ->
                            fechaDialogo = Triple(anio, mes, dia)
                            mostrarDialogoGasto = true
                        }
                    )
                    2 -> SimuladorPrestamosScreen(viewModel)
                    3 -> PerfilScreen(viewModel)
                }
            }
        }

        if (mostrarDialogoGasto) {
            DialogoNuevoGasto(
                categorias = viewModel.categorias,
                fechaInicial = fechaDialogo,
                onDismiss = { mostrarDialogoGasto = false },
                onGuardar = { concepto, monto, categoria, anio, mes, dia ->
                    viewModel.agregarGasto(concepto, monto, categoria, anio, mes, dia)
                    mostrarDialogoGasto = false
                }
            )
        }
    }
}

// ---------------------------------------------------------
// 1. PANTALLA DE BIENVENIDA (NOMBRE, EDAD E INGRESO)
// ---------------------------------------------------------
@Composable
fun OnboardingScreen(viewModel: MainViewModel) {
    var nombre by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var ingreso by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoft)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                .background(PurpleVerticalGradient)
                .padding(24.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Column {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Gasto Hormiga",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Tu libreta financiera inteligente",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 15.sp
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Configura tu perfil",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    "Ingresa tus datos para personalizar tus KPIs y recomendaciones.",
                    fontSize = 13.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre Completo") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PurplePrimary) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = edad,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) edad = it },
                    label = { Text("Edad") },
                    leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null, tint = PurplePrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = ingreso,
                    onValueChange = { ingreso = it },
                    label = { Text("Ingreso Mensual ($ COP)") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = PurplePrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (error.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error, color = ExpenseRed, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val ingresoNum = ingreso.toDoubleOrNull() ?: 0.0
                        when {
                            nombre.isBlank() -> error = "Por favor ingresa tu nombre completo."
                            edad.isBlank() -> error = "Por favor ingresa tu edad."
                            ingresoNum <= 0.0 -> error = "Ingresa un monto mensual válido."
                            else -> viewModel.guardarPerfil(nombre, edad, ingresoNum)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) {
                    Text("Comenzar ahora", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------------------------------------------------------
// 2. DASHBOARD PRINCIPAL (INICIO)
// ---------------------------------------------------------
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onAbrirNuevoGasto: () -> Unit,
    onIrAGraficas: () -> Unit,
    onIrAPrestamos: () -> Unit
) {
    val perfil by viewModel.perfil.collectAsState()
    val gastos by viewModel.gastos.collectAsState()
    val totalGastado = viewModel.calcularTotalGastos()
    val disponible = perfil.ingresoMensual - totalGastado

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 84.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(PurpleVerticalGradient)
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Hola, ${perfil.nombre.substringBefore(" ")}",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 15.sp
                            )
                            Text(
                                "Balance Disponible",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.22f))
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatoMoneda(disponible),
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickActionCard("Nuevo Gasto", Icons.Default.AddCard, onAbrirNuevoGasto)
                QuickActionCard("Gráficas", Icons.Default.ShowChart, onIrAGraficas)
                QuickActionCard("Préstamos", Icons.Default.MonetizationOn, onIrAPrestamos)
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GradientKpiCard(
                    titulo = "Gastado Total",
                    valor = formatoMoneda(totalGastado),
                    modifier = Modifier.weight(1f)
                )
                GradientKpiCard(
                    titulo = "Proyección Anual",
                    valor = formatoMoneda(totalGastado * 12),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Movimientos Recientes", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text("${gastos.size} registros", fontSize = 12.sp, color = TextMuted)
            }
        }

        if (gastos.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = PurpleSoft, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Aún no registras gastos hormiga", fontWeight = FontWeight.SemiBold, color = TextDark)
                        Text("Toca el botón + para anotar tu primer gasto.", fontSize = 12.sp, color = TextMuted)
                    }
                }
            }
        } else {
            items(gastos, key = { it.id }) { gasto ->
                GastoItemCard(
                    gasto = gasto,
                    efectoLatte = viewModel.calcularEfectoLatte(gasto.monto),
                    onEliminar = { viewModel.eliminarGasto(gasto.id) }
                )
            }
        }
    }
}

@Composable
fun QuickActionCard(titulo: String, icono: ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier
                .size(68.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(icono, contentDescription = titulo, tint = PurplePrimary, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(titulo, fontSize = 12.sp, color = TextDark, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun GradientKpiCard(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(PurpleGradient)
            .padding(16.dp)
    ) {
        Column {
            Text(titulo, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(valor, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GastoItemCard(gasto: Gasto, efectoLatte: Double, onEliminar: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PurpleLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconoCategoria(gasto.categoria),
                    contentDescription = null,
                    tint = PurplePrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(gasto.concepto, fontWeight = FontWeight.Bold, color = TextDark, fontSize = 15.sp)
                Text("${gasto.categoria} • ${gasto.fechaFormateada}", fontSize = 11.sp, color = TextMuted)
                Text(
                    "Hábito anual: ${formatoMoneda(efectoLatte)}",
                    fontSize = 11.sp,
                    color = PurplePrimary,
                    fontWeight = FontWeight.Medium
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("-${formatoMoneda(gasto.monto)}", color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                IconButton(onClick = onEliminar, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------
// 3. PANTALLA DE ESTADÍSTICAS, GRÁFICA DIARIA/MENSUAL Y CALENDARIO
// ---------------------------------------------------------
@Composable
fun EstadisticasScreen(
    viewModel: MainViewModel,
    onAgregarGastoEnFecha: (Int, Int, Int) -> Unit
) {
    val perfil by viewModel.perfil.collectAsState()
    val gastos by viewModel.gastos.collectAsState()
    val porcentaje = viewModel.calcularPorcentajeGastado()
    val totalGastado = viewModel.calcularTotalGastos()

    // Modo de gráfica: true = Diaria, false = Mensual
    var esVistaDiaria by remember { mutableStateOf(true) }

    // Estado del calendario (inicializado en el día actual)
    val hoy = remember { Calendar.getInstance() }
    var anioActual by remember { mutableIntStateOf(hoy.get(Calendar.YEAR)) }
    var mesActual by remember { mutableIntStateOf(hoy.get(Calendar.MONTH) + 1) } // 1..12
    var diaSeleccionado by remember { mutableIntStateOf(hoy.get(Calendar.DAY_OF_MONTH)) }

    // Recalcular al cambiar gastos o fechas
    val datosGrafica = remember(gastos, esVistaDiaria, anioActual, mesActual, diaSeleccionado) {
        if (esVistaDiaria) {
            viewModel.obtenerDatosGraficaDiaria(anioActual, mesActual, diaSeleccionado)
        } else {
            viewModel.obtenerDatosGraficaMensual(anioActual)
        }
    }
    val diasMarcados = remember(gastos, anioActual, mesActual) {
        viewModel.diasConGastos(anioActual, mesActual)
    }
    val gastosDelDia = remember(gastos, anioActual, mesActual, diaSeleccionado) {
        viewModel.gastosPorFecha(anioActual, mesActual, diaSeleccionado)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 88.dp)
    ) {
        // Encabezado con selector Diaria / Mensual
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(PurpleVerticalGradient)
                .padding(20.dp)
        ) {
            Column {
                Text("Gráficas y Calendario", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Control diario y mensual de tus gastos", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                Spacer(modifier = Modifier.height(16.dp))

                // Selector tipo pill (Diaria / Mensual)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (esVistaDiaria) Color.White else Color.Transparent)
                            .clickable { esVistaDiaria = true }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Gráfica Diaria",
                            color = if (esVistaDiaria) PurplePrimary else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!esVistaDiaria) Color.White else Color.Transparent)
                            .clickable { esVistaDiaria = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Gráfica Mensual",
                            color = if (!esVistaDiaria) PurplePrimary else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(20.dp)) {
            // 1. Gráfica de Barras Dinámica (Diaria o Mensual)
            val maxValor = (datosGrafica.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        if (esVistaDiaria) "Gastos Diarios (Últimos 7 días)" else "Gastos Mensuales ($anioActual)",
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        fontSize = 16.sp
                    )
                    Text(
                        if (esVistaDiaria)
                            "Hasta el $diaSeleccionado de ${viewModel.nombresMeses[mesActual - 1]}"
                        else
                            "Desliza horizontalmente para ver los 12 meses",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    if (esVistaDiaria) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            datosGrafica.forEach { (etiqueta, monto) ->
                                BarraGraficaItem(etiqueta, monto, maxValor, Modifier.weight(1f))
                            }
                        }
                    } else {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            items(datosGrafica) { (etiqueta, monto) ->
                                BarraGraficaItem(etiqueta, monto, maxValor, Modifier.width(38.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Calendario Interactivo de Gastos
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Cabecera Mes y Año con flechas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (mesActual == 1) {
                                mesActual = 12
                                anioActual -= 1
                            } else {
                                mesActual -= 1
                            }
                            diaSeleccionado = 1
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior", tint = PurplePrimary)
                        }
                        Text(
                            "${viewModel.nombresMeses[mesActual - 1]} $anioActual",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextDark
                        )
                        IconButton(onClick = {
                            if (mesActual == 12) {
                                mesActual = 1
                                anioActual += 1
                            } else {
                                mesActual += 1
                            }
                            diaSeleccionado = 1
                        }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente", tint = PurplePrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Días de la semana
                    val diasSemana = listOf("L", "M", "M", "J", "V", "S", "D")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        diasSemana.forEach { d ->
                            Text(
                                text = d,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Grilla de días del mes
                    val calMes = remember(anioActual, mesActual) {
                        Calendar.getInstance().apply { set(anioActual, mesActual - 1, 1) }
                    }
                    val totalDiasMes = calMes.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val primerDiaSemana = calMes.get(Calendar.DAY_OF_WEEK)
                    val offset = if (primerDiaSemana == Calendar.SUNDAY) 6 else primerDiaSemana - 2

                    val celdas = buildList<Int?> {
                        repeat(offset) { add(null) }
                        for (d in 1..totalDiasMes) add(d)
                        while (size % 7 != 0) add(null)
                    }

                    celdas.chunked(7).forEach { semana ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            semana.forEach { dia ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (dia != null) {
                                        val seleccionado = dia == diaSeleccionado
                                        val tieneGasto = diasMarcados.contains(dia)
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        seleccionado -> PurplePrimary
                                                        tieneGasto -> PurpleLight
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .clickable { diaSeleccionado = dia }
                                        ) {
                                            Text(
                                                text = dia.toString(),
                                                fontSize = 13.sp,
                                                fontWeight = if (seleccionado || tieneGasto) FontWeight.Bold else FontWeight.Normal,
                                                color = if (seleccionado) Color.White else TextDark
                                            )
                                            if (tieneGasto) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(if (seleccionado) Color.White else PurplePrimary)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = PurpleLight)

                    // Detalle de gastos del día seleccionado
                    val totalDia = gastosDelDia.sumOf { it.monto }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Gastos del $diaSeleccionado de ${viewModel.nombresMeses[mesActual - 1]}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextDark
                            )
                            Text(
                                "Total del día: ${formatoMoneda(totalDia)}",
                                fontSize = 12.sp,
                                color = if (totalDia > 0) ExpenseRed else TextMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        FilledTonalButton(
                            onClick = { onAgregarGastoEnFecha(anioActual, mesActual, diaSeleccionado) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = PurpleLight,
                                contentColor = PurplePrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Anotar", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (gastosDelDia.isEmpty()) {
                        Text(
                            "No hay gastos registrados en esta fecha.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        gastosDelDia.forEach { gasto ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(BackgroundSoft)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        iconoCategoria(gasto.categoria),
                                        contentDescription = null,
                                        tint = PurplePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(gasto.concepto, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextDark)
                                        Text(gasto.categoria, fontSize = 11.sp, color = TextMuted)
                                    }
                                }
                                Text("-${formatoMoneda(gasto.monto)}", color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Gráfica Circular (Presupuesto Consumido)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Presupuesto Consumido", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(20.dp))

                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(170.dp)) {
                        Canvas(modifier = Modifier.size(150.dp)) {
                            drawArc(
                                color = PurpleLight,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                brush = PurpleGradient,
                                startAngle = -90f,
                                sweepAngle = (porcentaje / 100f) * 360f,
                                useCenter = false,
                                style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${porcentaje.toInt()}%",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextDark
                            )
                            Text("de tu ingreso", fontSize = 12.sp, color = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GradientKpiCard("Ingresos", formatoMoneda(perfil.ingresoMensual), Modifier.weight(1f))
                        GradientKpiCard("Gastos", formatoMoneda(totalGastado), Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun BarraGraficaItem(etiqueta: String, monto: Double, maxValor: Double, modifier: Modifier = Modifier) {
    val fraccion = (monto / maxValor).toFloat().coerceIn(0.06f, 1f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = modifier
    ) {
        if (monto > 0) {
            val textoMonto = if (monto >= 1000) "${(monto / 1000).toInt()}k" else monto.toInt().toString()
            Text(
                textoMonto,
                fontSize = 10.sp,
                color = PurplePrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(22.dp)
                .height((115 * fraccion).dp)
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .background(
                    if (monto > 0) PurpleVerticalGradient
                    else Brush.verticalGradient(listOf(PurpleLight, PurpleLight))
                )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            etiqueta,
            fontSize = 10.sp,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------
// 4. SIMULADOR DE PRÉSTAMOS
// ---------------------------------------------------------
@Composable
fun SimuladorPrestamosScreen(viewModel: MainViewModel) {
    var montoPrestamo by remember { mutableStateOf("") }
    var tasaInteres by remember { mutableStateOf("") }
    var plazoMeses by remember { mutableStateOf("") }
    var resultados by remember { mutableStateOf<Triple<Double, Double, Double>?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Simulador de Préstamos", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Text("Evalúa si un crédito conviene antes de endeudarte", fontSize = 13.sp, color = TextMuted)

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                OutlinedTextField(
                    value = montoPrestamo,
                    onValueChange = { montoPrestamo = it },
                    label = { Text("Monto a solicitar ($ COP)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = tasaInteres,
                    onValueChange = { tasaInteres = it },
                    label = { Text("Tasa de Interés Anual (E.A %)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = plazoMeses,
                    onValueChange = { plazoMeses = it },
                    label = { Text("Plazo en Meses") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = {
                        val m = montoPrestamo.toDoubleOrNull() ?: 0.0
                        val t = tasaInteres.toDoubleOrNull() ?: 0.0
                        val p = plazoMeses.toIntOrNull() ?: 0
                        resultados = viewModel.simularPrestamo(m, t, p)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) {
                    Text("Calcular Viabilidad", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        resultados?.let { (cuota, total, intereses) ->
            Spacer(modifier = Modifier.height(20.dp))
            val montoOriginal = montoPrestamo.toDoubleOrNull() ?: 0.0
            val esRiesgoso = intereses > montoOriginal * 0.30

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Resultado del Análisis", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GradientKpiCard("Cuota Mensual", formatoMoneda(cuota), Modifier.weight(1f))
                        GradientKpiCard("Total Intereses", formatoMoneda(intereses), Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Total a pagar al banco: ${formatoMoneda(total)}", fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (esRiesgoso) Color(0xFFFFEBEE) else Color(0xFFE8F5E9))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (esRiesgoso)
                                "Alerta financiera: Pagarás más del 30% en intereses. Evalúa reducir el plazo o ahorrar una cuota inicial."
                            else
                                "Crédito manejable: Los intereses están por debajo del 30% del capital solicitado.",
                            color = if (esRiesgoso) ExpenseRed else IncomeGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// 5. PERFIL Y REGLA 50/30/20
// ---------------------------------------------------------
@Composable
fun PerfilScreen(viewModel: MainViewModel) {
    val perfil by viewModel.perfil.collectAsState()
    var nuevoIngreso by remember { mutableStateOf(perfil.ingresoMensual.toLong().toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Mi Perfil", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(PurpleVerticalGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        perfil.nombre.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(perfil.nombre, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text("${perfil.edad} años", fontSize = 14.sp, color = TextMuted)
                    Text("Ingreso: ${formatoMoneda(perfil.ingresoMensual)}", fontSize = 13.sp, color = PurplePrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Guía Regla 50 / 30 / 20", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                Text("Distribución recomendada de tu ingreso", fontSize = 12.sp, color = TextMuted)
                Spacer(modifier = Modifier.height(14.dp))
                FilaRegla("50% Necesidades", perfil.ingresoMensual * 0.50, PurplePrimary)
                Spacer(modifier = Modifier.height(10.dp))
                FilaRegla("30% Deseos y Gastos Hormiga", perfil.ingresoMensual * 0.30, PurpleSoft)
                Spacer(modifier = Modifier.height(10.dp))
                FilaRegla("20% Ahorro e Inversión", perfil.ingresoMensual * 0.20, IncomeGreen)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Actualizar Ingreso Mensual", fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = nuevoIngreso,
                    onValueChange = { nuevoIngreso = it },
                    label = { Text("Nuevo Ingreso ($ COP)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        nuevoIngreso.toDoubleOrNull()?.let { if (it > 0) viewModel.actualizarIngreso(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) {
                    Text("Guardar Cambios")
                }
            }
        }
    }
}

@Composable
fun FilaRegla(etiqueta: String, monto: Double, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BackgroundSoft)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(etiqueta, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextDark)
        Text(formatoMoneda(monto), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// ---------------------------------------------------------
// DIÁLOGO PARA AGREGAR GASTO CON SELECTOR DE FECHA Y CATEGORÍA
// ---------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoNuevoGasto(
    categorias: List<String>,
    fechaInicial: Triple<Int, Int, Int>,
    onDismiss: () -> Unit,
    onGuardar: (String, Double, String, Int, Int, Int) -> Unit
) {
    var concepto by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf(categorias.first()) }
    var anio by remember { mutableIntStateOf(fechaInicial.first) }
    var mes by remember { mutableIntStateOf(fechaInicial.second) }
    var dia by remember { mutableIntStateOf(fechaInicial.third) }
    var mostrarDatePicker by remember { mutableStateOf(false) }

    val fechaTexto = String.format(Locale.getDefault(), "%02d/%02d/%04d", dia, mes, anio)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = { Text("Nuevo Gasto Hormiga", fontWeight = FontWeight.Bold, color = TextDark) },
        text = {
            Column {
                OutlinedTextField(
                    value = concepto,
                    onValueChange = { concepto = it },
                    label = { Text("Concepto (Ej. Café, Taxi)") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = monto,
                    onValueChange = { monto = it },
                    label = { Text("Monto ($ COP)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Botón para elegir el día del gasto
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PurpleLight)
                        .clickable { mostrarDatePicker = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fecha: $fechaTexto", fontWeight = FontWeight.SemiBold, color = TextDark, fontSize = 13.sp)
                    }
                    Text("Cambiar", color = PurplePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Categoría:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categorias) { cat ->
                        FilterChip(
                            selected = categoriaSeleccionada == cat,
                            onClick = { categoriaSeleccionada = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = monto.toDoubleOrNull()
                    if (concepto.isNotBlank() && m != null && m > 0) {
                        onGuardar(concepto, m, categoriaSeleccionada, anio, mes, dia)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextMuted)
            }
        }
    )

    if (mostrarDatePicker) {
        val initialMillis = remember(anio, mes, dia) {
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(anio, mes - 1, dia, 0, 0, 0)
            }.timeInMillis
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = millis
                        }
                        anio = calUtc.get(Calendar.YEAR)
                        mes = calUtc.get(Calendar.MONTH) + 1
                        dia = calUtc.get(Calendar.DAY_OF_MONTH)
                    }
                    mostrarDatePicker = false
                }) {
                    Text("Aceptar", color = PurplePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDatePicker = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

fun iconoCategoria(categoria: String): ImageVector = when (categoria) {
    "Comida" -> Icons.Default.Fastfood
    "Café" -> Icons.Default.LocalCafe
    "Transporte" -> Icons.Default.DirectionsBus
    "Antojos" -> Icons.Default.Icecream
    else -> Icons.Default.ShoppingBag
}

fun formatoMoneda(monto: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    format.maximumFractionDigits = 0
    return format.format(monto)
}