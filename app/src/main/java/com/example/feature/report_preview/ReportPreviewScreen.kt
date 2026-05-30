package com.example.feature.report_preview

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportPreviewScreen(
    viewModel: ReportPreviewViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Trigger report compilation on launch
    LaunchedEffect(Unit) {
        viewModel.generateReport(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostics Report", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                "Compiled Diagnostics Dossier",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Exportable plain-text dossier containing raw sensor baselines, power health metrics, and previous sandbox throttle runs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            when (val state = uiState) {
                is ReportUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ReportUiState.Success -> {
                    // Report text box displaying dossier log terminal-style
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF1E1E1E), shape = MaterialTheme.shapes.small)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                            .testTag("report_text_box")
                    ) {
                        Text(
                            text = state.reportText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF4CAF50),
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val file = state.reportFile
                            if (file != null && file.exists()) {
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "com.aistudio.sensorscope.uxqzp.provider",
                                        file
                                    )
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Share Diagnostics Dossier Report"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "Report file not compiled yet", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_report_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Diagnostics Dossier")
                    }
                }
            }
        }
    }
}
