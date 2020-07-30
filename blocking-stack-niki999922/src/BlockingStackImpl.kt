import java.util.concurrent.atomic.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Suppress("UNCHECKED_CAST")
class BlockingStackImpl<E> : BlockingStack<E> {

// ==========================
// Segment Queue Synchronizer
// ==========================

    inner class Queue<E> {
        private val head: AtomicReference<QueueNode<E>>
        private val tail: AtomicReference<QueueNode<E>>

        init {
            head = AtomicReference(QueueNode())
            tail = AtomicReference(head.get())
        }

        fun enqueue(element: E) {
            while (true) {
                val currentHead = head.get()
                if (checkHead(currentHead)) {
                    val newHead = currentHead.next.get()
                    if (head.compareAndSet(currentHead, newHead)) {
                        newHead.action!!.resume(element)
                        break
                    }
                }
            }
        }

        suspend fun dequeue(): E {
            return suspendCoroutine { continuation ->
                while (true) {
                    val currentTail = tail.get()
                    val newTail = QueueNode(continuation)

                    if (currentTail.next.compareAndSet(null, newTail)) {
                        tail.compareAndSet(currentTail, newTail)
                        break
                    }
                }
            }
        }

        private fun checkHead(curHead: QueueNode<E>) = curHead.next.get() != null && curHead != tail.get()

        inner class QueueNode<E>(
                val action: Continuation<E>? = null,
                val next: AtomicReference<QueueNode<E>> = AtomicReference<QueueNode<E>>(null)
        )
    }

    private val queue = Queue<E>()

// ==============
// Blocking Stack
// ==============

    private val head = AtomicReference<Node<E>?>(null)
    private val amount = AtomicLong()

    override fun push(element: E) {
        if (this.amount.getAndIncrement() < 0) {
            queue.enqueue(element)
            return
        }
        while (true) {
            val currentHead = head.get()
            if (currentHead?.element == SUSPENDED) {
                if (head.compareAndSet(currentHead, currentHead.next.get())) {
                    queue.enqueue(element)
                    return
                }
            } else {
                if (head.compareAndSet(currentHead, Node(element, AtomicReference(currentHead)))) {
                    return
                }
            }
        }
    }

    override suspend fun pop(): E {
        if (this.amount.getAndDecrement() <= 0) {
            return queue.dequeue()
        }
        while (true) {
            val currentHead = head.get()
            if (currentHead == null) {
                if (head.compareAndSet(currentHead, Node<E>(SUSPENDED, AtomicReference(null)))) {
                    return queue.dequeue()
                }
            } else {
                if (head.compareAndSet(currentHead, currentHead.next.get())) {
                    return currentHead.element as E
                }
            }
        }
    }
}

private class Node<E>(val element: Any?, val next: AtomicReference<Node<E>?>)

private val SUSPENDED = Any()