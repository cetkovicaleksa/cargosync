package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.config.properties.AppProperties;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final AppProperties appProperties;

    public void sendActivationEmail(String to, String firstName, String token) {
        var ctx = new Context();
        ctx.setVariable("firstName", firstName);
        ctx.setVariable("activationLink",
                appProperties.frontendUrl() + "/auth/activate?token=" + token);

        String html = templateEngine.process("email/activation", ctx);

        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Activate your CargoSync account");
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Activation email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send activation email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send activation email", e);
        }
    }

}
