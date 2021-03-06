/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package net.sourceforge.pmd.util;

import java.util.Iterator;

/**
 * A singleton iterator that never has anything.
 * 
 * @author Brian Remedios
 *
 * @param <T>
 */
public final class EmptyIterator<T extends Object> implements Iterator<T> {

    @SuppressWarnings("rawtypes")
    public static final Iterator INSTANCE = new EmptyIterator();

    @SuppressWarnings("unchecked")
    public static <T extends Object> Iterator<T> instance() {
        return INSTANCE;
    }

    private EmptyIterator() {
    }

    public boolean hasNext() {
        return false;
    }

    public T next() {
        return null;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}