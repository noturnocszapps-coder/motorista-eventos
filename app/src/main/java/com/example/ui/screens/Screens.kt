package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.PathEffect
import kotlin.math.pow
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entities.DriverPartner
import com.example.data.entities.DriverSettings
import com.example.data.entities.RideMessage
import com.example.data.entities.RideRequest
import com.example.ui.theme.*
import com.example.ui.viewmodel.RoxouViewModel
import kotlinx.coroutines.launch

// -----------------------------------------------------------------------------
// CORE ROUTING & PORTAL CONTAINER
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoxouAppPortal(viewModel: RoxouViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val driverStatus by viewModel.driverStatus.collectAsStateWithLifecycle()
    val driverSettings by viewModel.driverSettings.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf("login") }
    var selectedRideIdForDetail by remember { mutableStateOf<String?>(null) }
    var showProfileSelectionMenu by remember { mutableStateOf(false) }

    // Navigation lists inside active scopes
    val passengerTab = remember { mutableStateOf("home") }
    val adminTab = remember { mutableStateOf("dashboard") }

    Scaffold(
        topBar = {
            if (currentUser != null && currentScreen != "login") {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = androidx.compose.ui.text.buildAnnotatedString {
                                        append("ROXOU")
                                        pushStyle(androidx.compose.ui.text.SpanStyle(color = Color.White))
                                        append("RESERVA")
                                        pop()
                                    },
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = RoxouPrimaryLight,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            when (driverStatus?.status) {
                                                "online" -> RoxouOnlineGreen
                                                "ocupado" -> RoxouBusyOrange
                                                else -> RoxouOfflineGrey
                                            }
                                        )
                                        .size(8.dp)
                                )
                            }
                            Text(
                                text = "Motorista Privado",
                                fontSize = 11.sp,
                                color = RoxouGrayText,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    },
                    actions = {
                        // Profile Toggle Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(RoxouSurfaceVariant)
                                .clickable { showProfileSelectionMenu = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = when (currentUser?.role) {
                                    "admin" -> Icons.Default.Shield
                                    "parceiro" -> Icons.Default.DirectionsCar
                                    else -> Icons.Default.Person
                                },
                                contentDescription = "Profile icon",
                                tint = RoxouPrimaryLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentUser?.name?.take(8) ?: "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = RoxouBackground,
                        titleContentColor = Color.White
                    )
                )
            }
        },
        bottomBar = {
            if (currentUser != null && currentScreen != "login") {
                when (currentUser?.role) {
                    "passageiro" -> {
                        NavigationBar(
                            containerColor = RoxouNavBarBg,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .testTag("passenger_navbar")
                                .navigationBarsPadding()
                        ) {
                            NavigationBarItem(
                                selected = passengerTab.value == "home",
                                onClick = {
                                    passengerTab.value = "home"
                                    currentScreen = "passenger_portal"
                                },
                                icon = { 
                                    Icon(
                                        imageVector = Icons.Default.Home, 
                                        contentDescription = "Início"
                                    ) 
                                },
                                label = { Text("Início", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = RoxouPrimaryLight,
                                    selectedTextColor = RoxouPrimaryLight,
                                    indicatorColor = Color.White.copy(alpha = 0.05f),
                                    unselectedIconColor = RoxouGrayText,
                                    unselectedTextColor = RoxouGrayText
                                )
                            )
                            NavigationBarItem(
                                selected = passengerTab.value == "solicitar",
                                onClick = {
                                    passengerTab.value = "solicitar"
                                    currentScreen = "passenger_portal"
                                },
                                icon = { 
                                    Icon(
                                        imageVector = Icons.Default.AddCircle, 
                                        contentDescription = "Solicitar"
                                    ) 
                                },
                                label = { Text("Solicitar", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = RoxouPrimaryLight,
                                    selectedTextColor = RoxouPrimaryLight,
                                    indicatorColor = Color.White.copy(alpha = 0.05f),
                                    unselectedIconColor = RoxouGrayText,
                                    unselectedTextColor = RoxouGrayText
                                )
                            )
                            NavigationBarItem(
                                selected = passengerTab.value == "reservas",
                                onClick = {
                                    passengerTab.value = "reservas"
                                    currentScreen = "passenger_portal"
                                },
                                icon = { 
                                    Icon(
                                        imageVector = Icons.Default.History, 
                                        contentDescription = "Reservas"
                                    ) 
                                },
                                label = { Text("Minhas Viagens", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = RoxouPrimaryLight,
                                    selectedTextColor = RoxouPrimaryLight,
                                    indicatorColor = Color.White.copy(alpha = 0.05f),
                                    unselectedIconColor = RoxouGrayText,
                                    unselectedTextColor = RoxouGrayText
                                )
                            )
                        }
                    }
                    "admin" -> {
                        NavigationBar(
                            containerColor = RoxouNavBarBg,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .testTag("admin_navbar")
                                .navigationBarsPadding()
                        ) {
                            NavigationBarItem(
                                selected = adminTab.value == "dashboard",
                                onClick = {
                                    adminTab.value = "dashboard"
                                    currentScreen = "admin_portal"
                                },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Painel") },
                                label = { Text("Solicitações", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = RoxouPrimaryLight,
                                    selectedTextColor = RoxouPrimaryLight,
                                    indicatorColor = Color.White.copy(alpha = 0.05f),
                                    unselectedIconColor = RoxouGrayText,
                                    unselectedTextColor = RoxouGrayText
                                )
                            )
                            NavigationBarItem(
                                selected = adminTab.value == "agenda",
                                onClick = {
                                    adminTab.value = "agenda"
                                    currentScreen = "admin_portal"
                                },
                                icon = { Icon(Icons.Default.DateRange, contentDescription = "Agenda") },
                                label = { Text("Agenda", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = RoxouPrimaryLight,
                                    selectedTextColor = RoxouPrimaryLight,
                                    indicatorColor = Color.White.copy(alpha = 0.05f),
                                    unselectedIconColor = RoxouGrayText,
                                    unselectedTextColor = RoxouGrayText
                                )
                            )
                            NavigationBarItem(
                                selected = adminTab.value == "configuracoes",
                                onClick = {
                                    adminTab.value = "configuracoes"
                                    currentScreen = "admin_portal"
                                },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Valores") },
                                label = { Text("Configure", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = RoxouPrimaryLight,
                                    selectedTextColor = RoxouPrimaryLight,
                                    indicatorColor = Color.White.copy(alpha = 0.05f),
                                    unselectedIconColor = RoxouGrayText,
                                    unselectedTextColor = RoxouGrayText
                                )
                            )
                            NavigationBarItem(
                                selected = adminTab.value == "motoristas",
                                onClick = {
                                    adminTab.value = "motoristas"
                                    currentScreen = "admin_portal"
                                },
                                icon = { Icon(Icons.Default.Group, contentDescription = "Motoristas") },
                                label = { Text("Parceiros", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = RoxouPrimaryLight,
                                    selectedTextColor = RoxouPrimaryLight,
                                    indicatorColor = Color.White.copy(alpha = 0.05f),
                                    unselectedIconColor = RoxouGrayText,
                                    unselectedTextColor = RoxouGrayText
                                )
                            )
                        }
                    }
                    "parceiro" -> {
                        NavigationBar(
                            containerColor = RoxouNavBarBg,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .testTag("partner_navbar")
                                .navigationBarsPadding()
                        ) {
                            NavigationBarItem(
                                selected = true,
                                onClick = { currentScreen = "partner_portal" },
                                icon = { Icon(Icons.Default.DirectionsCar, contentDescription = "Minhas Escalas") },
                                label = { Text("Minhas Escalas", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = RoxouPrimaryLight,
                                    selectedTextColor = RoxouPrimaryLight,
                                    indicatorColor = Color.White.copy(alpha = 0.05f),
                                    unselectedIconColor = RoxouGrayText,
                                    unselectedTextColor = RoxouGrayText
                                )
                            )
                        }
                    }
                }
            }
        },
        containerColor = RoxouBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(RoxouBackground)
        ) {
            // Screen router
            when (currentScreen) {
                "login" -> LoginScreen(onLoginSuccess = { role ->
                    if (role == "admin") {
                        currentScreen = "admin_portal"
                        adminTab.value = "dashboard"
                    } else if (role == "parceiro") {
                        currentScreen = "partner_portal"
                    } else {
                        currentScreen = "passenger_portal"
                        passengerTab.value = "home"
                    }
                }, viewModel = viewModel)

                "passenger_portal" -> {
                    when (passengerTab.value) {
                        "home" -> PassengerHomeScreen(
                            viewModel = viewModel,
                            onNavigateToRequest = { passengerTab.value = "solicitar" },
                            onNavigateToReservations = { passengerTab.value = "reservas" }
                        )
                        "solicitar" -> PassengerRequestScreen(viewModel = viewModel, onRequestCreated = {
                            passengerTab.value = "reservas"
                        })
                        else -> PassengerReservationsScreen(viewModel = viewModel, onSelectRide = { id ->
                            selectedRideIdForDetail = id
                            currentScreen = "ride_detail"
                        })
                    }
                }

                "admin_portal" -> {
                    when (adminTab.value) {
                        "dashboard" -> AdminDashboardScreen(
                            viewModel = viewModel,
                            onSelectRide = { id ->
                                selectedRideIdForDetail = id
                                currentScreen = "ride_detail"
                            }
                        )
                        "agenda" -> AdminAgendaScreen(
                            viewModel = viewModel,
                            onSelectRide = { id ->
                                selectedRideIdForDetail = id
                                currentScreen = "ride_detail"
                            }
                        )
                        "configuracoes" -> AdminSettingsScreen(viewModel = viewModel)
                        "motoristas" -> AdminDriversScreen(viewModel = viewModel)
                    }
                }

                "partner_portal" -> {
                    PartnerDashboardScreen(
                        viewModel = viewModel,
                        onSelectRide = { id ->
                            selectedRideIdForDetail = id
                            currentScreen = "ride_detail"
                        }
                    )
                }

                "ride_detail" -> {
                    val id = selectedRideIdForDetail
                    if (id != null) {
                        RideDetailChatScreen(
                            requestId = id,
                            viewModel = viewModel,
                            onBack = {
                                when (currentUser?.role) {
                                    "admin" -> currentScreen = "admin_portal"
                                    "parceiro" -> currentScreen = "partner_portal"
                                    else -> currentScreen = "passenger_portal"
                                }
                            }
                        )
                    } else {
                        currentScreen = "login"
                    }
                }
            }

            // Floating Profile Swapper menu
            if (showProfileSelectionMenu) {
                ProfileSwapperDialog(
                    viewModel = viewModel,
                    onDismiss = { showProfileSelectionMenu = false },
                    onSwitched = { role ->
                        showProfileSelectionMenu = false
                        selectedRideIdForDetail = null
                        if (role == "admin") {
                            currentScreen = "admin_portal"
                            adminTab.value = "dashboard"
                        } else if (role == "parceiro") {
                            currentScreen = "partner_portal"
                        } else {
                            currentScreen = "passenger_portal"
                            passengerTab.value = "home"
                        }
                    }
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// PROFILE SWITCHER UTILITY DIALOG
// -----------------------------------------------------------------------------

@Composable
fun ProfileSwapperDialog(
    viewModel: RoxouViewModel,
    onDismiss: () -> Unit,
    onSwitched: (String) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = RoxouSurface,
            border = BorderStroke(1.dp, RoxouDivider),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Simulador de Perfis (MVP)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Text(
                    text = "Como o aplicativo é fechado, use os botões abaixo para alternar papéis e testar as interações de forma síncrona.",
                    fontSize = 12.sp,
                    color = RoxouGrayText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                Divider(color = RoxouDivider, modifier = Modifier.padding(bottom = 16.dp))

                // Profile 1: Passageiro
                ProfileSelectItem(
                    title = "Passageiro (Cliente)",
                    summary = "Maurício Souza • Solicitar, ver estimativa, chat",
                    icon = Icons.Default.Person,
                    isSelected = currentUser?.role == "passageiro",
                    onClick = {
                        viewModel.selectProfile("passageiro_id", "Maurício Souza", "mauricio@gmail.com", "passageiro")
                        onSwitched("passageiro")
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Profile 2: Motorista Admin
                ProfileSelectItem(
                    title = "Motorista Particular (Admin)",
                    summary = "Rax • Aprovar valores, gerenciar agenda, configurar preços",
                    icon = Icons.Default.Shield,
                    isSelected = currentUser?.role == "admin",
                    onClick = {
                        viewModel.selectProfile("admin_id", "Rax - Motorista", "atendimento@roxou.com.br", "admin")
                        onSwitched("admin")
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Profile 3: Motorista Parceiro
                ProfileSelectItem(
                    title = "Preposto / Motorista Parceiro",
                    summary = "Felipe Azevedo • Atender corridas atribuídas pelo Admin",
                    icon = Icons.Default.DirectionsCar,
                    isSelected = currentUser?.role == "parceiro",
                    onClick = {
                        viewModel.selectProfile("driver_partner_id", "Felipe Azevedo", "felipe.motorista@luxo.com", "parceiro")
                        onSwitched("parceiro")
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                TextButton(onClick = onDismiss) {
                    Text("Fechar", color = RoxouPrimaryLight)
                }
            }
        }
    }
}

@Composable
fun ProfileSelectItem(
    title: String,
    summary: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) RoxouSurfaceVariant else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) RoxouPrimary else RoxouDivider,
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isSelected) RoxouPrimary else RoxouSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else RoxouPrimaryLight,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1.0f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = summary,
                fontSize = 11.sp,
                color = RoxouGrayText
            )
        }
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = RoxouOnlineGreen,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// 1. TELA DE LOGIN (Premium Dark / Roxou Identity)
// -----------------------------------------------------------------------------

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    viewModel: RoxouViewModel
) {
    var loading by remember { mutableStateOf(false) }
    var showAccountChooser by remember { mutableStateOf(false) }
    var showCustomAccountInput by remember { mutableStateOf(false) }
    
    var customName by remember { mutableStateOf("") }
    var customEmail by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val handleSignIn: (String, String, String, String) -> Unit = { id, name, email, role ->
        showAccountChooser = false
        showCustomAccountInput = false
        loading = true
        errorMessage = ""
        
        viewModel.selectProfile(id, name, email, role) { success ->
            loading = false
            if (success) {
                val user = viewModel.currentUser.value
                val finalRole = user?.role ?: role
                onLoginSuccess(finalRole)
            } else {
                errorMessage = "Falha ao conectar com o Supabase. Usando modo de simulação local."
                viewModel.selectProfile(id, name, email, role) { _ ->
                    onLoginSuccess(role)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RoxouBackground)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper section: Brand Visual
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(RoxouPrimary, RoxouSecondary)
                        )
                    )
                    .size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "Roxou Logo",
                    tint = Color.White,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = androidx.compose.ui.text.buildAnnotatedString {
                    append("ROXOU")
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = Color.White))
                    append("RESERVA")
                    pop()
                },
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = RoxouPrimaryLight,
                letterSpacing = (-1).sp
            )

            Text(
                text = "Motorista Particular de Confiança",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = RoxouPrimaryLight,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Sistema privado online para agendamentos, orçamentos e atendimento particular. Perfeito para manter passageiro e motorista sincronizados.",
                fontSize = 13.sp,
                color = RoxouGrayText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                lineHeight = 18.sp
            )
        }

        // Center visual card
        Card(
            colors = CardDefaults.cardColors(containerColor = RoxouSurface),
            border = BorderStroke(1.dp, RoxouDivider),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Key value assertions
                LoginFeatureRow(icon = Icons.Default.Lock, text = "Canal 100% privado e fechado")
                LoginFeatureRow(icon = Icons.Default.CheckCircle, text = "Sincronização em Tempo Real (Supabase)")
                LoginFeatureRow(icon = Icons.Default.AccountBalanceWallet, text = "Preços calculados na regra oficial")
            }
        }

        // Bottom interactive button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = RoxouPrimaryLight,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (loading) {
                CircularProgressIndicator(color = RoxouPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Autenticando via Supabase Auth...", color = RoxouGrayText, fontSize = 12.sp)
            } else {
                Button(
                    onClick = {
                        showAccountChooser = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_login_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = RoxouPrimary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Google Logo Mock",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Entrar com Google",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.clickable {
                        // Directly trigger admin flow with seeded contacto.fh3 account or toggle swapper
                        handleSignIn("admin_id", "Felipe Azevedo (Rax)", "contato.fh3@gmail.com", "admin")
                    }
                ) {
                    Text(
                        text = "É o motorista particular? ",
                        fontSize = 12.sp,
                        color = RoxouGrayText
                    )
                    Text(
                        text = "Entrar no painel",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoxouPrimaryLight
                    )
                }
            }
        }
    }

    // Dynamic Google Account Chooser Dialog
    if (showAccountChooser) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAccountChooser = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = RoxouSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, RoxouDivider),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Fazer login com o Google",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Escolha um perfil para testar a sincronização online real:",
                        fontSize = 13.sp,
                        color = RoxouGrayText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    // Account Option 1: Admin Seed
                    Button(
                        onClick = {
                            handleSignIn("admin_id", "Felipe Azevedo", "contato.fh3@gmail.com", "admin")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x1F9C27B0)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Felipe Azevedo (Rax)", color = RoxouPrimaryLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("contato.fh3@gmail.com (ADMIN)", color = RoxouOnlineGreen, fontSize = 11.sp)
                        }
                    }

                    // Account Option 2: Active User Email
                    Button(
                        onClick = {
                            handleSignIn("active_user_id", "Noturno CSZ", "noturnocszapps@gmail.com", "passenger")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoxouSurfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Noturno CSZ (Você)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("noturnocszapps@gmail.com (PASSAGEIRO)", color = RoxouGrayText, fontSize = 11.sp)
                        }
                    }

                    // Account Option 3: Default Seed Passenger
                    Button(
                        onClick = {
                            handleSignIn("passageiro_id", "Maurício Souza", "mauricio@gmail.com", "passenger")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoxouSurfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Maurício Souza", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("mauricio@gmail.com (PASSAGEIRO)", color = RoxouGrayText, fontSize = 11.sp)
                        }
                    }

                    Divider(color = RoxouDivider, modifier = Modifier.padding(vertical = 8.dp))

                    TextButton(
                        onClick = {
                            showAccountChooser = false
                            showCustomAccountInput = true
                        }
                    ) {
                        Text("Usar outra conta Google...", color = RoxouPrimaryLight, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Custom Account Input Dialog
    if (showCustomAccountInput) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showCustomAccountInput = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = RoxouSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, RoxouDivider),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Configurar conta Google",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Nome Completo") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = RoxouPrimary,
                            unfocusedBorderColor = RoxouDivider
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = customEmail,
                        onValueChange = { customEmail = it },
                        label = { Text("E-mail Google") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = RoxouPrimary,
                            unfocusedBorderColor = RoxouDivider
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCustomAccountInput = false }) {
                            Text("Cancelar", color = RoxouGrayText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (customEmail.isNotBlank()) {
                                    val finalRole = if (customEmail.trim().equals("contato.fh3@gmail.com", ignoreCase = true)) "admin" else "passenger"
                                    val name = if (customName.isNotBlank()) customName else "Passageiro Extra"
                                    handleSignIn("custom_user_id_" + System.currentTimeMillis(), name, customEmail.trim().lowercase(), finalRole)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RoxouPrimary)
                        ) {
                            Text("Confirmar Log In")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginFeatureRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = RoxouPrimaryLight,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = RoxouOnSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

// -----------------------------------------------------------------------------
// 2. PASSAGEIRO: TELA INICIAL (HOME SCREEN)
// -----------------------------------------------------------------------------

@Composable
fun PassengerHomeScreen(
    viewModel: RoxouViewModel,
    onNavigateToRequest: () -> Unit,
    onNavigateToReservations: () -> Unit
) {
    val driverStatus by viewModel.driverStatus.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("passenger_home"),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper section: Brand and Greeting
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(RoxouPrimary, RoxouSecondary)
                        )
                    )
                    .size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "Roxou Logo",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Olá, ${currentUser?.name ?: "Ricardo"}",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Text(
                text = "Seja bem-vindo ao seu portal de agendamento de motorista particular.",
                fontSize = 14.sp,
                color = RoxouGrayText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp)
            )
        }

        // Driver Status Section (Online, Ocupado, Offline)
        Card(
            colors = CardDefaults.cardColors(containerColor = RoxouSurface),
            border = BorderStroke(1.dp, RoxouDivider),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "STATUS DO MOTORISTA PARTICULAR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoxouGrayText,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusColor = when (driverStatus?.status) {
                        "online" -> RoxouOnlineGreen
                        "ocupado" -> RoxouBusyOrange
                        else -> RoxouOfflineGrey
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(statusColor)
                            .size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (driverStatus?.status) {
                            "online" -> "Disponível (Online) para reserva"
                            "ocupado" -> "Ocupado em atendimento"
                            else -> "Offline (Indisponível no momento)"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Navigation Actions Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Button(
                onClick = onNavigateToRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("request_quote_entry_button"),
                colors = ButtonDefaults.buttonColors(containerColor = RoxouPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Solicitar orçamento",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            OutlinedButton(
                onClick = onNavigateToReservations,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("my_reservations_entry_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RoxouPrimaryLight),
                border = BorderStroke(1.dp, RoxouPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = RoxouPrimaryLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Minhas reservas",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoxouPrimaryLight
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 3. PASSAGEIRO: NOVO ORÇAMENTO SCREEN
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerRequestScreen(
    viewModel: RoxouViewModel,
    onRequestCreated: () -> Unit
) {
    val driverStatus by viewModel.driverStatus.collectAsStateWithLifecycle()
    val driverSettings by viewModel.driverSettings.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var dateTime by remember { mutableStateOf("") }
    var tripType by remember { mutableStateOf("ida") } // "ida", "volta", "ida_volta"
    var passengerCount by remember { mutableStateOf(1) }
    var notes by remember { mutableStateOf("") }
    
    var estimatedDistanceKmText by remember { mutableStateOf("12.0") }
    val estimatedDistanceKm by remember(estimatedDistanceKmText) {
        derivedStateOf {
            estimatedDistanceKmText.replace(",", ".").toDoubleOrNull() ?: 12.0
        }
    }

    LaunchedEffect(origin, destination) {
        if (origin.isNotBlank() && destination.isNotBlank()) {
            val p1 = com.example.data.repository.MapRoutingService.geocodeLocation(origin)
            val p2 = com.example.data.repository.MapRoutingService.geocodeLocation(destination)
            val distance = com.example.data.repository.MapRoutingService.calculateDistanceKm(p1, p2)
            estimatedDistanceKmText = "%.1f".format(distance)
        }
    }

    val estimatedDurationMinutes by remember(origin, destination) {
        derivedStateOf {
            if (origin.isNotBlank() && destination.isNotBlank()) {
                com.example.data.repository.MapRoutingService.estimateTimeMinutes(estimatedDistanceKm)
            } else {
                25
            }
        }
    }

    // Live reactive estimate
    val liveEstimate = viewModel.calculateLiveEstimate(estimatedDistanceKm, tripType, passengerCount)

    var showSuccessBanner by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("request_form"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Status Banner
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                // Greeting section matching Immersive UI
                Text(
                    text = androidx.compose.ui.text.buildAnnotatedString {
                        append("Olá, ")
                        pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = Color.White))
                        append(currentUser?.name ?: "Ricardo")
                        pop()
                        append(".")
                    },
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    color = RoxouOnBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Para onde vamos hoje?",
                    fontSize = 14.sp,
                    color = RoxouGrayText,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = RoxouSurface),
                    border = BorderStroke(1.dp, RoxouDivider),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "STATUS DO MOTORISTA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoxouGrayText,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    val statusColor = when (driverStatus?.status) {
                                        "online" -> RoxouOnlineGreen
                                        "ocupado" -> RoxouBusyOrange
                                        else -> RoxouOfflineGrey
                                    }
                                    if (driverStatus?.status == "online") {
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(statusColor.copy(alpha = 0.25f))
                                                .size(16.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(statusColor)
                                            .size(8.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (driverStatus?.status) {
                                        "online" -> "Motorista Online"
                                        "ocupado" -> "Motorista em Viagem"
                                        else -> "Motorista Fora de Serviço"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(RoxouSurfaceVariant)
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Min: R$ %.2f".format(driverSettings?.minPrice ?: 25.0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoxouPrimaryLight
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Solicitar Reserva Particular",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White
            )
            Text(
                text = "Insira os detalhes do trecho para cálculo imediato de nossa tarifa regulamentada.",
                fontSize = 12.sp,
                color = RoxouGrayText
            )
        }

        // Live interactive routing map
        if (origin.isNotBlank() && destination.isNotBlank()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Visualização de Rota Maps",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoxouPrimaryLight,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    RoxouInteractiveRouteMap(
                        originAddress = origin,
                        destinationAddress = destination,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Form Inputs
        item {
            OutlinedTextField(
                value = origin,
                onValueChange = { origin = it },
                label = { Text("Ponto de Origem") },
                placeholder = { Text("Ex: Aeroporto Internacional") },
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = RoxouPrimaryLight) },
                modifier = Modifier.fillMaxWidth().testTag("origin_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoxouPrimary,
                    unfocusedBorderColor = RoxouDivider,
                    focusedLabelColor = RoxouPrimaryLight,
                    unfocusedLabelColor = RoxouGrayText,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text("Ponto de Destino") },
                placeholder = { Text("Ex: Hotel Fênix ou Endereço Comercial") },
                leadingIcon = { Icon(Icons.Default.Navigation, contentDescription = null, tint = RoxouPrimaryLight) },
                modifier = Modifier.fillMaxWidth().testTag("destination_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoxouPrimary,
                    unfocusedBorderColor = RoxouDivider,
                    focusedLabelColor = RoxouPrimaryLight,
                    unfocusedLabelColor = RoxouGrayText,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = dateTime,
                onValueChange = { dateTime = it },
                label = { Text("Data e Horário") },
                placeholder = { Text("Ex: 30 de Maio às 14:00") },
                leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, tint = RoxouPrimaryLight) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoxouPrimary,
                    unfocusedBorderColor = RoxouDivider,
                    focusedLabelColor = RoxouPrimaryLight,
                    unfocusedLabelColor = RoxouGrayText,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Segmented Choose Box
        item {
            Column {
                Text(
                    text = "Tipo de Viagem",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoxouGrayText,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(RoxouSurfaceVariant)
                        .padding(4.dp)
                ) {
                    listOf("ida" to "Apenas Ida", "ida_volta" to "Ida & Volta").forEach { (type, label) ->
                        val isSelected = tripType == type
                        Box(
                            modifier = Modifier
                                .weight(1.0f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) RoxouPrimary else Color.Transparent)
                                .clickable { tripType = type }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else RoxouOnSurface
                            )
                        }
                    }
                }
            }
        }

        // Passenger selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Quantidade de Passageiros",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Máximo permitido: ${driverSettings?.maxPassengers ?: 4} pessoas.",
                        fontSize = 11.sp,
                        color = RoxouGrayText
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(RoxouSurfaceVariant)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = { if (passengerCount > 1) passengerCount-- },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Remove", tint = Color.White)
                    }
                    Text(
                        text = passengerCount.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    IconButton(
                        onClick = { if (passengerCount < (driverSettings?.maxPassengers ?: 4)) passengerCount++ },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                    }
                }
            }
        }

        // Automated Google Maps breakdown badge (Priority 1 satisfied)
        if (origin.isNotBlank() && destination.isNotBlank()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RoxouSurface),
                    border = BorderStroke(1.dp, RoxouDivider),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Distância Regulada Google Maps",
                                fontSize = 10.sp,
                                color = RoxouGrayText,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Distância: %.1f km".format(estimatedDistanceKm),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Tempo Estimado",
                                fontSize = 10.sp,
                                color = RoxouGrayText,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "%d minutos".format(estimatedDurationMinutes),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoxouPrimaryLight
                            )
                        }
                    }
                }
            }
        }

        // Manual Distance input representation (PRIORITY 4)
        item {
            OutlinedTextField(
                value = estimatedDistanceKmText,
                onValueChange = { estimatedDistanceKmText = it },
                label = { Text("Distância Estimada (km)") },
                placeholder = { Text("Ex: 15.5") },
                leadingIcon = { Icon(Icons.Default.Map, contentDescription = null, tint = RoxouPrimaryLight) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth().testTag("distance_km_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoxouPrimary,
                    unfocusedBorderColor = RoxouDivider,
                    focusedLabelColor = RoxouPrimaryLight,
                    unfocusedLabelColor = RoxouGrayText,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Notes and extra info
        item {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Mensagem Opcional / Observações") },
                placeholder = { Text("Ex: Preciso de cadeirinha para bebê, ou mala extra.") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoxouPrimary,
                    unfocusedBorderColor = RoxouDivider,
                    focusedLabelColor = RoxouPrimaryLight,
                    unfocusedLabelColor = RoxouGrayText,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )
        }

        // Live estimate breakdown cost
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(colors = listOf(RoxouPrimary, RoxouSecondary)))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SERVIÇO PRIVADO • REGULADO",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "R$ %.2f".format(liveEstimate),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Sinal Pix de 50%: R$ %.2f".format(liveEstimate / 2.0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoxouPrimaryLight
                            )
                            Text(
                                text = "Tarifa fixa oficial sem tarifas dinâmicas",
                                fontSize = 9.sp,
                                color = RoxouGrayText.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = null,
                            tint = RoxouOnlineGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Rules text card
        item {
            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(
                    text = "Regra de reserva privada:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoxouPrimaryLight
                )
                Text(
                    text = driverSettings?.prepaymentRules ?: "",
                    fontSize = 11.sp,
                    color = RoxouGrayText,
                    lineHeight = 14.sp
                )
            }
        }

        // Submit action trigger
        item {
            Button(
                onClick = {
                    if (origin.isNotBlank() && destination.isNotBlank()) {
                        viewModel.submitRequest(
                            origin = origin,
                            destination = destination,
                            dateTime = if (dateTime.isBlank()) "Em instantes" else dateTime,
                            tripType = tripType,
                            passengerCount = passengerCount,
                            notes = notes,
                            estimatedDistance = estimatedDistanceKm
                        )
                        showSuccessBanner = true
                        origin = ""
                        destination = ""
                        dateTime = ""
                        notes = ""
                    }
                },
                enabled = origin.isNotBlank() && destination.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_request_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoxouPrimary,
                    disabledContainerColor = RoxouDivider
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Confirmar & Solicitar Orçamento",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }

    // Success dialog callback
    if (showSuccessBanner) {
        AlertDialog(
            onDismissRequest = {
                showSuccessBanner = false
                onRequestCreated()
            },
            title = { Text("Preposto Enviado!", fontWeight = FontWeight.Bold) },
            text = { Text("O orçamento foi transmitido ao painel privado do motorista. Você receberá uma notificação visual em instantes.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessBanner = false
                        onRequestCreated()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoxouPrimary)
                ) {
                    Text("Ok, Ver Minhas Viagens", color = Color.White)
                }
            },
            containerColor = RoxouSurface,
            titleContentColor = Color.White,
            textContentColor = RoxouOnSurface
        )
    }
}

// -----------------------------------------------------------------------------
// 3. PASSAGEIRO: MINHAS RESERVAS SCREEN
// -----------------------------------------------------------------------------

@Composable
fun PassengerReservationsScreen(
    viewModel: RoxouViewModel,
    onSelectRide: (String) -> Unit
) {
    val activeUserRequests by viewModel.activeUserRequests.collectAsStateWithLifecycle()

    var activeFilterTab by remember { mutableStateOf("ativas") } // "ativas", "concluidas"

    val displayedRides = remember(activeUserRequests, activeFilterTab) {
        activeUserRequests.filter {
            if (activeFilterTab == "ativas") {
                it.status in listOf("pendente", "enviada", "aprovada", "confirmada")
            } else {
                it.status in listOf("recusada", "concluída", "cancelada")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Minhas Reservas Particular",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.White
        )
        Text(
            text = "Acompanhe orçamentos e converse com o motorista diretamente no chat.",
            fontSize = 12.sp,
            color = RoxouGrayText,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Custom filter pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1.0f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (activeFilterTab == "ativas") RoxouSurfaceVariant else RoxouSurface)
                    .border(1.dp, if (activeFilterTab == "ativas") RoxouPrimary else RoxouDivider, RoundedCornerShape(10.dp))
                    .clickable { activeFilterTab = "ativas" }
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ativas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (activeFilterTab == "ativas") Color.White else RoxouGrayText
                )
                Text(
                    text = "${activeUserRequests.count { it.status in listOf("pendente", "enviada", "aprovada", "confirmada") }} viagens",
                    fontSize = 10.sp,
                    color = RoxouGrayText
                )
            }

            Column(
                modifier = Modifier
                    .weight(1.0f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (activeFilterTab == "concluidas") RoxouSurfaceVariant else RoxouSurface)
                    .border(1.dp, if (activeFilterTab == "concluidas") RoxouPrimary else RoxouDivider, RoundedCornerShape(10.dp))
                    .clickable { activeFilterTab = "concluidas" }
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Histórico",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (activeFilterTab == "concluidas") Color.White else RoxouGrayText
                )
                Text(
                    text = "${activeUserRequests.count { it.status in listOf("recusada", "concluída", "cancelada") }} finalizadas",
                    fontSize = 10.sp,
                    color = RoxouGrayText
                )
            }
        }

        // Requests lists
        if (displayedRides.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AirportShuttle,
                        contentDescription = "No rides",
                        tint = RoxouSurfaceVariant,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Nenhuma viagem encontrada nesta aba",
                        color = RoxouOnSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Crie uma solicitação acima para iniciar.",
                        color = RoxouGrayText,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1.0f).testTag("requests_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayedRides) { ride ->
                    PassengerRideItemCard(ride = ride, onSelect = { onSelectRide(ride.id) })
                }
            }
        }
    }
}

