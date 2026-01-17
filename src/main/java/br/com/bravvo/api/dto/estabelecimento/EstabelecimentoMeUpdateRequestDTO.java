package br.com.bravvo.api.dto.estabelecimento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EstabelecimentoMeUpdateRequestDTO {

	@NotBlank(message = "Nome é obrigatório.")
	@Size(max = 120, message = "Nome deve ter no máximo 120 caracteres.")
	private String nome;

	@Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres.")
	private String telefone;

	@Size(max = 60, message = "Ramo de atuação deve ter no máximo 60 caracteres.")
	private String ramoAtuacao;

	@Size(max = 255, message = "Endereço deve ter no máximo 255 caracteres.")
	private String endereco;

	@Size(max = 20, message = "Número deve ter no máximo 20 caracteres.")
	private String numero;

	@Size(max = 100, message = "Bairro deve ter no máximo 100 caracteres.")
	private String bairro;

	@Size(max = 100, message = "Estado deve ter no máximo 100 caracteres.")
	private String estado;

	@Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres.")
	private String cidade;

	public String getNome() {
		return nome;
	}

	public String getTelefone() {
		return telefone;
	}

	public String getRamoAtuacao() {
		return ramoAtuacao;
	}

	public String getEndereco() {
		return endereco;
	}

	public String getNumero() {
		return numero;
	}

	public String getBairro() {
		return bairro;
	}

	public String getEstado() {
		return estado;
	}

	public String getCidade() {
		return cidade;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public void setTelefone(String telefone) {
		this.telefone = telefone;
	}

	public void setRamoAtuacao(String ramoAtuacao) {
		this.ramoAtuacao = ramoAtuacao;
	}

	public void setEndereco(String endereco) {
		this.endereco = endereco;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	public void setBairro(String bairro) {
		this.bairro = bairro;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}

	public void setCidade(String cidade) {
		this.cidade = cidade;
	}
}
