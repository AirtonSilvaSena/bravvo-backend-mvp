package br.com.bravvo.api.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Bloqueios pontuais do funcionário.
 *
 * Tabela: funcionario_bloqueios
 *
 * Permite bloquear:
 * - dia inteiro (start=00:00, end=00:00 do dia seguinte)
 * - intervalo específico (ex: 13:00 até 17:00)
 *
 * Observações do schema:
 * - estabelecimento_id existe e é DEFAULT NULL.
 * - created_at é gerenciado pelo banco (DEFAULT current_timestamp()).
 */
@Entity
@Table(
        name = "funcionario_bloqueios",
        indexes = {
                @Index(name = "idx_func_bloq_funcionario", columnList = "funcionario_id"),
                @Index(name = "idx_func_bloq_periodo", columnList = "funcionario_id,start_dt,end_dt"),
                @Index(name = "idx_fb_estabelecimento_funcionario", columnList = "estabelecimento_id,funcionario_id"),
                @Index(name = "idx_fb_estabelecimento_periodo", columnList = "estabelecimento_id,funcionario_id,start_dt,end_dt")
        }
)
public class FuncionarioBloqueio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * BD: bigint unsigned NOT NULL
     * FK: fk_func_bloq_user -> users(id) ON DELETE CASCADE
     */
    @Column(name = "funcionario_id", nullable = false)
    private Long funcionarioId;

    /**
     * BD: bigint unsigned DEFAULT NULL
     * FK: fk_fb_estabelecimentos -> estabelecimentos(id)
     */
    @Column(name = "estabelecimento_id")
    private Long estabelecimentoId;

    @Column(name = "start_dt", nullable = false)
    private LocalDateTime startDt;

    @Column(name = "end_dt", nullable = false)
    private LocalDateTime endDt;

    @Column(name = "motivo", length = 255)
    private String motivo;

    /**
     * BD controla via DEFAULT current_timestamp()
     */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // getters/setters

    public Long getId() { return id; }

    public Long getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Long funcionarioId) { this.funcionarioId = funcionarioId; }

    public Long getEstabelecimentoId() { return estabelecimentoId; }
    public void setEstabelecimentoId(Long estabelecimentoId) { this.estabelecimentoId = estabelecimentoId; }

    public LocalDateTime getStartDt() { return startDt; }
    public void setStartDt(LocalDateTime startDt) { this.startDt = startDt; }

    public LocalDateTime getEndDt() { return endDt; }
    public void setEndDt(LocalDateTime endDt) { this.endDt = endDt; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}