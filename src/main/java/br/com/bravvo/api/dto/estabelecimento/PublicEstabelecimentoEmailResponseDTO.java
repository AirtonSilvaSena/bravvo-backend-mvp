package br.com.bravvo.api.dto.estabelecimento;

/**
 * Resposta pública para resolver quais estabelecimentos estão associados a um e-mail.
 * Usado no fluxo de login/onboarding quando o usuário informa o e-mail e precisa escolher o estabelecimento (slug).
 */
public class PublicEstabelecimentoEmailResponseDTO {

    private Long id;
    private String nome;
    private String slug;

    public PublicEstabelecimentoEmailResponseDTO() {}

    public PublicEstabelecimentoEmailResponseDTO(Long id, String nome, String slug) {
        this.id = id;
        this.nome = nome;
        this.slug = slug;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
}