package br.com.bravvo.api.repository;

import br.com.bravvo.api.entity.FuncionarioBloqueio;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository dos bloqueios do funcionário.
 */
public interface FuncionarioBloqueioRepository extends JpaRepository<FuncionarioBloqueio, Long> {

	List<FuncionarioBloqueio> findByFuncionarioIdOrderByStartDtAsc(Long funcionarioId);

	/**
	 * Retorna bloqueios que se SOBREPOEM à janela [from, to]. (Útil futuramente na
	 * disponibilidade pública.)
	 */
	@Query("""
			    select b
			    from FuncionarioBloqueio b
			    where b.funcionarioId = :funcionarioId
			      and b.startDt < :to
			      and b.endDt > :from
			    order by b.startDt asc
			""")
	List<FuncionarioBloqueio> findOverlapping(@Param("funcionarioId") Long funcionarioId,
			@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
