import java.util.concurrent.atomic.AtomicReference

class Solution(val env: Environment) : Lock<Solution.Node> {
    private val tail = AtomicReference<Node?>()

    override fun lock(): Node {
        val my = Node()
        my.bool.set(true)
        val pred = tail.getAndSet(my)
        if (pred != null) {
            pred.next.value = my
            while (my.bool.value) {
                env.park()
            }
        }
        return my
    }

    override fun unlock(node: Node) {
        if (node.next.value == null) {
            if (tail.compareAndSet(node, null)) {
                return
            } else {
                while (node.next.value == null) {
                    //wait...
                }
            }
        }
        node.next.value!!.bool.set(false)
        env.unpark(node.next.value!!.thread)
    }

    class Node {
        val thread = Thread.currentThread() // запоминаем поток, которые создал узел
        val bool = AtomicReference<Boolean>(false)
        val next: AtomicReference<Node?> = AtomicReference()
    }
}