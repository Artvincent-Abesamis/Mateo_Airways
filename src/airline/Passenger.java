package airline;

public class Passenger extends User {
    private static final long serialVersionUID = 1L;
    public Passenger(String u, String p, String n, String e) { super(u, p, n, e); }
    @Override public Role getRole() { return Role.PASSENGER; }
}
