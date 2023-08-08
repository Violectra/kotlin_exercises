package processor

fun main() {
    while (true) {
        println(
            """
        1. Add matrices
        2. Multiply matrix by a constant
        3. Multiply matrices
        4. Transpose matrix
        5. Calculate a determinant
        6. Inverse matrix
        0. Exit
        Your choice:
    """.trimIndent()
        )
        when (readln()) {
            "1" -> addMatrices()
            "2" -> multiplyByConstant()
            "3" -> multiplyMatrices()
            "4" -> matrixTransposition()
            "5" -> calculateDeterminant()
            "6" -> inverseMatrix()
            "0" -> return
        }
    }
}

fun inverseMatrix() {
    val (r1, c1) = readDimensions()
    if (r1 != c1) {
        println("ERROR")
        return
    }
    val m1 = readMatrix(r1, c1)
    val ba = BooleanArray (r1 + c1) { true }
    val map = mutableMapOf<String, Double>()
    val det = determinant(r1, m1, ba, map)
    if (det == 0.0) {
        println("This matrix doesn't have an inverse.")
    }
    val d = 1 / det
    val calc = { r: Int, c: Int ->
        val ba2 = booleans(ba, c, r)
        d * coefficient(r + c) * determinant(r1 - 1, copyArrayWithoutColumnAndRow(m1, r, c), ba2, map)
    }
    println("The result is:")
    printResponse(r1, c1, calc)
}

fun calculateDeterminant() {
    val (r1, c1) = readDimensions()
    if (r1 != c1) {
        println("ERROR")
        return
    }
    val m1 = readMatrix(r1, c1)
    println("The result is:")
    val ba = BooleanArray (r1 + c1) { true }
    ba.hashCode()
    println(determinant(r1, m1, ba, mutableMapOf()))
}

fun determinant(size: Int, m1: Array<DoubleArray>, ba: BooleanArray, map: MutableMap<String, Double>): Double {
    val key = ba.map { if (it) 1 else 0 }.joinToString("")
    if (map.containsKey(key)) {
        return map[key]!!
    }
    if (size == 1) {
        map[key] = m1[0][0]
        return m1[0][0]
    }
    if (size == 2) {
        val res = m1[0][0] * m1[1][1] - m1[1][0] * m1[0][1]
        map[key] = res
        return res
    }
    var sum = 0.0
    for (i in 0 until size) {
        val newArray = copyArrayWithoutColumnAndRow(m1, i, 0)
        val ba2 = booleans(ba, 0, i)
        sum += m1[0][i] * coefficient(i) * determinant(size - 1, newArray, ba2, map)
    }
    map[key] = sum
    return sum
}

private fun booleans(ba: BooleanArray, r: Int, c: Int): BooleanArray {
    var trueCounter = 0
    val half = ba.size / 2
    return BooleanArray(ba.size) {
        if (it == half) {
            trueCounter = 0
        }
        if (ba[it]) {
            trueCounter++
        }
        if (trueCounter == c + 1 && it >= half) {
            false
        } else if (trueCounter == r + 1 && it < half) {
            false
        } else {
            ba[it]
        }
    }
}

private fun coefficient(i: Int) = if (i.mod(2) == 0) 1 else -1

fun copyArrayWithoutColumnAndRow(m1: Array<DoubleArray>, c: Int, r: Int = 0): Array<DoubleArray> {
    return Array (m1.size - 1) {
        val row = if (it < r) m1[it] else m1[it + 1]
        DoubleArray (m1.size - 1) {
            if (it < c) row[it] else row[it + 1]
        }
    }
}

fun matrixTransposition() {
    println(
        """
        1. Main diagonal
        2. Side diagonal
        3. Vertical line
        4. Horizontal line
    """.trimIndent()
    )

    val transpositionType = getTranspositionType(readln())!!
    val (r1, c1) = readDimensions()
    if (r1 != c1) {
        println("ERROR")
        return
    }
    val m1 = readMatrix(r1, c1)
    val call = { r: Int, c: Int ->
        when (transpositionType) {
            TranspositionType.MAIN_DIAGONAL -> m1[c][r]
            TranspositionType.SIDE_DIAGONAL -> m1[c1 - c - 1][r1 - r - 1]
            TranspositionType.VERTICAL -> m1[r][c1 - c - 1]
            TranspositionType.HORIZONTAL -> m1[r1 - r - 1][c]
        }
    }
    printResponse(r1, c1, call)

}

enum class TranspositionType(val typeNumber: String) {
    MAIN_DIAGONAL("1"),
    SIDE_DIAGONAL("2"),
    VERTICAL("3"),
    HORIZONTAL("4")
}


private fun getTranspositionType(typeNumber: String): TranspositionType? {
    return TranspositionType.values().find { it.typeNumber == typeNumber }
}

private const val PATTERN = "%.10f "

private fun multiplyByConstant() {
    val (r1, c1) = readDimensions()
    val m1 = readMatrix(r1, c1)
    val multiplier = readln().toDouble()
    val calc = { r: Int, c: Int -> m1[r][c] * multiplier }
    printResponse(r1, c1, calc)
}

private fun addMatrices() {
    val (r1, c1) = readDimensions()
    val m1 = readMatrix(r1, c1)
    val (r2, c2) = readDimensions()
    val m2 = readMatrix(r2, c2)
    if (r1 != r2 || c1 != c2) {
        println("ERROR")
        return
    }
    val calc = { r: Int, c: Int -> m1[r][c] + m2[r][c] }
    printResponse(r1, c1, calc)
}

private fun multiplyMatrices() {
    val (r1, c1) = readDimensions()
    val m1 = readMatrix(r1, c1)
    val (r2, c2) = readDimensions()
    val m2 = readMatrix(r2, c2)
    if (c1 != r2) {
        println("ERROR")
        return
    }
    val calc = { r: Int, c: Int -> IntArray(c1) { it }.sumOf { m1[r][it] * m2[it][c] } }
    printResponse(r1, c2, calc)
}

private fun readDimensions() = readln().split(" ").map { it.toInt() }

private fun readMatrix(r1: Int, c1: Int) = Array(r1) {
    readln().split(" ", limit = c1).map { it.toDouble() }.toDoubleArray()
}

private fun printResponse(r1: Int, c1: Int, function: (Int, Int) -> Double) {
    for (r in 0 until r1) {
        for (c in 0 until c1) {
            print(String.format(PATTERN, function.invoke(r, c)))
        }
        println()
    }
}
