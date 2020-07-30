package linked_list_set;

import kotlinx.atomicfu.AtomicRef;

import java.util.concurrent.locks.ReentrantLock;

public class SetImpl implements Set {
    /**
     * Returns the {@link Window}, where cur.x < x <= next.x
     */
    private Window findWindow(int x) {
        while (true) {
            Node currentNode = head;
            Node nextNode = currentNode.getFromObject();
            boolean flag = false;
            while (nextNode.x < x) {
                CommonInterface tmp = nextNode.next.getValue();
                boolean removed = tmp instanceof Removed;
                if (removed) {
                    if (!currentNode.next.compareAndSet(nextNode, ((Removed) tmp).removedNode)) {
                        flag = true;
                        break;
                    } else {
                        nextNode = ((Removed) tmp).removedNode;
                    }
                } else {
                    currentNode = nextNode;
                    nextNode = (Node) tmp;
                }
            }
            if (flag) {
                continue;
            }
            CommonInterface CI = nextNode.next.getValue();
            if (!(CI instanceof Removed)) {
                return new Window(currentNode, nextNode);
            }
            currentNode.next.compareAndSet(nextNode, ((Removed) CI).removedNode);
        }
    }

    @Override
    public boolean add(int x) {
        while (true) {
            Window window = findWindow(x);
            if (window.second.x == x) {
                return false;
            } else {
                Node node = new Node(x, window.second);
                if (window.first.next.compareAndSet(window.second, node)) {
                    return true;
                }
            }
        }
    }

    @Override
    public boolean remove(int x) {
        while (true) {
            Window window = findWindow(x);
            if (window.second.x != x) {
                return false;
            } else {
                CommonInterface curNextNode = window.second.next.getValue();
                if (curNextNode instanceof Removed) {
                    return false;
                }
                Removed newNextNode = new Removed((Node) curNextNode);
                if (window.second.next.compareAndSet(curNextNode, newNextNode)) {
                    window.first.next.compareAndSet(window.second, curNextNode);
                    return true;
                }
            }
        }
    }

    @Override
    public boolean contains(int x) {
        return findWindow(x).second.x == x;
    }

    private final Node head = new Node(Integer.MIN_VALUE, new Node(Integer.MAX_VALUE, null));

    private interface CommonInterface {
    }

    private class Node implements CommonInterface {
        AtomicRef<CommonInterface> next;
        int x;

        Node(int x, CommonInterface next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }

        Node getFromObject() {
            if (next == null) {
                return null;
            }
            CommonInterface CI = next.getValue();
            if (CI instanceof Removed) {
                return ((Removed) CI).removedNode;
            } else {
                return (Node) CI;
            }
        }
    }

    private class Removed implements CommonInterface {
        final Node removedNode;

        Removed(Node node) {
            this.removedNode = node;
        }
    }

    private class Window {
        Node first;
        Node second;

        Window(Node first, Node second) {
            this.first = first;
            this.second = second;
        }
    }
}