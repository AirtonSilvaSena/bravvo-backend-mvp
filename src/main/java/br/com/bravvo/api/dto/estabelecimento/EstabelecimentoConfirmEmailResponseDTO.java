package br.com.bravvo.api.dto.estabelecimento;

public class EstabelecimentoConfirmEmailResponseDTO {
	private boolean success = true;
	private String message;

	public EstabelecimentoConfirmEmailResponseDTO(String message) {
		this.message = message;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getMessage() {
		return message;
	}
}
