package airline;

import java.io.Serializable;

public class Seat implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;          // e.g. "3A"
    private final FareClass fareClass;
    private boolean booked;

    public Seat(String id, FareClass fareClass) {
        this.id = id;
        this.fareClass = fareClass;
    }

    public String getId() { return id; }
    public FareClass getFareClass() { return fareClass; }
    public boolean isBooked() { return booked; }
    void setBooked(boolean booked) { this.booked = booked; }
}
