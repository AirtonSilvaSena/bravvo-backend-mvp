package br.com.bravvo.api.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Agendamento (MVP - hora marcada).
 *
 * Tabela: agendamentos
 *
 * MVP (decisões): - Sem relacionamentos JPA (evita cascade/fetch). - status
 * como String (sem enum/converter), valores no DB:
 * 'pendente','confirmado','em_atendimento','concluido','cancelado' - cliente
 * pode ser: - cadastrado (cliente_id != null) - visitante
 * (cliente_nome/telefone/email)
 *
 * Regras: - protocolo é único. - inicio/fim são obrigatórios.
 */
@Entity
@Table(name = "agendamentos")
public class Agendamento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "protocolo", nullable = false, length = 30, unique = true)
	private String protocolo;

	/**
	 * MVP: banco tem enum('hora_marcada'), mas agora está varchar? (ok). Vamos
	 * manter o default lógico "hora_marcada".
	 */
	@Column(name = "tipo", nullable = false, length = 30)
	private String tipo = "hora_marcada";

	@Column(name = "servico_id", nullable = false)
	private Long servicoId;

	@Column(name = "funcionario_id", nullable = false)
	private Long funcionarioId;

	@Column(name = "cliente_id")
	private Long clienteId;

	@Column(name = "cliente_nome", length = 120)
	private String clienteNome;

	@Column(name = "cliente_telefone", length = 30)
	private String clienteTelefone;

	@Column(name = "cliente_email", length = 180)
	private String clienteEmail;

	@Column(name = "inicio", nullable = false)
	private LocalDateTime inicio;

	@Column(name = "fim", nullable = false)
	private LocalDateTime fim;

	/**
	 * MVP: String para não mexer em enum/converter agora. Valores:
	 * 'pendente','confirmado','em_atendimento','concluido','cancelado'
	 */
	@Column(name = "status", nullable = false, length = 30)
	private String status = "pendente";

	@Column(name = "observacoes", length = 500)
	private String observacoes;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		var now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;

		if (this.tipo == null)
			this.tipo = "hora_marcada";
		if (this.status == null)
			this.status = "pendente";
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
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
