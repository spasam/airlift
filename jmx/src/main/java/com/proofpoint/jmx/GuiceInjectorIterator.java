package com.proofpoint.jmx;

import com.google.common.collect.Sets;
import com.google.inject.Injector;
import com.google.inject.Key;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Set;

class GuiceInjectorIterator implements Iterator<Class<?>>, Iterable<Class<?>>
{
    private final Set<Key<?>> visited = Sets.newHashSet();
    private final Iterator<Key<?>> keyIterator;
    private final Injector injector;

    private boolean needsReset = true;
    private Class<?> currentClass = null;
    private GuiceDependencyIterator currentDependencyIterator = null;

    /**
     * @param injector the injector to iterate over
     */
    public GuiceInjectorIterator(Injector injector)
    {
        this.injector = injector;
        keyIterator = injector.getBindings().keySet().iterator();
    }

    @Override
    public boolean hasNext()
    {
        checkReset();
        return (currentClass != null);
    }

    @Override
    public Class<?> next()
    {
        needsReset = true;
        return currentClass;
    }

    @Override
    public Iterator<Class<?>> iterator()
    {
        return new GuiceInjectorIterator(injector);
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    private void checkReset()
    {
        if (!needsReset) {
            return;
        }
        needsReset = false;

        currentClass = null;
        if (currentDependencyIterator != null) {
            if (currentDependencyIterator.hasNext()) {
                currentClass = currentDependencyIterator.next();
            }
            else {
                currentDependencyIterator = null;
            }
        }

        while ((currentClass == null) && keyIterator.hasNext()) {
            Key<?> key = keyIterator.next();
            currentClass = parseKey(visited, key);
            if (currentClass == null) {
                continue;
            }

            currentDependencyIterator = new GuiceDependencyIterator(key.getTypeLiteral());
            currentDependencyIterator = currentDependencyIterator.substituteVisitedSet(visited);
        }
    }

    static Class<?> parseKey(Set<Key<?>> visited, Key<?> key)
    {
        if (visited.contains(key)) {
            return null;
        }
        visited.add(key);

        Class<?> clazz;
        Type type = key.getTypeLiteral().getType();
        if (type instanceof GenericArrayType) {
            type = ((GenericArrayType) type).getGenericComponentType();
        }
        if (type instanceof Class) {
            clazz = (Class<?>) type;
        }
        else {
            clazz = key.getTypeLiteral().getRawType();
        }

        return clazz;
    }
}
