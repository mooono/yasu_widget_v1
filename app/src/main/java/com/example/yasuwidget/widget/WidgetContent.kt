package com.example.yasuwidget.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.yasuwidget.domain.model.DisplayMode
import com.example.yasuwidget.usecase.DepartureDisplay
import com.example.yasuwidget.usecase.WidgetUiState

/**
 * Widget UI content (Glance composable)
 * UI-REQ-001: Display mode-specific information
 */
@Composable
fun WidgetContent(uiState: WidgetUiState) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(12.dp)
            .background(ColorProvider(android.graphics.Color.WHITE))
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = uiState.headerTitle,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = "更新 ${uiState.lastUpdatedAtText}",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = ColorProvider(android.graphics.Color.GRAY)
                )
            )
        }
        
        Spacer(modifier = GlanceModifier.height(8.dp))
        
        // Status message if present
        uiState.statusMessage?.let { message ->
            Text(
                text = message,
                style = TextStyle(
                    fontSize = 14.sp,
                    color = ColorProvider(android.graphics.Color.RED)
                )
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
        }
        
        // Content based on mode
        when (uiState.mode) {
            DisplayMode.TRAIN_ONLY -> {
                uiState.train?.let { TrainSection(it) }
            }
            DisplayMode.TRAIN_AND_BUS -> {
                uiState.train?.let { TrainSection(it) }
                Spacer(modifier = GlanceModifier.height(8.dp))
                uiState.bus?.let { BusSection(it) }
            }
            DisplayMode.BUS_ONLY -> {
                uiState.bus?.let { BusSection(it) }
            }
        }
        
        Spacer(modifier = GlanceModifier.height(8.dp))
        
        // Action buttons
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Horizontal.End
        ) {
            Text(
                text = "🔄 更新",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = ColorProvider(android.graphics.Color.BLUE)
                ),
                modifier = GlanceModifier.clickable(
                    actionRunCallback<RefreshAction>()
                )
            )
        }
    }
}

@Composable
fun TrainSection(train: com.example.yasuwidget.usecase.TrainSection) {
    Column {
        if (train.lineName.isNotEmpty()) {
            Text(
                text = train.lineName,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = ColorProvider(android.graphics.Color.GRAY)
                )
            )
        }
        
        // Up direction
        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(
                text = "↑",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.width(4.dp))
            if (train.upDepartures.isEmpty()) {
                Text(
                    text = "---",
                    style = TextStyle(fontSize = 14.sp)
                )
            } else {
                DepartureList(train.upDepartures)
            }
        }
        
        Spacer(modifier = GlanceModifier.height(4.dp))
        
        // Down direction
        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(
                text = "↓",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.width(4.dp))
            if (train.downDepartures.isEmpty()) {
                Text(
                    text = "---",
                    style = TextStyle(fontSize = 14.sp)
                )
            } else {
                DepartureList(train.downDepartures)
            }
        }
    }
}

@Composable
fun BusSection(bus: com.example.yasuwidget.usecase.BusSection) {
    Column {
        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(
                text = "Bus ${bus.direction}",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        Spacer(modifier = GlanceModifier.height(4.dp))
        
        if (bus.departures.isEmpty()) {
            Text(
                text = "---",
                style = TextStyle(fontSize = 14.sp)
            )
        } else {
            DepartureList(bus.departures)
        }
    }
}

@Composable
fun DepartureList(departures: List<DepartureDisplay>) {
    Row(
        horizontalAlignment = Alignment.Horizontal.Start
    ) {
        departures.forEachIndexed { index, departure ->
            if (index > 0) {
                Text(text = " / ", style = TextStyle(fontSize = 14.sp))
            }
            val displayText = if (departure.destination != null) {
                "${departure.time} ${departure.destination}"
            } else {
                departure.time
            }
            Text(
                text = displayText,
                style = TextStyle(fontSize = 14.sp)
            )
        }
    }
}
