package faaqueue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static faaqueue.FAAQueue.Node.NODE_SIZE;

public class FAAQueue<T> implements Queue<T> {
    private static final Object DONE = new Object();

    private AtomicReference<Node> head;
    private AtomicReference<Node> tail;

    public FAAQueue() {
        Node dummy = new Node();
        head = new AtomicReference<>(dummy);
        tail = new AtomicReference<>(dummy);
    }

    @Override
    public void enqueue(T x) {
        while (true) {
            Node t = tail.get();
            Node tNext = t.next.get();
            if (tNext != null) {
                tail.compareAndSet(t, tNext);
                continue;
            }
            int enqIdx = t.enqIdx.getAndIncrement();
            if (enqIdx >= NODE_SIZE) {
                Node newT = new Node(x);
                t = tail.get();
                Node nextPtr = t.next.get();
                if (t == tail.get()) {
                    if (nextPtr == null) {
                        if (tail.get().next.compareAndSet(null, newT)) break;
                    } else {
                        tail.compareAndSet(t, nextPtr);
                    }
                }
            } else {
                if (t.data.compareAndSet(enqIdx, null, x)) return;
            }
        }
    }

    @Override
    public T dequeue() {
        while (true) {
            Node h = head.get();
            if (h.isEmpty()) {
                Node headNext = h.next.get();
                if (headNext == null) return null;
                head.compareAndSet(h, headNext);
            } else {
                int deqIdx = h.deqIdx.getAndIncrement();
                if (deqIdx >= NODE_SIZE) continue;
                Object res = h.data.getAndSet(deqIdx, DONE);
                if (res == null) {
                    continue;
                }
                return (T) res;
            }
        }

    }

    static class Node {
        static final int NODE_SIZE = 2;

        private AtomicReference<Node> next = new AtomicReference<>();
        private AtomicInteger enqIdx = new AtomicInteger(1);
        private AtomicInteger deqIdx = new AtomicInteger();
        private final AtomicReferenceArray<Object> data = new AtomicReferenceArray<>(new Object[NODE_SIZE]);

        Node() {
        }

        Node(Object x) {
            data.set(0, x);
        }

        private boolean isEmpty() {
            return deqIdx.get() >= enqIdx.get() ||
                    deqIdx.get() >= NODE_SIZE;
        }
    }
}