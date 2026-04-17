// app/src/main/java/com/example/nexonpayadminpanel/ui/dashboard/DashboardScreen.kt
package com.example.nexonpayadminpanel.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexonpayadminpanel.retrofit.ConfigItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onConfigClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Stany dla modali
    var showSettingsModal by remember { mutableStateOf(false) }
    var showAddModal by remember { mutableStateOf(false) }
    var configToDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Twoje Konfiguracje", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(onClick = { showSettingsModal = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ustawienia")
                    }
                    IconButton(onClick = { viewModel.fetchConfigs() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Odśwież")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddModal = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj Konfigurację")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                is DashboardState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is DashboardState.Success -> {
                    if (state.configs.isEmpty()) {
                        Text(
                            text = "Brak konfiguracji. Dodaj pierwszą!",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.configs) { config ->
                                ConfigCard(
                                    config = config,
                                    onClick = { onConfigClick(config.configname) },
                                    onDelete = { configToDelete = config.configname } // Otwiera okno potwierdzenia
                                )
                            }
                        }
                    }
                }
                is DashboardState.Error -> {
                    Text(state.message, modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    // Modal Ustawień Terminala
    if (showSettingsModal) {
        TerminalSettingsModal(
            onDismiss = { showSettingsModal = false },
            onChangeLogin = {
                viewModel.changeTerminalLogin(it)
                showSettingsModal = false
            },
            onChangePassword = {
                viewModel.changeTerminalPassword(it)
                showSettingsModal = false
            }
        )
    }

    // Modal Dodawania Konfiguracji
    if (showAddModal) {
        AddConfigModal(
            onDismiss = { showAddModal = false },
            onConfirm = { name, currency ->
                viewModel.addNewConfig(name, currency)
                showAddModal = false
            }
        )
    }

    // Modal Potwierdzenia Usunięcia
    configToDelete?.let { configId ->
        AlertDialog(
            onDismissRequest = { configToDelete = null },
            title = { Text("Usunąć konfigurację?", fontWeight = FontWeight.Bold) },
            text = { Text("Czy na pewno chcesz usunąć sklep '$configId'? Tego nie da się cofnąć.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteConfig(configId)
                    configToDelete = null
                }) {
                    Text("Usuń", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { configToDelete = null }) {
                    Text("Anuluj", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

// ==========================================
// FUNKCJE POMOCNICZE (MODALE I KARTY)
// ==========================================

@Composable
fun TerminalSettingsModal(
    onDismiss: () -> Unit,
    onChangeLogin: (String) -> Unit,
    onChangePassword: (String) -> Unit
) {
    var newLogin by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ustawienia Terminala", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Możesz zaktualizować dane dostępowe dla terminala.", fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))

                OutlinedTextField(
                    value = newLogin,
                    onValueChange = { newLogin = it },
                    label = { Text("Nowy Login") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                Button(
                    onClick = { onChangeLogin(newLogin) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = newLogin.isNotBlank()
                ) {
                    Text("Zmień Login")
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Nowe Hasło") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                Button(
                    onClick = { onChangePassword(newPassword) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = newPassword.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Zmień Hasło")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zamknij", color = MaterialTheme.colorScheme.error)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddConfigModal(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("PLN") }

    // Stany dla Dropdown Menu
    var expanded by remember { mutableStateOf(false) }
    val currencyOptions = listOf("PLN", "USD", "EUR")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nowa Konfiguracja", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // Pole Nazwy
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa Sklepu") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Pole Waluty (Dropdown)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = currency,
                        onValueChange = {},
                        readOnly = true, // Użytkownik nie może wpisać tekstu z palca
                        label = { Text("Waluta") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        currencyOptions.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    currency = selectionOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, currency) }, enabled = name.isNotBlank()) {
                Text("Dodaj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj", color = MaterialTheme.colorScheme.primary) }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun ConfigCard(config: ConfigItem, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {

            // Wyraźny przycisk Usuwania (Czerwony kosz)
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Usuń Sklep",
                    tint = MaterialTheme.colorScheme.error // Używamy koloru błędu (czerwonego)
                )
            }

            Column(modifier = Modifier.padding(16.dp).padding(end = 32.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = config.configname,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Text(
                            text = config.currency_shortname,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Utworzono: ${config.creationtimestamp?.substringBefore("T") ?: "N/A"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}