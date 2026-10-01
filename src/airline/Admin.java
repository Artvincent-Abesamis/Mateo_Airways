package airline;

public class Admin extends User {
    private static final long serialVersionUID = 1L;
    public Admin(String u, String p, String n, String e) { super(u, p, n, e); }
    @Override public Role getRole() { return Role.ADMIN; }
}
