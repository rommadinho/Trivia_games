import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;

public class TriviaClient extends JFrame {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private JTextArea questionArea;
    private JButton aBtn, bBtn, cBtn;
    private JLabel statusLabel, timerLabel;
    private JTextField ipField, portField, nameField;
    private JButton connectBtn;

    private Timer questionTimer;
    private int timeLeft = 20;

    public TriviaClient() {
        setTitle("Trivia Client - Anjay Edition");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Connection Panel
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("IP:"));
        ipField = new JTextField("127.0.0.1", 10);
        topPanel.add(ipField);
        topPanel.add(new JLabel("Port:"));
        portField = new JTextField("1234", 5);
        topPanel.add(portField);
        topPanel.add(new JLabel("Username:"));
        nameField = new JTextField("anjay", 8);
        topPanel.add(nameField);
        connectBtn = new JButton("Connect");
        topPanel.add(connectBtn);
        add(topPanel, BorderLayout.NORTH);

        // Question area
        questionArea = new JTextArea();
        questionArea.setEditable(false);
        questionArea.setFont(new Font("Serif", Font.BOLD, 16));
        add(new JScrollPane(questionArea), BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel();
        aBtn = new JButton("A");
        bBtn = new JButton("B");
        cBtn = new JButton("C");
        buttonPanel.add(aBtn);
        buttonPanel.add(bBtn);
        buttonPanel.add(cBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        // Status Panel
        JPanel statusPanel = new JPanel();
        statusLabel = new JLabel("Status: Not connected");
        timerLabel = new JLabel("Time left: 20");
        statusPanel.add(statusLabel);
        statusPanel.add(timerLabel);
        add(statusPanel, BorderLayout.PAGE_END);

        // Disable answer buttons initially
        setAnswerButtonsEnabled(false);

        // Action Listeners
        connectBtn.addActionListener(e -> connectToServer());
        aBtn.addActionListener(e -> sendAnswer("a"));
        bBtn.addActionListener(e -> sendAnswer("b"));
        cBtn.addActionListener(e -> sendAnswer("c"));
    }

    private void connectToServer() {
        String ip = ipField.getText();
        int port = Integer.parseInt(portField.getText());
        String username = nameField.getText();

        try {
            socket = new Socket(ip, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println(username);
            statusLabel.setText("Connected as " + username);
            connectBtn.setEnabled(false);

            // Start listener thread
            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.startsWith("QUESTION")) {
                            showQuestion(line.substring(9)); // remove "QUESTION:"
                        } else if (line.startsWith("RESULT")) {
                            showResult(line.substring(7));
                        } else if (line.startsWith("END")) {
                            showFinalScore(line.substring(4));
                            setAnswerButtonsEnabled(false);
                        }
                    }
                } catch (IOException e) {
                    showError("Disconnected from server.");
                }
            }).start();

        } catch (IOException ex) {
            showError("Connection failed!");
        }
    }

    private void showQuestion(String data) {
        questionArea.setText(data);
        setAnswerButtonsEnabled(true);
        startTimer();
    }

    private void sendAnswer(String answer) {
        out.println(answer);
        setAnswerButtonsEnabled(false);
        stopTimer();
        statusLabel.setText("Jawaban dikirim: " + answer.toUpperCase());
    }

    private void showResult(String result) {
        statusLabel.setText("Hasil: " + result);
    }

    private void showFinalScore(String score) {
        JOptionPane.showMessageDialog(this, "Game Selesai!\n" + score, "Hasil Akhir", JOptionPane.INFORMATION_MESSAGE);
    }

    private void setAnswerButtonsEnabled(boolean enabled) {
        aBtn.setEnabled(enabled);
        bBtn.setEnabled(enabled);
        cBtn.setEnabled(enabled);
    }

    private void startTimer() {
        timeLeft = 20;
        timerLabel.setText("Time left: " + timeLeft);
        if (questionTimer != null) questionTimer.cancel();

        questionTimer = new Timer();
        questionTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                timeLeft--;
                SwingUtilities.invokeLater(() -> timerLabel.setText("Time left: " + timeLeft));
                if (timeLeft <= 0) {
                    stopTimer();
                    SwingUtilities.invokeLater(() -> {
                        setAnswerButtonsEnabled(false);
                        out.println("timeout");
                        statusLabel.setText("Waktu habis! Jawaban tidak terkirim.");
                    });
                }
            }
        }, 1000, 1000);
    }

    private void stopTimer() {
        if (questionTimer != null) {
            questionTimer.cancel();
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
        statusLabel.setText("Status: " + message);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TriviaClient gui = new TriviaClient();
            gui.setVisible(true);
        });
    }
}
