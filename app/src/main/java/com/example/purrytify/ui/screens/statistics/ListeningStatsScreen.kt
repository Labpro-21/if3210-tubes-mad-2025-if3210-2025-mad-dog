package com.example.purrytify.ui.screens.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.db.dao.ListeningActivityDao.DailyListeningStats
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningStatsScreen(
    viewModel: ListeningStatsViewModel = viewModel(),
    year: Int = LocalDate.now().year,
    month: Int = LocalDate.now().monthValue,
    onNavigateBack: () -> Unit
) {
    val stats by viewModel.dailyStats.collectAsState()
    val monthYear by viewModel.monthYear.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val averageMinutes by viewModel.averageMinutesPerDay.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDailyStats()
    }

    LaunchedEffect(year, month) {
        viewModel.loadDailyStats(year, month)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listening Statistics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Black)
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
            } else {
                Text(
                    text = monthYear ?: "Listening Activity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Average minutes per day
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF212121)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Daily Average",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "${String.format("%.1f", averageMinutes)} minutes",
                            color = Color(0xFF4CAF50),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Chart
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF212121)
                    )
                ) {
                    if (stats.isNotEmpty()) {
                        val entries = stats.map { stat ->
                            val day = LocalDate.parse(stat.date).dayOfMonth.toFloat()
                            FloatEntry(
                                x = day,
                                y = stat.totalMinutes.toFloat()
                            )
                        }

                        val chartEntryModel = entryModelOf(entries)

                        ProvideChartStyle {
                            Chart(
                                chart = lineChart(),
                                model = chartEntryModel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                startAxis = startAxis(
                                    valueFormatter = { value, _ ->
                                        String.format("%.1f", value)
                                    }
                                ),
                                bottomAxis = bottomAxis(
                                    valueFormatter = { value, _ ->
                                        value.toInt().toString()
                                    }
                                )
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No listening data available",
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
} 