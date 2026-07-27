package balance.users.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.url:https://lospolloshermanos.org}")
    private String appUrl;

    @Value("${app.from.email:}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendWelcomeEmail(String toEmail, String fullName, String username, String tempPassword) {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("MAIL_USERNAME / APP_FROM_EMAIL no configurado — email no enviado para {}", username);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("Los Pollos Hermanos <" + fromEmail + ">");
            helper.setTo(toEmail);
            helper.setSubject("Bienvenido a Los Pollos Hermanos — Tu acceso al sistema");
            helper.setText(buildWelcomeHtml(fullName, username, tempPassword), true);
            mailSender.send(message);
            log.info("Email de bienvenida enviado a {} ({})", username, toEmail);
        } catch (Exception e) {
            log.error("Error al enviar email a {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildWelcomeHtml(String fullName, String username, String tempPassword) {
        return """
            <div style="font-family:sans-serif;max-width:520px;margin:0 auto;padding:24px">
              <img src="https://pub-7e31005d201d4d34894758b2b1d00d9a.r2.dev/logo.png"
                   alt="Los Pollos Hermanos" style="height:60px;margin-bottom:24px"/>
              <h2 style="color:#1C1814">Bienvenido, %s</h2>
              <p>Tu cuenta fue creada en el sistema de <strong>Los Pollos Hermanos</strong>.</p>
              <div style="background:#FDF4E3;border:1px solid #E0A830;border-radius:8px;padding:16px;margin:20px 0">
                <p style="margin:0 0 8px"><strong>Usuario:</strong> %s</p>
                <p style="margin:0 0 8px"><strong>Contraseña temporal:</strong>
                  <code style="background:#1C1814;color:#F0BA40;padding:2px 8px;border-radius:4px">%s</code>
                </p>
              </div>
              <p>Al ingresar por primera vez deberás cambiar tu contraseña.</p>
              <a href="%s" style="display:inline-block;background:#C98A1A;color:#fff;padding:12px 24px;
                border-radius:8px;text-decoration:none;font-weight:700;margin:8px 0">
                Ingresar al sistema
              </a>
              <p style="color:#857F77;font-size:12px;margin-top:24px">
                Si no esperabas este email, podés ignorarlo.
              </p>
            </div>
            """.formatted(fullName, username, tempPassword, appUrl);
    }
}
