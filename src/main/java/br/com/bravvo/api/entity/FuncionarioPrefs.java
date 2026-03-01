package br.com.bravvo.api.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Preferências do funcionário.
 *
 * Tabela: funcionario_prefs
 * PK: funcionario_id
 *
 * Observações do schema:
 * - estabelecimento_id existe e é DEFAULT NULL.
 * - prefs_json é LONGTEXT (utf8mb4_bin) NOT NULL, com CHECK json_valid(prefs_json).
 * - updated_at é gerenciado pelo banco (DEFAULT current_timestamp ON UPDATE current_timestamp).
 */
@Entity
@Table(
        name = "funcionario_prefs",
        indexes = {
                @Index(name = "idx_fp_estabelecimento_funcionario", columnList = "estabelecimento_id,funcionario_id")
        }
)
public class FuncionarioPrefs {

    /**
     * BD: funcionario_id bigint unsigned NOT NULL (PK)
     * FK: fk_prefs_funcionario -> users(id)
     */
    @Id
    @Column(name = "funcionario_id", nullable = false)
    private Long funcionarioId;

    /**
     * BD: estabelecimento_id bigint unsigned DEFAULT NULL
     * FK: fk_fp_estabelecimentos -> estabelecimentos(id)
     */
    @Column(name = "estabelecimento_id")
    private Long estabelecimentoId;

    /**
     * BD: prefs_json LONGTEXT NOT NULL com CHECK json_valid(prefs_json)
     *
     * Mantemos como String (JSON em texto).
     * IMPORTANTe: não usar columnDefinition="json" porque no banco não é tipo JSON.
     */
    @Lob
    @Column(name = "prefs_json", nullable = false, columnDefinition = "longtext")
    private String prefsJson;

    /**
     * BD controla via DEFAULT current_timestamp() ON UPDATE current_timestamp()
     */
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    // getters/setters

    public Long getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Long funcionarioId) { this.funcionarioId = funcionarioId; }

    public Long getEstabelecimentoId() { return estabelecimentoId; }
    public void setEstabelecimentoId(Long estabelecimentoId) { this.estabelecimentoId = estabelecimentoId; }

    public String getPrefsJson() { return prefsJson; }
    public void setPrefsJson(String prefsJson) { this.prefsJson = prefsJson; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
}