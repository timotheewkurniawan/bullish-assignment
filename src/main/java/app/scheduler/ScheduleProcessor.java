package app.scheduler;

import app.utils.ClasspathScanner;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduleProcessor {

    private final ScheduledExecutorService executor;

    public ScheduleProcessor() {
        this.executor = Executors.newScheduledThreadPool(
                Runtime.getRuntime().availableProcessors()
        );
    }

    public void scanAndRegister(String ... basePackages) {
        for (String basePackage : basePackages){
            List<Class<?>> classes = ClasspathScanner.findClasses(basePackage);

            for (Class<?> clazz : classes) {
                boolean hasAnnotatedMethod = false;

                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(ScheduleEveryFiveMins.class)) {
                        hasAnnotatedMethod = true;
                        break;
                    }
                }

                if (hasAnnotatedMethod) {
                    try {
                        Object instance = clazz.getDeclaredConstructor().newInstance();
                        register(instance);
                    } catch (NoSuchMethodException e) {
                        System.err.println("Skipping " + clazz.getName()
                                + ": no public no-arg constructor");
                    } catch (Exception e) {
                        System.err.println("Failed to instantiate " + clazz.getName());
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    public void register(Object target) {
        for (Method method : target.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(ScheduleEveryFiveMins.class)) {
                validateMethod(method);
                method.setAccessible(true);

                executor.scheduleAtFixedRate(() -> {
                    try {
                        method.invoke(target);
                    } catch (Exception e) {
                        System.err.println("Scheduled method [" + method.getName()
                                + "] threw an exception:");
                        e.printStackTrace();
                    }
                }, 0, 5, TimeUnit.MINUTES);

                System.out.println("Scheduled: " + target.getClass().getSimpleName()
                        + "." + method.getName() + "() every 5 minutes");
            }
        }
    }

    private void validateMethod(Method method) {
        if (method.getParameterCount() != 0) {
            throw new IllegalArgumentException(
                    "@ScheduleEveryFiveMins can only be applied to no-arg methods. "
                            + "Offending method: " + method.getName());
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                System.err.println("Scheduler did not terminate gracefully — forced shutdown");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}