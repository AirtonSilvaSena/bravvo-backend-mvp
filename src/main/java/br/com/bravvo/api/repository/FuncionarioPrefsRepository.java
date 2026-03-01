package br.com.bravvo.api.repository;

import br.com.bravvo.api.entity.FuncionarioPrefs;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository da tabela funcionario_prefs.
 */
public interface FuncionarioPrefsRepository extends JpaRepository<FuncionarioPrefs, Long> {
	
	Optional<FuncionarioPrefs> findByEstabelecimentoIdAndFuncionarioId(Long estabelecimentoId, Long funcionarioId);

	List<FuncionarioPrefs> findAllByEstabelecimentoIdAndFuncionarioIdIn(Long estabelecimentoId, List<Long> funcionarioIds);
}
