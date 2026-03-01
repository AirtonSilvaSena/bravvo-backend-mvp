package br.com.bravvo.api.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity Agendamento - alinhada com o schema atual do banco.
 *
 * Tabela: agendamentos
 *
 * Observações do schema: - estabelecimento_id é DEFAULT NULL (nullable). -
 * protocolo é NOT NULL e possui unique por (estabelecimento_id, protocolo). -
 * tipo e status são varchar(50). - created_at e updated_at possuem defaults no
 * banco (current_timestamp).
 */
@Entity
@Table(name = "agendamentos", uniqueConstraints = {
		@UniqueConstraint(name = "uk_ag_estabelecimento_protocolo", columnNames = { "estabelecimento_id",
				"protocolo" }) }, indexes = {
						@Index(name = "idx_ag_funcionario_inicio", columnList = "funcionario_id,inicio"),
						@Index(name = "idx_ag_status_inicio", columnList = "status,inicio"),
						@Index(name = "idx_ag_cliente_id", columnList = "cliente_id"),
						@Index(name = "idx_ag_estabelecimento_inicio", columnList = "estabelecimento_id,inicio"),
						@Index(name = "idx_ag_estabelecimento_funcionario_inicio", columnList = "estabelecimento_id,funcionario_id,inicio"),
						@Index(name = "idx_ag_estabelecimento_status_inicio", columnList = "estabelecimento_id,status,inicio") })
public class Agendamento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * BD: bigint unsigned DEFAULT NULL
	 */
	@Column(name = "estabelecimento_id")
	private Long estabelecimentoId;

	/**
	 * BD: varchar(30) NOT NULL
	 */
	@Column(name = "protocolo", nullable = false, length = 30)
	private String protocolo;

	/**
	 * BD: varchar(50) NOT NULL DEFAULT 'hora_marcada'
	 */
	@Column(name = "tipo", nullable = false, length = 50)
	private String tipo = "hora_marcada";

	/**
	 * BD: bigint unsigned NOT NULL
	 */
	@Column(name = "servico_id", nullable = false)
	private Long servicoId;

	/**
	 * BD: bigint unsigned NOT NULL
	 */
	@Column(name = "funcionario_id", nullable = false)
	private Long funcionarioId;

	/**
	 * BD: bigint unsigned DEFAULT NULL
	 */
	@Column(name = "cliente_id")
	private Long clienteId;

	/**
	 * BD: varchar(120) DEFAULT NULL
	 */
	@Column(name = "cliente_nome", length = 120)
	private String clienteNome;

	/**
	 * BD: varchar(30) DEFAULT NULL
	 */
	@Column(name = "cliente_telefone", length = 30)
	private String clienteTelefone;

	/**
	 * BD: varchar(180) DEFAULT NULL
	 */
	@Column(name = "cliente_email", length = 180)
	private String clienteEmail;

	/**
	 * BD: datetime NOT NULL
	 */
	@Column(name = "inicio", nullable = false)
	private LocalDateTime inicio;

	/**
	 * BD: datetime NOT NULL
	 */
	@Column(name = "fim", nullable = false)
	private LocalDateTime fim;

	/**
	 * BD: varchar(50) NOT NULL DEFAULT 'pendente'
	 */
	@Column(name = "status", nullable = false, length = 50)
	private String status = "pendente";

	/**
	 * BD: varchar(500) DEFAULT NULL
	 */
	@Column(name = "observacoes", length = 500)
	private String observacoes;

	/**
	 * BD: datetime NOT NULL DEFAULT current_timestamp()
	 */
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/**
	 * BD: datetime NOT NULL DEFAULT current_timestamp() ON UPDATE
	 * current_timestamp()
	 */
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		// Mantém defaults do Java (coerente com defaults do BD)
		if (this.tipo == null || this.tipo.isBlank())
			this.tipo = "hora_marcada";
		if (this.status == null || this.status.isBlank())
			this.status = "pendente";

		// Se não vier setado, preenche (o BD também preenche por default)
		LocalDateTime now = LocalDateTime.now();
		if (this.createdAt == null)
			this.createdAt = now;
		if (this.updatedAt == null)
			this.updatedAt = now;
	}

	@PreUpdate
	protected void onUpdate() {
		// O BD atualiza por ON UPDATE; aqui mantemos atualizado também.
		this.updatedAt = LocalDateTime.now();
	}

	// getters e setters

	public Long getId() {
		return id;
	}

	public Long getEstabelecimentoId() {
		return estabelecimentoId;
	}

	public void setEstabelecimentoId(Long estabelecimentoId) {
		this.estabelecimentoId = estabelecimentoId;
	}

	public String getProtocolo() {
		return protocolo;
	}

	public void setProtocolo(String protocolo) {
		this.protocolo = protocolo;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public Long getServicoId() {
		return servicoId;
	}

	public void setServicoId(Long servicoId) {
		this.servicoId = servicoId;
	}

	public Long getFuncionarioId() {
		return funcionarioId;
	}

	public void setFuncionarioId(Long funcionarioId) {
		this.funcionarioId = funcionarioId;
	}

	public Long getClienteId() {
		return clienteId;
	}

	public void setClienteId(Long clienteId) {
		this.clienteId = clienteId;
	}

	public String getClienteNome() {
		return clienteNome;
	}

	public void setClienteNome(String clienteNome) {
		this.clienteNome = clienteNome;
	}

	public String getClienteTelefone() {
		return clienteTelefone;
	}

	public void setClienteTelefone(String clienteTelefone) {
		this.clienteTelefone = clienteTelefone;
	}

	public String getClienteEmail() {
		return clienteEmail;
	}

	public void setClienteEmail(String clienteEmail) {
		this.clienteEmail = clienteEmail;
	}

	public LocalDateTime getInicio() {
		return inicio;
	}

	public void setInicio(LocalDateTime inicio) {
		this.inicio = inicio;
	}

	public LocalDateTime getFim() {
		return fim;
	}

	public void setFim(LocalDateTime fim) {
		this.fim = fim;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getObservacoes() {
		return observacoes;
	}

	public void setObservacoes(String observacoes) {
		this.observacoes = observacoes;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}