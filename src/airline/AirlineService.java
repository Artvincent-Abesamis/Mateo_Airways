package airline;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** Business logic + role-based access control. All rules live here, not in the UI. */
public class AirlineService {
    private static final String PNR_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final DataStore store;
    private final SecureRandom rnd = new SecureRandom();

    public AirlineService(DataStore store) { this.store = store; }

    // ------------------------------------------------------------ accounts
    public Passenger register(String username, String password, String fullName, String email) {
        if (username == null || username.isBlank() || password == null || password.length() < 4)
            throw new IllegalArgumentException("Username required and password must be at least 4 characters.");
        username = username.trim().toLowerCase();
        if (username.contains(" ")) throw new IllegalArgumentException("Username cannot contain spaces.");
        if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("Full name is required.");
        if (store.getUsers().containsKey(username))
            throw new IllegalArgumentException("Username already taken.");
        Passenger p = new Passenger(username, password, fullName.trim(), email == null ? "" : email.trim());
        store.getUsers().put(p.getUsername(), p);
        store.save();
        return p;
    }

    public User login(String username, String password) {
        User u = store.getUsers().get(username == null ? "" : username.trim().toLowerCase());
        if (u == null || !u.checkPassword(password)) throw new IllegalArgumentException("Invalid username or password.");
        if (!u.isActive()) throw new IllegalStateException("This account has been deactivated.");
        return u;
    }

