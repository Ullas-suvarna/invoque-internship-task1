package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.CurrencyUtils

data class PieChartSlice(
    val label: String,
    val value: Double,
    val color: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveDonutChart(
    slices: List<PieChartSlice>,
    totalAmount: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    if (slices.isEmpty() || totalAmount <= 0) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No expense data for this period",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(700))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val strokeWidth = 28.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val topLeft = Offset(
                    (size.width - radius * 2) / 2,
                    (size.height - radius * 2) / 2
                )
                val arcSize = Size(radius * 2, radius * 2)

                var startAngle = -90f
                slices.forEach { slice ->
                    val sweepAngle = ((slice.value / totalAmount).toFloat() * 360f) * progress.value
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total Spent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyUtils.formatCompactAmount(totalAmount, currencySymbol),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend chips
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slices.forEach { slice ->
                val pct = ((slice.value / totalAmount) * 100).toInt()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(slice.color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${slice.label} ($pct%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

data class BarChartItem(
    val label: String,
    val income: Double,
    val expense: Double
)

@Composable
fun IncomeExpenseBarChart(
    items: List<BarChartItem>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No financial data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxVal = remember(items) {
        val max = items.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0.0
        if (max <= 0) 100.0 else max
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(items) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(600))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(10.dp).background(IncomeGreen, CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Box(modifier = Modifier.size(10.dp).background(ExpenseRed, CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Expense", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height - 25.dp.toPx()
            val groupWidth = canvasWidth / items.size
            val barWidth = (groupWidth * 0.28f).coerceAtMost(22.dp.toPx())
            val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())

            items.forEachIndexed { index, item ->
                val groupCenterX = (index * groupWidth) + (groupWidth / 2)
                val incomeX = groupCenterX - barWidth - 2.dp.toPx()
                val expenseX = groupCenterX + 2.dp.toPx()

                val incomeBarHeight = ((item.income / maxVal) * canvasHeight * progress.value).toFloat()
                val expenseBarHeight = ((item.expense / maxVal) * canvasHeight * progress.value).toFloat()

                // Income bar
                if (incomeBarHeight > 0) {
                    drawRoundRect(
                        color = IncomeGreen,
                        topLeft = Offset(incomeX, canvasHeight - incomeBarHeight),
                        size = Size(barWidth, incomeBarHeight),
                        cornerRadius = cornerRadius
                    )
                }

                // Expense bar
                if (expenseBarHeight > 0) {
                    drawRoundRect(
                        color = ExpenseRed,
                        topLeft = Offset(expenseX, canvasHeight - expenseBarHeight),
                        size = Size(barWidth, expenseBarHeight),
                        cornerRadius = cornerRadius
                    )
                }

                // Base line
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(0f, canvasHeight),
                    end = Offset(canvasWidth, canvasHeight),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        // Labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            items.forEach { item ->
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class TrendPoint(
    val label: String,
    val amount: Double
)

@Composable
fun SpendingTrendLineChart(
    points: List<TrendPoint>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No spending trend available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxVal = remember(points) {
        val max = points.maxOfOrNull { it.amount } ?: 0.0
        if (max <= 0) 100.0 else max
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(650))
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height - 20.dp.toPx()
            val stepX = if (points.size > 1) canvasWidth / (points.size - 1) else canvasWidth

            val linePath = Path()
            val fillPath = Path()

            val coordinates = points.mapIndexed { index, point ->
                val x = index * stepX
                val normalizedY = ((point.amount / maxVal) * canvasHeight * progress.value).toFloat()
                val y = canvasHeight - normalizedY
                Offset(x, y)
            }

            if (coordinates.isNotEmpty()) {
                linePath.moveTo(coordinates.first().x, coordinates.first().y)
                fillPath.moveTo(coordinates.first().x, canvasHeight)
                fillPath.lineTo(coordinates.first().x, coordinates.first().y)

                for (i in 1 until coordinates.size) {
                    val prev = coordinates[i - 1]
                    val curr = coordinates[i]
                    val controlX1 = (prev.x + curr.x) / 2
                    val controlY1 = prev.y
                    val controlX2 = (prev.x + curr.x) / 2
                    val controlY2 = curr.y

                    linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, curr.x, curr.y)
                    fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, curr.x, curr.y)
                }

                fillPath.lineTo(coordinates.last().x, canvasHeight)
                fillPath.close()

                // Draw gradient area
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.35f),
                            primaryColor.copy(alpha = 0.02f)
                        ),
                        startY = 0f,
                        endY = canvasHeight
                    )
                )

                // Draw curve
                drawPath(
                    path = linePath,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw point circles
                coordinates.forEach { pt ->
                    drawCircle(
                        color = primaryColor,
                        radius = 4.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = pt
                    )
                }
            }
        }

        // Labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { point ->
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
