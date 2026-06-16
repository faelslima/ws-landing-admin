package br.eti.logos.service.email;

public interface EmailService {

    void send(String to, String subject, String htmlContent);

    void enviarBoasVindas(String to, String nomeIgreja, String nomeResponsavel, String lang);

    void enviarFalhaPagamento(String to, String nomeResponsavel, String planoNome, String retryUrl, String lang);

    void enviarCancelamentoEstorno(String to, String nomeResponsavel, String planoNome, String retryUrl, String lang);

    void enviarReativacao(String to, String nomeResponsavel, String planoNome, String lang);
}
