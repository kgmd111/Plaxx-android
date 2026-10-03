package com.plaxx.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plaxx.app.ui.viewmodels.LoginState
import com.plaxx.app.ui.viewmodels.ScheduleViewModel

@Composable
fun AuthScreen(
    viewModel: ScheduleViewModel,
    onLoginSuccess: () -> Unit,
) {
    var tab by remember { mutableStateOf(0) }
    var showingRegister by remember { mutableStateOf(false) }
    val loginState by viewModel.loginState.collectAsState()

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) onLoginSuccess()
    }

    if (showingRegister) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            RegisterFlow(
                viewModel = viewModel,
                onCancel = { showingRegister = false },
                onSuccess = { showingRegister = false }
            )
        }
        return
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Radial gradient w tle
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        radius = 1400f
                    )
                )
        )

        Column(
            Modifier
                .fillMaxWidth(0.92f)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(28.dp)
        ) {
            Text(
                "📅",
                fontSize = 48.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                "Plaxx",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                "Akademicki plan zajec",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(28.dp))

            AuthTabs(current = tab, onChange = { tab = it })

            Spacer(Modifier.height(24.dp))

            if (tab == 0) {
                LoginForm(viewModel, loginState, onLoginSuccess)
            } else {
                RegisterEntry(onStart = { showingRegister = true })
            }
        }
    }
}

@Composable
private fun AuthTabs(current: Int, onChange: (Int) -> Unit) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(5.dp)
    ) {
        val halfWidth = (maxWidth - 8.dp) / 2

        Box(
            Modifier
                .offset(x = if (current == 0) 0.dp else halfWidth + 4.dp)
                .width(halfWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
        )

        Row(Modifier.fillMaxSize()) {
            listOf("Zaloguj sie", "Zaloz konto").forEachIndexed { i, label ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onChange(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (current == i) Color.White
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginForm(
    viewModel: ScheduleViewModel,
    loginState: LoginState,
    onLoginSuccess: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(Modifier.fillMaxWidth()) {
        AppTextField(
            value = username,
            onChange = { username = it },
            label = "Nazwa uzytkownika",
            leadingIcon = "👤"
        )
        Spacer(Modifier.height(14.dp))
        AppTextField(
            value = password,
            onChange = { password = it },
            label = "Haslo",
            leadingIcon = "🔒",
            isPassword = true
        )
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { viewModel.login(username, password) },
            enabled = loginState !is LoginState.Loading &&
                    username.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            if (loginState is LoginState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Zaloguj sie", fontWeight = FontWeight.SemiBold)
            }
        }

        if (loginState is LoginState.Error) {
            Spacer(Modifier.height(12.dp))
            Text(
                loginState.message,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun RegisterEntry(onStart: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "Zaloz konto w 3 krokach:",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Text("1. Dane konta", fontSize = 12.sp)
        Text("2. Plik z planem (.ics / .xlsx)", fontSize = 12.sp)
        Text("3. Wybor grup", fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Rozpocznij rejestracje", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun AppTextField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    leadingIcon: String,
    isPassword: Boolean = false,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            leadingIcon = { Text(leadingIcon, fontSize = 18.sp) },
            visualTransformation = if (isPassword) PasswordVisualTransformation()
                                   else androidx.compose.ui.text.input.VisualTransformation.None,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
