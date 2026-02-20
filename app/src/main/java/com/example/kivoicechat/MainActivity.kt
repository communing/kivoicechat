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
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(messages) { message ->
                val isUser = message.role == "user"
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
                    Box(modifier = Modifier.background(if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)).padding(12.dp)) { Text(message.content) }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = inputText, onValueChange = { inputText = it }, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { if (inputText.isNotBlank()) { viewModel.sendMessage(inputText); inputText = "" } }) { Text("Senden") }
        }
    }
}
