package br.com.bravvo.api.dto.auth;

import br.com.bravvo.api.enums.PerfilUser;

public class MeResponseDTO {

	private Long id;
	private String nome;
	private String email;
	private String telefone;
	private PerfilUser perfil;

	public MeResponseDTO(Long id, String nome, String email, String telefone, PerfilUser perfil) {
		this.id = id;
		this.nome = nome;
		this.email = email;
		this.telefone = telefone;
		this.perfil = perfil;
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public String getEmail() {
		return email;
	}

	public String getTelefone() {
		return telefone;
	}

	public PerfilUser getPerfil() {
		return perfil;
	}
}
