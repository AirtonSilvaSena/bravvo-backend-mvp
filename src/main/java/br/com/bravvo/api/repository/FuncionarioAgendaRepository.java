package br.com.bravvo.api.repository;

import br.com.bravvo.api.entity.FuncionarioAgenda;
import br.com.bravvo.api.entity.FuncionarioAgendaId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository da agenda semanal do funcionário.
 */
public interface FuncionarioAgendaRepository extends JpaRepository<FuncionarioAgenda, FuncionarioAgendaId> {

    /**
     * Lista a agenda de todos os dias daquele funcionário.
     */
    List<FuncionarioAgenda> findByIdFuncionarioId(Long funcionarioId);
}
