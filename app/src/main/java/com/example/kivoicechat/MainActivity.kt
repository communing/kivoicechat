package com.example.kivoicechat

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    
    private lateinit var chatViewModel: ChatViewModel
    private var isListening = mutableStateOf(false)

    // Dieser Receiver fängt den Text auf, den der VoiceService im Hintergrund erkennt
    private val speechReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val text = intent?.getStringExtra("text")
            if (!text.isNullOrBlank()) {
                // Das aktuell im UI gewählte Modell abrufen (über kleinen Trick mit SharedPreferences oder direktes Senden)
                // Da der Broadcast im Hintergrund ankommt, senden wir es einfach ab.
                // Das ViewModel kümmert sich um die Logik.
                chatViewModel.sendMessage(text, "openrouter/auto") // Vorerst Auto-Routing für Sprachbefehle
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val apiKeyManager = ApiKeyManager(this)
        val database = AppDatabase.getDatabase(this, "super_secret_db_password_123".toByteArray())
        chatViewModel = ChatViewModel(ChatRepository(apiKeyManager), database.chatDao())

        // Registriere den Receiver, um Nachrichten vom VoiceService zu erhalten
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(speechReceiver, IntentFilter("com.example.kivoicechat.SPEECH_RECOGNIZED"), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(speechReceiver, IntentFilter("com.example.kivoicechat.SPEECH_RECOGNIZED"))
        }

        setContent {
            var hasApiKey by remember { mutableStateOf(apiKeyManager.hasApiKey()) }
            if (!hasApiKey) SetupScreen(apiKeyManager) { hasApiKey = true }
            else ChatScreen(chatViewModel, isListening, ::toggleListening)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(speechReceiver)
    }

    private fun toggleListening(start: Boolean) {
        val intent = Intent(this, VoiceService::class.java)
        if (start) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            isListening.value = true
        } else {
            stopService(intent)
            isListening.value = false
        }
    }
}

@Composable
fun SetupScreen(apiKeyManager: ApiKeyManager, onSetupComplete: () -> Unit) {
    var apiKey by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
        Text("Willkommen!", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = apiKey, onValueChange = { apiKey = it },
            label = { Text("OpenRouter API Key") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { if (apiKey.isNotBlank()) { apiKeyManager.saveApiKey(apiKey); onSetupComplete() } }, modifier = Modifier.fillMaxWidth()) { Text("Speichern") }
    }
}

@Composable
fun ChatScreen(viewModel: ChatViewModel, isListeningState: State<Boolean>, onToggleListen: (Boolean) -> Unit) {
    var inputText by remember { mutableStateOf("") }
    val messages by viewModel.messages.collectAsState(initial = emptyList())
    val availableModels by viewModel.modelList.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf("openrouter/auto" to "🌟 Auto (OpenRouter entscheidet)") }
    
    val isListening by isListeningState

    // Berechtigungs-Abfrage für das Mikrofon
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) { onToggleListen(!isListening) }
        }
    )

    LaunchedEffect(availableModels) {
        if (availableModels.isNotEmpty() && selectedModel.first == "openrouter/auto") {
            selectedModel = availableModels[0]
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedButton(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (availableModels.isEmpty()) "Lade Modelle..." else "🧠 Modell: ${selectedModel.second}")
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(messages) { message ->
                val isUser = message.role == "user"
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
                    Box(modifier = Modifier.background(if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)).padding(12.dp)) { 
                        Text(message.content) 
                    }
                    if (!isUser && message.modelName != null) {
                        Text(text = "🤖 ${message.modelName}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            
            // NEU: Der Mikrofon-Button
            FilledTonalButton(
                onClick = { 
                    // Berechtigung anfragen oder Mikrofon umschalten
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isListening) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(if (isListening) "🛑 Stop" else "🎤 Rec")
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(value = inputText, onValueChange = { inputText = it }, modifier = Modifier.weight(1f))
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(onClick = { 
                if (inputText.isNotBlank()) { 
                    viewModel.sendMessage(inputText, selectedModel.first) 
                    inputText = "" 
                } 
            }) { Text("Senden") }
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxHeight(0.8f).fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("KI-Modell auswählen", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (availableModels.isEmpty()) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    else LazyColumn(modifier = Modifier.weight(1f)) {
                        items(availableModels) { modelPair ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { selectedModel = modelPair; showDialog = false }.padding(vertical = 12.dp)) {
                                Text(text = modelPair.second, fontSize = 16.sp)
                            }
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showDialog = false }, modifier = Modifier.align(Alignment.End)) { Text("Schließen") }
                }
            }
        }
    }
}
