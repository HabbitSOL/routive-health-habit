package com.routive.healthhabit.auth.mail;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import static org.assertj.core.api.Assertions.*;

class SmtpAccountMailSenderTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void sendsActualSmtpMessageToLoopbackOnly(boolean reset) throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
             var executor = Executors.newSingleThreadExecutor()) {
            server.setSoTimeout(5000);
            Future<String> received = executor.submit(() -> {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(5000);
                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                    reply(output, "220 localhost test SMTP");
                    StringBuilder data = new StringBuilder();
                    boolean collecting = false;
                    String line;
                    while ((line = input.readLine()) != null) {
                        if (collecting) {
                            if (line.equals(".")) { collecting = false; reply(output, "250 accepted"); }
                            else data.append(line).append("\r\n");
                        } else if (line.equalsIgnoreCase("DATA")) {
                            collecting = true; reply(output, "354 continue");
                        } else if (line.equalsIgnoreCase("QUIT")) {
                            reply(output, "221 bye"); break;
                        } else {
                            reply(output, "250 OK");
                        }
                    }
                    return data.toString();
                }
            });
            JavaMailSenderImpl mail = new JavaMailSenderImpl();
            mail.setHost("127.0.0.1");
            mail.setPort(server.getLocalPort());
            mail.setDefaultEncoding("UTF-8");
            mail.getJavaMailProperties().setProperty("mail.smtp.connectiontimeout", "3000");
            mail.getJavaMailProperties().setProperty("mail.smtp.timeout", "3000");
            mail.getJavaMailProperties().setProperty("mail.smtp.writetimeout", "3000");
            SmtpAccountMailSender sender = new SmtpAccountMailSender(mail, new MailDeliveryProperties(
                    "smtp", "sender@example.invalid", "https://example.invalid/verify-email", "https://example.invalid/reset-password"));
            if (reset) sender.sendPasswordReset("recipient@example.invalid", "test-token");
            else sender.sendVerification("recipient@example.invalid", "test-token");
            String raw = received.get(8, TimeUnit.SECONDS);
            MimeMessage message = new MimeMessage(Session.getInstance(new Properties()),
                    new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)));
            assertThat(message.getAllRecipients()[0].toString()).isEqualTo("recipient@example.invalid");
            assertThat(message.getContent().toString()).contains(
                    reset ? "/reset-password#token=test-token" : "/verify-email#token=test-token");
            assertThat(message.getSubject()).contains(reset ? "비밀번호 재설정" : "이메일 인증");
        }
    }

    private static void reply(BufferedWriter output, String response) throws IOException {
        output.write(response + "\r\n");
        output.flush();
    }
}
