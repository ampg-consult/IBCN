package com.ampgconsult.ibcn.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ampgconsult.ibcn.ui.viewmodels.UsernameViewModel

@Composable
fun UsernameSelectionScreen(
    onUsernameCreated: () -> Unit,
    viewModel: UsernameViewModel = hiltViewModel()
) {
    val username by viewModel.username.collectAsState()
    val validationState by viewModel.validationState.collectAsState()
    val isChecking by viewModel.isChecking.collectAsState()
    val isAvailable by viewModel.isAvailable.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Choose Your Identity",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { viewModel.onUsernameChanged(it) },
            label = { Text("Username") },
            placeholder = { Text("@AlexBrown") },
            modifier = Modifier.fillMaxWidth(),
            isError = validationState is UsernameViewModel.ValidationState.Error || isAvailable == false,
            supportingText = {
                when {
                    validationState is UsernameViewModel.ValidationState.Error -> {
                        Text((validationState as UsernameViewModel.ValidationState.Error).message)
                    }
                    isChecking -> {
                        Text("Checking availability...")
                    }
                    isAvailable == true -> {
                        Text("Username available!", color = Color(0xFF4CAF50))
                    }
                    isAvailable == false -> {
                        Text("Username already taken", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            singleLine = true
        )

        if (suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Suggestions:",
                modifier = Modifier.align(Alignment.Start),
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestions) { suggestion ->
                    SuggestionChip(
                        onClick = { viewModel.selectSuggestion(suggestion) },
                        label = { Text(suggestion) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { 
                viewModel.submitUsernameWithInfo(firstName, lastName, onUsernameCreated)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isAvailable == true && 
                      validationState is UsernameViewModel.ValidationState.Valid && 
                      firstName.isNotBlank() && 
                      lastName.isNotBlank() &&
                      !isSubmitting,
            shape = MaterialTheme.shapes.medium
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Get Started")
            }
        }
    }
}
