package org.jenkins.tools.test.util;

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

    /**
     * Locate and loads any services of the specified type, ordering the returned list according to the services {@link HookOrder} annotation.
     * @param <T> the class of the service type
     * @param cls the interface or abstract class representing the service
     * @param externalJars list of extra jars to be searched for services in addition to the current classloader
     * @return an List of discovered services of type {@code T} sorted by {@link HookOrderComparator}
     */
    public static <T> List<T> loadServices(Class<T> cls, Set<File> externalJars) {
        ClassLoader cl = createExternalClassLoader(externalJars);
        ServiceLoader<T> sl = ServiceLoader.load(cls, cl);
        ArrayList<T> serviceList = new ArrayList<>();
        for (T service : sl) {
            serviceList.add(service);
        }
        serviceList.sort(new HookOrderComparator());
        return serviceList;
    }

    private static ClassLoader createExternalClassLoader(Set<File> externalJars) {
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
