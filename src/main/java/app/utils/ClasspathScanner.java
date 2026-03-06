package app.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClasspathScanner {


    public static List<Class<?>> findClasses(String basePackage) {
        List<Class<?>> classes = new ArrayList<>();
        String path = basePackage.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try {
            Enumeration<URL> resources = classLoader.getResources(path);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String protocol = resource.getProtocol();

                if ("file".equals(protocol)) {
                    File directory = new File(resource.toURI());
                    scanDirectory(directory, basePackage, classes);
                } else if ("jar".equals(protocol)) {
                    String jarPath = resource.getPath()
                            .substring(5, resource.getPath().indexOf('!'));
                    scanJar(new File(jarPath), path, classes);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Classpath scanning failed", e);
        }

        return classes;
    }

    private static void scanDirectory(File directory, String packageName,
                                      List<Class<?>> classes) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "."
                        + file.getName().replace(".class", "");
                loadIfValid(className, classes);
            }
        }
    }

    private static void scanJar(File jarFile, String pathPrefix,
                                List<Class<?>> classes) throws IOException {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String entryName = entries.nextElement().getName();
                if (entryName.startsWith(pathPrefix)
                        && entryName.endsWith(".class")) {
                    String className = entryName
                            .replace('/', '.')
                            .replace(".class", "");
                    loadIfValid(className, classes);
                }
            }
        }
    }

    private static void loadIfValid(String className, List<Class<?>> classes) {
        try {
            classes.add(Class.forName(className));
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            // skip classes that can't be loaded
        }
    }
}