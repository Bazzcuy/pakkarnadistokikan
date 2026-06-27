package com.bagas.stokikan.model;

public class OptionItem {
    public final int id;
    public final String label;

    public OptionItem(int id, String label) {
        this.id = id;
        this.label = label;
    }

    @Override
    public String toString() { return label; }
}
