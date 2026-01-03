package br.com.bravvo.api.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO utilizado para atualização dos dados do PRÓPRIO usuário autenticado.
 *
 * Endpoint: PUT /api/auth/me
 *
 * Regras de negócio: - Atualiza apenas nome, telefone e senha - Email, perfil e
 * status (ativo) NÃO podem ser alterados por este endpoint - Não há validação
 * por perfil (ADMIN, FUNCIONARIO, CLIENTE), pois o usuário só pode alterar os
 * próprios dados
 */
public class UserMeUpdateRequestDTO {

	@NotBlank(message = "Nome é obrigatório")
	@Size(min = 2, max = 120, message = "Nome deve ter entre 2 e 120 caracteres")
	private String nome;

	@Size(max = 30, message = "Telefone deve ter no máximo 30 caracteres")
	private String telefone;

	/**
	 * Senha opcional. Caso seja enviada, o sistema irá: - gerar o hash - substituir
	 * o senhaHash atual
	 */
	@Size(min = 6, max = 72, message = "Senha deve ter entre 6 e 72 caracteres")
	private String senha;

	// getters e setters
	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
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
}
