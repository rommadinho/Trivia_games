import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

public class TriviaServer {
    private static final int PORT = 12345;
    private static final int MIN_PLAYERS = 2;
    private static final int TIMEOUT = 20000; // 20 detik
    private static List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private static List<String[]> questions = new ArrayList<>();

    public static void main(String[] args) {
        loadQuestions("question.txt");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server dimulai di port " + PORT);

            ExecutorService pool = Executors.newCachedThreadPool();

            // Accept clients
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler);
                pool.execute(handler);

                if (clients.size() >= MIN_PLAYERS) {
                    Thread.sleep(3000); // Tunggu sebentar
                    startGame();
                }
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
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

    private static void loadQuestions(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                    questions.add(parts);
                }
            }
        } catch (IOException e) {
            System.out.println("Gagal memuat soal.");
        }
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

    private static void startGame() {
        System.out.println("Game dimulai dengan " + clients.size() + " pemain.");
        for (String[] q : questions) {
            String question = q[0];
            String answer = q[1].trim().toLowerCase();

            Map<ClientHandler, String> answers = new ConcurrentHashMap<>();
            CountDownLatch latch = new CountDownLatch(clients.size());

            // Kirim soal ke semua client
            for (ClientHandler client : clients) {
                client.askQuestion(question, latch, answers);
            }

            try {
                boolean completed = latch.await(TIMEOUT, TimeUnit.MILLISECONDS);
                if (!completed) {
                    System.out.println("Beberapa pemain tidak menjawab tepat waktu.");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Cek jawaban dan update skor
            for (Map.Entry<ClientHandler, String> entry : answers.entrySet()) {
                if (entry.getValue().equalsIgnoreCase(answer)) {
                    entry.getKey().addScore(10);
                }
            }

            // Kirim skor sementara
            broadcastScores();
        }

        // Game selesai
        announceWinner();
        System.exit(0);
    }

    private static void broadcastScores() {
        for (ClientHandler client : clients) {
            client.send("Skor sementara Anda: " + client.getScore());
        }
    }

    private static void announceWinner() {
        int maxScore = clients.stream().mapToInt(ClientHandler::getScore).max().orElse(0);
        List<String> winners = new ArrayList<>();
        for (ClientHandler client : clients) {
            if (client.getScore() == maxScore) {
                winners.add(client.getUsername());
            }
        }

        String result = "Permainan selesai. Pemenang: " + String.join(", ", winners);
        for (ClientHandler client : clients) {
            client.send(result);
        }

        System.out.println(result);
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String username;
        private int score = 0;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public String getUsername() {
            return username;
        }

        public int getScore() {
            return score;
        }

        public void addScore(int s) {
            score += s;
        }

        public void send(String message) {
            out.println(message);
        }

        public void askQuestion(String question, CountDownLatch latch, Map<ClientHandler, String> answers) {
            new Thread(() -> {
                try {
                    out.println("SOAL: " + question);
                    socket.setSoTimeout(TIMEOUT);
                    String response = in.readLine();
                    answers.put(this, response);
                } catch (IOException e) {
                    answers.put(this, ""); // dianggap salah
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("Masukkan username:");
                username = in.readLine();
                System.out.println(username + " telah terhubung.");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
