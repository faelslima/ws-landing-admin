package br.eti.logos.service.email;

public interface EmailService {

    void send(String to, String subject, String htmlContent);

    void enviarBoasVindas(String to, String nomeIgreja, String nomeResponsavel, String lang);
}
