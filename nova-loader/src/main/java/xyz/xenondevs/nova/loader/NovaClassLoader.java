package xyz.xenondevs.nova.loader;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class NovaClassLoader extends URLClassLoader {
    
    private final Set<String> injectedClasses = new HashSet<>();
    
    public NovaClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }
    
    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return loadClass(name, resolve, true);
    }
    
    public Class<?> loadClass(String name, boolean resolve, boolean checkParents) throws ClassNotFoundException {
        Class<?> c = null;
        
        // workaround library conflict for kyori-adventure on paper servers (fixme)
        if (name.startsWith("net.kyori.adventure")) {
            try {
                c = getParent().loadClass(name);
            } catch(ClassNotFoundException e) {
                // ignored
            }
        }
        
        if (c == null) {
            synchronized (getClassLoadingLock(name)) {
                // check in jvm
                c = findLoadedClass(name);
        
                // check Nova classes and libraries before parent to prevent it from using old patch classes
                // (which stay in memory after reloading because patched code references them)
                if (c == null && !injectedClasses.contains(name) ) {
                    c = findClassOrNull(name);
                }
            }
        }
        
        // check parent loader
        if (c == null && checkParents) {
            c = getParent().loadClass(name);
        }
        
        if (c == null) {
            throw new ClassNotFoundException(name);
        }
        
        if (resolve) {
            synchronized (getClassLoadingLock(name)) {
                resolveClass(c);
            }
        }
        
        return c;
    }
    
    private Class<?> findClassOrNull(String name) {
        try {
            return findClass(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
    
    public void addInjectedClass(String name) {
        injectedClasses.add(name);
    }
    
    public void addInjectedClasses(Collection<String> names) {
        injectedClasses.addAll(names);
    }
    
}
