package app.jobs;

import app.scheduler.ScheduleEveryFiveMins;

import java.util.Date;

public class MyJob {

    @ScheduleEveryFiveMins
    public void myJob() {
        System.out.println("The current time is: " + new Date());
    }
}