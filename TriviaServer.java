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
    private void startServer() {
        log("Server dimulai...");
        loadQuestions();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            log("🚀 TriviaServer berjalan di port " + PORT);

            while (clients.size() < MAX_PLAYERS) {
                Socket socket = serverSocket.accept();
                ClientHandler client = new ClientHandler(socket);
                clients.add(client);
                client.start();
            }

            log("Jumlah pemain lengkap, memulai game...");
            broadcast("Game dimulai!");

            for (Question q : questions) {
                broadcast("SOAL:" + q);
                log("[KIRIM SOAL] " + q);

                for (ClientHandler c : clients)
                    c.currentAnswer = null;

                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < 20000) {
                    boolean allAnswered = true;
                    for (ClientHandler c : clients) {
                        if (c.currentAnswer == null) {
                            allAnswered = false;
                            break;
                        }
                    }
                    if (allAnswered)
                        break;
                    Thread.sleep(200);
                }

                for (ClientHandler c : clients) {
                    if (q.correctAnswer.equalsIgnoreCase(c.currentAnswer)) {
                        c.score += 10;
                        c.send("✅ Jawaban Anda BENAR! Skor: " + c.score);
                    } else {
                        c.send("❌ Jawaban SALAH! Skor: " + c.score);
                    }
                }

                broadcast("\n📊 Klasemen Sementara:");
                for (ClientHandler c : clients) {
                    broadcast("👤 " + c.name + ": " + c.score + " poin");
                }

                broadcastLeaderboard();
            }

            broadcast("\n📢 Permainan selesai! Berikut skor akhir:");
            for (ClientHandler c : clients) {
                broadcast("👤 " + c.name + ": " + c.score + " poin");
            }

            announceWinners();

            for (ClientHandler c : clients) {
                c.send("END:Terima kasih telah bermain!");
            }

        } catch (IOException | InterruptedException e) {
            log("❌ Error: " + e.getMessage());
        }
    }

    private void loadQuestions() {
        try (BufferedReader reader = new BufferedReader(new FileReader("question.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;
                String q = line;
                String a = reader.readLine();
                String b = reader.readLine();
                String c = reader.readLine();
                String answer = reader.readLine();
                questions.add(new Question(q, a, b, c, answer.trim()));
            }
            log("✅ Soal berhasil dimuat: " + questions.size());
        } catch (IOException e) {
            log("❌ Gagal membaca soal: " + e.getMessage());
        }
    }

    private void broadcast(String message) {
        for (ClientHandler c : clients) {
            c.send(message);
        }
        log("[BROADCAST] " + message);
    }

    private void broadcastLeaderboard() {
        StringBuilder leaderboard = new StringBuilder("LEADERBOARD:");
        clients.stream()
                .sorted(Comparator.comparingInt(c -> -c.score))
                .forEach(c -> leaderboard.append(c.name).append("=").append(c.score).append(","));
        if (leaderboard.charAt(leaderboard.length() - 1) == ',') {
            leaderboard.setLength(leaderboard.length() - 1);
        }
        broadcast(leaderboard.toString());
    }
    private void announceWinners() {
     
        List<ClientHandler> sorted = new ArrayList<>(clients);
        sorted.sort(Comparator.comparingInt(c -> -c.score)); 

        log("🏆 Skor Akhir:");
        StringBuilder klasemen = new StringBuilder("\n🎖️ Klasemen Akhir:\n");

        for (int i = 0; i < sorted.size(); i++) {
            ClientHandler c = sorted.get(i);

            String medal = switch (i) {
                case 0 -> "🥇";
                case 1 -> "🥈";
                case 2 -> "🥉";
                default -> "🎖️";
            };

            String message = medal + " Juara " + (i + 1) + ": " + c.name + " - " + c.score + " poin";
            klasemen.append(message).append("\n");

            log(message);               
            c.send("END: " + message);  
        }

        
        broadcast("LEADERBOARD:" + getFinalLeaderboardString(sorted));
        broadcast(klasemen.toString()); 
    }

    private String getFinalLeaderboardString(List<ClientHandler> list) {
        StringBuilder sb = new StringBuilder();
        for (ClientHandler c : list) {
            if (sb.length() > 0) sb.append(",");
            sb.append(c.name).append("=").append(c.score);
        }
        return sb.toString();
    }
    class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        public String name;
        public int score = 0;
        public String currentAnswer = null;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        public void run() {
            try {
                send("Masukkan username:");
                name = in.readLine();
                log("[JOIN] " + name + " bergabung.");

                while (true) {
                    String input = in.readLine();
                    if (input == null)
                        break;
                    if (input.equalsIgnoreCase("a") || input.equalsIgnoreCase("b") || input.equalsIgnoreCase("c")) {
                        currentAnswer = input;
                        log("[JAWABAN] " + name + ": " + input);
                    }
                }
            } catch (IOException e) {
                log("❌ Client terputus: " + name);
            }
        }

        public void send(String message) {
            out.println(message);
        }
    }

    static class Question {
        String question, a, b, c, correctAnswer;

        public Question(String question, String a, String b, String c, String correctAnswer) {
            this.question = question;
            this.a = a;
            this.b = b;
            this.c = c;
            this.correctAnswer = correctAnswer;
        }

        @Override
        public String toString() {
            return question + " | a. " + a + " | b. " + b + " | c. " + c;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TriviaServerGUI::new);
    }
}


