package br.com.bravvo.api.dto.estabelecimento;

public class EstabelecimentoPreRegisterResponseDTO {
	private boolean success = true;
	private String message;

	public EstabelecimentoPreRegisterResponseDTO(String message) {
		this.message = message;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getMessage() {
		return message;
	}
}
