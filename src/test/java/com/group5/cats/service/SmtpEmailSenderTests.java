package com.group5.cats.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import com.group5.cats.model.NotificationOutbox;

class SmtpEmailSenderTests {
    @Test
    @SuppressWarnings("unchecked")
    void sendsToLocalSmtpServerWithSubjectBodyAndLoginLink() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            server.setSoTimeout(10000);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<String> received = executor.submit(() -> {
                    try (Socket socket = server.accept()) {
                        socket.setSoTimeout(10000);
                        var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                        var writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                        writer.print("220 localhost test SMTP\r\n"); writer.flush();
                        StringBuilder message = new StringBuilder();
                        boolean data = false;
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (data) {
                                if (line.equals(".")) {
                                    data = false;
                                    writer.print("250 accepted\r\n"); writer.flush();
                                } else message.append(line).append("\n");
                            } else if (line.equals("DATA")) {
                                data = true;
                                writer.print("354 send message\r\n"); writer.flush();
                            } else if (line.equals("QUIT")) {
                                writer.print("221 bye\r\n"); writer.flush(); break;
                            } else {
                                writer.print("250 OK\r\n"); writer.flush();
                            }
                        }
                        return message.toString();
                    }
                });
                JavaMailSenderImpl mail = new JavaMailSenderImpl();
                mail.setHost(InetAddress.getLoopbackAddress().getHostAddress());
                mail.setPort(server.getLocalPort());
                mail.getJavaMailProperties().setProperty("mail.smtp.connectiontimeout", "5000");
                mail.getJavaMailProperties().setProperty("mail.smtp.timeout", "5000");
                ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
                when(provider.getIfAvailable()).thenReturn(mail);
                NotificationOutbox notification = new NotificationOutbox();
                notification.setRecipientEmailSnapshot("manager@example.test");
                notification.setSubject("CATS application submitted");
                notification.setBody("Course: Spring\nLog in: http://localhost:8080/employee/login");
                new SmtpEmailSender(provider, "cats@example.test").send(notification);
                String message = received.get(10, TimeUnit.SECONDS);
                assertTrue(message.contains("To: manager@example.test"));
                assertTrue(message.contains("Subject: CATS application submitted"));
                assertTrue(message.contains("http://localhost:8080/employee/login"));
            } finally {
                executor.shutdownNow();
            }
        }
    }
}