@Composable
fun PassengerRideItemCard(
    ride: RideRequest,
    onSelect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RoxouSurface),
        border = BorderStroke(1.dp, RoxouDivider),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Upper row: Date and badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = RoxouPrimaryLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = ride.dateTime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Status Badge Custom
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (ride.status) {
                                "pendente" -> RoxouBusyOrange.copy(alpha = 0.2f)
                                "aprovada" -> RoxouPrimaryLight.copy(alpha = 0.2f)
                                "confirmada" -> RoxouOnlineGreen.copy(alpha = 0.2f)
                                "concluída" -> Color.Gray.copy(alpha = 0.2f)
                                "recusada" -> Color.Red.copy(alpha = 0.2f)
                                "cancelada" -> Color.Red.copy(alpha = 0.2f)
                                else -> RoxouSurfaceVariant
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = ride.status.uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        color = when (ride.status) {
                            "pendente" -> RoxouBusyOrange
                            "aprovada" -> RoxouPrimaryLight
                            "confirmada" -> RoxouOnlineGreen
                            "concluída" -> Color.White
                            "recusada" -> Color.Red
                            "cancelada" -> Color.Red
                            else -> RoxouGrayText
                        }
                    )
                }
            }

            Divider(color = RoxouDivider, modifier = Modifier.padding(vertical = 10.dp))

            // Route points info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(RoxouPrimary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = ride.origin,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    color = RoxouOnSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(RoxouOnlineGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = ride.destination,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    color = RoxouOnSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Price estimate and button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (ride.status == "pendente") "Orçamento Estimado:" else "Valor Confirmado:",
                        fontSize = 10.sp,
                        color = RoxouGrayText
                    )
                    Text(
                        text = "R$ %.2f".format(if (ride.status == "pendente") ride.priceEstimate else ride.finalPrice),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (ride.status == "pendente") RoxouOnSurface else RoxouOnlineGreen
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (ride.assignedDriverName != null) {
                        Text(
                            text = "Condutor: ${ride.assignedDriverName}",
                            fontSize = 11.sp,
                            color = RoxouGrayText,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RoxouPrimary)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Chat",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Conversar",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 4. DETALHE DA VIAGEM & CHAT EM TEMPO REAL
// -----------------------------------------------------------------------------

@Composable
fun RideDetailChatScreen(
    requestId: String,
    viewModel: RoxouViewModel,
    onBack: () -> Unit
) {
    val rideRequestFlow = remember(requestId) { viewModel.getRequestById(requestId) }
    val ride by rideRequestFlow.collectAsStateWithLifecycle(null)
    
    val messagesFlow = remember(requestId) { viewModel.getMessagesForRide(requestId) }
    val messages by messagesFlow.collectAsStateWithLifecycle(emptyList())

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val driverSettings by viewModel.driverSettings.collectAsStateWithLifecycle()

    var typedMessageText by remember { mutableStateOf("") }
    val messageListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Scroll chat to end when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            messageListState.animateScrollToItem(messages.size - 1)
        }
    }

    // Stop background chat synchronization loop when leaving this screen
    DisposableEffect(requestId) {
        onDispose {
            viewModel.stopChatSync(requestId)
        }
    }

    if (ride == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = RoxouPrimary)
        }
        return
    }

    val activeRide = ride!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RoxouBackground)
    ) {
        // Back Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RoxouSurface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1.0f)) {
                Text(
                    text = "Viagem para ${activeRide.passengerName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = activeRide.dateTime,
                    fontSize = 11.sp,
                    color = RoxouGrayText
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (activeRide.status) {
                            "pendente" -> RoxouBusyOrange.copy(alpha = 0.2f)
                            "aprovada" -> RoxouPrimaryLight.copy(alpha = 0.2f)
                            "confirmada" -> RoxouOnlineGreen.copy(alpha = 0.2f)
                            "concluída" -> Color.Gray.copy(alpha = 0.2f)
                            else -> Color.Red.copy(alpha = 0.2f)
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = activeRide.status.uppercase(),
                    color = when (activeRide.status) {
                        "pendente" -> RoxouBusyOrange
                        "aprovada" -> RoxouPrimaryLight
                        "confirmada" -> RoxouOnlineGreen
                        "concluída" -> Color.White
                        else -> Color.Red
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Details Panel & Booking Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = RoxouSurface),
            border = BorderStroke(1.dp, RoxouDivider),
            shape = RoundedCornerShape(0.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Route endpoints visual representation
                Row {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 4.dp, end = 10.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(RoxouPrimary))
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(RoxouDivider))
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(RoxouOnlineGreen))
                    }
                    Column {
                        Text(text = activeRide.origin, fontSize = 12.sp, maxLines = 1, color = RoxouOnSurface)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = activeRide.destination, fontSize = 12.sp, maxLines = 1, color = RoxouOnSurface)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = RoxouOnlineGreen, modifier = Modifier.size(16.dp))
                            Text(
                                text = "R$ %.2f".format(if (activeRide.status == "pendente") activeRide.priceEstimate else activeRide.finalPrice),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                        Text(
                            text = if (activeRide.tripType == "ida_volta") "Ida & Volta" else "Apenas Ida",
                            fontSize = 11.sp,
                            color = RoxouGrayText
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Condutor Escalado:",
                            fontSize = 10.sp,
                            color = RoxouGrayText
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(RoxouSurfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = activeRide.assignedDriverName ?: "A designar (Admin)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoxouPrimaryLight
                            )
                        }
                    }
                }

                if (activeRide.notes.isNotBlank()) {
                    Divider(color = RoxouDivider, modifier = Modifier.padding(vertical = 10.dp))
                    Text(
                        text = "Notas: ${activeRide.notes}",
                        fontSize = 11.sp,
                        color = RoxouGrayText
                    )
                }

                // If approved and not paid yet (Pix instructions simulator)
                if (activeRide.status == "aprovada" && !activeRide.paymentConfirmed && currentUser?.role == "passageiro") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = RoxouPrimary.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, RoxouPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Aprovado! Garanta sua agenda com o sinal:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoxouPrimaryLight
                            )
                            Text(
                                text = "A reserva foi aceita. Faça o Pix do sinal de 50%% (R$ %.2f) para o Pix do motorista e envie comprovante no chat abaixo.".format(activeRide.finalPrice / 2.0),
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Text(
                                text = "Chave Pix Celular: (Simulador) Interno do App",
                                fontSize = 10.sp,
                                color = RoxouGrayText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Real-Time Message Feed
        LazyColumn(
            state = messageListState,
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp).testTag("chat_messages_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                val isMe = message.senderId == currentUser?.id
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 14.dp,
                                    topEnd = 14.dp,
                                    bottomStart = if (isMe) 14.dp else 2.dp,
                                    bottomEnd = if (isMe) 2.dp else 14.dp
                                )
                            )
                            .background(
                                if (isMe) RoxouPrimary else RoxouSurfaceVariant
                            )
                            .border(
                                1.dp,
                                if (isMe) Color.Transparent else RoxouDivider,
                                RoundedCornerShape(
                                    topStart = 14.dp,
                                    topEnd = 14.dp,
                                    bottomStart = if (isMe) 14.dp else 2.dp,
                                    bottomEnd = if (isMe) 2.dp else 14.dp
                                )
                            )
                            .padding(12.dp)
                            .widthIn(max = 280.dp)
                    ) {
                        Column {
                            if (!isMe) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = message.senderName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = RoxouPrimaryLight
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(RoxouDivider)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = message.senderRole.uppercase(),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RoxouGrayText
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                            }
                            Text(
                                text = message.message,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Chat input controller
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RoxouSurface)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = typedMessageText,
                onValueChange = { typedMessageText = it },
                placeholder = { Text("Digite sua mensagem...") },
                modifier = Modifier
                    .weight(1.0f)
                    .testTag("chat_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoxouPrimary,
                    unfocusedBorderColor = RoxouDivider,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = RoxouBackground,
                    unfocusedContainerColor = RoxouBackground
                ),
                shape = RoundedCornerShape(20.dp),
                maxLines = 2
            )

            Spacer(modifier = Modifier.width(10.dp))

            FloatingActionButton(
                onClick = {
                    if (typedMessageText.isNotBlank()) {
                        viewModel.sendChatMessage(activeRide.id, typedMessageText)
                        typedMessageText = ""
                    }
                },
                containerColor = RoxouPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(44.dp).testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 5. MOTORISTA ADMIN: DASHBOARD PRINCIPAL
// -----------------------------------------------------------------------------

@Composable
fun AdminDashboardScreen(
    viewModel: RoxouViewModel,
    onSelectRide: (String) -> Unit
) {
    val allRequests by viewModel.allRequests.collectAsStateWithLifecycle()
    val driverStatus by viewModel.driverStatus.collectAsStateWithLifecycle()
    val partners by viewModel.partners.collectAsStateWithLifecycle()

    var editingRideForPriceApprove by remember { mutableStateOf<RideRequest?>(null) }
    var inputPriceText by remember { mutableStateOf("") }
    
    var rejectingRideWithReason by remember { mutableStateOf<RideRequest?>(null) }
    var inputRejectReasonText by remember { mutableStateOf("") }

    var showingPartnerAssignByRequest by remember { mutableStateOf<RideRequest?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp).testTag("admin_dashboard_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Status Controller
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = RoxouSurface),
                border = BorderStroke(1.dp, RoxouDivider),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Status de Disponibilidade Pública",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoxouGrayText
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("online" to "Online", "ocupado" to "Ocupado", "offline" to "Offline").forEach { (v, l) ->
                            val isSelected = driverStatus?.status == v
                            Box(
                                modifier = Modifier
                                    .weight(1.0f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) {
                                            when (v) {
                                                "online" -> RoxouOnlineGreen
                                                "ocupado" -> RoxouBusyOrange
                                                else -> RoxouOfflineGrey
                                            }
                                        } else RoxouSurfaceVariant
                                    )
                                    .clickable { viewModel.updateDriverStatus(v) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = l,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) {
                                        if (v == "online") Color.Black else Color.White
                                    } else RoxouOnSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fast Stats Grid row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(RoxouSurface)
                        .border(1.dp, RoxouDivider, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text("Pendentes", fontSize = 11.sp, color = RoxouGrayText)
                        Text(
                            text = "${allRequests.count { it.status == "pendente" }}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = RoxouBusyOrange
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(RoxouSurface)
                        .border(1.dp, RoxouDivider, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text("Confirmadas", fontSize = 11.sp, color = RoxouGrayText)
                        Text(
                            text = "${allRequests.count { it.status == "confirmada" }}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = RoxouOnlineGreen
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(RoxouSurface)
                        .border(1.dp, RoxouDivider, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text("Geral (Total)", fontSize = 11.sp, color = RoxouGrayText)
                        Text(
                            text = "${allRequests.size}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = RoxouPrimaryLight
                        )
                    }
                }
            }
        }

        // Section Title: Orcamentos Pendentes
        item {
            Text(
                text = "Orçamentos Pendentes de Aprovação",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        }

        val pendingRides = allRequests.filter { it.status == "pendente" }
        if (pendingRides.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RoxouSurface),
                    border = BorderStroke(1.dp, RoxouDivider),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Nenhum orçamento pendente.", color = RoxouGrayText, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(pendingRides) { request ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = RoxouSurface),
                    border = BorderStroke(1.dp, RoxouDivider),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Passageiro: ${request.passengerName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = request.passengerEmail,
                                    fontSize = 10.sp,
                                    color = RoxouGrayText
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(RoxouSurfaceVariant)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "%.1f km".format(request.estimatedKm),
                                    fontSize = 11.sp,
                                    color = RoxouPrimaryLight,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Divider(color = RoxouDivider, modifier = Modifier.padding(vertical = 10.dp))

                        // Route
                        Text(text = "Origem: ${request.origin}", fontSize = 11.sp, color = RoxouOnSurface)
                        Text(text = "Destino: ${request.destination}", fontSize = 11.sp, color = RoxouOnSurface, modifier = Modifier.padding(top = 2.dp))
                        Text(text = "Data/Hora: ${request.dateTime}", fontSize = 11.sp, color = RoxouPrimaryLight, modifier = Modifier.padding(top = 4.dp), fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Estimativa Base:", fontSize = 10.sp, color = RoxouGrayText)
                                Text("R$ %.2f".format(request.priceEstimate), fontSize = 18.sp, color = RoxouOnlineGreen, fontWeight = FontWeight.Black)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        editingRideForPriceApprove = request
                                        inputPriceText = "%.2f".format(request.priceEstimate)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoxouPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Aprovar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        rejectingRideWithReason = request
                                        inputRejectReasonText = "Agenda cheia para esta data"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Recusar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Active Bookings Queue
        item {
            Text(
                text = "Cronograma / Reservas Confirmadas",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        }

        val activeRides = allRequests.filter { it.status in listOf("aprovada", "confirmada", "concluída") }
        if (activeRides.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RoxouSurface),
                    border = BorderStroke(1.dp, RoxouDivider),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Nenhuma viagem ativa agendada.", color = RoxouGrayText, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(activeRides) { request ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = RoxouSurface),
                    border = BorderStroke(1.dp, RoxouDivider),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = request.passengerName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (request.paymentConfirmed) RoxouOnlineGreen.copy(alpha = 0.2f)
                                                else RoxouBusyOrange.copy(alpha = 0.2f)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (request.paymentConfirmed) "Sinal Pago" else "Pix Pendente",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (request.paymentConfirmed) RoxouOnlineGreen else RoxouBusyOrange
                                        )
                                    }
                                }
                                Text(
                                    text = "Valor Final: R$ %.2f".format(request.finalPrice),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoxouOnlineGreen
                                )
                            }

                            // Quick status label click to detail
                            Button(
                                onClick = { onSelectRide(request.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = RoxouSurfaceVariant),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(12.dp), tint = RoxouPrimaryLight)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ver Chat", fontSize = 10.sp, color = RoxouPrimaryLight)
                                }
                            }
                        }

                        Divider(color = RoxouDivider, modifier = Modifier.padding(vertical = 10.dp))

                        Text(text = "De: ${request.origin}", fontSize = 11.sp, color = RoxouOnSurface)
                        Text(text = "Para: ${request.destination}", fontSize = 11.sp, color = RoxouOnSurface)
                        Text(text = "Partida: ${request.dateTime}", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))

                        // Assigned driver row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(RoxouSurfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Condutor: ${request.assignedDriverName ?: "Você (Motorista Principal)"}",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Alterar",
                                fontSize = 10.sp,
                                color = RoxouPrimaryLight,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { showingPartnerAssignByRequest = request }
                            )
                        }

                        // Flow actions
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!request.paymentConfirmed) {
                                Button(
                                    onClick = { viewModel.setPaymentConfirmed(request.id, true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoxouOnlineGreen),
                                    modifier = Modifier.weight(1.0f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Text("Confirmar Sinal", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            } else if (request.status == "aprovada") {
                                Button(
                                    onClick = { viewModel.updateRequestStatus(request.id, "confirmada") },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoxouPrimary),
                                    modifier = Modifier.weight(1.0f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Text("Confirmar Agenda", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (request.status == "confirmada") {
                                Button(
                                    onClick = { viewModel.updateRequestStatus(request.id, "concluída") },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoxouOnlineGreen),
                                    modifier = Modifier.weight(1.0f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Text("Finalizar Viagem", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { viewModel.updateRequestStatus(request.id, "cancelada") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                                modifier = Modifier.weight(1.0f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Text("Cancelar", fontSize = 10.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Approve Quote price editor
    if (editingRideForPriceApprove != null) {
        val ride = editingRideForPriceApprove!!
        Dialog(onDismissRequest = { editingRideForPriceApprove = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = RoxouSurface,
                border = BorderStroke(1.dp, RoxouDivider),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Aprovar Orçamento Privado",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Ajuste o valor final calculado com base em particularidades, se houver.",
                        fontSize = 11.sp,
                        color = RoxouGrayText,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = inputPriceText,
                        onValueChange = { inputPriceText = it },
                        label = { Text("Valor Final Ajustado (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("add_price_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = RoxouPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { editingRideForPriceApprove = null }) {
                            Text("Cancelar", color = RoxouGrayText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val price = inputPriceText.toDoubleOrNull() ?: ride.priceEstimate
                                viewModel.approveQuote(ride.id, price)
                                editingRideForPriceApprove = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RoxouPrimary)
                        ) {
                            Text("Aprovar e Enviar", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Modal: Decline Quote reason
    if (rejectingRideWithReason != null) {
        val ride = rejectingRideWithReason!!
        Dialog(onDismissRequest = { rejectingRideWithReason = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = RoxouSurface,
                border = BorderStroke(1.dp, RoxouDivider),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Recusar Solicitação",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Informe ao cliente o motivo do cancelamento / recusa de seu orçamento particular.",
                        fontSize = 11.sp,
                        color = RoxouGrayText,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = inputRejectReasonText,
                        onValueChange = { inputRejectReasonText = it },
                        label = { Text("Motivo / Explicação") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = RoxouPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { rejectingRideWithReason = null }) {
                            Text("Voltar", color = RoxouGrayText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.rejectQuote(ride.id, inputRejectReasonText)
                                rejectingRideWithReason = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Confirmar Recusa", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Modal: Assigned driver helper
    if (showingPartnerAssignByRequest != null) {
        val ride = showingPartnerAssignByRequest!!
        Dialog(onDismissRequest = { showingPartnerAssignByRequest = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = RoxouSurface,
                border = BorderStroke(1.dp, RoxouDivider),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Selecione o Condutor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Distribua esta reserva para motoristas parceiros cadastrados:",
                        fontSize = 11.sp,
                        color = RoxouGrayText,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Partner selected list
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Option 1: Owner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (ride.assignedDriverId == null) RoxouPrimary else RoxouSurfaceVariant)
                                .clickable {
                                    viewModel.assignDriver(ride.id, null)
                                    showingPartnerAssignByRequest = null
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Você (Motorista Principal)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            if (ride.assignedDriverId == null) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                            }
                        }

                        // Options 2+: Partners
                        partners.forEach { partner ->
                            val isSelected = ride.assignedDriverId == partner.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) RoxouPrimary else RoxouSurfaceVariant)
                                    .clickable {
                                        viewModel.assignDriver(ride.id, partner)
                                        showingPartnerAssignByRequest = null
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(partner.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Status: Ativo • Nota: %.1f".format(partner.rating), color = RoxouGrayText, fontSize = 10.sp)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        modifier = Modifier.align(Alignment.End),
                        onClick = { showingPartnerAssignByRequest = null }
                    ) {
                        Text("Fechar", color = RoxouPrimaryLight)
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 6. MOTORISTA ADMIN: AGENDA (DAY / WEEK / MONTH VIEW)
// -----------------------------------------------------------------------------

@Composable
fun AdminAgendaScreen(
    viewModel: RoxouViewModel,
    onSelectRide: (String) -> Unit
) {
    val allRequests by viewModel.allRequests.collectAsStateWithLifecycle()
    
    var agendaTab by remember { mutableStateOf("hoje") } // "hoje", "semana", "mes"

    val filteredAgenda = remember(allRequests, agendaTab) {
        allRequests.filter { it.status in listOf("aprovada", "confirmada") }.filter {
            // Simulated day classification based on raw dates
            when (agendaTab) {
                "hoje" -> it.dateTime.contains("30") || it.dateTime.contains("Hoje") || it.dateTime.contains("instantes")
                "semana" -> !it.dateTime.contains("28") && !it.dateTime.contains("27")
                else -> true // Entire list
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Minha Agenda Particular",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.White
        )
        Text(
            text = "Visualize seu cronograma fechado de atendimento personalizado.",
            fontSize = 12.sp,
            color = RoxouGrayText,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Triple styled Segmented selectors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RoxouSurface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("hoje" to "Hoje/Próximos", "semana" to "Esta Semana", "mes" to "Mês / Tudo").forEach { (v, l) ->
                val isSelected = agendaTab == v
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) RoxouPrimary else Color.Transparent)
                        .clickable { agendaTab = v }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = l,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else RoxouOnSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredAgenda.isEmpty()) {
            Box(
                modifier = Modifier.weight(1.0f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = RoxouSurfaceVariant, modifier = Modifier.size(60.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Sem compromissos nesta aba da agenda.", color = RoxouGrayText, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1.0f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredAgenda) { ride ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = RoxouSurface),
                        border = BorderStroke(1.dp, RoxouDivider),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectRide(ride.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Column 1: Time visual indicator
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(RoxouSurfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .width(70.dp)
                            ) {
                                Text(
                                    text = if (ride.dateTime.contains(" às ")) ride.dateTime.substringAfter(" às ") else "Partida",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = RoxouPrimaryLight,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (ride.dateTime.contains(" de ")) ride.dateTime.substringBefore(" às ").take(6) else "Hoje",
                                    fontSize = 10.sp,
                                    color = RoxouGrayText,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Column 2: Route info
                            Column(modifier = Modifier.weight(1.0f)) {
                                Text(
                                    text = "Cli: ${ride.passengerName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Origem: ${ride.origin}",
                                    fontSize = 11.sp,
                                    color = RoxouGrayText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Destino: ${ride.destination}",
                                    fontSize = 11.sp,
                                    color = RoxouGrayText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (ride.assignedDriverName != null) {
                                    Text(
                                        text = "Condutor asignado: ${ride.assignedDriverName}",
                                        fontSize = 10.sp,
                                        color = RoxouOnlineGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Dynamic Status Arrow check
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Detail",
                                tint = RoxouGrayText
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 7. MOTORISTA ADMIN: CONFIGURAÇÃO DE PREÇOS
// -----------------------------------------------------------------------------

@Composable
fun AdminSettingsScreen(viewModel: RoxouViewModel) {
    val driverSettings by viewModel.driverSettings.collectAsStateWithLifecycle()

    var minPrice by remember { mutableStateOf("") }
    var pricePerKm by remember { mutableStateOf("") }
    var roundTripFee by remember { mutableStateOf("") }
    var waitFee by remember { mutableStateOf("") }
    var nightFee by remember { mutableStateOf("") }
    var maxPassengers by remember { mutableStateOf("") }
    var waitToleranceMinutes by remember { mutableStateOf("") }
    var prepaymentRules by remember { mutableStateOf("") }
    var cancellationRules by remember { mutableStateOf("") }

    // Synchronize initial input loads when settings flow updates
    LaunchedEffect(driverSettings) {
        val s = driverSettings
        if (s != null) {
            minPrice = s.minPrice.toString()
            pricePerKm = s.pricePerKm.toString()
            roundTripFee = s.roundTripFee.toString()
            waitFee = s.waitFee.toString()
            nightFee = s.nightFee.toString()
            maxPassengers = s.maxPassengers.toString()
            waitToleranceMinutes = s.waitToleranceMinutes.toString()
            prepaymentRules = s.prepaymentRules
            cancellationRules = s.cancellationRules
        }
    }

    var showSavedToast by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Configurações de Preços & Regras",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White
            )
            Text(
                text = "Configure os valores de sua tabela privada de agendamentos. Clientes verão o valor estimado em tempo real antes da solicitação.",
                fontSize = 12.sp,
                color = RoxouGrayText
            )
        }

        // Section 1: Preços base
        item {
            Text("Parâmetros Gerais de Cobrança", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RoxouPrimaryLight)
            Spacer(modifier = Modifier.height(4.dp))
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = minPrice,
                    onValueChange = { minPrice = it },
                    label = { Text("Valor Mínimo (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.0f),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = RoxouPrimary),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = pricePerKm,
                    onValueChange = { pricePerKm = it },
                    label = { Text("Valor por KM (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.0f),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = RoxouPrimary),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = roundTripFee,
                    onValueChange = { roundTripFee = it },
                    label = { Text("Taxa Ida e Volta (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.0f),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = RoxouPrimary),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = waitFee,
                    onValueChange = { waitFee = it },
                    label = { Text("Taxa de Espera (R$/h)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.0f),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = RoxouPrimary),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nightFee,
                    onValueChange = { nightFee = it },
                    label = { Text("Adicional Noturno (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.0f),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = RoxouPrimary),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = waitToleranceMinutes,
                    onValueChange = { waitToleranceMinutes = it },
                    label = { Text("Tolerância Espera (min)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.0f),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = RoxouPrimary),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        item {
            OutlinedTextField(
                value = maxPassengers,
                onValueChange = { maxPassengers = it },
                label = { Text("Número Máximo de Passageiros") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = RoxouPrimary),
                shape = RoundedCornerShape(10.dp)
            )
        }

        // Section 2: Regras e Textos
        item {
            Divider(color = RoxouDivider, modifier = Modifier.padding(vertical = 4.dp))
            Text("Políticas & Regras Contratuais", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RoxouPrimaryLight)
            Spacer(modifier = Modifier.height(4.dp))
        }

        item {
            OutlinedTextField(
                value = prepaymentRules,
                onValueChange = { prepaymentRules = it },
                label = { Text("Texto Padrão de Adiantamento") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = RoxouPrimary),
                shape = RoundedCornerShape(10.dp),
                maxLines = 4
            )
        }

        item {
            OutlinedTextField(
                value = cancellationRules,
                onValueChange = { cancellationRules = it },
                label = { Text("Texto Regulamento de Cancelamento") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = RoxouPrimary),
                shape = RoundedCornerShape(10.dp),
                maxLines = 4
            )
        }

        // Action Trigger
        item {
            Button(
                onClick = {
                    val settings = com.example.data.entities.DriverSettings(
                        minPrice = minPrice.toDoubleOrNull() ?: 25.0,
                        pricePerKm = pricePerKm.toDoubleOrNull() ?: 4.5,
                        roundTripFee = roundTripFee.toDoubleOrNull() ?: 15.0,
                        waitFee = waitFee.toDoubleOrNull() ?: 10.0,
                        nightFee = nightFee.toDoubleOrNull() ?: 12.0,
                        maxPassengers = maxPassengers.toIntOrNull() ?: 4,
                        waitToleranceMinutes = waitToleranceMinutes.toIntOrNull() ?: 15,
                        prepaymentRules = prepaymentRules,
                        cancellationRules = cancellationRules
                    )
                    viewModel.updateDriverSettings(settings)
                    showSavedToast = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoxouPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Gravar Configurações de Preços", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }

    if (showSavedToast) {
        AlertDialog(
            onDismissRequest = { showSavedToast = false },
            title = { Text("Configurações Salvas!", fontWeight = FontWeight.Bold) },
            text = { Text("As novas tabelas de tarifas foram aplicadas imediatamente em toda a rede local.") },
            confirmButton = {
                TextButton(onClick = { showSavedToast = false }) {
                    Text("OK", color = RoxouPrimaryLight)
                }
            },
            containerColor = RoxouSurface,
            titleContentColor = Color.White,
            textContentColor = RoxouOnSurface
        )
    }
}

// -----------------------------------------------------------------------------
// 8. MOTORISTA ADMIN: MOTORISTAS PARCEIROS
// -----------------------------------------------------------------------------

@Composable
fun AdminDriversScreen(viewModel: RoxouViewModel) {
    val partners by viewModel.partners.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    var showingAddForm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Motoristas Parceiros",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
                Text(
                    text = "Delegue escalas para terceiros de forma privada.",
                    fontSize = 12.sp,
                    color = RoxouGrayText
                )
            }

            IconButton(
                onClick = { showingAddForm = !showingAddForm },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(RoxouPrimary)
            ) {
                Icon(
                    imageVector = if (showingAddForm) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = "Add driver toggle",
                    tint = Color.White
                )
            }
        }

        // Expandable input form
        AnimatedVisibility(
            visible = showingAddForm,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = RoxouSurface),
                border = BorderStroke(1.dp, RoxouDivider),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cadastrar Motorista Parceiro", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome Completo") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = RoxouPrimary),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail para Acesso Google") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = RoxouPrimary),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Celular / Contato") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = RoxouPrimary),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (name.isNotBlank() && email.isNotBlank()) {
                                viewModel.addPartnerDriver(name, email, phone)
                                name = ""
                                email = ""
                                phone = ""
                                showingAddForm = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoxouPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cadastrar Condutor", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // List
        LazyColumn(
            modifier = Modifier.weight(1.0f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(partners) { partner ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = RoxouSurface),
                    border = BorderStroke(1.dp, RoxouDivider),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(RoxouSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = RoxouPrimaryLight)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(partner.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Email: ${partner.email}", color = RoxouGrayText, fontSize = 11.sp)
                                if (partner.phone.isNotBlank()) {
                                    Text("Tel: ${partner.phone}", color = RoxouGrayText, fontSize = 11.sp)
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = "Rating", tint = RoxouBusyOrange, modifier = Modifier.size(16.dp))
                            Text(
                                text = "%.1f".format(partner.rating),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            IconButton(onClick = { viewModel.removePartnerDriver(partner.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = Color.Red.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 9. MOTORISTA PARCEIRO: DASHBOARD DE ESCALAS ATRIBUÍDAS
// -----------------------------------------------------------------------------

@Composable
fun PartnerDashboardScreen(
    viewModel: RoxouViewModel,
    onSelectRide: (String) -> Unit
) {
    val activeUserRequests by viewModel.activeUserRequests.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Minhas Escalas Atribuídas",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.White
        )
        Text(
            text = "Olá ${currentUser?.name}! Abaixo estão as viagens designadas pelo Motorista Principal.",
            fontSize = 12.sp,
            color = RoxouGrayText,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (activeUserRequests.isEmpty()) {
            Box(
                modifier = Modifier.weight(1.0f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = RoxouSurfaceVariant, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Nenhuma escala atribuída no momento.",
                        color = RoxouOnSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Aguarde instruções via painel do administrador.",
                        color = RoxouGrayText,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1.0f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(activeUserRequests) { request ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = RoxouSurface),
                        border = BorderStroke(1.dp, RoxouDivider),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectRide(request.id) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Upper Date
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(request.dateTime, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoxouPrimaryLight)

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(RoxouPrimary.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(request.status.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = RoxouPrimaryLight)
                                }
                            }

                            Divider(color = RoxouDivider, modifier = Modifier.padding(vertical = 10.dp))

                            Text("Cliente: ${request.passengerName}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("De: ${request.origin}", fontSize = 12.sp, color = RoxouOnSurface)
                            Text("Para: ${request.destination}", fontSize = 12.sp, color = RoxouOnSurface)

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { onSelectRide(request.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoxouPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Chat & Detalhes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (request.status == "confirmada") {
                                    Button(
                                        onClick = { viewModel.updateRequestStatus(request.id, "concluída") },
                                        colors = ButtonDefaults.buttonColors(containerColor = RoxouOnlineGreen),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Finalizar Viagem", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Interactive Google Maps / Routing Canvas Simulation Component
// -----------------------------------------------------------------------------
@Composable
fun RoxouInteractiveRouteMap(
    originAddress: String,
    destinationAddress: String,
    liveLocation: com.example.data.entities.DriverLiveLocation? = null,
    modifier: Modifier = Modifier
) {
    val originGeo = remember(originAddress) { com.example.data.repository.MapRoutingService.geocodeLocation(originAddress) }
    val destGeo = remember(destinationAddress) { com.example.data.repository.MapRoutingService.geocodeLocation(destinationAddress) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(RoxouSurface)
            .border(BorderStroke(1.dp, RoxouDivider), RoundedCornerShape(16.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = this.size.width
            val height = this.size.height

            // 1. Technical background grid lines
            val gridColor = RoxouDivider.copy(alpha = 0.3f)
            val gridSpacing = maxOf(this.run { 40.dp.toPx() }, 10f)
            var x = 0f
            while (x < width) {
                drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(x, 0f), end = androidx.compose.ui.geometry.Offset(x, height), strokeWidth = 1f)
                x += gridSpacing
            }
            var y = 0f
            while (y < height) {
                drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(width, y), strokeWidth = 1f)
                y += gridSpacing
            }

            // 2. Define coordinate pin placements
            val originX = width * 0.25f
            val originY = height * 0.7f
            val destX = width * 0.75f
            val destY = height * 0.3f

            // 3. Draw route - quadratic bezier curved route
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(originX, originY)
                quadraticTo(width * 0.45f, height * 0.2f, destX, destY)
            }

            // Route neon glow backing
            drawPath(
                path = path,
                color = RoxouPrimary.copy(alpha = 0.2f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = this.run { 8.dp.toPx() },
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )

            // Dynamic core route path (dotted)
            drawPath(
                path = path,
                color = RoxouPrimaryLight,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = this.run { 2.5.dp.toPx() },
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                )
            )

            // 4. Origin Indicator (Online Green color accent)
            drawCircle(
                color = RoxouOnlineGreen.copy(alpha = 0.25f),
                radius = this.run { 12.dp.toPx() },
                center = androidx.compose.ui.geometry.Offset(originX, originY)
            )
            drawCircle(
                color = RoxouOnlineGreen,
                radius = this.run { 5.dp.toPx() },
                center = androidx.compose.ui.geometry.Offset(originX, originY)
            )

            // 5. Destination Indicator (Red / Purple neon)
            drawCircle(
                color = RoxouPrimary.copy(alpha = 0.25f),
                radius = this.run { 12.dp.toPx() },
                center = androidx.compose.ui.geometry.Offset(destX, destY)
            )
            drawCircle(
                color = RoxouPrimaryLight,
                radius = this.run { 5.dp.toPx() },
                center = androidx.compose.ui.geometry.Offset(destX, destY)
            )

            // 6. Live Moving Driver pin with pulsing radar circle
            if (liveLocation != null) {
                val progress = when (liveLocation.status) {
                    "a_caminho" -> 0.0
                    "chegou" -> 0.0
                    else -> {
                        val totalLat = destGeo.latitude - originGeo.latitude
                        val currLat = liveLocation.latitude - originGeo.latitude
                        if (totalLat != 0.0) (currLat / totalLat).coerceIn(0.0..1.0) else 0.5
                    }
                }

                val t = progress.toFloat()
                val ctrlX = width * 0.45f
                val ctrlY = height * 0.2f

                val driverX: Float
                val driverY: Float

                if (liveLocation.status == "a_caminho") {
                    val totalLat = originGeo.latitude - com.example.data.repository.MapRoutingService.CURRENT_USER_LOCATION.latitude
                    val currLat = liveLocation.latitude - com.example.data.repository.MapRoutingService.CURRENT_USER_LOCATION.latitude
                    val walkT = if (totalLat != 0.0) (currLat / totalLat).coerceIn(0.0..1.0).toFloat() else 0.5f
                    driverX = (width * 0.5f) + (originX - (width * 0.5f)) * walkT
                    driverY = (height * 0.5f) + (originY - (height * 0.5f)) * walkT
                } else if (liveLocation.status == "chegou") {
                    driverX = originX
                    driverY = originY
                } else {
                    driverX = (1 - t).pow(2) * originX + 2 * (1 - t) * t * ctrlX + t.pow(2) * destX
                    driverY = (1 - t).pow(2) * originY + 2 * (1 - t) * t * ctrlY + t.pow(2) * destY
                }

                // Radar expansion ring
                val sinCycle = kotlin.math.sin(System.currentTimeMillis() / 250.0).toFloat()
                val pulseRadius = this.run { 14.dp.toPx() } + (sinCycle * this.run { 4.dp.toPx() })
                
                drawCircle(
                    color = RoxouPrimaryLight.copy(alpha = 0.35f),
                    radius = pulseRadius,
                    center = androidx.compose.ui.geometry.Offset(driverX, driverY)
                )

                // Pulse indicator core
                drawCircle(
                    color = Color.White,
                    radius = this.run { 7.dp.toPx() },
                    center = androidx.compose.ui.geometry.Offset(driverX, driverY)
                )
                drawCircle(
                    color = RoxouPrimary,
                    radius = this.run { 4.dp.toPx() },
                    center = androidx.compose.ui.geometry.Offset(driverX, driverY)
                )
            }
        }

        // Top labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "A: " + if (originAddress.length > 15) originAddress.take(12) + "..." else originAddress,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoxouOnlineGreen
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "B: " + if (destinationAddress.length > 15) destinationAddress.take(12) + "..." else destinationAddress,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoxouPrimaryLight
                )
            }
        }

        // Bottom specs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (liveLocation != null) "LIVE TRACKING: ACTIVE" else "GOOGLE MAPS ENGINE",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (liveLocation != null) RoxouOnlineGreen else RoxouGrayText
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "LAT/LNG CALC OK",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = RoxouPrimaryLight
                )
            }
        }
    }
}
