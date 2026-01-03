package br.com.bravvo.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Protocolo para rastreabilidade/auditoria.
 *
 * Tabela: protocolos - codigo: identificador humano único (VARCHAR 30) - tipo:
 * no banco é ENUM (ex: 'agendamento'). No MVP usamos String. - dadosJson:
 * snapshot (LONGTEXT) opcional
 */
@Entity
@Table(name = "protocolos")
public class Protocolo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "codigo", nullable = false, length = 30, unique = true)
	private String codigo;

	@Column(name = "tipo", nullable = false, length = 30)
	private String tipo;

	@Column(name = "dados_json", columnDefinition = "longtext")
	private String dadosJson;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public String getDadosJson() {
		return dadosJson;
	}

	public void setDadosJson(String dadosJson) {
		this.dadosJson = dadosJson;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
