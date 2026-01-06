package br.com.bravvo.api.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class SmtpMailService implements MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public SmtpMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendVerificationCode(String toEmail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            // ✅ IMPORTANTE: definir remetente
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Confirmação de e-mail - Bravvo");
            helper.setText("Seu código de confirmação é: " + code, false);

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao enviar e-mail de confirmação.", e);
        }
    }
}

