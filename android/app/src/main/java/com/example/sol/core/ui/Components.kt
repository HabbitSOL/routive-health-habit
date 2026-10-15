package com.example.sol.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SolButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(text) }
}

@Composable
fun SolTextField(value: String, onValueChange: (String) -> Unit, label: String,
    error: String? = null, keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    transformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null) {
    OutlinedTextField(value = value, onValueChange = onValueChange,
        label = { Text(label) }, isError = error != null, shape = RoundedCornerShape(16.dp),
        supportingText = error?.let { { Text(it) } }, singleLine = true,
        keyboardOptions = keyboardOptions, visualTransformation = transformation,
        trailingIcon = trailingIcon, modifier = Modifier.fillMaxWidth())
}

@Composable
fun SolCard(title: String, outlined: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        colors = if (outlined) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) else CardDefaults.cardColors(),
        border = if (outlined) BorderStroke(1.dp, Color(0xFFE5E7EB)) else null) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}
