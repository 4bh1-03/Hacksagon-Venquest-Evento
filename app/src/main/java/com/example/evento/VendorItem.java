package com.example.evento;

public class VendorItem {
    private String name;
    private double price;
    private String description;

    public VendorItem() {} // Required for Firebase

    public VendorItem(String name, String price, String description) {
        this.name = name;
        this.price = Double.parseDouble(price);
        this.description = description;
    }

    public VendorItem(String s, String price) {
    }



    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getDescription() { return description; }
}
