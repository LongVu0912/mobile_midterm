package com.example.mobile_midterm;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FirebaseDataClass {
    private String imageUrl;
    private String imageName;

    public FirebaseDataClass() {
    }

    public FirebaseDataClass(String imageUrl, String imageName) {
        this.imageUrl = imageUrl;
        this.imageName = imageName;
    }
}
