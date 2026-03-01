package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service;

import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.config.properties.AppProperties;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.order.model.Order;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final AppProperties appProperties;

    public void sendInvoiceEmail(String to, String firstName, Order order, byte[] pdfBytes) {
        var ctx = new Context();
        ctx.setVariable("firstName", firstName);
        ctx.setVariable("orderId", order.getId());
        ctx.setVariable("order", order);

        String html = templateEngine.process("email/invoice", ctx);

        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Your CargoSync order #" + order.getId() + " confirmed");
            helper.setText(html, true);
            helper.addAttachment(
                    "invoice-" + order.getId() + ".pdf",
                    new ByteArrayDataSource(pdfBytes, "application/pdf")
            );
            mailSender.send(message);
            log.info("Invoice email sent to {} for order {}", to, order.getId());
        } catch (Exception e) {
            log.error("Failed to send invoice email to {} for order {}: {}", to, order.getId(), e.getMessage());
            throw new RuntimeException("Failed to send invoice email", e);
        }
    }

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
