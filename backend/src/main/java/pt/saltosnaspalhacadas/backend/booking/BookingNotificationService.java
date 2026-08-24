package pt.saltosnaspalhacadas.backend.booking;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BookingNotificationService {

    private static final Logger log = LoggerFactory.getLogger(BookingNotificationService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-PT"));

    private final boolean enabled;
    private final String host;
    private final int port;
    private final boolean ssl;
    private final boolean startTls;
    private final String username;
    private final String password;
    private final String from;

    public BookingNotificationService(
            @Value("${app.booking.email.enabled:false}") boolean enabled,
            @Value("${app.booking.email.smtp-host:}") String host,
            @Value("${app.booking.email.smtp-port:25}") int port,
            @Value("${app.booking.email.smtp-ssl:false}") boolean ssl,
            @Value("${app.booking.email.smtp-starttls:false}") boolean startTls,
            @Value("${app.booking.email.username:}") String username,
            @Value("${app.booking.email.password:}") String password,
            @Value("${app.booking.email.from:no-reply@saltosnaspalhacadas.pt}") String from) {
        this.enabled = enabled;
        this.host = host == null ? "" : host.trim();
        this.port = port;
        this.ssl = ssl;
        this.startTls = startTls;
        this.username = username == null ? "" : username.trim();
        this.password = password == null ? "" : password;
        this.from = from == null || from.isBlank() ? "no-reply@saltosnaspalhacadas.pt" : from.trim();
    }

    public void sendReceivedConfirmation(Booking booking) {
        if (booking.getContactEmail() == null || booking.getContactEmail().isBlank()) {
            return;
        }

        String subject = "Pedido de agendamento recebido";
        String body = """
                Olá %s,

                Recebemos o teu pedido de orçamento e agendamento para %s.

                O animador vai analisar a informação enviada e entrará em contacto contigo por email ou telemóvel.

                Obrigado,
                Saltos nas Palhaçadas
                """.formatted(
                booking.getContactName(),
                DATE_FORMATTER.format(booking.getEventDate()));

        if (!enabled || host.isBlank()) {
            log.info("Email de confirmação preparado para {} sobre o pedido {}", booking.getContactEmail(), booking.getId());
            return;
        }

        try {
            sendSmtp(booking.getContactEmail(), subject, body);
        } catch (IOException exception) {
            log.warn("Não foi possível enviar o email de confirmação do pedido {}", booking.getId(), exception);
        }
    }

    private void sendSmtp(String to, String subject, String body) throws IOException {
        Socket socket = openSocket();
        try {
            BufferedReader reader = reader(socket);
            BufferedWriter writer = writer(socket);

            expectOk(reader);
            command(writer, reader, "EHLO saltosnaspalhacadas.pt");

            if (startTls && !ssl) {
                command(writer, reader, "STARTTLS");
                socket = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(socket, host, port, true);
                reader = reader(socket);
                writer = writer(socket);
                command(writer, reader, "EHLO saltosnaspalhacadas.pt");
            }

            if (!username.isBlank()) {
                command(writer, reader, "AUTH LOGIN");
                command(writer, reader, Base64.getEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8)));
                command(writer, reader, Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8)));
            }

            command(writer, reader, "MAIL FROM:<%s>".formatted(from));
            command(writer, reader, "RCPT TO:<%s>".formatted(to));
            command(writer, reader, "DATA");
            writer.write("From: Saltos nas Palhaçadas <%s>\r\n".formatted(from));
            writer.write("To: <%s>\r\n".formatted(to));
            writer.write("Subject: %s\r\n".formatted(subject));
            writer.write("Content-Type: text/plain; charset=UTF-8\r\n");
            writer.write("\r\n");
            writer.write(body.replace("\n.", "\n..").replace("\n", "\r\n"));
            writer.write("\r\n.\r\n");
            writer.flush();
            expectOk(reader);
            command(writer, reader, "QUIT");
        } finally {
            socket.close();
        }
    }

    private Socket openSocket() throws IOException {
        if (ssl) {
            return SSLSocketFactory.getDefault().createSocket(host, port);
        }
        return new Socket(host, port);
    }

    private static BufferedReader reader(Socket socket) throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    }

    private static BufferedWriter writer(Socket socket) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    private static void command(BufferedWriter writer, BufferedReader reader, String command) throws IOException {
        writer.write(command);
        writer.write("\r\n");
        writer.flush();
        expectOk(reader);
    }

    private static void expectOk(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("Resposta vazia do servidor SMTP");
        }
        String code = line.length() >= 3 ? line.substring(0, 3) : line;
        while (line.length() > 3 && line.charAt(3) == '-') {
            line = reader.readLine();
            if (line == null) {
                throw new IOException("Resposta SMTP incompleta");
            }
        }
        if (!code.startsWith("2") && !code.startsWith("3")) {
            throw new IOException("Resposta SMTP inesperada: " + line);
        }
    }
}
