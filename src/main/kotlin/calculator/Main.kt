package calculator

import java.util.*


fun main() {
    val map = mutableMapOf<String, String>()
    val varNameRegexp = """[a-zA-Z]+""".toRegex()
    val numberRegexp = """-?[0-9]+""".toRegex()
    val opers = listOf('*', '+', '-', '/', '^', '(', ')')

    mainCycle@ while (true) {
        val input = readln().replace(" ", "")
        when {
            input == "" -> {
                continue
            }
            input == "/exit" -> {
                println("Bye!")
                return
            }
            input == "/help" -> {
                println("The program calculates the sum of numbers")
            }
            input.first() == '/' -> {
                println("Unknown command")
            }
            input.contains("=") -> {
                val (varName, varValue) = input.split("=")
                if (!varNameRegexp.matches(varName)) {
                    println("Invalid identifier")
                } else if (map.containsKey(varValue)) {
                    map[varName] = map[varValue]!!
                } else if (varNameRegexp.matches(varValue)) {
                    println("Unknown variable")
                } else if (numberRegexp.matches(varValue)) {
                    map[varName] = varValue
                } else {
                    println("Invalid assignment")
                }
            }
            else -> {
                val listOfItems = mutableListOf<Item>()
                for (s in input) {
                    // unite sequences of +- signs
                    if (opers.contains(s)) {
                        if (s == '+' && (listOfItems.last().oper == Operation.PLUS || listOfItems.last().oper == Operation.MINUS)) {
                            //skip
                        } else if (s == '-' && (listOfItems.last().oper == Operation.PLUS || listOfItems.last().oper == Operation.MINUS)) {
                            listOfItems.set(
                                listOfItems.indices.last,
                                Item(if (listOfItems.last().oper == Operation.PLUS) Operation.MINUS else Operation.PLUS)
                            )
                        } else {
                            listOfItems.add(Item(getByCode(s)))
                        }
                    }
                    if (s.isDigit()) {
                        if (listOfItems.isNotEmpty() && listOfItems.last().value.isNotEmpty() && listOfItems.last().value.last().isDigit()) {
                            listOfItems.last().value = listOfItems.last().value + s
                        } else {
                            listOfItems.add(Item(null, s.toString()))
                        }
                    }
                    if (s.isLetter()) {
                        if (listOfItems.isNotEmpty() && listOfItems.last().value.isNotEmpty() && listOfItems.last().value.last().isLetter()) {
                            listOfItems.last().value = listOfItems.last().value + s
                        } else {
                            listOfItems.add(Item(null, s.toString()))
                        }
                    }
                }

                val listRes = mutableListOf<Item>()
                val operStack = Stack<Operation>()
                for (s in listOfItems) {
                    if (s.oper != null) {
                        val op = s.oper
                        if (operStack.isEmpty()) {
                            if (op == Operation.RIGHT_PAR) {
                                println("Invalid expression")
                                continue@mainCycle
                            } else {
                                operStack.push(op)
                            }
                        } else if (op == Operation.LEFT_PAR) {
                            operStack.push(op)
                        } else if (op == Operation.RIGHT_PAR) {
                            var pop = operStack.pop()
                            while (pop != Operation.LEFT_PAR) {
                                listRes.add(Item(pop))
                                if (operStack.isEmpty()) {
                                    println("Invalid expression")
                                    continue@mainCycle
                                }
                                pop = operStack.pop()
                            }
                        } else {
                            if (op.precedence > operStack.peek().precedence) {
                                operStack.push(op)
                            } else {
                                while (operStack.isNotEmpty() && op.precedence <= operStack.peek().precedence) {
                                    listRes.add(Item(operStack.pop()))
                                }
                                operStack.push(op)
                            }
                        }
                    } else {
                        if (map.containsKey(s.value)) {
                            listRes.add(Item(map[s.value]!!))
                        } else {
                            if (numberRegexp.matches(s.value)) {
                                listRes.add(Item(s.value))
                            } else {
                                println("Unknown variable")
                                continue@mainCycle
                            }
                        }
                    }
                }
                while (operStack.isNotEmpty()) {
                    val pop = operStack.pop()
                    if (pop == Operation.LEFT_PAR || pop == Operation.RIGHT_PAR) {
                        println("Invalid expression")
                        continue@mainCycle
                    }
                    listRes.add(Item(pop))
                }
                val resStack = Stack<Item>()
                for (r in listRes) {
                    if (r.oper == null) {
                        resStack.push(r)
                    } else {
                        if (resStack.isEmpty()) {
                            println("Invalid expression")
                            continue@mainCycle
                        }
                        val a2 = resStack.pop().value.toBigInteger()
                        if (resStack.isEmpty()) {
                            println("Invalid expression")
                            continue@mainCycle
                        }
                        val a1 = resStack.pop().value.toBigInteger()
                        resStack.push(Item(when (r.oper) {
                            Operation.PLUS -> a1 + a2
                            Operation.MINUS -> a1 - a2
                            Operation.MULT -> a1 * a2
                            Operation.DIV -> a1 / a2
                            Operation.POWER -> Math.pow(a1.toDouble(), a2.toDouble()).toInt()
                            else -> throw Exception("Unexpected operation: " + r.oper)
                        }.toString()))
                    }
                }
                if (resStack.size != 1) {
                    println("Invalid expression")
                    continue@mainCycle
                } else { println(resStack.pop().value) }
            }
        }
    }
}

class Item(val oper: Operation?, var value: String = "") {
    constructor(value: String): this(null, value)
}

enum class Operation(val code: Char, val precedence: Int) {
    PLUS('+', 1),
    MINUS('-', 1),
    MULT('*', 2),
    DIV('/', 2),
    POWER('^', 3),
    LEFT_PAR('(', 0),
    RIGHT_PAR(')', 0);
}

fun getByCode(code: Char): Operation? {
    return Operation.values().find { o -> o.code == code }
}