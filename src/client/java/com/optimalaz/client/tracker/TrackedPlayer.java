package com.optimalaz.client.tracker;

public class TrackedPlayer {
    private final String name;
    private double x, y, z;
    private String dimension = "overworld";
    private boolean online = false;
    private boolean visible = false;

    public TrackedPlayer(String name) { this.name = name; }
    public String getName() { return name; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public String getDimension() { return dimension; }
    public boolean isOnline() { return online; }
    public boolean isVisible() { return visible; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setZ(double z) { this.z = z; }
    public void setDimension(String dimension) { this.dimension = dimension; }
    public void setOnline(boolean online) { this.online = online; }
    public void setVisible(boolean visible) { this.visible = visible; }
}
