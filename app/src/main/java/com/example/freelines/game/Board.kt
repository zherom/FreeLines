package com.example.freelines.game

import java.util.LinkedList
import java.util.Queue

// Data Transfer Object for serialization
data class BoardData(val grid: List<Pair<Position, Ball>>, val size: Int)

data class Position(val row: Int, val col: Int)

class Board(val size: Int = 9) {

    private var grid: MutableList<Pair<Position, Ball>> = mutableListOf()

    // Secondary constructor to restore a board from data
    constructor(boardData: BoardData) : this(boardData.size) {
        this.grid = boardData.grid.toMutableList()
    }

    fun toBoardData(): BoardData {
        return BoardData(grid.toList(), size)
    }

    fun getBallAt(position: Position): Ball? {
        return grid.find { it.first == position }?.second
    }

    fun placeBall(ball: Ball, position: Position) {
        grid.removeAll { it.first == position }
        grid.add(position to ball)
    }

    fun removeBalls(positions: List<Position>) {
        grid.removeAll { it.first in positions }
    }

    fun moveBall(from: Position, to: Position) {
        val ball = getBallAt(from)
        if (ball != null) {
            grid.removeAll { it.first == from }
            grid.removeAll { it.first == to }
            grid.add(to to ball)
        }
    }

    fun isFull(): Boolean {
        return grid.size >= size * size
    }

    fun hasPath(from: Position, to: Position): Boolean {
        if (getBallAt(to) != null) return false
        val visited = mutableSetOf<Position>()
        val queue: Queue<Position> = LinkedList()
        visited.add(from)
        queue.add(from)
        val allBallPositions = grid.map { it.first }.toSet()
        while (queue.isNotEmpty()) {
            val current = queue.poll()!!
            if (current == to) return true
            for (neighbor in getNeighbors(current)) {
                if (neighbor !in visited && !allBallPositions.contains(neighbor)) {
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
        for (pos in grid.map { it.first }) {
            lines.addAll(findLineAt(pos))
        }
        return lines.toList()
    }

    fun copy(): Board {
        return Board(this.toBoardData())
    }

    private fun findLineAt(pos: Position): List<Position> {
        val ball = getBallAt(pos) ?: return emptyList()
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
        var r = start.row + dRow
        var c = start.col + dCol
        while (r in 0 until size && c in 0 until size && getBallAt(Position(r, c))?.color == ball.color) {
            line.add(Position(r, c))
            r += dRow
            c += dCol
        }
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
