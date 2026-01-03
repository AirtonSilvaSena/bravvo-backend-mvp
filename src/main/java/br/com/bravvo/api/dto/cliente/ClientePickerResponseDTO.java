package br.com.bravvo.api.dto.cliente;

/**
 * DTO de resposta para seleção de cliente pelo FUNCIONÁRIO.
 *
 * Usado em: GET /api/clientes
 *
 * Regras: - Expor apenas o necessário para localizar o cliente e confirmar
 * seleção. - NÃO expor campos sensíveis.
 */
public class ClientePickerResponseDTO {

	private Long id;
	private String nome;
	private String telefone;
	private String email;

	public ClientePickerResponseDTO() {
	}

	public ClientePickerResponseDTO(Long id, String nome, String telefone, String email) {
		this.id = id;
		this.nome = nome;
		this.telefone = telefone;
		this.email = email;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
