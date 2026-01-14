package org.example.project.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun IntegerInput(
    value: UInt,
    onValueChange: (UInt) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minValue: UInt = 0u,
    maxValue: UInt = UInt.MAX_VALUE
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = {
                if (value > minValue) {
                    onValueChange(value - 1u)
                }
            },
            enabled = value > minValue
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease")
        }

        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.widthIn(min = 40.dp)
        )

        IconButton(
            onClick = {
                if (value < maxValue) {
                    onValueChange(value + 1u)
                }
            },
            enabled = value < maxValue
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase")
        }
    }
}
