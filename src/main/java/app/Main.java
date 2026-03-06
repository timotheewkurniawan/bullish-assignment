package app;

import app.scheduler.ScheduleProcessor;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        ScheduleProcessor processor = new ScheduleProcessor();

        String basePackageName = Main.class.getPackageName();
        processor.scanAndRegister(basePackageName);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down scheduler...");
            processor.shutdown();
        }));

    }
}