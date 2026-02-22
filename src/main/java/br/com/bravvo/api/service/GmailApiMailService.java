package br.com.bravvo.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
public class GmailApiMailService implements MailService {

    private final GmailTokenService tokenService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gmail.from}")
    private String fromEmail;

    public GmailApiMailService(GmailTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public void sendVerificationCode(String toEmail, String code) {
        try {
            String subject = "Confirmação de e-mail - Bravvo";

            String text =
                    "Bravvo - Código de verificação: " + code + "\n\n" +
                    "Use este código para confirmar seu cadastro.\n" +
                    "Válido por poucos minutos.\n\n" +
                    "Bem-vindo ao Bravvo 👋\n\n" +
                    "Estamos felizes em ter você conosco.\n\n" +
                    "O Bravvo foi desenvolvido para simplificar a gestão do seu estabelecimento, " +
                    "centralizando agendamentos, equipe e atendimento em um único sistema.\n\n" +
                    "Se você não solicitou este acesso, basta desconsiderar este e-mail.\n\n" +
                    "— Equipe Bravvo";

            // RFC 5322 (email raw)
            String rawEmail =
                    "From: " + fromEmail + "\r\n" +
                    "To: " + toEmail + "\r\n" +
                    "Subject: " + subject + "\r\n" +
                    "MIME-Version: 1.0\r\n" +
                    "Content-Type: text/plain; charset=UTF-8\r\n" +
                    "\r\n" +
                    text;

            // Gmail API exige base64url (sem + / e sem =)
            String encoded = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(rawEmail.getBytes(StandardCharsets.UTF_8));

            String url = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(tokenService.getAccessToken());

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("raw", encoded), headers);

            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);

            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Falha ao enviar e-mail via Gmail API. Status: " + resp.getStatusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException("Falha ao enviar e-mail de confirmação (Gmail API).", e);
        }
    }
}