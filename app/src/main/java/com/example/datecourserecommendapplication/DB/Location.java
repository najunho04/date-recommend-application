package com.example.datecourserecommendapplication.DB;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class Location {

    @SerializedName("place_name")
    private String name;
    @SerializedName("address_name")
    private String address;
    @SerializedName("y")
    private Double latitude;
    @SerializedName("x")
    private Double longitude;
    @SerializedName("id")
    private String placeId; // optional
    private int itemIndex;

    // Firebase에서 객체 직렬화할 때 필요
    public Location() {}

    public Location(String name, String address, double latitude, double longitude) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Location(Location other) {
        this.name = other.name;
        this.address = other.address;
        this.latitude = other.latitude;
        this.longitude = other.longitude;
        this.placeId = other.placeId;
        this.itemIndex = other.itemIndex;
    }

    // Getter & Setter
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }

    public int getItemIndex() { return itemIndex; }
    public void setItemIndex(int itemIndex) { this.itemIndex = itemIndex; }

    @NonNull
    @Override
    public String toString() {
        return "Location{" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", placeId='" + placeId + '\'' +
                ", itemIndex=" + itemIndex +
                '}';
    }
}
