package msqueue;

import kotlinx.atomicfu.AtomicRef;

public class MSQueue implements Queue {
    private AtomicRef<Node> head, tail;

    MSQueue() {
        Node nodeTmp = new Node(0, null);
        head = new AtomicRef<>(nodeTmp);
        tail = new AtomicRef<>(nodeTmp);
    }

    @Override
    public void enqueue(int x) {
        Node newTail = new Node(x, null);
        while (true) {
//            System.out.println("enqueue");
            Node curTail = tail.getValue();
            if (curTail.next.compareAndSet(null, newTail)) {
                tail.compareAndSet(curTail, newTail);
                return;
            } else {
                tail.compareAndSet(curTail, curTail.next.getValue());
            }
        }
    }

    @Override
    public int dequeue() {
        while (true) {
//            System.out.println("dequeue");
            Node curHead = head.getValue();
            if (tail.compareAndSet(curHead, curHead)) {
                return Integer.MIN_VALUE;
            }
            Node headNext = curHead.next.getValue();
            if (head.compareAndSet(curHead, headNext)) {
                return headNext.x;
            }
        }
    }

    @Override
    public int peek() {
        while (true) {
//            System.out.println("peek");
            Pair pair = new Pair(head.getValue(), head.getValue().next.getValue());
            if (tail.compareAndSet(pair.first, pair.first)) {
                return Integer.MIN_VALUE;
            }
            if (pair.second != null && head.compareAndSet(pair.first, pair.first)) {
                return pair.second.x;
            }
        }
    }

    private class Node {
        final AtomicRef<Node> next;
        final int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }
    }

    private class Pair {
        final Node first;
        final Node second;

        private Pair(Node first, Node second) {
            this.first = first;
            this.second = second;
        }
    }
}