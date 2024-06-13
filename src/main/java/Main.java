import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    public static final int PORT_NUMBER = 9999;

    public static void main(String[] args) {
        List<String> paths;
        try {
            paths = Files
                    .walk(Paths.get("./public"))
                    .filter(Files::isRegularFile)
                    .map(f -> "/" + f.getFileName())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Server server = new Server(paths);

        // добавление хендлеров (обработчиков)
        for (String path : paths) {
            server.addHandler("GET", path, (request, responseStream) ->
                    handleDefaultGet(request, responseStream));
        }



        server.addHandler("GET", "", (request, responseStream) ->
                handleFileNotFound(request, responseStream));
        server.addHandler("GET", "/classic.html", (request, responseStream) ->
                handleClassicHtml(request, responseStream));
        server.addHandler("POST", "/messages", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) {
                // TODO: handlers code
            }
        });


        server.listen(PORT_NUMBER);
    }

    public static void handleFileNotFound(Request request, BufferedOutputStream responseStream) {
        try {
            responseStream.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            responseStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void handleDefaultGet(Request request, BufferedOutputStream responseStream) {
        final var filePath = Path.of(".", "public", request.getUrl());
        try {
            final var mimeType = Files.probeContentType(filePath);
            final var length = Files.size(filePath);

            responseStream.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, responseStream);
            responseStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void handleClassicHtml(Request request, BufferedOutputStream responseStream) {
        try {
            final var filePath = Path.of(".", "public", request.getUrl());
            final var mimeType = Files.probeContentType(filePath);

            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            responseStream.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            responseStream.write(content);
            responseStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}