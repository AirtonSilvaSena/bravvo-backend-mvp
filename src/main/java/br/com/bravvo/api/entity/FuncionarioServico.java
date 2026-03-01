package br.com.bravvo.api.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entidade que representa o vínculo: Funcionário (User) ↔ Serviço.
 *
 * Tabela: funcionario_servicos
 *
 * Schema:
 * - PK: (funcionario_id, servico_id)
 * - estabelecimento_id: DEFAULT NULL
 * - created_at: DEFAULT current_timestamp()
 */
@Entity
@Table(
        name = "funcionario_servicos",
        indexes = {
                @Index(name = "fk_fs_servico", columnList = "servico_id"),
                @Index(name = "idx_fs_estabelecimento_funcionario", columnList = "estabelecimento_id,funcionario_id"),
                @Index(name = "idx_fs_estabelecimento_servico", columnList = "estabelecimento_id,servico_id")
        }
)
public class FuncionarioServico {

    @EmbeddedId
    private FuncionarioServicoId id;

    /**
     * BD: bigint unsigned DEFAULT NULL
     * FK: fk_fs_estabelecimentos -> estabelecimentos(id)
     */
    @Column(name = "estabelecimento_id")
    private Long estabelecimentoId;

    /**
     * BD controla via DEFAULT current_timestamp()
     */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // getters/setters

    public FuncionarioServicoId getId() { return id; }
    public void setId(FuncionarioServicoId id) { this.id = id; }

    public Long getEstabelecimentoId() { return estabelecimentoId; }
    public void setEstabelecimentoId(Long estabelecimentoId) { this.estabelecimentoId = estabelecimentoId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}