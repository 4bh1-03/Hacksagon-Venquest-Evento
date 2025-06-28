package com.example.evento;

public class VendorItem2 {
    private String name;
    private String price;

    public VendorItem2() {
        // Required empty constructor for Firebase
    }

    public VendorItem2(String name, String price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public String getPrice() {
        return price;
    }
}
