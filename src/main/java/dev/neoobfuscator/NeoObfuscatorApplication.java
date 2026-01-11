package dev.neoobfuscator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class NeoObfuscatorApplication {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_LIME = "\u001B[92m";
    private static final String ANSI_RED = "\u001B[91m";
    private static final String ANSI_CYAN = "\u001B[96m";
    private static final String ANSI_YELLOW = "\u001B[93m";
    private static final String ANSI_DIM = "\u001B[2m";
    private static final String ANSI_BOLD = "\u001B[1m";

    private static final int PORT = 8080;

    public static void main(String[] args) {
        // Print banner
        printBanner();

        // Kill any existing process on port 8080
        killProcessOnPort(PORT);

        // Start Spring Boot
        ConfigurableApplicationContext context = SpringApplication.run(NeoObfuscatorApplication.class, args);

        // Print access info after startup
        printAccessInfo(context);
    }

    /**
     * Kill any process currently using the specified port.
     * Works on Windows. Falls back gracefully on other OS.
     */
    private static void killProcessOnPort(int port) {
        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                // Windows: Use netstat to find PID and taskkill to terminate
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c",
                        "for /f \"tokens=5\" %a in ('netstat -ano ^| findstr :" + port
                                + " ^| findstr LISTENING') do @echo %a");
                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    String pid = line.trim();
                    if (!pid.isEmpty() && pid.matches("\\d+")) {
                        System.out.println(ANSI_YELLOW + "  ⚠ " + ANSI_RESET + "Stopping existing process on port "
                                + port + " (PID: " + pid + ")");

                        // Kill the process
                        ProcessBuilder killPb = new ProcessBuilder("taskkill", "/F", "/PID", pid);
                        killPb.redirectErrorStream(true);
                        Process killProcess = killPb.start();
                        killProcess.waitFor();

                        // Small delay to ensure port is released
                        Thread.sleep(500);
                    }
                }
                process.waitFor();
            } else {
                // Linux/Mac: Use lsof and kill
                ProcessBuilder pb = new ProcessBuilder("sh", "-c", "lsof -ti:" + port);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String pid = reader.readLine();
                if (pid != null && !pid.isEmpty()) {
                    System.out.println(ANSI_YELLOW + "  ⚠ " + ANSI_RESET + "Stopping existing process on port " + port
                            + " (PID: " + pid.trim() + ")");

                    ProcessBuilder killPb = new ProcessBuilder("kill", "-9", pid.trim());
                    killPb.start().waitFor();
                    Thread.sleep(500);
                }
                process.waitFor();
            }
        } catch (Exception e) {
            // Silently ignore - if we can't kill, Spring will fail with a clear message
        }
    }

    private static void printBanner() {
        System.out.println();
        System.out.println(
                ANSI_LIME + "    ███╗   ██╗███████╗ ██████╗  ██████╗ ██╗   ██╗ █████╗ ██████╗ ██████╗ " + ANSI_RESET);
        System.out.println(
                ANSI_LIME + "    ████╗  ██║██╔════╝██╔═══██╗██╔════╝ ██║   ██║██╔══██╗██╔══██╗██╔══██╗" + ANSI_RESET);
        System.out.println(
                ANSI_LIME + "    ██╔██╗ ██║█████╗  ██║   ██║██║  ███╗██║   ██║███████║██████╔╝██║  ██║" + ANSI_RESET);
        System.out.println(
                ANSI_LIME + "    ██║╚██╗██║██╔══╝  ██║   ██║██║   ██║██║   ██║██╔══██║██╔══██╗██║  ██║" + ANSI_RESET);
        System.out.println(
                ANSI_LIME + "    ██║ ╚████║███████╗╚██████╔╝╚██████╔╝╚██████╔╝██║  ██║██║  ██║██████╔╝" + ANSI_RESET);
        System.out.println(
                ANSI_LIME + "    ╚═╝  ╚═══╝╚══════╝ ╚═════╝  ╚═════╝  ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═════╝ " + ANSI_RESET);
        System.out.println();
        System.out.println(ANSI_DIM + "                  Java Obfuscation Web Application" + ANSI_RESET);
        System.out.println(ANSI_DIM + "                     Powered by " + ANSI_RED + "Skidfuscator" + ANSI_RESET);
        System.out.println();
    }

    private static void printAccessInfo(ConfigurableApplicationContext context) {
        try {
            Environment env = context.getEnvironment();
            String port = env.getProperty("server.port", "8080");
            String hostAddress = InetAddress.getLocalHost().getHostAddress();

            System.out.println();
            System.out.println(
                    ANSI_GREEN + "  ✓ " + ANSI_RESET + ANSI_BOLD + "Server started successfully!" + ANSI_RESET);
            System.out.println();
            System.out.println(ANSI_DIM + "  ┌──────────────────────────────────────────────────────" + ANSI_RESET);
            System.out.println(ANSI_DIM + "  │" + ANSI_RESET + "  " + ANSI_CYAN + "Local:" + ANSI_RESET
                    + "      http://localhost:" + port + "                  " + ANSI_DIM + "" + ANSI_RESET);
            System.out.println(ANSI_DIM + "  │" + ANSI_RESET + "  " + ANSI_CYAN + "Network:" + ANSI_RESET
                    + "    http://" + padRight(hostAddress + ":" + port, 25) + ANSI_DIM + "" + ANSI_RESET);
            System.out.println(ANSI_DIM + "  └──────────────────────────────────────────────────────" + ANSI_RESET);
            System.out.println();
            System.out.println(
                    ANSI_YELLOW + "  → " + ANSI_RESET + ANSI_DIM + "Press Ctrl+C to stop the server" + ANSI_RESET);
            System.out.println();

        } catch (Exception e) {
            // Fallback if IP detection fails
            String port = context.getEnvironment().getProperty("server.port", "8080");
            System.out.println();
            System.out.println(ANSI_GREEN + "  ✓ " + ANSI_RESET + "Server running at: http://localhost:" + port);
            System.out.println();
        }
    }

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}
