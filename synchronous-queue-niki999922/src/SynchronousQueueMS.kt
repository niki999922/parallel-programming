import java.util.concurrent.atomic.AtomicReference
import javax.swing.text.StyledEditorKit
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class SynchronousQueueMS<E> : SynchronousQueue<E> {
    //    private val senders = ArrayList<Pair<Continuation<Unit>, E>>() // pair = continuation + element
//    private val receivers = ArrayList<Continuation<E>>()
    private var head: AtomicReference<Node>
    private var tail: AtomicReference<Node>

    init {
        val dummy = Node(null, Type.SEND)
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    override suspend fun send(element: E) {
        val offer = Node(element, Type.SEND)
        while (true) {
            val t = tail.get()
            var h = head.get()
            if (h == t || t.type == Type.SEND) {
                val n = t.next.get()
                if (t == tail.get()) {
                    if (n != null) {
                        tail.compareAndSet(t, n)
                    } else {
                        val res = suspendCoroutine<Boolean> { cont ->
                            offer.coroutineSend = (cont to element)
                            if (t.next.compareAndSet(n, offer)) {
                                tail.compareAndSet(t, offer)
                            } else {
                                cont.resume(false)
                            }
                        }
                        if (!res) {
                            continue
                        }
                        h = head.get()
                        if (offer == h.next.get()) {
                            head.compareAndSet(h, offer)
                        }
                        return
                    }
                }
            } else {
                val n = h.next.get()
                if (t != tail.get() || h != head.get() || n == null) {
                    continue
                }
                val success = n.x.compareAndSet(null, element)
                head.compareAndSet(h, n)
                if (success) {
                    n.coroutineReceive!!.resume(true)
                    return
                }
            }
        }
    }

    override suspend fun receive(): E {
        val offer = Node(null, Type.RECEIVE)
        while (true) {
            val t = tail.get()
            var h = head.get()
            if (h == t || t.type == Type.RECEIVE) {
                val n = t.next.get()
                if (t == tail.get()) {
                    if (null != n) {
                        tail.compareAndSet(t, n)
                    } else {
                        val res = suspendCoroutine<Boolean> { cont ->
                            offer.coroutineReceive = cont
                            if (t.next.compareAndSet(n, offer)) {
                                tail.compareAndSet(t, offer)
                            } else {
                                cont.resume(false)
                            }
                        }
                        if (!res){
                            continue
                        }
                        h = head.get()
                        if (offer == h.next.get()) {
                            head.compareAndSet(h, offer)
                        }
                        return offer.x.get()!!
                    }
                }
            } else {
                val n = h.next.get()
                if (t != tail.get() || h != head.get() || n == null) {
                    continue
                }
                val (s, elem) = n.coroutineSend!!
                val success = n.x.compareAndSet(elem, null)
                head.compareAndSet(h, n)
                if (success) {
                    s.resume(true)
                    return elem
                }
            }
        }
    }


    private enum class Type {
        SEND,
        RECEIVE
    }

    private inner class Node(x: E?, val type: Type) {
        val x = AtomicReference(x)
        val next = AtomicReference<Node?>(null)
        var coroutineSend: Pair<Continuation<Boolean>, E>? = null
        var coroutineReceive: Continuation<Boolean>? = null
    }
}
