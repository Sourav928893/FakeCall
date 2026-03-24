package com.sourav.fakecall.models;

public class ScheduledCall {
    private int id;
    private String callerName;
    private long triggerTime;
    private boolean isRepeatDaily;

    public ScheduledCall(int id, String callerName, long triggerTime, boolean isRepeatDaily) {
        this.id = id;
        this.callerName = callerName;
        this.triggerTime = triggerTime;
        this.isRepeatDaily = isRepeatDaily;
    }

    public int getId() { return id; }
    public String getCallerName() { return callerName; }
    public long getTriggerTime() { return triggerTime; }
    public boolean isRepeatDaily() { return isRepeatDaily; }
}
