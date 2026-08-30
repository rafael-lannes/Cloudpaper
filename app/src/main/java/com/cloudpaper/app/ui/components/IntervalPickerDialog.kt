package com.cloudpaper.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class IntervalOption(val minutes: Long, val label: String)

val PRESET_INTERVALS = listOf(
    IntervalOption(15L, "A cada 15 minutos (Mínimo padrão)"),
    IntervalOption(30L, "A cada 30 minutos"),
    IntervalOption(60L, "A cada 1 hora"),
    IntervalOption(120L, "A cada 2 horas"),
    IntervalOption(240L, "A cada 4 horas"),
    IntervalOption(360L, "A cada 6 horas"),
    IntervalOption(720L, "A cada 12 horas"),
    IntervalOption(1440L, "A cada 24 horas (Diário)")
)

@Composable
fun IntervalPickerDialog(
    currentMinutes: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var selectedMinutes by remember { mutableStateOf(currentMinutes) }
    var customInput by remember { mutableStateOf("") }
    var isCustomSelected by remember {
        mutableStateOf(PRESET_INTERVALS.none { it.minutes == currentMinutes })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Frequência de Troca")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Escolha o intervalo para troca automática do papel de parede:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                PRESET_INTERVALS.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (!isCustomSelected && selectedMinutes == option.minutes),
                                onClick = {
                                    selectedMinutes = option.minutes
                                    isCustomSelected = false
                                }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (!isCustomSelected && selectedMinutes == option.minutes),
                            onClick = {
                                selectedMinutes = option.minutes
                                isCustomSelected = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = option.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Custom Interval Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isCustomSelected,
                            onClick = { isCustomSelected = true }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isCustomSelected,
                        onClick = { isCustomSelected = true }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Personalizado (minutos):", style = MaterialTheme.typography.bodyLarge)
                }

                if (isCustomSelected) {
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = { customInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Minutos (ex: 45)") },
                        placeholder = { Text("Ex: 45") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 40.dp, top = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isCustomSelected) {
                        val parsed = customInput.toLongOrNull()
                        if (parsed != null && parsed > 0) {
                            onConfirm(parsed)
                        } else {
                            onConfirm(selectedMinutes)
                        }
                    } else {
                        onConfirm(selectedMinutes)
                    }
                    onDismiss()
                }
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
