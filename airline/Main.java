package airline;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Console user interface. */
public class Main {
    private static final Scanner in = new Scanner(System.in);
    private static DataStore store;
    private static AirlineService svc;

    public static void main(String[] args) {
        store = DataStore.load();
        svc = new AirlineService(store);
        System.out.println("=====================================");
        System.out.println("   SKYLINE AIRLINE RESERVATION SYSTEM");
        System.out.println("=====================================");
        System.out.println("Demo logins: admin/admin123, agent/agent123, juan/juan123");
        try {
            while (true) {
                System.out.println("\n--- MAIN MENU ---\n1. Login\n2. Register (passenger)\n0. Exit");
                switch (ask("Choose")) {
                    case "1": login(); break;
                    case "2": registerFlow(); break;
                    case "0": store.save(); System.out.println("Goodbye!"); return;
                    default: System.out.println("Invalid choice.");
                }
            }
        } catch (NoSuchElementException eof) {
            store.save();
        }
    }

    // ------------------------------------------------------------ auth
    private static void login() {
        try {
            User u = svc.login(ask("Username"), ask("Password"));
            System.out.println("\nWelcome, " + u.getFullName() + " [" + u.getRole() + "]");
            switch (u.getRole()) {
                case ADMIN: adminMenu(u); break;
                case AGENT: agentMenu(u); break;
                default: passengerMenu(u);
            }
        } catch (RuntimeException e) { System.out.println("! " + e.getMessage()); }
    }

    private static String registerFlow() {
        try {
            Passenger p = svc.register(ask("Username"), ask("Password (min 4 chars)"), ask("Full name"), ask("Email"));
            System.out.println("Account created: " + p.getUsername());
            return p.getUsername();
        } catch (RuntimeException e) { System.out.println("! " + e.getMessage()); return null; }
    }

