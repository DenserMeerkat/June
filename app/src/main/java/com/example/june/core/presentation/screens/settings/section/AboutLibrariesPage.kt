package com.example.june.core.presentation.screens.settings.section

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer

import com.example.june.R
import com.example.june.core.navigation.AppNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutLibrariesPage() {
    val navigator = koinInject<AppNavigator>()
    val libraries = produceLibraries().value

    Scaffold(
        modifier = Modifier.widthIn(max = 1000.dp), topBar = {
            TopAppBar(title = { Text(stringResource(R.string.about_libraries)) }, navigationIcon = {
                IconButton(
                    onClick = { navigator.navigateBack() }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back_24px),
                        contentDescription = "Back"
                    )
                }
            })
        }) { padding ->
        LibrariesContainer(
            libraries = libraries,
            typography = MaterialTheme.typography,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        )
    }
}