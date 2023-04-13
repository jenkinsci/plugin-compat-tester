package org.jenkins.tools.test.util;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import org.jenkins.tools.test.model.hook.HookOrder;
import org.jenkins.tools.test.model.hook.HookOrderComparator;

/**
 * Helper utilities to aid working with {@link ServiceLoader services}.
 */
public class ServiceHelper {

    private final ClassLoader classLoader;

    /**
     * Creates a new {@code ServiceHelper} instance that will load services from a {@link ClassLoader} constructed from
     * the {@link ServiceHelper}'s classloader with the addition of classes in {@code externalJars}.
     *
     * @param externalJars a possibly empty set of extra files to add to the {@link ClassLoader}
     */
    public ServiceHelper(@NonNull Set<File> externalJars) {
        classLoader = createExternalClassLoader(externalJars);
    }

    /**
     * Locate and loads any services of the specified type, ordering the returned list according to the services {@link HookOrder} annotation.
     * @param <T> the class of the service type
     * @param cls the interface or abstract class representing the service
     * @return an List of discovered services of type {@code T} sorted by {@link HookOrderComparator}
     */
    public <T> List<T> loadServices(Class<T> cls) {
        ServiceLoader<T> sl = ServiceLoader.load(cls, classLoader);
        ArrayList<T> serviceList = new ArrayList<>();
        for (T service : sl) {
            serviceList.add(service);
        }
        serviceList.sort(new HookOrderComparator());
        return serviceList;
    }

    private ClassLoader createExternalClassLoader(Set<File> externalJars) {
        if (externalJars.isEmpty()) {
            ServiceHelper.class.getClassLoader();
        }
        List<URL> urls = new ArrayList<>();
        for (File jar : externalJars) {
            try {
                urls.add(jar.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new UncheckedIOException("Failed to setup ClassLoader with external JAR", e);
            }
        }
        return new URLClassLoader(urls.toArray(new URL[0]), ServiceHelper.class.getClassLoader());
    }
}
