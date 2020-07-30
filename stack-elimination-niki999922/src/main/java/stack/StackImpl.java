package stack;

import kotlinx.atomicfu.AtomicRef;

import java.util.ArrayList;

public class StackImpl implements Stack {
    private static final int LIST_SIZE = 40;
    private static final int STEPS = 23;
    private static final int MAGIC_CONST = 31;

    private ArrayList<AtomicRef<Node>> list;

    private static class Node {
        final AtomicRef<Node> next;
        final int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }
    }

    // head pointer
    private AtomicRef<Node> head = new AtomicRef<>(null);
    private static final Node emptyElement = new Node(Integer.MIN_VALUE, null);


    public StackImpl() {
        list = new ArrayList<>();
        for (int i = 0; i < LIST_SIZE; i++) {
            list.add(new AtomicRef<>(emptyElement));
        }
    }

    @Override
    public void push(int x) {
        Node node = new Node(x, null);
        int pos = Math.abs((int) ((MAGIC_CONST * Math.random()) % LIST_SIZE));
        int shift = 0;
        for (int i = 0; i < STEPS; i++) {
            if (list.get((pos + i) % LIST_SIZE).compareAndSet(emptyElement, node)) {
                shift = i;
                break;
            }
        }
        if (list.get((pos + shift) % LIST_SIZE).compareAndSet(node, emptyElement)) {
            while (true) {
                Node curHead = head.getValue();
                Node newHead = new Node(x, curHead);

                if (head.compareAndSet(curHead, newHead)) {
                    return;
                }
            }
        }
    }

    @Override
    public int pop() {
        int pos = Math.abs((int) ((MAGIC_CONST * Math.random()) % LIST_SIZE));
        for (int i = 0; i < STEPS; i++) {
            if (!list.get((pos + i) % LIST_SIZE).compareAndSet(emptyElement, emptyElement)) {
                Node node = list.get((pos + i) % LIST_SIZE).getValue();
                if (list.get((pos + i) % LIST_SIZE).compareAndSet(node, emptyElement) && node != emptyElement) {
                    return node.x;
                }
            }
        }
        while (true) {
            Node curHead = head.getValue();
            if (curHead == null) {
                return Integer.MIN_VALUE;
            } else {
                if (head.compareAndSet(curHead, curHead.next.getValue())) {
                    return curHead.x;
                }
            }
        }
    }
}
