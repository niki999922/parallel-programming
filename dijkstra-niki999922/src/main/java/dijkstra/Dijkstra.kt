package dijkstra

import kotlinx.atomicfu.atomic
import java.util.*
import java.util.concurrent.Phaser
import java.util.concurrent.locks.ReentrantLock
import kotlin.Comparator
import kotlin.concurrent.thread

fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    start.distance = 0
    val multiQueue = MultiQueue(workers * 2, Comparator { o1:Node, o2:Node -> o1.distance.compareTo(o2.distance) })
    multiQueue.add(start)
    var activeNodes = 1
    val onFinish = Phaser(workers + 1)
    repeat(workers) {
        thread {
            while (true) {
                val currentNode = multiQueue.poll()
                if (currentNode == null) {
                    activeNodes--
                    if (multiQueue.isEmpty() && activeNodes <= 0) break else continue
                }
                for (edge in currentNode.outgoingEdges) {
                    while (currentNode.distance + edge.weight < edge.to.distance) {
                        val target = currentNode.distance + edge.weight
                        val distance = edge.to.distance
                        if (target < distance && edge.to.casDistance(distance, target)) {
                            multiQueue.add(edge.to)
                            activeNodes++
                            break
                        }
                    }
                }
                activeNodes--
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}

internal class MultiQueue(private val n: Int, private val comparator: Comparator<Node>) {
    private val pqs = Array(n) { PriorityQueue(comparator) }
    private val locks =  Array(n) { ReentrantLock() }
    private val size = atomic(0)

    private val rPriorityQueueLock
        get() = random.nextInt(n).let { pqs[it] to locks[it] }

    fun isEmpty() = size.value == 0

    fun add(node: Node) {
        while (true) {
            rPriorityQueueLock.let {
                if (it.second.tryLock()) {
                    it.first.add(node)
                    size.incrementAndGet()
                    it.second.unlock()
                    return
                }
            }
        }
    }

    fun poll(): Node? {
        val pair1 = rPriorityQueueLock
        val pair2 = rPriorityQueueLock
        var result: Node? = null

        if (pair1.second.tryLock()) {
            val vertex1 = pair1.first.peek()
            if (pair2.second.tryLock()) {
                val vertex2 = pair2.first.peek()
                result = when {
                    vertex1 == null -> pair2.first.poll()
                    vertex2 == null -> pair1.first.poll()
                    vertex1.distance < vertex2.distance -> pair1.first.poll()
                    else -> pair2.first.poll()
                }
                pair2.second.unlock()
            }
            pair1.second.unlock()
        }

        if (result != null) {
            size.decrementAndGet()
        }
        return result
    }

    companion object {
        private val random = Random()
    }
}