    // ------------------------------------------------------------ menus
    private static void adminMenu(User u) {
        while (true) {
            System.out.println("\n--- ADMIN MENU ---\n1. List all flights\n2. Add flight\n3. Update flight\n"
                    + "4. Update flight status\n5. Remove flight\n6. View all reservations\n7. List users\n"
                    + "8. Deactivate/Reactivate user\n9. Reports\n0. Logout");
            String c = ask("Choose");
            try {
                switch (c) {
                    case "1": printFlights(svc.searchFlights(null, null, null)); break;
                    case "2": addFlight(u); break;
                    case "3": updateFlight(u); break;
                    case "4": {
                        String id = ask("Flight ID");
                        FlightStatus s = FlightStatus.valueOf(ask("Status (SCHEDULED/DELAYED/CANCELLED)").toUpperCase());
                        svc.updateStatus(u, id, s);
                        System.out.println("Status updated.");
                        break;
                    }
                    case "5": {
                        int n = svc.removeFlight(u, ask("Flight ID"));
                        System.out.println("Flight removed. " + n + " reservation(s) cancelled.");
                        break;
                    }
                    case "6": svc.allReservations(u).forEach(System.out::println); break;
                    case "7": svc.listUsers(u).forEach(System.out::println); break;
                    case "8": {
                        String name = ask("Username");
                        boolean activate = ask("Activate? (y = activate, n = deactivate)").equalsIgnoreCase("y");
                        svc.setUserActive(u, name, activate);
                        System.out.println("Done.");
                        break;
                    }
                    case "9": System.out.println(svc.report(u)); break;
                    case "0": return;
                    default: System.out.println("Invalid choice.");
                }
            } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
                System.out.println("! " + e.getMessage());
            }
        }
    }

    private static void agentMenu(User u) {
        while (true) {
            System.out.println("\n--- BOOKING AGENT MENU ---\n1. Search flights\n2. Book for a passenger\n"
                    + "3. View passenger bookings\n4. Modify booking (change seat)\n5. Cancel booking\n"
                    + "6. Register walk-in passenger\n0. Logout");
            String c = ask("Choose");
            try {
                switch (c) {
                    case "1": searchFlow(); break;
                    case "2": bookFlow(u, ask("Passenger username")); break;
                    case "3": showBookings(u, ask("Passenger username")); break;
                    case "4": modifyFlow(u); break;
                    case "5": cancelFlow(u); break;
                    case "6": registerFlow(); break;
                    case "0": return;
                    default: System.out.println("Invalid choice.");
                }
            } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
                System.out.println("! " + e.getMessage());
            }
        }
    }

    private static void passengerMenu(User u) {
        while (true) {
            System.out.println("\n--- PASSENGER MENU ---\n1. Search flights\n2. Book a flight\n3. My bookings\n"
                    + "4. Modify booking (change seat)\n5. Cancel booking\n6. Update profile\n0. Logout");
            String c = ask("Choose");
            try {
                switch (c) {
                    case "1": searchFlow(); break;
                    case "2": bookFlow(u, u.getUsername()); break;
                    case "3": showBookings(u, u.getUsername()); break;
                    case "4": modifyFlow(u); break;
                    case "5": cancelFlow(u); break;
                    case "6":
                        svc.updateProfile(u, ask("New full name (blank = keep)"), ask("New email (blank = keep)"),
                                ask("New password (blank = keep)"));
                        System.out.println("Profile updated.");
                        break;
                    case "0": return;
                    default: System.out.println("Invalid choice.");
                }
            } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
                System.out.println("! " + e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------ flows
    private static void searchFlow() {
        String o = ask("Origin (blank = any)");
        String d = ask("Destination (blank = any)");
        String ds = ask("Date yyyy-MM-dd (blank = any)");
        LocalDate date = ds.isBlank() ? null : LocalDate.parse(ds);
        printFlights(svc.searchFlights(o, d, date));
    }

    private static void bookFlow(User actor, String passengerUsername) {
        printFlights(svc.searchFlights(null, null, null));
        Flight f = svc.getFlight(ask("Flight ID to book"));
        printSeatMap(f);
        String seat = ask("Seat (e.g. 7A)").toUpperCase();
        System.out.println("Payment: 1. Credit Card  2. GCash  3. PayPal");
        PaymentMethod m = PaymentMethod.values()[Integer.parseInt(ask("Method")) - 1];
        Seat s = f.getSeat(seat);
        if (s != null) System.out.printf("Total: PHP %,.2f (%s)%n", f.priceFor(s.getFareClass()), s.getFareClass().getLabel());
        if (!ask("Confirm and pay? (y/n)").equalsIgnoreCase("y")) { System.out.println("Cancelled."); return; }
        Reservation r = svc.book(actor, passengerUsername, f.getId(), seat, m);
        System.out.println("\nPayment successful!\n" + svc.ticket(r));
    }

    private static void showBookings(User actor, String username) {
        List<Reservation> list = svc.reservationsFor(actor, username);
        if (list.isEmpty()) { System.out.println("No bookings found."); return; }
        list.forEach(System.out::println);
        String pnr = ask("Enter PNR to view e-ticket (blank to skip)");
        if (!pnr.isBlank())
            list.stream().filter(r -> r.getPnr().equalsIgnoreCase(pnr)).findFirst()
                .ifPresentOrElse(r -> System.out.println(svc.ticket(r)), () -> System.out.println("PNR not in list."));
    }

    private static void modifyFlow(User actor) {
        String pnr = ask("PNR");
        Reservation r = null;
        for (Reservation x : svc.reservationsFor(actor, actor.getRole() == Role.PASSENGER ? actor.getUsername()
                : ask("Passenger username")))
            if (x.getPnr().equalsIgnoreCase(pnr)) r = x;
        if (r == null) throw new IllegalArgumentException("Reservation not found.");
        printSeatMap(svc.getFlight(r.getFlightId()));
        double diff = svc.changeSeat(actor, pnr, ask("New seat"));
        System.out.printf("Seat changed. Price difference: PHP %,.2f%n", diff);
    }

    private static void cancelFlow(User actor) {
        String pnr = ask("PNR to cancel");
        if (ask("Are you sure? (y/n)").equalsIgnoreCase("y")) {
            svc.cancel(actor, pnr);
            System.out.println("Reservation cancelled. Seat released.");
        }
    }

    private static void addFlight(User u) {
        Flight f = svc.addFlight(u, ask("Origin"), ask("Destination"),
                LocalDateTime.parse(ask("Departure (yyyy-MM-dd HH:mm)").replace(' ', 'T')),
                Double.parseDouble(ask("Economy base price")));
        System.out.println("Added: " + f);
    }

    private static void updateFlight(User u) {
        String id = ask("Flight ID");
        String o = ask("New origin (blank = keep)");
        String d = ask("New destination (blank = keep)");
        String t = ask("New departure yyyy-MM-dd HH:mm (blank = keep)");
        String p = ask("New base price (blank = keep)");
        svc.updateFlight(u, id, o.isBlank() ? null : o, d.isBlank() ? null : d,
                t.isBlank() ? null : LocalDateTime.parse(t.replace(' ', 'T')),
                p.isBlank() ? null : Double.parseDouble(p));
        System.out.println("Flight updated.");
    }

    // ------------------------------------------------------------ display / input
    private static void printFlights(List<Flight> list) {
        if (list.isEmpty()) System.out.println("No flights found.");
        list.forEach(System.out::println);
    }

    private static void printSeatMap(Flight f) {
        System.out.println("\nSeat map for " + f.getId() + "   ([XX] = taken)");
        for (int row = 1; row <= Flight.ROWS; row++) {
            StringBuilder sb = new StringBuilder(String.format("%2d ", row));
            FareClass fc = null;
            for (int i = 0; i < Flight.LETTERS.length; i++) {
                Seat s = f.getSeat(row + String.valueOf(Flight.LETTERS[i]));
                fc = s.getFareClass();
                sb.append(s.isBooked() ? "[XX ]" : String.format("[%-3s]", s.getId())).append(i == 1 ? "   " : " ");
            }
            System.out.println(sb + " " + fc.getLabel() + String.format(" (PHP %,.0f)", f.priceFor(fc)));
        }
    }

    private static String ask(String prompt) {
        System.out.print(prompt + ": ");
        return in.nextLine().trim();
    }
}
