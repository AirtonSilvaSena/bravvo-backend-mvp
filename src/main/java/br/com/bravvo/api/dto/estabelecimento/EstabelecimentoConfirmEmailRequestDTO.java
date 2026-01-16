package br.com.bravvo.api.dto.estabelecimento;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EstabelecimentoConfirmEmailRequestDTO {

	@NotBlank
	@Email
	private String email;

	@NotBlank
	@Size(min = 4, max = 10)
	private String codigo;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}
}
