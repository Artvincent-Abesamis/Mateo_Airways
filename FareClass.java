package airline;

/** Fare classes, each tied to a price multiplier applied to the flight's base price. */
public enum FareClass {
    ECONOMY("Economy", 1.0),
    BUSINESS("Business", 2.0),
    FIRST("First Class", 3.5);

    private final String label;
    private final double multiplier;

    FareClass(String label, double multiplier) {
        this.label = label;
        this.multiplier = multiplier;
    }

    public String getLabel() { return label; }
    public double getMultiplier() { return multiplier; }
}
