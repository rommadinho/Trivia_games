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

    
    private JLabel scoreLabel;
    private JTextArea questionArea;
    private JButton aBtn, bBtn, cBtn;
    private JLabel statusLabel, timerLabel;
    private JTextField ipField, portField, nameField;
    private JButton connectBtn;
    private JLabel resultLabel;

    private Timer questionTimer;
    private int timeLeft = 20;

    private JFrame leaderboardFrame;
    private JTable leaderboardTable;
    private DefaultTableModel leaderboardModel;

    public TriviaClient() {
        setTitle("Trivia Client - UAS Edition");
        setSize(800, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("IP:"));
        ipField = new JTextField("", 10);
        topPanel.add(ipField);
        topPanel.add(new JLabel("Port:"));
        portField = new JTextField("12345", 5);
        topPanel.add(portField);
        topPanel.add(new JLabel("Username:"));
        nameField = new JTextField("", 8);
        topPanel.add(nameField);
        connectBtn = new JButton("Connect");
        topPanel.add(connectBtn);
        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(1, 2));

        JPanel questionPanel = new JPanel(new BorderLayout());
        questionArea = new JTextArea();
        questionArea.setEditable(false);
        questionArea.setFont(new Font("Serif", Font.BOLD, 16));
        questionPanel.add(new JScrollPane(questionArea), BorderLayout.CENTER);

        resultLabel = new JLabel("", SwingConstants.CENTER);
        resultLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        resultLabel.setForeground(Color.BLUE);
        resultLabel.setVisible(false);
        questionPanel.add(resultLabel, BorderLayout.SOUTH);
        centerPanel.add(questionPanel);

        leaderboardModel = new DefaultTableModel(new Object[]{"Nama", "Skor"}, 0);
        leaderboardTable = new JTable(leaderboardModel);
        JPanel leaderboardPanel = new JPanel(new BorderLayout());
        leaderboardPanel.add(new JLabel("Leaderboard", SwingConstants.CENTER), BorderLayout.NORTH);
        leaderboardPanel.add(new JScrollPane(leaderboardTable), BorderLayout.CENTER);
        centerPanel.add(leaderboardPanel);

        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());

        JPanel buttonPanel = new JPanel();
        aBtn = new JButton("A");
        bBtn = new JButton("B");
        cBtn = new JButton("C");
        buttonPanel.add(aBtn);
        buttonPanel.add(bBtn);
        buttonPanel.add(cBtn);
        bottomPanel.add(buttonPanel, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Status: Not connected");
        timerLabel = new JLabel("Time left: 20");
        statusPanel.add(statusLabel);
        statusPanel.add(timerLabel);
        bottomPanel.add(statusPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        setAnswerButtonsEnabled(false);

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

            new Thread(() -> {
                try {
                    String serverMessage = in.readLine();
                    if (serverMessage != null) {
                        statusLabel.setText(serverMessage);
                        out.println(username);
                    }

                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.startsWith("SOAL:")) {
                            showQuestion(line.substring(6));
                        } else if (line.startsWith("RESULT:")) {
                            showResult(line.substring(8));
                        } else if (line.startsWith("SCORE:")) {
                            scoreLabel.setText("Skor: " + line.substring(6));
                        } else if (line.startsWith("END:")) {
                            showFinalScore(line.substring(5));
                        } else if (line.startsWith("LEADERBOARD:")) {
                            showLeaderboard(line.substring(12));
                        }
                    }
                } catch (IOException e) {
                    showError("Terputus dari server.");
                }
            }).start();

            connectBtn.setEnabled(false);
            statusLabel.setText("Terhubung ke server.");

        } catch (IOException ex) {
            showError("Gagal terhubung!");
        }
    }
}
