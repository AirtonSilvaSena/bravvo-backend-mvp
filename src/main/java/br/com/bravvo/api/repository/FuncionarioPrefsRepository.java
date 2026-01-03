package br.com.bravvo.api.repository;

import br.com.bravvo.api.entity.FuncionarioPrefs;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository da tabela funcionario_prefs.
 */
public interface FuncionarioPrefsRepository extends JpaRepository<FuncionarioPrefs, Long> {
}
