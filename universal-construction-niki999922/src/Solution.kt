class Solution : AtomicCounter {
    private val commonNode = Node(0)
    private val my = lazy { ThreadLocal.withInitial { 0 } }.value
    private val last = ThreadLocal.withInitial { commonNode }

    private class Node(val x: Int) {
        val next = Consensus<Node>()
    }

    override fun getAndAdd(x: Int): Int {
        val node = Node(x)
        var res = 0
        while (last.get() != node) {
            res = my.get()
            last.set(last.get().next.decide(node))
            my.set(my.get() + last.get().x)
        }
        return res
    }

}
