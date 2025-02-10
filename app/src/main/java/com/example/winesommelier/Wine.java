package com.example.winesommelier;

public class Wine {
    public String name;
    public String winery;
    public String category;
    public String designation;
    public String varietal;
    public String appellation;
    public String alcohol;
    public String price;
    public String rating;
    public String reviewer;
    public String review;
    public String globalReviewCount;

    public Wine(String name, String winery, String category, String designation,
                String varietal, String appellation, String alcohol, String price,
                String rating, String reviewer, String review, String globalReviewCount) {
        this.name = name;
        this.winery = winery;
        this.category = category;
        this.designation = designation;
        this.varietal = varietal;
        this.appellation = appellation;
        this.alcohol = alcohol;
        this.price = price;
        this.rating = rating;
        this.reviewer = reviewer;
        this.review = review;
        this.globalReviewCount = globalReviewCount;
    }
}
