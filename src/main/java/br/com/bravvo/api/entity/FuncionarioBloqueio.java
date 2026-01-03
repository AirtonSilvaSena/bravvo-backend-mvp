package br.com.bravvo.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Bloqueios pontuais do funcionário.
 *
 * Tabela: funcionario_bloqueios
 *
 * Permite bloquear: - dia inteiro (start=00:00, end=00:00 do dia seguinte) -
 * intervalo específico (ex: 13:00 até 17:00)
 */
@Entity
@Table(name = "funcionario_bloqueios")
public class FuncionarioBloqueio {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "funcionario_id", nullable = false)
	private Long funcionarioId;

	@Column(name = "start_dt", nullable = false)
	private LocalDateTime startDt;

	@Column(name = "end_dt", nullable = false)
	private LocalDateTime endDt;

	@Column(length = 255)
	private String motivo;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Long getFuncionarioId() {
		return funcionarioId;
	}

	public void setFuncionarioId(Long funcionarioId) {
		this.funcionarioId = funcionarioId;
	}

	public LocalDateTime getStartDt() {
		return startDt;
	}

	public void setStartDt(LocalDateTime startDt) {
		this.startDt = startDt;
	}

	public LocalDateTime getEndDt() {
		return endDt;
	}

	public void setEndDt(LocalDateTime endDt) {
		this.endDt = endDt;
	}

	public String getMotivo() {
		return motivo;
	}

	public void setMotivo(String motivo) {
		this.motivo = motivo;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
