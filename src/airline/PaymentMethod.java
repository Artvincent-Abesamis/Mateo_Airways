package airline;

public enum PaymentMethod {
    CREDIT_CARD("Credit Card"), GCASH("GCash"), PAYPAL("PayPal");

    private final String label;
    PaymentMethod(String label) { this.label = label; }
    public String getLabel() { return label; }
}
