package br.com.bravvo.api.dto.salao;

public class SalaoConfirmEmailResponseDTO {
	private boolean success = true;
	private String message;

	public SalaoConfirmEmailResponseDTO(String message) {
		this.message = message;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getMessage() {
		return message;
	}
}
