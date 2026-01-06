package br.com.bravvo.api.service;

public interface MailService {
    void sendVerificationCode(String toEmail, String code);
}
