package com.bagas.stokikan.model;

public class OptionItem {
    private final int id;
    private final String label;

    public OptionItem(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public int getId() { return id; }
    public String getLabel() { return label; }

    @Override
    public String toString() { return label; }
}
