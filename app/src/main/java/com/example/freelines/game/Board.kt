package com.example.freelines.game

import java.util.LinkedList
import java.util.Queue

data class Position(val row: Int, val col: Int)

class Board(val size: Int = 9) {

    private val grid: MutableMap<Position, Ball> = mutableMapOf()

    fun getBallAt(position: Position): Ball? {
        return grid[position]
    }

    fun placeBall(ball: Ball, position: Position) {
        grid[position] = ball
    }

    fun removeBalls(positions: List<Position>) {
        for (position in positions) {
            grid.remove(position)
        }
    }

    fun moveBall(from: Position, to: Position) {
        val ball = getBallAt(from)
        if (ball != null) {
            removeBalls(listOf(from))
            placeBall(ball, to)
        }
    }

    fun hasPath(from: Position, to: Position): Boolean {
        if (getBallAt(to) != null) return false

        val visited = mutableSetOf<Position>()
        val queue: Queue<Position> = LinkedList()

        visited.add(from)
        queue.add(from)

        while (queue.isNotEmpty()) {
            val current = queue.poll()

            if (current == to) {
                return true
            }

            for (neighbor in getNeighbors(current)) {
                if (neighbor !in visited && getBallAt(neighbor) == null) {
                    visited.add(neighbor)
                    queue.add(neighbor)
                }
            }
        }

        return false
    }

    private fun getNeighbors(position: Position): List<Position> {
        val (row, col) = position
        val neighbors = mutableListOf<Position>()

        if (row > 0) neighbors.add(Position(row - 1, col))
        if (row < size - 1) neighbors.add(Position(row + 1, col))
        if (col > 0) neighbors.add(Position(row, col - 1))
        if (col < size - 1) neighbors.add(Position(row, col + 1))

        return neighbors
    }

    fun findLines(): List<Position> {
        val lines = mutableSetOf<Position>()

        for (row in 0 until size) {
            for (col in 0 until size) {
                val pos = Position(row, col)
                getBallAt(pos)?.let {
                    lines.addAll(findLineAt(pos))
                }
            }
        }
        return lines.toList()
    }

    private fun findLineAt(pos: Position): List<Position> {
        val lines = mutableListOf<Position>()
        val directions = listOf(listOf(1, 0), listOf(0, 1), listOf(1, 1), listOf(1, -1))

        for (dir in directions) {
            val line = findLineInDirection(pos, dir[0], dir[1])
            if (line.size >= 5) {
                lines.addAll(line)
            }
        }
        return lines
    }

    private fun findLineInDirection(start: Position, dRow: Int, dCol: Int): List<Position> {
        val ball = getBallAt(start) ?: return emptyList()
        val line = mutableListOf(start)

        // Check in the positive direction
        var r = start.row + dRow
        var c = start.col + dCol
        while (r in 0 until size && c in 0 until size && getBallAt(Position(r, c))?.color == ball.color) {
            line.add(Position(r, c))
            r += dRow
            c += dCol
        }

        // Check in the negative direction
        r = start.row - dRow
        c = start.col - dCol
        while (r in 0 until size && c in 0 until size && getBallAt(Position(r, c))?.color == ball.color) {
            line.add(Position(r, c))
            r -= dRow
            c -= dCol
        }

        return line
    }
}
