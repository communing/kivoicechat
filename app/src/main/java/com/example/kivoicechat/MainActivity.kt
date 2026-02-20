package com.example.kivoicechat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val apiKeyManager = ApiKeyManager(this)
        val database = AppDatabase.getDatabase(this, "super_secret_db_password_123".toByteArray())
        val chatViewModel = ChatViewModel(ChatRepository(apiKeyManager), database.chatDao())

        setContent {
            var hasApiKey by remember { mutableStateOf(apiKeyManager.hasApiKey()) }
            if (!hasApiKey) SetupScreen(apiKeyManager) { hasApiKey = true }
            else ChatScreen(chatViewModel)
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
fun ChatScreen(viewModel: ChatViewModel) {
    var inputText by remember { mutableStateOf("") }
    val messages by viewModel.messages.collectAsState(initial = emptyList())
    
    // NEU: Beobachtet unsere dynamisch geladene Liste aus dem Internet
    val availableModels by viewModel.modelList.collectAsState()
    
    var showDialog by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf("openrouter/auto" to "🌟 Auto (OpenRouter entscheidet)") }

    // Falls das ausgewählte Modell noch nicht initialisiert ist (beim ersten Start)
    LaunchedEffect(availableModels) {
        if (availableModels.isNotEmpty() && selectedModel.first == "openrouter/auto" && selectedModel.second == "🌟 Auto (OpenRouter entscheidet)") {
            selectedModel = availableModels[0]
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        
        // --- Modell-Auswahl Button ---
        OutlinedButton(
            onClick = { showDialog = true }, 
            modifier = Modifier.fillMaxWidth()
        ) {
            val buttonText = if (availableModels.isEmpty()) "Lade Modelle aus dem Internet..." else "🧠 Modell: ${selectedModel.second}"
            Text(buttonText)
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        // --- Der Chat-Verlauf ---
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(messages) { message ->
                val isUser = message.role == "user"
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
                    Box(modifier = Modifier.background(if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)).padding(12.dp)) { 
                        Text(message.content) 
                    }
                    if (!isUser && message.modelName != null) {
                        Text(
                            text = "🤖 ${message.modelName}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        // --- Eingabefeld ---
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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

    // --- NEU: Ein großer, scrollbarer Dialog für hunderte von Modellen ---
    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxHeight(0.8f).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("KI-Modell auswählen", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (availableModels.isEmpty()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(availableModels) { modelPair ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedModel = modelPair
                                            showDialog = false
                                        }
                                        .padding(vertical = 12.dp)
                                ) {
                                    Text(text = modelPair.second, fontSize = 16.sp)
                                }
                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Schließen") }
                }
            }
        }
    }
}
