package com.example.evento;

public class ServiceModel {
    private String companyName;
    private String serviceType;
    private String detailText;
    private String location;
    private String imageUrl;

    public ServiceModel() { }

    public ServiceModel(String companyName, String serviceType,
                        String detailText, String location, String imageUrl) {
        this.companyName = companyName;
        this.serviceType = serviceType;
        this.detailText = detailText;
        this.location = location;
        this.imageUrl = imageUrl;
    }

    public String getCompanyName() { return companyName; }
    public String getServiceType() { return serviceType; }
    public String getDetailText() { return detailText; }
    public String getLocation() { return location; }
    public String getImageUrl() { return imageUrl; }
}
