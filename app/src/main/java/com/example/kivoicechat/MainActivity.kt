package com.example.kivoicechat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
    
    // --- NEU: Modell-Liste und State für das Dropdown-Menü ---
    val availableModels = listOf(
        "openrouter/auto" to "Auto (Kostenlos/Beste Wahl)",
        "google/gemini-2.5-flash" to "Google Gemini 2.5 Flash",
        "anthropic/claude-3-haiku" to "Claude 3 Haiku",
        "meta-llama/llama-3-8b-instruct" to "Llama 3 (8B)",
        "openai/gpt-3.5-turbo" to "GPT-3.5 Turbo"
    )
    var expanded by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf(availableModels[0]) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        
        // --- NEU: Der Dropdown-Button ganz oben ---
        Box(modifier = Modifier.fillMaxWidth().wrapContentSize(Alignment.TopStart)) {
            OutlinedButton(
                onClick = { expanded = true }, 
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🧠 Modell: ${selectedModel.second}")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                availableModels.forEach { modelPair ->
                    DropdownMenuItem(
                        text = { Text(modelPair.second) },
                        onClick = {
                            selectedModel = modelPair
                            expanded = false // Menü schließen nach Auswahl
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        // --- Der bisherige Chat-Verlauf ---
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
        
        // --- Das Eingabefeld ---
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = inputText, onValueChange = { inputText = it }, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { 
                if (inputText.isNotBlank()) { 
                    // NEU: Wir übergeben die ausgewählte Modell-ID (z.B. "google/gemini-2.5-flash") an das ViewModel
                    viewModel.sendMessage(inputText, selectedModel.first) 
                    inputText = "" 
                } 
            }) { Text("Senden") }
        }
    }
}
