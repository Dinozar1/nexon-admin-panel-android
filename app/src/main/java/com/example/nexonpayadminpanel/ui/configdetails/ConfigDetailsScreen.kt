package com.example.nexonpayadminpanel.ui.configdetails

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexonpayadminpanel.retrofit.WalletEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigDetailsScreen(
    viewModel: ConfigDetailsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val context = LocalContext.current

    var showAddModal by remember { mutableStateOf(false) }
    var walletToDelete by remember { mutableStateOf<WalletEntry?>(null) }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.configName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Wróć") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.loadCryptos()
                showAddModal = true
            }) {
                Icon(Icons.Default.Add, "Dodaj Portfel")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // Karta Statystyk i Salda
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Statystyki Sklepu", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            Text("Operacje: ${uiState.stats?.totalOperations ?: 0}")
                            Text("Klienci: ${uiState.stats?.uniqueClients ?: 0}")
                            Text("Wolumen: ${uiState.stats?.totalVolumeFiat ?: "0.0"} ${uiState.stats?.fiatCurrency ?: ""}")

                            Spacer(Modifier.height(16.dp))
                            Divider()
                            Spacer(Modifier.height(16.dp))

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Całkowite Saldo:", fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = balance ?: "****", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { viewModel.toggleBalance() }) {
                                        Icon(
                                            // Proste renderowanie ikony zależnie od stanu
                                            imageVector = if (balance == null) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Pokaż saldo"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item { Text("Podpięte Portfele", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(top = 16.dp)) }

                items(uiState.wallets) { wallet ->
                    WalletCard(
                        wallet = wallet,
                        onDelete = {
                            // Przekazujemy CAŁY obiekt portfela do stanu,
                            // aby modal miał dostęp do cryptoId i networkId
                            walletToDelete = wallet
                        }
                    )
                }
            }
        }
    }

    if (showAddModal) {
        AddWalletModal(
            viewModel = viewModel,
            onDismiss = { showAddModal = false },
            onConfirm = { cryptoId, network, pubKey -> // DODANO cryptoId
                viewModel.addWallet(cryptoId, network, pubKey)
                showAddModal = false
            }
        )
    }

    walletToDelete?.let { wallet ->
        AlertDialog(
            onDismissRequest = { walletToDelete = null },
            title = { Text("Usunąć portfel?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Czy na pewno chcesz usunąć ten portfel?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = wallet.publickey,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Przekazujemy komplet danych wymaganych przez backend
                        viewModel.deleteWallet(
                            cryptoId = wallet.cryptocurrencyname, // np. "Bitcoin"
                            networkId = wallet.networkname,       // np. "Ethereum Mainnet"
                            publicKey = wallet.publickey          // Adres portfela
                        )
                        walletToDelete = null
                    }
                ) {
                    Text("Usuń", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { walletToDelete = null }) {
                    Text("Anuluj")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun WalletCard(wallet: WalletEntry, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth()) {
            IconButton(onClick = onDelete, Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Default.Delete, "Usuń", tint = MaterialTheme.colorScheme.error)
            }
            Column(Modifier.padding(16.dp).padding(end = 32.dp)) {
                Text(wallet.cryptocurrencyname, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(wallet.networkname, color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text(wallet.publickey, fontSize = 12.sp, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWalletModal(
    viewModel: ConfigDetailsViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    val cryptos by viewModel.availableCryptos.collectAsState()
    val networks by viewModel.availableNetworks.collectAsState()

    var selectedCryptoFullName by remember { mutableStateOf("") } // Pełna nazwa dla zapisu (np. "Bitcoin")
    var selectedNetwork by remember { mutableStateOf("") }
    var publicKey by remember { mutableStateOf("") }

    var cryptoExpanded by remember { mutableStateOf(false) }
    var networkExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nowy Portfel", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // 1. Wybór Krypto
                ExposedDropdownMenuBox(expanded = cryptoExpanded, onExpandedChange = { cryptoExpanded = !cryptoExpanded }) {
                    OutlinedTextField(
                        value = selectedCryptoFullName, onValueChange = {}, readOnly = true,
                        label = { Text("Wybierz Kryptowalutę") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cryptoExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = cryptoExpanded, onDismissRequest = { cryptoExpanded = false }) {
                        cryptos.forEach { crypto ->
                            DropdownMenuItem(
                                text = { Text(crypto.cryptocurrencyname) },
                                onClick = {
                                    // Zapisujemy pełną nazwę do wysłania na serwer
                                    selectedCryptoFullName = crypto.cryptocurrencyname

                                    // Ale sieci pobieramy przy użyciu skrótu (tak jak w dok. API: np. USDT)
                                    viewModel.loadNetworks(crypto.shortcryptocurrencyname)

                                    selectedNetwork = ""
                                    cryptoExpanded = false
                                }
                            )
                        }
                    }
                }

                // 2. Wybór Sieci (Zależny od Krypto)
                ExposedDropdownMenuBox(expanded = networkExpanded, onExpandedChange = { if(selectedCryptoFullName.isNotEmpty()) networkExpanded = !networkExpanded }) {
                    OutlinedTextField(
                        value = selectedNetwork, onValueChange = {}, readOnly = true,
                        label = { Text("Wybierz Sieć") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = networkExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        enabled = selectedCryptoFullName.isNotEmpty()
                    )
                    ExposedDropdownMenu(expanded = networkExpanded, onDismissRequest = { networkExpanded = false }) {
                        networks.forEach { network ->
                            DropdownMenuItem(
                                text = { Text(network.networkname) },
                                onClick = {
                                    selectedNetwork = network.networkname
                                    networkExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = publicKey, onValueChange = { publicKey = it },
                    label = { Text("Adres (Public Key)") }, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                // WYSYŁAMY PEŁNĄ NAZWĘ (selectedCryptoFullName) ZAMIAST SKRÓTU!
                onClick = { onConfirm(selectedCryptoFullName, selectedNetwork, publicKey) },
                enabled = selectedCryptoFullName.isNotEmpty() && selectedNetwork.isNotEmpty() && publicKey.isNotEmpty()
            ) { Text("Dodaj") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj", color = MaterialTheme.colorScheme.primary) } },
        containerColor = MaterialTheme.colorScheme.surface
    )
}