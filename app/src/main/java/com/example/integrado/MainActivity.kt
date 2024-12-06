package com.example.integrado

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.integrado.ui.theme.IntegradoTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        setContent {
            IntegradoTheme {
                MainScreen()
            }
        }
    }

    @Composable
    fun MainScreen() {
        var isLoggedIn by remember { mutableStateOf(false) }

        if (isLoggedIn) {
            RegisterScreen()
        } else {
            LoginScreen(onLoginSuccess = { isLoggedIn = true })
        }
    }

    @Composable
    fun LoginScreen(onLoginSuccess: () -> Unit) {
        var email by remember { mutableStateOf(TextFieldValue()) }
        var password by remember { mutableStateOf(TextFieldValue()) }
        var message by remember { mutableStateOf("") }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            content = { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    TextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Senha") },
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            authenticateUser(email.text, password.text) { success, errorMessage ->
                                if (success) {
                                    onLoginSuccess()
                                } else {
                                    message = errorMessage ?: "Falha na autenticação: tente novamente."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Entrar")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(message)
                }
            }
        )
    }

    private fun authenticateUser(
        email: String,
        password: String,
        callback: (Boolean, String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.i(TAG, "Login bem-sucedido")
                    callback(true, null)
                } else {
                    val errorMessage = when (task.exception?.message) {
                        "O formato do email está incorreto." -> "Formato de email inválido."
                        "Usuário não encontrado. O usuário pode ter sido excluído." -> "Usuário não encontrado."
                        "Senha incorreta." -> "Senha incorreta."
                        else -> "Erro ao realizar login: Email ou senha inválido"
                    }
                    Log.e(TAG, "Falha no login: $errorMessage")
                    callback(false, errorMessage)
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    @Composable
    fun RegisterScreen() {
        var nome by remember { mutableStateOf("") }
        var telefone by remember { mutableStateOf("") }
        val clientes = remember { mutableStateListOf<Client>() }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(text = "App Firebase Firestore", modifier = Modifier.align(Alignment.CenterHorizontally))

            TextFieldWithLabel(label = "Nome:", value = nome, onValueChange = { nome = it })
            TextFieldWithLabel(label = "Telefone:", value = telefone, onValueChange = { telefone = it })

            Button(
                onClick = {
                    val pessoas = hashMapOf("nome" to nome, "telefone" to telefone)
                    db.collection("autenticador").add(pessoas)
                        .addOnSuccessListener { documentReference ->
                            Log.d("TAG", "DocumentSnapshot written with ID: ${documentReference.id}")
                            fetchClientes(clientes)
                        }
                        .addOnFailureListener { e ->
                            val errorMessage = "Erro ao adicionar documento: ${e.message}"
                            Log.w("TAG", errorMessage, e)
                            Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                        }
                },
                modifier = Modifier.padding(vertical = 10.dp)
            ) {
                Text(text = "Cadastrar")
            }

            LaunchedEffect(Unit) {
                fetchClientes(clientes)
            }

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(clientes) { cliente ->
                    ClientRow(cliente) { clientId ->
                        db.collection("autenticador").document(clientId).delete()
                            .addOnSuccessListener {
                                Log.d("TAG", "Documento deletado com sucesso!")
                                clientes.remove(cliente)
                            }
                            .addOnFailureListener { e ->
                                val errorMessage = "Erro ao deletar documento: ${e.message}"
                                Log.w("TAG", errorMessage, e)
                                Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                            }
                    }
                }
            }
        }
    }

    @Composable
    fun TextFieldWithLabel(label: String, value: String, onValueChange: (String) -> Unit) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Text(text = label)
            TextField(value = value, onValueChange = onValueChange)
        }
    }

    @Composable
    fun ClientRow(cliente: Client, onDelete: (String) -> Unit) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(0.5f)) {
                Text(text = cliente.nome)
            }
            Column(modifier = Modifier.weight(0.5f)) {
                Text(text = cliente.telefone)
            }
            Column(modifier = Modifier.weight(0.5f)) {
                Button(onClick = { onDelete(cliente.id) }) {
                    Text(text = "Deletar")
                }
            }
        }
    }

    data class Client(val id: String, val nome: String, val telefone: String)

    private fun fetchClientes(clientes: SnapshotStateList<Client>) {
        clientes.clear()
        db.collection("autenticador")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val client = Client(
                        id = document.id,
                        nome = document.getString("nome") ?: "--",
                        telefone = document.getString("telefone") ?: "--"
                    )
                    clientes.add(client)
                }
            }
            .addOnFailureListener { exception ->
                val errorMessage = "Erro ao buscar documentos: ${exception.message}"
                Log.w("TAG", errorMessage, exception)
                Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
            }
    }
}
