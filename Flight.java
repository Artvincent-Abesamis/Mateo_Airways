package airline;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Flight implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final int ROWS = 12;
    public static final char[] LETTERS = {'A', 'B', 'C', 'D'};

    private final String id;
    private String origin;
    private String destination;
    private LocalDateTime departure;
    private double basePrice;
    private FlightStatus status = FlightStatus.SCHEDULED;
    private final Map<String, Seat> seats = new LinkedHashMap<>();

    public Flight(String id, String origin, String destination, LocalDateTime departure, double basePrice) {
        this.id = id;
        this.origin = origin;
        this.destination = destination;
        this.departure = departure;
        this.basePrice = basePrice;
        buildSeats();
    }

    /** Rows 1-2 First Class, 3-5 Business, 6-12 Economy. */
    private void buildSeats() {
        for (int row = 1; row <= ROWS; row++) {
            FareClass fc = row <= 2 ? FareClass.FIRST : row <= 5 ? FareClass.BUSINESS : FareClass.ECONOMY;
            for (char c : LETTERS) {
                String sid = row + String.valueOf(c);
                seats.put(sid, new Seat(sid, fc));
            }
        }
    }

    public String getId() { return id; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public LocalDateTime getDeparture() { return departure; }
    public double getBasePrice() { return basePrice; }
    public FlightStatus getStatus() { return status; }

    public void setOrigin(String origin) { this.origin = origin; }
    public void setDestination(String destination) { this.destination = destination; }
    public void setDeparture(LocalDateTime departure) { this.departure = departure; }
    public void setBasePrice(double basePrice) { this.basePrice = basePrice; }
    public void setStatus(FlightStatus status) { this.status = status; }

    public Seat getSeat(String seatId) { return seats.get(seatId == null ? "" : seatId.toUpperCase()); }
    public Collection<Seat> getSeats() { return Collections.unmodifiableCollection(seats.values()); }
    public int capacity() { return seats.size(); }
    public int bookedCount() {
        int n = 0;
        for (Seat s : seats.values()) if (s.isBooked()) n++;
        return n;
    }
    public double priceFor(FareClass fc) { return basePrice * fc.getMultiplier(); }

    @Override
    public String toString() {
        return String.format("%-7s %-16s -> %-16s %s  %-9s  Economy: PHP %,.2f  Seats left: %d",
                id, origin, destination, departure.format(FMT), status,
                priceFor(FareClass.ECONOMY), capacity() - bookedCount());
    }
}
