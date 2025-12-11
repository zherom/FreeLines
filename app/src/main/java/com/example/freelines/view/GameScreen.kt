package com.example.freelines.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.freelines.game.BallColor
import com.example.freelines.game.Position
import com.example.freelines.viewmodel.GameViewModel

@Composable
fun GameScreen(gameViewModel: GameViewModel = viewModel()) {
    val board by gameViewModel.board.collectAsState()
    val selectedPosition by gameViewModel.selectedBallPosition.collectAsState()
    val score by gameViewModel.score.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Score: $score",
            fontSize = 24.sp,
            modifier = Modifier.padding(16.dp)
        )

        Text(
            text = "Ein Geschenk für dich!", // Personalized message
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(board.size),
            modifier = Modifier.fillMaxSize()
        ) {
            items(board.size * board.size) { index ->
                val row = index / board.size
                val col = index % board.size
                val position = Position(row, col)
                val ball = board.getBallAt(position)

                Cell(
                    ball = ball,
                    isSelected = selectedPosition == position,
                    onClick = { gameViewModel.onCellClicked(position) }
                )
            }
        }
    }
}

@Composable
fun Cell(
    ball: com.example.freelines.game.Ball?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val modifier = if (isSelected) {
        Modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .padding(4.dp)
            .border(2.dp, Color.Black)
            .clickable(onClick = onClick)
    } else {
        Modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .padding(4.dp)
            .clickable(onClick = onClick)
    }

    Canvas(modifier = modifier) {
        drawCircle(
            color = Color.LightGray,
            radius = size.minDimension / 2,
            center = Offset(size.width / 2, size.height / 2)
        )
        if (ball != null) {
            drawCircle(
                color = ball.color.toComposeColor(),
                radius = size.minDimension / 2,
                center = Offset(size.width / 2, size.height / 2)
            )
        }
    }
}

fun BallColor.toComposeColor(): Color {
    return when (this) {
        BallColor.RED -> Color.Red
        BallColor.GREEN -> Color.Green
        BallColor.BLUE -> Color.Blue
        BallColor.YELLOW -> Color.Yellow
        BallColor.PURPLE -> Color(0xFF800080) // Purple
        BallColor.ORANGE -> Color(0xFFFFA500) // Orange
    }
}
