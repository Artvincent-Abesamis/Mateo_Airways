package airline;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

/** Holds all system data and persists it to a file with Java serialization. */
@SuppressWarnings("serial")
public class DataStore implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String FILE = "airline-data.ser";

    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<String, Flight> flights = new LinkedHashMap<>();
    private final Map<String, Reservation> reservations = new LinkedHashMap<>();
    private int nextFlightNo = 1;

    public Map<String, User> getUsers() { return users; }
    public Map<String, Flight> getFlights() { return flights; }
    public Map<String, Reservation> getReservations() { return reservations; }
    public String nextFlightId() { return String.format("PR%03d", nextFlightNo++); }

    public static DataStore load() {
        File f = new File(FILE);
        if (f.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
                return (DataStore) in.readObject();
            } catch (Exception e) {
                System.out.println("Could not read saved data (" + e.getMessage() + "). Starting fresh.");
            }
        }
        DataStore ds = new DataStore();
        ds.seed();
        return ds;
    }

    public void save() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE))) {
            out.writeObject(this);
        } catch (IOException e) {
            System.out.println("Warning: could not save data: " + e.getMessage());
        }
    }

    private void seed() {
        users.put("admin", new Admin("admin", "admin123", "System Administrator", "admin@skyline.example"));
        users.put("agent", new BookingAgent("agent", "agent123", "Ana Agent", "agent@skyline.example"));
        users.put("juan", new Passenger("juan", "juan123", "Juan Dela Cruz", "juan@example.com"));

        LocalDateTime base = LocalDateTime.now().plusDays(3).withHour(6).withMinute(30).withSecond(0).withNano(0);
        addSeedFlight("Manila", "Cebu", base, 3200);
        addSeedFlight("Manila", "Davao", base.plusHours(5), 4100);
        addSeedFlight("Cebu", "Manila", base.plusDays(1), 3100);
        addSeedFlight("Manila", "Tokyo", base.plusDays(2).plusHours(3), 11500);
    }

    private void addSeedFlight(String o, String d, LocalDateTime t, double price) {
        Flight f = new Flight(nextFlightId(), o, d, t, price);
        flights.put(f.getId(), f);
    }
}
