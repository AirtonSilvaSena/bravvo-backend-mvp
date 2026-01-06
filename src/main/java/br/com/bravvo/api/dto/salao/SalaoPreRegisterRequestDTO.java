package br.com.bravvo.api.dto.salao;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SalaoPreRegisterRequestDTO {

	@NotBlank
	@Size(max = 120)
	private String nome;

	@NotBlank
	@Email
	@Size(max = 180)
	private String email;

	@Size(max = 30)
	private String telefone;

	@NotBlank
	@Size(min = 6, max = 60)
	private String senha;

	@NotBlank
	@Size(min = 3, max = 60)
	private String slug;

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getTelefone() {
		return telefone;
	}

	public void setTelefone(String telefone) {
		this.telefone = telefone;
	}

	public String getSenha() {
		return senha;
	}

	public void setSenha(String senha) {
		this.senha = senha;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}
}
