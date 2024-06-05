import java.util.List;

public class Main {

    public static final int PORT_NUMBER = 9999;

    public static void main(String[] args) {
        Server server = new Server(List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js",
                "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js"));

        server.listen(PORT_NUMBER);
    }
}