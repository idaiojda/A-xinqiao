package com.example.xinqiaobackend.model;

public class CreateGroupRequest {
    private String name;
    private String description;
    private String schedule;
    private int capacity;

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getSchedule() { return schedule; }
    public int getCapacity() { return capacity; }
}