    public void updateProfile(User actor, String fullName, String email, String newPassword) {
        if (fullName != null && !fullName.isBlank()) actor.setFullName(fullName.trim());
        if (email != null && !email.isBlank()) actor.setEmail(email.trim());
        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.length() < 4) throw new IllegalArgumentException("Password must be at least 4 characters.");
            actor.changePassword(newPassword);
        }
        store.save();
    }

    public List<User> listUsers(User actor) {
        require(actor, Role.ADMIN);
        return new ArrayList<>(store.getUsers().values());
    }

    public void setUserActive(User actor, String username, boolean active) {
        require(actor, Role.ADMIN);
        User u = store.getUsers().get(username.trim().toLowerCase());
        if (u == null) throw new IllegalArgumentException("User not found.");
        if (u.getUsername().equals(actor.getUsername())) throw new IllegalArgumentException("You cannot deactivate yourself.");
        u.setActive(active);
        store.save();
    }

    // ------------------------------------------------------------ flights
    public List<Flight> searchFlights(String origin, String destination, LocalDate date) {
        return store.getFlights().values().stream()
                .filter(f -> origin == null || origin.isBlank() || f.getOrigin().equalsIgnoreCase(origin.trim()))
                .filter(f -> destination == null || destination.isBlank() || f.getDestination().equalsIgnoreCase(destination.trim()))
                .filter(f -> date == null || f.getDeparture().toLocalDate().equals(date))
                .sorted(Comparator.comparing(Flight::getDeparture))
                .collect(Collectors.toList());
    }

    public Flight getFlight(String id) {
        Flight f = store.getFlights().get(id == null ? "" : id.toUpperCase());
        if (f == null) throw new IllegalArgumentException("Flight not found.");
        return f;
    }

    public Flight addFlight(User actor, String origin, String dest, LocalDateTime dep, double basePrice) {
        require(actor, Role.ADMIN);
        if (origin.isBlank() || dest.isBlank() || origin.equalsIgnoreCase(dest))
            throw new IllegalArgumentException("Origin and destination must be different and non-empty.");
        if (basePrice <= 0) throw new IllegalArgumentException("Price must be positive.");
        if (dep.isBefore(LocalDateTime.now())) throw new IllegalArgumentException("Departure must be in the future.");
        Flight f = new Flight(store.nextFlightId(), origin.trim(), dest.trim(), dep, basePrice);
        store.getFlights().put(f.getId(), f);
        store.save();
        return f;
    }

    public void updateFlight(User actor, String id, String origin, String dest, LocalDateTime dep, Double price) {
        require(actor, Role.ADMIN);
        Flight f = getFlight(id);
        String newOrigin = origin != null ? origin.trim() : f.getOrigin();
        String newDest = dest != null ? dest.trim() : f.getDestination();
        if (newOrigin.equalsIgnoreCase(newDest))
            throw new IllegalArgumentException("Origin and destination must be different.");
        if (price != null && price <= 0) throw new IllegalArgumentException("Price must be positive.");
        if (dep != null && dep.isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Departure must be in the future.");
        f.setOrigin(newOrigin);
        f.setDestination(newDest);
        if (dep != null) f.setDeparture(dep);
        if (price != null) f.setBasePrice(price);
        store.save();
    }

    public void updateStatus(User actor, String id, FlightStatus status) {
        require(actor, Role.ADMIN);
        getFlight(id).setStatus(status);
        store.save();
    }

    /** Removes a flight and cancels every reservation on it. Returns how many bookings were cancelled. */
    public int removeFlight(User actor, String id) {
        require(actor, Role.ADMIN);
        Flight f = getFlight(id);
        int n = 0;
        for (Reservation r : store.getReservations().values())
            if (r.getFlightId().equals(f.getId()) && r.isConfirmed()) { r.cancel(); n++; }
        store.getFlights().remove(f.getId());
        store.save();
        return n;
    }

    // ------------------------------------------------------------ reservations
    public Reservation book(User actor, String passengerUsername, String flightId, String seatId, PaymentMethod method) {
        require(actor, Role.PASSENGER, Role.AGENT);
        User passenger = checkPassengerAccess(actor, passengerUsername);
        Flight f = getFlight(flightId);
        ensureBookable(f);
        Seat seat = f.getSeat(seatId);
        if (seat == null) throw new IllegalArgumentException("Seat does not exist.");
        if (seat.isBooked()) throw new IllegalStateException("Seat " + seat.getId() + " is already taken.");

        double price = f.priceFor(seat.getFareClass());
        String txn = processPayment(method, price);   // simulated gateway
        seat.setBooked(true);
        Reservation r = new Reservation(newPnr(), passenger.getUsername(), actor.getUsername(), f.getId(),
                seat.getId(), seat.getFareClass(), price, method, txn);
        store.getReservations().put(r.getPnr(), r);
        store.save();
        return r;
    }

    /** Change seat (and therefore possibly fare class). Returns the price difference (positive = extra charge). */
    public double changeSeat(User actor, String pnr, String newSeatId) {
        require(actor, Role.PASSENGER, Role.AGENT);
        Reservation r = getOwnedReservation(actor, pnr);
        if (!r.isConfirmed()) throw new IllegalStateException("Reservation is cancelled.");
        Flight f = getFlight(r.getFlightId());
        ensureBookable(f);
        Seat target = f.getSeat(newSeatId);
        if (target == null) throw new IllegalArgumentException("Seat does not exist.");
        if (target.getId().equals(r.getSeatId())) throw new IllegalStateException("You are already in seat " + target.getId() + ".");
        if (target.isBooked()) throw new IllegalStateException("Seat " + target.getId() + " is already taken.");

        Seat old = f.getSeat(r.getSeatId());
        double newPrice = f.priceFor(target.getFareClass());
        double diff = newPrice - r.getPrice();
        if (old != null) old.setBooked(false);
        target.setBooked(true);
        r.reassign(target.getId(), target.getFareClass(), newPrice);
        store.save();
        return diff;
    }

    public void cancel(User actor, String pnr) {
        require(actor, Role.PASSENGER, Role.AGENT);
        Reservation r = getOwnedReservation(actor, pnr);
        if (!r.isConfirmed()) throw new IllegalStateException("Reservation is already cancelled.");
        Flight f = store.getFlights().get(r.getFlightId());
        if (f != null && f.getSeat(r.getSeatId()) != null) f.getSeat(r.getSeatId()).setBooked(false); // free the seat
        r.cancel();
        store.save();
    }

    public List<Reservation> reservationsFor(User actor, String passengerUsername) {
        require(actor, Role.PASSENGER, Role.AGENT);
        User p = checkPassengerAccess(actor, passengerUsername);
        return store.getReservations().values().stream()
                .filter(r -> r.getPassengerUsername().equals(p.getUsername()))
                .collect(Collectors.toList());
    }

    public List<Reservation> allReservations(User actor) {
        require(actor, Role.ADMIN);
        return new ArrayList<>(store.getReservations().values());
    }

    public String ticket(Reservation r) {
        Flight f = store.getFlights().get(r.getFlightId());
        if (f == null) return "Flight " + r.getFlightId() + " no longer exists; this reservation (" + r.getPnr() + ") was cancelled.";
        User u = store.getUsers().get(r.getPassengerUsername());
        return r.toTicket(f, u == null ? r.getPassengerUsername() : u.getFullName());
    }

    // ------------------------------------------------------------ analytics
    public String report(User actor) {
        require(actor, Role.ADMIN);
        long confirmed = store.getReservations().values().stream().filter(Reservation::isConfirmed).count();
        long cancelled = store.getReservations().size() - confirmed;
        double revenue = store.getReservations().values().stream()
                .filter(Reservation::isConfirmed).mapToDouble(Reservation::getPrice).sum();
        StringBuilder sb = new StringBuilder();
        sb.append("=== SYSTEM REPORT ===\n");
        sb.append(String.format("Flights: %d | Users: %d%n", store.getFlights().size(), store.getUsers().size()));
        sb.append(String.format("Confirmed reservations: %d | Cancelled: %d%n", confirmed, cancelled));
        sb.append(String.format("Total revenue: PHP %,.2f%n%n", revenue));
        sb.append(String.format("%-7s %-30s %-10s %s%n", "Flight", "Route", "Seats", "Load"));
        for (Flight f : store.getFlights().values()) {
            int pct = f.capacity() == 0 ? 0 : f.bookedCount() * 100 / f.capacity();
            sb.append(String.format("%-7s %-30s %3d/%-6d %d%%%n", f.getId(),
                    f.getOrigin() + " -> " + f.getDestination(), f.bookedCount(), f.capacity(), pct));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------ helpers
    private void require(User actor, Role... allowed) {
        for (Role r : allowed) if (actor.getRole() == r) return;
        throw new SecurityException("Access denied for role " + actor.getRole() + ".");
    }

    /** Passengers may only act on themselves; agents may act on any passenger account. */
    private User checkPassengerAccess(User actor, String passengerUsername) {
        User p = store.getUsers().get(passengerUsername == null ? "" : passengerUsername.trim().toLowerCase());
        if (p == null || p.getRole() != Role.PASSENGER) throw new IllegalArgumentException("Passenger account not found.");
        if (actor.getRole() == Role.PASSENGER && !actor.getUsername().equals(p.getUsername()))
            throw new SecurityException("You can only access your own bookings.");
        return p;
    }

    private Reservation getOwnedReservation(User actor, String pnr) {
        Reservation r = store.getReservations().get(pnr == null ? "" : pnr.toUpperCase());
        if (r == null) throw new IllegalArgumentException("Reservation not found.");
        if (actor.getRole() == Role.PASSENGER && !r.getPassengerUsername().equals(actor.getUsername()))
            throw new SecurityException("You can only access your own bookings.");
        return r;
    }

    private void ensureBookable(Flight f) {
        if (f.getStatus() == FlightStatus.CANCELLED) throw new IllegalStateException("Flight " + f.getId() + " is cancelled.");
        if (f.getDeparture().isBefore(LocalDateTime.now())) throw new IllegalStateException("Flight has already departed.");
    }

    private String newPnr() {
        String pnr;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) sb.append(PNR_CHARS.charAt(rnd.nextInt(PNR_CHARS.length())));
            pnr = sb.toString();
        } while (store.getReservations().containsKey(pnr));
        return pnr;
    }

    /** Simulated payment gateway. */
    private String processPayment(PaymentMethod m, double amount) {
        if (m == null || amount <= 0) throw new IllegalArgumentException("Invalid payment.");
        return m.name().substring(0, 2) + "-" + (100000 + rnd.nextInt(900000));
    }
}
