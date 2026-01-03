package br.com.bravvo.api.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Preferências do funcionário.
 *
 * Tabela: funcionario_prefs
 * PK: funcionario_id
 * Campos:
 * - prefs_json (JSON livre)
 * - updated_at
 */
@Entity
@Table(name = "funcionario_prefs")
public class FuncionarioPrefs {

    @Id
    @Column(name = "funcionario_id")
    private Long funcionarioId;

    /**
     * Armazena preferências do funcionário em JSON:
     *
     * Exemplo esperado:
     * {
     *   "servicos": {
     *     "1": { "duracaoMin": 30 },
     *     "2": { "duracaoMin": 45 }
     *   }
     * }
     */
    @Column(name = "prefs_json", columnDefinition = "json")
    private String prefsJson;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Long funcionarioId) { this.funcionarioId = funcionarioId; }

    public String getPrefsJson() { return prefsJson; }
    public void setPrefsJson(String prefsJson) { this.prefsJson = prefsJson; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
