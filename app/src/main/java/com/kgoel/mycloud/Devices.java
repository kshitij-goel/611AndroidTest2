package com.kgoel.mycloud;

public class Devices {
    private final int name;
    private final int imageResource;

    public Devices(int name, int imageResource) {
        this.name = name;
        this.imageResource = imageResource;
    }

    public int getName(){ return name; }

    public int getImageResource() { return imageResource; }
}
