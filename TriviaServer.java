import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

public class TriviaServer {
    private static final int PORT = 12345;
    private static final int MAX_PLAYERS = 3;

    private static List<Question> questions = new ArrayList<>();
    private static List<ClientHandler> clients = new ArrayList<>();

    private JTextArea logArea;

    public TriviaServer() {
        setTitle("🎮 Trivia Server GUI");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);
        setVisible(true);

        new Thread(this::startServer).start();
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
        System.out.println(message);
    }
}
