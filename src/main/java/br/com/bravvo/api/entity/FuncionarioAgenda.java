package br.com.bravvo.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Agenda semanal do funcionário.
 *
 * Tabela: funcionario_agenda PK: (funcionario_id, dia_semana)
 *
 * Regras do MVP: - Suporta até 2 janelas por dia: - janela1: inicio_1 -> fim_1
 * - janela2 (opcional): inicio_2 -> fim_2 - O "almoço" é o intervalo entre
 * fim_1 e inicio_2 - Se ativo=false, o funcionário não trabalha nesse dia
 */
@Entity
@Table(name = "funcionario_agenda")
public class FuncionarioAgenda {

	@EmbeddedId
	private FuncionarioAgendaId id;

	@Column(name = "inicio_1")
	private LocalTime inicio1;

	@Column(name = "fim_1")
	private LocalTime fim1;

	@Column(name = "inicio_2")
	private LocalTime inicio2;

	@Column(name = "fim_2")
	private LocalTime fim2;

	@Column(nullable = false)
	private Boolean ativo = true;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
		if (this.ativo == null)
			this.ativo = true;
	}

	public FuncionarioAgendaId getId() {
		return id;
	}

	public void setId(FuncionarioAgendaId id) {
		this.id = id;
	}

	public LocalTime getInicio1() {
		return inicio1;
	}

	public void setInicio1(LocalTime inicio1) {
		this.inicio1 = inicio1;
	}

	public LocalTime getFim1() {
		return fim1;
	}

	public void setFim1(LocalTime fim1) {
		this.fim1 = fim1;
	}

	public LocalTime getInicio2() {
		return inicio2;
	}

	public void setInicio2(LocalTime inicio2) {
		this.inicio2 = inicio2;
	}

	public LocalTime getFim2() {
		return fim2;
	}

	public void setFim2(LocalTime fim2) {
		this.fim2 = fim2;
	}

	public Boolean getAtivo() {
		return ativo;
	}

	public void setAtivo(Boolean ativo) {
		this.ativo = ativo;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
