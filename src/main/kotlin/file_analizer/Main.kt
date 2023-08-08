package analyzer

import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors


fun main(args: Array<String>) {
    val (folder, patternsFile) = args
    val start = System.nanoTime()
    val patterns = File(patternsFile).readLines()
        .map { it.split(";").map { s -> s.removeSurrounding("\"") } }
        .sortedByDescending { it[0].toInt() }
        .toList()
    val fileFolder = File(folder)
    val executorService = Executors.newFixedThreadPool(fileFolder.list()!!.size)
    fileFolder.listFiles()?.map { file ->
        executorService.submit(Callable {
            val readText = file.readText(Charsets.UTF_8)
            val type = findPatterns(readText, patterns)
            Pair<String, String>(file.name, type)
        })
    }.orEmpty().forEach {
        val (name, result) = it.get()
        println("$name: $result")
    }
    val time = String.format("%.3f", (System.nanoTime() - start) / 1000000000.0)
    println("It took $time seconds")
}

fun findPatterns(readText: String, patterns: Collection<List<String>>): String {
    for ((_, pattern, type) in patterns) {
        if (findPatternRabinKarp(readText, pattern)) {
            return type
        }
    }
    return "Unknown file type"
}

fun findPatternKmp(readText: String, pattern: String): Boolean {
    val prefixFun = calculatePrefixFun(pattern)
    var index = 0
    loop@ while (index < readText.length - pattern.length + 1) {
        for (i in pattern.indices) {
            if (readText[index + i] != pattern[i]) {
                if (i == 0) {
                    index++
                } else {
                    index += i - prefixFun[i - 1]
                }
                continue@loop
            }
        }
        return true
    }
    return false
}

fun checkEquals(readText: String, pattern: String, startIndex: Int): Boolean {
    for (i in pattern.indices) {
        if (readText[startIndex + i] != pattern[i]) {
            return false
        }
    }
    return true
}

fun findPatternRabinKarp(readText: String, pattern: String): Boolean {
    if (pattern.length > readText.length) {
        return false
    }
    val hash = calculateHash(pattern).toLong()
    val index = readText.length - pattern.length
    var hashSum: Long = calculateHash(readText.substring(index)).toLong()
    val pow = power(3, pattern.length - 1)
    if (hashSum == hash && checkEquals(readText, pattern, index)) {
        return true
    }
    loop@ for (i in index - 1 downTo 0) {
        hashSum = ((hashSum - (getCode(readText, i + pattern.length) * pow)) * 3
                + getCode(readText, i)).mod(11).toLong()
        if (hashSum == hash && checkEquals(readText, pattern, i)) {
            return true
        }
    }
    return false
}

fun power(baseVal: Int, exponentVal: Int): Int {
    return if (exponentVal != 0) {
        (baseVal * power(baseVal, exponentVal - 1)).mod(11)
    } else 1
}

fun calculateHash(pattern: String): Int {
    var sum = 0
    var power = 1
    for (i in pattern.indices) {
        sum += getCode(pattern, i) * power
        power = (power * 3).mod(11)
    }
    return sum.mod(11)
}

private fun getCode(pattern: String, index: Int): Int = pattern[index].code

private fun calculatePrefixFun(pattern: String): IntArray {
    val prefixFun = IntArray(pattern.length)
    loop@ for (i in 1 until pattern.length) {
        var index = prefixFun[i - 1]
        if (pattern[index] == pattern[i]) {
            prefixFun[i] = index + 1
        } else {
            while (index > 0) {
                index = prefixFun[index - 1]
                if (pattern[index] == pattern[i]) {
                    prefixFun[i] = index + 1
                    continue@loop
                }
            }
        }
    }
    return prefixFun
}
fun findPatternNaive(readText: String, pattern: String): Boolean {
    loop@ for (index in 0 until readText.length - pattern.length + 1) {
        for (i in pattern.indices) {
            if (readText[index + i] != pattern[i]) {
                continue@loop
            }
        }
        return true
    }
    return false
}
