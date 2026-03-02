package br.com.bravvo.api.dto.agendamento;


public class PublicEstabelecimentoEmailResponseDTO {

	private String estabelecimento;
	private String slug;


	public PublicEstabelecimentoEmailResponseDTO() {
	}

	public PublicEstabelecimentoEmailResponseDTO(String estabelecimento, String slug) {
		this.estabelecimento = estabelecimento;
		this.slug = slug;

	}

	public String getEstabelecimento() {
		return estabelecimento;
	}

	public void setEstabelecimento(String estabelecimento) {
		this.estabelecimento = estabelecimento;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

}
