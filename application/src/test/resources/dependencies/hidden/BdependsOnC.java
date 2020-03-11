package dependencies.hidden;

public class BdependsOnC {
    private final C c = new C();


    public void hi() {
        c.hi();
    }
}
