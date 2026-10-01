package airline;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Reservation implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String pnr;
    private final String passengerUsername;
    private final String bookedBy;
    private final String flightId;
    private String seatId;
    private FareClass fareClass;
    private double price;
    private ReservationStatus status = ReservationStatus.CONFIRMED;
    private final PaymentMethod paymentMethod;
    private final String transactionRef;
    private final LocalDateTime createdAt = LocalDateTime.now();

    public Reservation(String pnr, String passengerUsername, String bookedBy, String flightId, String seatId,
                       FareClass fareClass, double price, PaymentMethod method, String transactionRef) {
        this.pnr = pnr;
        this.passengerUsername = passengerUsername;
        this.bookedBy = bookedBy;
        this.flightId = flightId;
        this.seatId = seatId;
        this.fareClass = fareClass;
        this.price = price;
        this.paymentMethod = method;
        this.transactionRef = transactionRef;
    }

    public String getPnr() { return pnr; }
    public String getPassengerUsername() { return passengerUsername; }
    public String getBookedBy() { return bookedBy; }
    public String getFlightId() { return flightId; }
    public String getSeatId() { return seatId; }
    public FareClass getFareClass() { return fareClass; }
    public double getPrice() { return price; }
    public ReservationStatus getStatus() { return status; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getTransactionRef() { return transactionRef; }
    public boolean isConfirmed() { return status == ReservationStatus.CONFIRMED; }

    void reassign(String seatId, FareClass fc, double newPrice) {
        this.seatId = seatId;
        this.fareClass = fc;
        this.price = newPrice;
    }
    void cancel() { this.status = ReservationStatus.CANCELLED; }

    @Override
    public String toString() {
        return String.format("%-7s %-14s %-7s Seat %-4s %-12s PHP %,10.2f  %s",
                pnr, passengerUsername, flightId, seatId, fareClass.getLabel(), price, status);
    }

    /** Electronic ticket text. */
    public String toTicket(Flight f, String passengerName) {
        String line = "+" + "=".repeat(52) + "+\n";
        return line
             + String.format("| %-50s |%n", "ELECTRONIC TICKET  -  PNR: " + pnr)
             + line
             + String.format("| %-50s |%n", "Passenger : " + passengerName)
             + String.format("| %-50s |%n", "Flight    : " + f.getId() + " (" + f.getStatus() + ")")
             + String.format("| %-50s |%n", "Route     : " + f.getOrigin() + " -> " + f.getDestination())
             + String.format("| %-50s |%n", "Departure : " + f.getDeparture().format(Flight.FMT))
             + String.format("| %-50s |%n", "Seat/Class: " + seatId + " / " + fareClass.getLabel())
             + String.format("| %-50s |%n", String.format("Paid      : PHP %,.2f via %s", price, paymentMethod.getLabel()))
             + String.format("| %-50s |%n", "Txn Ref   : " + transactionRef)
             + line;
    }
}
