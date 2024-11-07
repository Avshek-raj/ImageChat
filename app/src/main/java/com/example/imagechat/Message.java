package com.example.imagechat;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Message {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "sender")
    public String sender;

//    @ColumnInfo(name = "imageUri")
//    public String imageUri;

    @ColumnInfo(name = "resourceName")
    public String resourceName;

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    public byte[] imageBytes;
}

