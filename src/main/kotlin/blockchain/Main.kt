package blockchain

import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.util.concurrent.TimeUnit
import kotlin.random.Random

private const val MINERS_NUMBER = 10
private const val CHATTER_NUMBER = 5
private const val MAGIC_MAX = 99999999

private const val MAX_BLOCKS = 15

private const val MIN_TIME = 1
private const val MAX_TIME = 1

private const val INITIAL_AMOUNT = 100L
private const val BLOCK_AMOUNT = 100
private const val AMOUNT = 30L

fun main() {
    val keyGen = KeyPairGenerator.getInstance("RSA")
    keyGen.initialize(1024)
    val blockchain = Blockchain(0, mutableListOf())

    repeat(CHATTER_NUMBER) {
        val chatter = Chatter("chatter${it + 1}", blockchain, keyGen)
        chatter.start()
    }
    repeat(MINERS_NUMBER) {
        val miner = Miner("miner${it + 1}", blockchain)
        miner.start()
    }
    repeat(CHATTER_NUMBER) {
        val chatter = Chatter("miner${it + 1}", blockchain, keyGen)
        chatter.start()
    }
    while (blockchain.counter < MAX_BLOCKS) {
        TimeUnit.SECONDS.sleep(1)
    }
}

class Chatter(private val id: String, private val blockchain: Blockchain, private val keyGen: KeyPairGenerator) : Thread() {
    override fun run() {
        val keyPair = keyGen.generateKeyPair()
        while (!isInterrupted) {
            TimeUnit.SECONDS.sleep(Random.nextLong(0, 10))
            val receiver = "chatter${Random.nextInt(CHATTER_NUMBER)}"
            val idCounter = blockchain.generateId()
            val data = messageDataToBytes(idCounter, id, receiver, AMOUNT)
            blockchain.sendMessage(
                Message(
                    id,
                    receiver,
                    AMOUNT,
                    sign(keyPair.private, data),
                    idCounter,
                    keyPair.public
                )
            )
        }
    }
}

class Miner(private val clientId: String, private val blockchain: Blockchain) : Thread() {
    override fun run() {
        while (!isInterrupted && blockchain.counter < MAX_BLOCKS) {
            val timeStamp: Long = System.currentTimeMillis()
            val prefix = "${blockchain.counter}-${timeStamp}-${blockchain.last?.hash ?: "0"}-"
            val counter = blockchain.counter
            var hash: String
            var magic: Int
            do {
                magic = Random.nextInt(MAGIC_MAX)
                hash = applySha256(prefix + magic)
            } while (!startsWithZeros(hash, blockchain.n) && counter == blockchain.counter)
            if (counter == blockchain.counter) {
                val duration: Long = (System.currentTimeMillis() - timeStamp) / 1000
                blockchain.suggestBlock(
                    Block(
                        blockchain.counter,
                        timeStamp,
                        blockchain.last,
                        hash,
                        magic,
                        clientId,
                        duration
                    )
                )
            }
        }
    }
}

fun printBlock(block: Block) {
    val data = if (block.messages.isEmpty()) "No transactions" else
        block.messages.joinToString("\n", "\n") { "${it.sender} sent ${it.amount} VC to ${it.receiver}" }
    println(
        """
Block:
Created by: ${block.creator}
${block.creator} gets 100 VC
Id: ${block.id}
Timestamp: ${block.timestamp}
Magic number: ${block.magic}
Hash of the previous block:
${block.prev?.hash ?: 0}
Hash of the block:
${block.hash} 
Block data: $data
Block was generating for ${block.seconds} seconds
    """.trimIndent()
    )
}

class Blockchain(var n: Int, private val messages: MutableList<Message>) {
    var counter = 0
    var last: Block? = null
    private var currentId = 0L

    fun generateId(): Long {
        currentId = Random.nextLong(currentId + 1, currentId + 1 + 10)
        return currentId
    }

    private fun modifyN(block: Block) {
        when {
            block.seconds < MIN_TIME && n < 5 -> {
                println("N was increased to ${++n}")
            }

            block.seconds > MAX_TIME -> {
                println("N was decreased by ${--n}")
            }

            else -> {
                println("N stays the same")
            }
        }
        println()
    }

    @Synchronized
    fun suggestBlock(block: Block) {
        if (block.prev == last && block.id == counter && startsWithZeros(block.hash, n)) {
            if (block.hash == applySha256("${counter}-${block.timestamp}-${last?.hash ?: 0}-${block.magic}")) {
                counter++
                val newBlock = Block(block, messages.toList(), currentId)
                messages.clear()
                last = newBlock
                printBlock(newBlock)
                modifyN(newBlock)
            }
        }
    }

    @Synchronized
    fun sendMessage(s: Message) {
        if ((last?.curId ?: -1) < s.id) {
            if (verifySignature(
                    messageDataToBytes(s.id, s.sender, s.receiver, s.amount),
                    s.signature,
                    s.publicKey
                )
            ) {
                if (calculate(s.sender) > s.amount) {
                    messages.add(s)
                }
            }
        }
    }

    private fun calculate(userId: String): Long {
        var res = INITIAL_AMOUNT
        res += messages.sumOf { messageToAmount(it, userId) }
        var cur: Block? = last ?: return res
        while (cur != null) {
            if (cur.creator == userId) {
                res += BLOCK_AMOUNT
            }
            res += cur.messages.sumOf { messageToAmount(it, userId) }
            cur = cur.prev
        }
        return res
    }

    private fun messageToAmount(it: Message, userId: String) = when {
        it.sender == userId && it.receiver == userId -> 0
        it.sender == userId -> -AMOUNT
        it.receiver == userId -> AMOUNT
        else -> 0
    }
}

private fun startsWithZeros(hash: String, n: Int): Boolean {
    for (i in 0 until n) {
        if (hash[i] != '0') {
            return false
        }
    }
    return true
}

private fun verifySignature(data: ByteArray, signature: ByteArray, key: PublicKey): Boolean {
    val sig = Signature.getInstance("SHA1withRSA")
    sig.initVerify(key)
    sig.update(data)
    return sig.verify(signature)
}

private fun sign(key: PrivateKey, data: ByteArray): ByteArray {
    val rsa = Signature.getInstance("SHA1withRSA")
    rsa.initSign(key)
    rsa.update(data)
    return rsa.sign()
}

private fun messageDataToBytes(id: Long, sender: String, rec: String, amount: Long) =
    "$id:$sender:$rec:$amount".toByteArray()

fun applySha256(input: String): String {
    return try {
        val digest = MessageDigest.getInstance("SHA-256")
        /* Applies sha256 to our input */
        val hash = digest.digest(input.toByteArray(charset("UTF-8")))
        val hexString = StringBuilder()
        for (elem in hash) {
            val hex = Integer.toHexString(0xff and elem.toInt())
            if (hex.length == 1) hexString.append('0')
            hexString.append(hex)
        }
        hexString.toString()
    } catch (e: Exception) {
        throw RuntimeException(e)
    }
}

data class Block(
    val id: Int,
    val timestamp: Long,
    val prev: Block?,
    val hash: String,
    val magic: Int,
    val creator: String,
    val seconds: Long,
    val messages: List<Message> = emptyList(),
    val curId: Long = 0
) {
    constructor(block: Block, messages: List<Message>, curId: Long) : this(
        block.id,
        block.timestamp,
        block.prev,
        block.hash,
        block.magic,
        block.creator,
        block.seconds,
        messages,
        curId
    )
}

data class Message(
    val sender: String,
    val receiver: String,
    val amount: Long,
    val signature: ByteArray,
    val id: Long,
    val publicKey: PublicKey
)