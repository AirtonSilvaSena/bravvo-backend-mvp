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
            String subjectEncoded = encodeSubjectRFC2047(subject);

            String text =
                    "Seu código Bravvo: " + code + "\n" +
                    "Válido por 10 minutos.\n\n" +

                    "Digite este código para confirmar seu e-mail.\n\n" +

                    "Por segurança, não compartilhe este código.\n" +
                    "Se você não solicitou, ignore esta mensagem.\n\n" +

                    "— Equipe Bravvo";

            String bodyQP = toQuotedPrintable(text);

            String rawEmail =
                    "From: " + fromEmail + "\r\n" +
                    "To: " + toEmail + "\r\n" +
                    "Subject: " + subjectEncoded + "\r\n" +
                    "MIME-Version: 1.0\r\n" +
                    "Content-Type: text/plain; charset=UTF-8\r\n" +
                    "Content-Transfer-Encoding: quoted-printable\r\n" +
                    "Content-Language: pt-BR\r\n" +
                    "\r\n" +
                    bodyQP;

            // Gmail exige Base64 URL-safe (sem padding)
            String encoded = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(rawEmail.getBytes(StandardCharsets.UTF_8));

            String url = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(tokenService.getAccessToken());

            HttpEntity<Map<String, String>> entity =
                    new HttpEntity<>(Map.of("raw", encoded), headers);

            ResponseEntity<String> resp =
                    restTemplate.postForEntity(url, entity, String.class);

            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException(
                        "Falha ao enviar e-mail via Gmail API. Status: "
                                + resp.getStatusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Falha ao enviar e-mail de confirmação (Gmail API).",
                    e
            );
        }
    }

    /**
     * Codifica o Subject em UTF-8 conforme RFC 2047
     * Formato: =?UTF-8?B?<base64>?=
     */
    private String encodeSubjectRFC2047(String subject) {
        String base64 = Base64.getEncoder()
                .encodeToString(subject.getBytes(StandardCharsets.UTF_8));
        return "=?UTF-8?B?" + base64 + "?=";
    }

    /**
     * Converte texto UTF-8 para quoted-printable.
     * Garante compatibilidade total com clientes como Outlook.
     */
    private String toQuotedPrintable(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        int lineLen = 0;

        for (byte b : bytes) {
            int c = b & 0xFF;

            if (c == '\r') continue;

            if (c == '\n') {
                sb.append("\r\n");
                lineLen = 0;
                continue;
            }

            String out;

            boolean safe =
                    (c >= 33 && c <= 60) ||
                    (c >= 62 && c <= 126) ||
                    c == 9 || c == 32;

            if (safe) {
                out = String.valueOf((char) c);
            } else {
                out = String.format("=%02X", c);
            }

            // Soft line break para manter limite RFC (76 chars)
            if (lineLen + out.length() > 73) {
                sb.append("=\r\n");
                lineLen = 0;
            }

            sb.append(out);
            lineLen += out.length();
        }

        return sb.toString();
    }
}