package com.example.integrado

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.integrado.ui.theme.IntegradoTheme
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity2 : ComponentActivity() {
    private val db = FirebaseFirestore.getInstance()

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IntegradoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(db)
                }
            }
        }
    }
}

@Composable
fun App(db: FirebaseFirestore) {
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
                        Log.d("TAG", "DocumentSnapshot written ID: ${documentReference.id}")
                        fetchClientes(db, clientes)
                    }
                    .addOnFailureListener { e ->
                        Log.w("TAG", "Error adding document", e)
                    }
            },
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            Text(text = "Cadastrar")
        }

        LaunchedEffect(Unit) {
            fetchClientes(db, clientes)
        }

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(clientes) { cliente ->
                ClientRow(cliente) { clientId ->
                    db.collection("autenticador").document(clientId).delete()
                        .addOnSuccessListener {
                            Log.d("TAG", "DocumentSnapshot successfully deleted!")
                            clientes.remove(cliente)
                        }
                        .addOnFailureListener { e ->
                            Log.w("TAG", "Error deleting document", e)
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

fun fetchClientes(db: FirebaseFirestore, clientes: SnapshotStateList<Client>) {
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
            Log.w("TAG", "Error getting documents: ", exception)
        }
}
