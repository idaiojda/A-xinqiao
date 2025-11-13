package com.example.xinqiao.room.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "authorizations")
public class AuthorizationEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String userName;
    public String counselorId;
    public String counselorName;
    public String scopes; // comma-separated: consult,test,diary
    public int durationDays; // 1/3/7
    public long startTimestamp;
    public long endTimestamp;
    public String status; // "已生效" / "已过期"
}

