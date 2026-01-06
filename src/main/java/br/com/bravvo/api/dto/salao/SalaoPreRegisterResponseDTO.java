package br.com.bravvo.api.dto.salao;

public class SalaoPreRegisterResponseDTO {
	private boolean success = true;
	private String message;

	public SalaoPreRegisterResponseDTO(String message) {
		this.message = message;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getMessage() {
		return message;
	}
}
