package com.example.mycar.ui.catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mycar.ui.theme.MyCarTheme
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class VehicleCatalogResult(
    val make: String,
    val vehicleTypes: List<String>,
    val models: List<String>
)

class VehicleCatalogActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyCarTheme {
                var make by remember { mutableStateOf("") }
                var catalogResult by remember { mutableStateOf<VehicleCatalogResult?>(null) }
                var isLoading by remember { mutableStateOf(false) }
                var error by remember { mutableStateOf<String?>(null) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Catalog modele auto") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { padding ->
                    VehicleCatalogScreen(
                        modifier = Modifier.padding(padding),
                        make = make,
                        onMakeChange = { make = it },
                        result = catalogResult,
                        isLoading = isLoading,
                        error = error,
                        onSearch = {
                            val searchedMake = make.trim()
                            if (searchedMake.isEmpty()) {
                                error = "Introdu o marcă auto."
                            } else {
                                isLoading = true
                                error = null
                                catalogResult = null
                                loadCatalog(
                                    make = searchedMake,
                                    onSuccess = {
                                        catalogResult = it
                                        isLoading = false
                                    },
                                    onError = {
                                        error = it
                                        isLoading = false
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    private fun loadCatalog(
        make: String,
        onSuccess: (VehicleCatalogResult) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val vehicleTypes = requestVehicleTypes(make)
                val models = requestModels(make)
                runOnUiThread {
                    onSuccess(
                        VehicleCatalogResult(
                            make = make,
                            vehicleTypes = vehicleTypes,
                            models = models
                        )
                    )
                }
            } catch (exception: Exception) {
                runOnUiThread { onError(exception.message ?: "Nu am putut încărca datele auto.") }
            }
        }.start()
    }

    private fun requestVehicleTypes(make: String): List<String> {
        val json = requestJson("GetVehicleTypesForMake", make)
        val results = json.getJSONArray("Results")
        return List(results.length()) { index ->
            results.getJSONObject(index).getString("VehicleTypeName")
        }.distinct().sorted()
    }

    private fun requestModels(make: String): List<String> {
        val json = requestJson("GetModelsForMake", make)
        val results = json.getJSONArray("Results")
        return List(results.length()) { index ->
            results.getJSONObject(index).getString("Model_Name")
        }.distinct().sorted()
    }

    private fun requestJson(endpoint: String, make: String): JSONObject {
        val encodedMake = URLEncoder.encode(make, "UTF-8")
        val connection = URL("https://vpic.nhtsa.dot.gov/api/vehicles/$endpoint/$encodedMake?format=json")
            .openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("Serverul a răspuns cu eroarea ${connection.responseCode}.")
        }

        return connection.inputStream.bufferedReader().use { reader ->
            JSONObject(reader.readText())
        }
    }
}

@Composable
fun VehicleCatalogScreen(
    modifier: Modifier = Modifier,
    make: String,
    onMakeChange: (String) -> Unit,
    result: VehicleCatalogResult?,
    isLoading: Boolean,
    error: String?,
    onSearch: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Caută modele după marcă folosind date publice NHTSA.",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = make,
            onValueChange = onMakeChange,
            label = { Text("Marca") },
            placeholder = { Text("Ex: Toyota, BMW, Ford") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onSearch, modifier = Modifier.fillMaxWidth(), enabled = !isLoading) {
            Text("Caută modele")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        if (error != null) {
            Text(text = error, color = Color.Red)
        }

        if (result != null) {
            VehicleCatalogResultView(result = result, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun VehicleCatalogResultView(result: VehicleCatalogResult, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Marca căutată: ${result.make}", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Tipuri vehicul:", style = MaterialTheme.typography.labelLarge)
                    if (result.vehicleTypes.isEmpty()) {
                        Text("Nu s-au găsit tipuri de vehicul.")
                    } else {
                        Text(result.vehicleTypes.joinToString(", "))
                    }
                }
            }
        }

        item {
            Text(
                text = "Modele găsite (${result.models.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (result.models.isEmpty()) {
            item {
                Text("Nu s-au găsit modele pentru această marcă.")
            }
        } else {
            items(result.models) { model ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = model,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
