// app/src/main/java/com/example/nexonpayadminpanel/ui/configdetails/ConfigDetailsScreen.kt
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexonpayadminpanel.R
import com.example.nexonpayadminpanel.retrofit.WalletEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigDetailsScreen(
    viewModel: ConfigDetailsViewModel,
    onBack: () -> Unit
) {
    // 1. STATE OBSERVERS: Automatically redraw UI when data changes
    val uiState by viewModel.uiState.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val context = LocalContext.current

    // Local UI states for dialogs
    var showAddModal by remember { mutableStateOf(false) }
    var walletToDelete by remember { mutableStateOf<WalletEntry?>(null) }

    // 2. EVENT LISTENER: Show Android Toasts for success/error messages
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    // 3. MAIN LAYOUT STRUCTURE
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.configName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, stringResource(R.string.back_button_desc)) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.loadCryptos() // Pre-load options for the dropdown
                showAddModal = true
            }) {
                Icon(Icons.Default.Add, stringResource(R.string.add_wallet_desc))
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            // Show loader while API request is pending
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            // 4. LAZY COLUMN: Efficient, scrollable list for dynamic content
            LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // Top section: Stats and Hidden Balance feature
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.shop_stats_title), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            Text("${stringResource(R.string.stats_operations)} ${uiState.stats?.totalOperations ?: 0}")
                            Text("${stringResource(R.string.stats_clients)} ${uiState.stats?.uniqueClients ?: 0}")
                            Text("${stringResource(R.string.stats_volume)} ${uiState.stats?.totalVolumeFiat ?: "0.0"} ${uiState.stats?.fiatCurrency ?: ""}")

                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(16.dp))

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.total_balance_label), fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = balance ?: "****", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { viewModel.toggleBalance() }) {
                                        // Dynamic eye icon based on visibility
                                        Icon(
                                            imageVector = if (balance == null) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = stringResource(R.string.show_balance_desc)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item { Text(stringResource(R.string.connected_wallets_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(top = 16.dp)) }

                // List rendering: iterate through wallets and render cards
                items(uiState.wallets) { wallet ->
                    WalletCard(
                        wallet = wallet,
                        onDelete = {
                            walletToDelete = wallet // Trigger deletion confirmation dialog
                        }
                    )
                }
            }
        }
    }

    // 5. MODALS AND DIALOGS

    // Add Wallet Flow
    if (showAddModal) {
        AddWalletModal(
            viewModel = viewModel,
            onDismiss = { showAddModal = false },
            onConfirm = { cryptoId, network, pubKey ->
                viewModel.addWallet(cryptoId, network, pubKey)
                showAddModal = false
            }
        )
    }

    // Delete Wallet Confirmation Flow
    walletToDelete?.let { wallet ->
        AlertDialog(
            onDismissRequest = { walletToDelete = null },
            title = { Text(stringResource(R.string.delete_wallet_dialog_title), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(R.string.delete_wallet_dialog_text))
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
                        viewModel.deleteWallet(
                            cryptoId = wallet.cryptocurrencyname,
                            networkId = wallet.networkname,
                            publicKey = wallet.publickey
                        )
                        walletToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete_button), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { walletToDelete = null }) {
                    Text(stringResource(R.string.cancel_button))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

// Reusable component for a single wallet entry
@Composable
fun WalletCard(wallet: WalletEntry, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth()) {
            IconButton(onClick = onDelete, Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Default.Delete, stringResource(R.string.delete_button), tint = MaterialTheme.colorScheme.error)
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

// Cascading form dialog for adding a new wallet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWalletModal(
    viewModel: ConfigDetailsViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    val cryptos by viewModel.availableCryptos.collectAsState()
    val networks by viewModel.availableNetworks.collectAsState()

    var selectedCryptoFullName by remember { mutableStateOf("") }
    var selectedNetwork by remember { mutableStateOf("") }
    var publicKey by remember { mutableStateOf("") }

    var cryptoExpanded by remember { mutableStateOf(false) }
    var networkExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_wallet_dialog_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // Step 1: Crypto selection dropdown
                ExposedDropdownMenuBox(expanded = cryptoExpanded, onExpandedChange = { cryptoExpanded = !cryptoExpanded }) {
                    OutlinedTextField(
                        value = selectedCryptoFullName, onValueChange = {}, readOnly = true,
                        label = { Text(stringResource(R.string.select_crypto_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cryptoExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = cryptoExpanded, onDismissRequest = { cryptoExpanded = false }) {
                        cryptos.forEach { crypto ->
                            DropdownMenuItem(
                                text = { Text(crypto.cryptocurrencyname) },
                                onClick = {
                                    selectedCryptoFullName = crypto.cryptocurrencyname
                                    // Trigger API call to fetch networks based on selection
                                    viewModel.loadNetworks(crypto.shortcryptocurrencyname)
                                    selectedNetwork = ""
                                    cryptoExpanded = false
                                }
                            )
                        }
                    }
                }

                // Step 2: Network selection dropdown (dynamically populated)
                ExposedDropdownMenuBox(expanded = networkExpanded, onExpandedChange = { if(selectedCryptoFullName.isNotEmpty()) networkExpanded = !networkExpanded }) {
                    OutlinedTextField(
                        value = selectedNetwork, onValueChange = {}, readOnly = true,
                        label = { Text(stringResource(R.string.select_network_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = networkExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        enabled = selectedCryptoFullName.isNotEmpty() // Disabled until crypto is selected
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

                // Step 3: Manual public key input
                OutlinedTextField(
                    value = publicKey, onValueChange = { publicKey = it },
                    label = { Text(stringResource(R.string.public_key_label)) }, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedCryptoFullName, selectedNetwork, publicKey) },
                enabled = selectedCryptoFullName.isNotEmpty() && selectedNetwork.isNotEmpty() && publicKey.isNotEmpty() // Form validation
            ) { Text(stringResource(R.string.add_button)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button), color = MaterialTheme.colorScheme.primary) } },
        containerColor = MaterialTheme.colorScheme.surface
    )
}