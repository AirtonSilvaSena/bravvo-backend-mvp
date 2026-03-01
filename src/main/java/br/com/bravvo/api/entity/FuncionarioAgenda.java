package br.com.bravvo.api.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Agenda semanal do funcionário.
 *
 * Tabela: funcionario_agenda
 * PK: (funcionario_id, dia_semana)
 *
 * Regras do MVP:
 * - Suporta até 2 janelas por dia:
 *   - janela1: inicio_1 -> fim_1
 *   - janela2 (opcional): inicio_2 -> fim_2
 * - Se ativo=false, o funcionário não trabalha nesse dia.
 *
 * Observações do schema:
 * - estabelecimento_id existe e é DEFAULT NULL.
 * - updated_at é gerenciado pelo banco (DEFAULT current_timestamp ON UPDATE current_timestamp).
 */
@Entity
@Table(
        name = "funcionario_agenda",
        indexes = {
                @Index(name = "idx_func_agenda_funcionario", columnList = "funcionario_id"),
                @Index(name = "idx_fa_estabelecimento_funcionario", columnList = "estabelecimento_id,funcionario_id")
        }
)
public class FuncionarioAgenda {

    @EmbeddedId
    private FuncionarioAgendaId id;

    /**
     * BD: bigint unsigned DEFAULT NULL
     */
    @Column(name = "estabelecimento_id")
    private Long estabelecimentoId;

    @Column(name = "inicio_1")
    private LocalTime inicio1;

    @Column(name = "fim_1")
    private LocalTime fim1;

    @Column(name = "inicio_2")
    private LocalTime inicio2;

    @Column(name = "fim_2")
    private LocalTime fim2;

    /**
     * BD: tinyint(1) NOT NULL DEFAULT 1
     */
    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    /**
     * BD controla via DEFAULT current_timestamp() ON UPDATE current_timestamp()
     */
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    // getters e setters

    public FuncionarioAgendaId getId() { return id; }
    public void setId(FuncionarioAgendaId id) { this.id = id; }

    public Long getEstabelecimentoId() { return estabelecimentoId; }
    public void setEstabelecimentoId(Long estabelecimentoId) { this.estabelecimentoId = estabelecimentoId; }

    public LocalTime getInicio1() { return inicio1; }
    public void setInicio1(LocalTime inicio1) { this.inicio1 = inicio1; }

    public LocalTime getFim1() { return fim1; }
    public void setFim1(LocalTime fim1) { this.fim1 = fim1; }

    public LocalTime getInicio2() { return inicio2; }
    public void setInicio2(LocalTime inicio2) { this.inicio2 = inicio2; }

    public LocalTime getFim2() { return fim2; }
    public void setFim2(LocalTime fim2) { this.fim2 = fim2; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
}