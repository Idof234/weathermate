package com.weathermate.entity;

public enum Units {
    CELSIUS("°C"),
    FAHRENHEIT("°F");

    private final String symbol;

    Units(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    /** All temperatures are stored in Celsius; this converts them for display. */
    public double convert(double celsius) {
        return this == CELSIUS ? celsius : celsius * 9 / 5 + 32;
    }
}
