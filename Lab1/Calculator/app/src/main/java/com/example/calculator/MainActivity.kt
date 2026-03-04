package com.example.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculator.ui.theme.CalculatorTheme
import com.example.calculator.ui.theme.CalculatorBackground
import com.example.calculator.ui.theme.CalculatorButtonFunction
import com.example.calculator.ui.theme.CalculatorButtonNumber
import com.example.calculator.ui.theme.CalculatorButtonOperator
import com.example.calculator.ui.theme.CalculatorButtonTextPrimary
import com.example.calculator.ui.theme.CalculatorButtonTextSecondary
import com.example.calculator.ui.theme.CalculatorDisplayText

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalculatorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CalculatorBackground
                ) {
                    CalculatorScreen()
                }
            }
        }
    }
}

private enum class Operation {
    ADD, SUBTRACT, MULTIPLY, DIVIDE
}

private data class CalculatorState(
    val display: String = "0",
    val operand: Double? = null,
    val pendingOperation: Operation? = null,
    val overwriteDisplay: Boolean = true,
    val error: String? = null
)

@Composable
private fun CalculatorScreen() {
    var state by remember { mutableStateOf(CalculatorState()) }

    fun safeUpdate(transform: (CalculatorState) -> CalculatorState) {
        // Если уже ошибка, разрешаем только полную очистку
        state = if (state.error != null) {
            transform(state).copy(error = null)
        } else {
            transform(state)
        }
    }

    fun clearAll() {
        state = CalculatorState()
    }

    fun inputDigit(digit: String) = safeUpdate { current ->
        if (current.overwriteDisplay || current.display == "0") {
            current.copy(
                display = digit,
                overwriteDisplay = false
            )
        } else if (current.display.length >= 12) {
            // ограничиваем длину, чтобы не вылезать за экран
            current
        } else {
            current.copy(display = current.display + digit)
        }
    }

    fun inputDecimal() = safeUpdate { current ->
        if (current.overwriteDisplay) {
            current.copy(display = "0.", overwriteDisplay = false)
        } else if (current.display.contains(".")) {
            current
        } else {
            current.copy(display = current.display + ".")
        }
    }

    fun toggleSign() = safeUpdate { current ->
        if (current.display == "0") current
        else if (current.display.startsWith("-")) {
            current.copy(display = current.display.removePrefix("-"))
        } else {
            current.copy(display = "-" + current.display)
        }
    }

    fun percent() = safeUpdate { current ->
        val value = current.display.toDoubleOrNull() ?: return@safeUpdate current
        val result = value / 100.0
        current.copy(
            display = formatNumber(result),
            overwriteDisplay = true
        )
    }

    fun applyOperation(op: Operation) = safeUpdate { current ->
        val currentValue = current.display.toDoubleOrNull()
        if (currentValue == null && current.operand == null) {
            current
        } else if (current.operand == null) {
            current.copy(
                operand = currentValue,
                pendingOperation = op,
                overwriteDisplay = true
            )
        } else if (currentValue == null) {
            current.copy(pendingOperation = op)
        } else {
            val result = tryPerformOperation(current.operand, currentValue, current.pendingOperation)
            if (result == null) {
                current.copy(
                    display = "Error",
                    operand = null,
                    pendingOperation = null,
                    overwriteDisplay = true,
                    error = "invalid_operation"
                )
            } else {
                current.copy(
                    display = formatNumber(result),
                    operand = result,
                    pendingOperation = op,
                    overwriteDisplay = true
                )
            }
        }
    }

    fun equals() = safeUpdate { current ->
        val currentValue = current.display.toDoubleOrNull()
        if (current.pendingOperation == null || current.operand == null || currentValue == null) {
            current
        } else {
            val result = tryPerformOperation(current.operand, currentValue, current.pendingOperation)
            if (result == null) {
                current.copy(
                    display = "Error",
                    operand = null,
                    pendingOperation = null,
                    overwriteDisplay = true,
                    error = "invalid_operation"
                )
            } else {
                current.copy(
                    display = formatNumber(result),
                    operand = null,
                    pendingOperation = null,
                    overwriteDisplay = true
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CalculatorBackground)
            .padding(horizontal = 8.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Экран
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                text = if (state.error != null) "Error" else state.display,
                color = CalculatorDisplayText,
                fontSize = 56.sp,
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Кнопки
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalculatorFunctionButton(
                    label = if (state.display != "0" || state.operand != null) "AC" else "AC",
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    onClick = { clearAll() }
                )
                CalculatorFunctionButton(
                    label = "+/-",
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    onClick = { toggleSign() }
                )
                CalculatorFunctionButton(
                    label = "%",
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    onClick = { percent() }
                )
                CalculatorOperatorButton(
                    label = "÷",
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    onClick = { applyOperation(Operation.DIVIDE) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalculatorNumberButton("7", Modifier.weight(1f).aspectRatio(1f)) { inputDigit("7") }
                CalculatorNumberButton("8", Modifier.weight(1f).aspectRatio(1f)) { inputDigit("8") }
                CalculatorNumberButton("9", Modifier.weight(1f).aspectRatio(1f)) { inputDigit("9") }
                CalculatorOperatorButton(
                    label = "×",
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    onClick = { applyOperation(Operation.MULTIPLY) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalculatorNumberButton("4", Modifier.weight(1f).aspectRatio(1f)) { inputDigit("4") }
                CalculatorNumberButton("5", Modifier.weight(1f).aspectRatio(1f)) { inputDigit("5") }
                CalculatorNumberButton("6", Modifier.weight(1f).aspectRatio(1f)) { inputDigit("6") }
                CalculatorOperatorButton(
                    label = "−",
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    onClick = { applyOperation(Operation.SUBTRACT) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalculatorNumberButton("1", Modifier.weight(1f).aspectRatio(1f)) { inputDigit("1") }
                CalculatorNumberButton("2", Modifier.weight(1f).aspectRatio(1f)) { inputDigit("2") }
                CalculatorNumberButton("3", Modifier.weight(1f).aspectRatio(1f)) { inputDigit("3") }
                CalculatorOperatorButton(
                    label = "+",
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    onClick = { applyOperation(Operation.ADD) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalculatorNumberButton(
                    label = "0",
                    modifier = Modifier
                        .weight(2f)
                        .aspectRatio(2f),
                    onClick = { inputDigit("0") }
                )
                CalculatorNumberButton(
                    label = ".",
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    onClick = { inputDecimal() }
                )
                CalculatorOperatorButton(
                    label = "=",
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    onClick = { equals() }
                )
            }
        }
    }
}

private fun tryPerformOperation(lhs: Double?, rhs: Double?, op: Operation?): Double? {
    if (lhs == null || rhs == null || op == null) return null
    return when (op) {
        Operation.ADD -> lhs + rhs
        Operation.SUBTRACT -> lhs - rhs
        Operation.MULTIPLY -> lhs * rhs
        Operation.DIVIDE -> if (rhs == 0.0) null else lhs / rhs
    }
}

private fun formatNumber(value: Double): String {
    // Убираем лишние нули и точку в конце
    val text = value.toString()
    return if (text.contains("E") || text.length > 12) {
        // ограничиваем длину и избегаем слишком длинных значений
        String.format("%.6g", value)
    } else {
        text.trimEnd('0').trimEnd('.')
    }
}

@Composable
private fun CalculatorNumberButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = CalculatorButtonNumber,
            contentColor = CalculatorButtonTextPrimary
        ),
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CalculatorFunctionButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = CalculatorButtonFunction,
            contentColor = CalculatorButtonTextSecondary
        ),
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CalculatorOperatorButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = CalculatorButtonOperator,
            contentColor = CalculatorButtonTextPrimary
        ),
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CalculatorPreview() {
    CalculatorTheme {
        Surface(color = CalculatorBackground) {
            CalculatorScreen()
        }
    }
}