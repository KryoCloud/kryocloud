package eu.kryocloud.common.concurrency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class BoundedPriorityQueue<E> implements BlockingQueue<E> {

    private final int capacity;
    private final Comparator<SequencedEntry<E>> entryComparator;
    private final PriorityQueue<SequencedEntry<E>> delegate;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();
    private final AtomicLong sequencer = new AtomicLong(0);

    public BoundedPriorityQueue(int capacity, Comparator<? super E> comparator) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");

        this.capacity = capacity;

        this.entryComparator = (a, b) -> {
            int cmp = comparator.compare(a.element, b.element);
            if (cmp != 0) return cmp;
            return Long.compare(a.sequence, b.sequence);
        };

        this.delegate = new PriorityQueue<>(Math.min(capacity, 256), entryComparator);
    }

    @Override
    public boolean offer(E e) {
        Objects.requireNonNull(e);
        lock.lock();
        try {
            if (delegate.size() >= capacity) return false;
            delegate.offer(new SequencedEntry<>(e, sequencer.getAndIncrement()));
            notEmpty.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(E e) throws InterruptedException {
        Objects.requireNonNull(e);
        lock.lockInterruptibly();
        try {
            while (delegate.size() >= capacity) {
                notFull.await();
            }
            delegate.offer(new SequencedEntry<>(e, sequencer.getAndIncrement()));
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        Objects.requireNonNull(e);
        long nanos = unit.toNanos(timeout);
        lock.lockInterruptibly();
        try {
            while (delegate.size() >= capacity) {
                if (nanos <= 0L) return false;
                nanos = notFull.awaitNanos(nanos);
            }
            delegate.offer(new SequencedEntry<>(e, sequencer.getAndIncrement()));
            notEmpty.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (delegate.isEmpty()) {
                notEmpty.await();
            }
            E item = delegate.poll().element;
            notFull.signal();
            return item;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        lock.lockInterruptibly();
        try {
            while (delegate.isEmpty()) {
                if (nanos <= 0L) return null;
                nanos = notEmpty.awaitNanos(nanos);
            }
            E item = delegate.poll().element;
            notFull.signal();
            return item;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E poll() {
        lock.lock();
        try {
            SequencedEntry<E> entry = delegate.poll();
            if (entry == null) return null;
            notFull.signal();
            return entry.element;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E peek() {
        lock.lock();
        try {
            SequencedEntry<E> entry = delegate.peek();
            return entry != null ? entry.element : null;
        } finally {
            lock.unlock();
        }
    }

    public E pollHead() {
        return poll();
    }

    @Override
    public int size() {
        lock.lock();
        try {
            return delegate.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        lock.lock();
        try {
            return delegate.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int remainingCapacity() {
        lock.lock();
        try {
            return capacity - delegate.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        lock.lock();
        try {
            return containsElement(o);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean remove(Object o) {
        lock.lock();
        try {
            Iterator<SequencedEntry<E>> it = delegate.iterator();
            while (it.hasNext()) {
                if (it.next().element.equals(o)) {
                    it.remove();
                    notFull.signal();
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        lock.lock();
        try {
            for (Object o : c) {
                if (!containsElement(o)) return false;
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    private boolean containsElement(Object o) {
        for (SequencedEntry<E> entry : delegate) {
            if (entry.element.equals(o)) return true;
        }
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        Objects.requireNonNull(c, "collection");
        lock.lock();
        try {
            boolean changed = false;
            for (E e : c) {
                Objects.requireNonNull(e);
                if (delegate.size() >= capacity) break;
                delegate.offer(new SequencedEntry<>(e, sequencer.getAndIncrement()));
                changed = true;
            }
            if (changed) notEmpty.signalAll();
            return changed;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c, "collection");
        lock.lock();
        try {
            boolean changed = delegate.removeIf(entry -> c.contains(entry.element));
            if (changed) notFull.signalAll();
            return changed;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c, "collection");
        lock.lock();
        try {
            boolean changed = delegate.removeIf(entry -> !c.contains(entry.element));
            if (changed) notFull.signalAll();
            return changed;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        lock.lock();
        try {
            delegate.clear();
            notFull.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        Objects.requireNonNull(c, "target collection must not be null");
        if (c == this) throw new IllegalArgumentException("cannot drain to self");
        lock.lock();
        try {
            int count = 0;
            while (count < maxElements) {
                SequencedEntry<E> entry = delegate.poll();
                if (entry == null) break;
                c.add(entry.element);
                count++;
            }
            if (count > 0) notFull.signalAll();
            return count;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterator<E> iterator() {
        lock.lock();
        try {
            List<SequencedEntry<E>> snapshot = new ArrayList<>(delegate);
            snapshot.sort(entryComparator);
            return snapshot.stream()
                    .map(entry -> entry.element)
                    .toList()
                    .iterator();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Object[] toArray() {
        lock.lock();
        try {
            List<SequencedEntry<E>> snapshot = new ArrayList<>(delegate);
            snapshot.sort(entryComparator);
            return snapshot.stream()
                    .map(entry -> entry.element)
                    .toArray();
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        lock.lock();
        try {
            List<SequencedEntry<E>> snapshot = new ArrayList<>(delegate);
            snapshot.sort(entryComparator);
            Object[] elements = snapshot.stream()
                    .map(entry -> entry.element)
                    .toArray();
            if (a.length < elements.length) {
                return (T[]) java.util.Arrays.copyOf(elements, elements.length, a.getClass());
            }
            System.arraycopy(elements, 0, a, 0, elements.length);
            if (a.length > elements.length) a[elements.length] = null;
            return a;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean add(E e) {
        if (!offer(e)) throw new IllegalStateException("Queue full (capacity=" + capacity + ")");
        return true;
    }

    @Override
    public E remove() {
        E item = poll();
        if (item == null) throw new NoSuchElementException();
        return item;
    }

    @Override
    public E element() {
        E item = peek();
        if (item == null) throw new NoSuchElementException();
        return item;
    }

    private record SequencedEntry<E>(E element, long sequence) {}
}
