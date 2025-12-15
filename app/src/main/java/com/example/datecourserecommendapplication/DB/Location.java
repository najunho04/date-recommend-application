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
    private int itemIndex = 0;
    private String region1; //시/도
    private String region2; //구/군
    private String region3; //동

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
        this.region1 = other.region1;
        this.region2 = other.region2;
        this.region3 = other.region3;
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
    public String getRegion1() {return region1;}
    public void setRegion1(String region1) {this.region1 = region1;}

    public String getRegion2() {return region2;}
    public void setRegion2(String region2) {this.region2 = region2;}

    public String getRegion3() {return region3;}
    public void setRegion3(String region3) {this.region3 = region3;}

    @NonNull
    @Override
    public String toString() {
        return "Location{" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", region1='" + region1 + '\'' +
                ", region2='" + region2 + '\'' +
                ", region3='" + region3 + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", placeId='" + placeId + '\'' +
                ", itemIndex=" + itemIndex +
                '}';
    }

    public void splitAddressIntoRegions() {

        if (address.isEmpty()) {
            return;
        }

        String[] parts = address.split(" ");

        // 최소 3단계 (시/도, 구/군, 동)
        region1 = parts.length > 0 ? parts[0] : "";
        region2 = parts.length > 1 ? parts[1] : "";
        region3 = parts.length > 2 ? parts[2] : "";

        setRegion1(region1);
        setRegion2(region2);
        setRegion3(region3);
    }
}
