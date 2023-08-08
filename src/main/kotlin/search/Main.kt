package search

import java.io.File
import java.lang.IllegalArgumentException
import kotlin.system.exitProcess

enum class SearchStrategy {
    ALL,
    ANY,
    NONE
}


fun main(args: Array<String>) {
    val file: File
    if (args[0] == "--data") {
        file = File(args[1])
    } else {
        exitProcess(0)
    }

    val people = buildList {
        file.forEachLine { line ->
            add(line)
        }
    }

    val indexMap = buildMap {

        for (p in people.indices) {
            people[p]
                .split(" ")
                .map { s -> s.trim().uppercase() }
                .forEach { s ->
                    val lil: MutableSet<Int> = getOrDefault(s, mutableSetOf<Int>())
                    lil.add(p)
                    put(s, lil)
                }

        }
    }

    while (true) {
        var q = -1
        while (q !in 0..2) {
            println(
                """
=== Menu ===
1. Find a person
2. Print all people
0. Exit
    """
            )

            try {
                q = readln().toInt()
                if (q !in 0..2) {
                    throw IllegalArgumentException("Incorrect")
                }
            } catch (e: Exception) {
                println("Incorrect option! Try again.")
            }
        }

        if (q == 0) {
            println("Bye!")
            exitProcess(0)
        }
        if (q == 1) {
            println("Select a matching strategy: ALL, ANY, NONE")
            val strategy = SearchStrategy.valueOf(readln().uppercase())
            println("Enter a name or email to search all suitable people.")
            val searchString = readln().uppercase()
            val searchItems = searchString.split(" ").map { s -> s.trim().uppercase() }
            val resSet = mutableSetOf<Int>()
            if (strategy == SearchStrategy.ANY) {
                for (search in searchItems) {
                    resSet.addAll(indexMap.getOrDefault(search, emptySet<Int>()))
                }
            } else if (strategy == SearchStrategy.ALL) {
                var prev = indexMap.getOrDefault(searchItems[0], emptySet<Int>())
                if (prev.isNotEmpty()) {
                    for (i in 1..searchItems.lastIndex) {
                        if (prev.isNotEmpty()) {
                            val curSet = indexMap.getOrDefault(searchItems[i], emptySet<Int>())
                            prev = prev.intersect(curSet)
                        } else {
                            break
                        }
                    }
                    resSet.addAll(prev)
                }

            } else {
                val seq = MutableList<Int> (people.size) { it }
                for (search in searchItems) {
                    seq.removeAll(indexMap.getOrDefault(search, emptySet<Int>()))
                }
                resSet.addAll(seq)
            }
            if (resSet.isNotEmpty()) {
                for (r in resSet) {
                    println(people[r])
                }
            } else {
                println("No matching people found.")
            }
        } else {
            println("=== List of people ===")
            for (p in people) {
                println(p)
            }
        }
    }
}