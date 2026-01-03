package br.com.bravvo.api.dto.servico;

import br.com.bravvo.api.enums.StatusServico;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * DTO de criação de Serviço.
 *
 * Endpoint: POST /api/servicos
 *
 * Regras: - nome, duracaoMin e valor são obrigatórios - descricao é opcional -
 * status é opcional (default: ATIVO no service/entity)
 */
public class ServicoCreateRequestDTO {

	@NotBlank(message = "Nome é obrigatório")
	@Size(min = 2, max = 120, message = "Nome deve ter entre 2 e 120 caracteres")
	private String nome;

	@Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
	private String descricao;

	@NotNull(message = "Duração (min) é obrigatória")
	@Min(value = 1, message = "Duração deve ser no mínimo 1 minuto")
	@Max(value = 1440, message = "Duração deve ser no máximo 1440 minutos")
	private Integer duracaoMin;

	@NotNull(message = "Valor é obrigatório")
	@DecimalMin(value = "0.00", inclusive = false, message = "Valor deve ser maior que 0")
	@Digits(integer = 8, fraction = 2, message = "Valor inválido (máx: 8 dígitos + 2 decimais)")
	private BigDecimal valor;

	/**
	 * Opcional. Se vier nulo, o service/entity assume ATIVO.
	 */
	private StatusServico status;

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public Integer getDuracaoMin() {
		return duracaoMin;
	}

	public void setDuracaoMin(Integer duracaoMin) {
		this.duracaoMin = duracaoMin;
	}

	public BigDecimal getValor() {
		return valor;
	}

	public void setValor(BigDecimal valor) {
		this.valor = valor;
	}

	public StatusServico getStatus() {
		return status;
	}

	public void setStatus(StatusServico status) {
		this.status = status;
	}
}
