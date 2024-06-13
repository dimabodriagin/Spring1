import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final List<String> validPaths;
    private final ExecutorService executorService;
    private final Map<String, Map<String, Handler>> handlers;

    public Server(List<String> validPaths) {
        this.validPaths = validPaths;
        this.executorService = Executors.newFixedThreadPool(64);
        this.handlers = new ConcurrentHashMap<>();
    }

    public void listen(int portNumber) {
        try (final var serverSocket = new ServerSocket(portNumber)) {
            while (true) {
                try {
                    final var socket = serverSocket.accept();
                    this.executorService.execute(() -> processConnection(socket));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processConnection(Socket socket) {
        try (
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {
            // read only request line for simplicity
            // must be in form GET /path HTTP/1.1
            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");

            if (parts.length != 3) {
                // just close socket
                return;
            }

            final var request = new Request(parts[0], parts[1], getHeaders(in), socket.getInputStream());

            if (!validPaths.contains(request.getUrl())) {
                handlers.get("").get(request.getMethod()).handle(request, out);
                return;
            }

            // special case for classic
            if (request.getUrl().equals("/classic.html")) {
                handlers.get(request.getUrl()).get(request.getMethod()).handle(request, out);
                return;
            }

            handlers.get(request.getUrl()).get(request.getMethod()).handle(request, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> getHeaders(BufferedReader in) {
        Map<String, String> headers = new HashMap<>();
        try {
            String line = in.readLine();

            while (line.length() > 0) {
                String[] lines = line.split(": ");
                headers.put(lines[0], lines[lines.length - 1] + "\r\n");
                line = in.readLine();
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return headers;
    }

    public void addHandler(String method, String url, Handler handler) {
        Map<String, Handler> map = new ConcurrentHashMap<>();
        map.put(method, handler);
        handlers.put(url, map);
    }
}
