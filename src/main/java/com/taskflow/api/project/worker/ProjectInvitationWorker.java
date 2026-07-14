package com.taskflow.api.project.worker;

import com.taskflow.api.project.dto.ProjectInvitationEmailEvent;
import com.taskflow.api.shared.config.RabbitMQConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectInvitationWorker {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine; // Inyectamos el motor de plantillas

    // Tomamos el correo remitente desde el properties/yml para no "quemarlo" en el código
    @Value("${spring.mail.username}")
    private String senderEmail;

    @RabbitListener(queues = RabbitMQConfig.INVITATION_QUEUE)
    public void processInvitationEmail(ProjectInvitationEmailEvent event) {
        log.info("Procesando envío de invitación para: {}", event.guestEmail());

        try {
            // VERSIÓN 1: TEXTO PLANO
            // SimpleMailMessage message = buildEmailMessage(event);
            // mailSender.send(message);
            // VERSIÓN 2: HTML CON THYMELEAF
            MimeMessage message = buildHtmlEmailMessage(event);
            mailSender.send(message);

            log.info("¡Correo enviado exitosamente a {}!", event.guestEmail());

        } catch (Exception e) {
            log.error("Error crítico enviando el correo a {}: {}", event.guestEmail(), e.getMessage());
            // Envolvemos en RuntimeException para que RabbitMQ detecte el error y pueda reintentar/descartar
            throw new RuntimeException(e);
        }
    }

    // MÉTODO NUEVO: THYMELEAF
    private MimeMessage buildHtmlEmailMessage(ProjectInvitationEmailEvent event) throws MessagingException {
        // 1. Creamos un MimeMessage, que es el formato que soporta HTML en lugar de texto simple
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        // El 'true' indica que es un mensaje multipart (soporta HTML, imágenes, adjuntos)
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(senderEmail);
        helper.setTo(event.guestEmail());
        helper.setSubject("¡Te han invitado al proyecto: " + event.projectName() + "!");

        // 2. Preparamos las variables que inyectaremos en los "huecos" del HTML
        Context context = new Context();
        context.setVariable("projectName", event.projectName());
        context.setVariable("role", event.role());

        String acceptanceUrl = "http://localhost:3000/invites/accept?token=" + event.token();
        context.setVariable("acceptanceUrl", acceptanceUrl);

        // 3. Compilamos el archivo HTML que está en src/main/resources/templates/mail/project-invitation.html
        String htmlContent = templateEngine.process("mail/project-invitation", context);

        // 4. Insertamos el HTML compilado al correo. El 'true' final le avisa a Gmail que renderice HTML.
        helper.setText(htmlContent, true);

        return mimeMessage;
    }

    // MÉTODO: TEXTO PLANO
    /*
    private SimpleMailMessage buildEmailMessage(ProjectInvitationEmailEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(event.guestEmail());
        message.setSubject("¡Te han invitado al proyecto: " + event.projectName() + "!");

        String acceptanceUrl = "http://localhost:3000/invites/accept?token=" + event.token();

        String body = "Hola,\n\n" +
                "Te han invitado a unirte al proyecto '" + event.projectName() + "' " +
                "con el rol de " + event.role() + ".\n\n" +
                "Haz clic en el siguiente enlace para aceptar la invitación (expira en 24 horas):\n" +
                acceptanceUrl + "\n\n" +
                "Saludos,\n" +
                "El equipo de TaskFlow";

        message.setText(body);
        return message;
    }
    */
}