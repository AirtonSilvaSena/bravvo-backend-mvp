package br.com.bravvo.api.repository;

import br.com.bravvo.api.entity.Agendamento;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository de Agendamentos.
 *
 * Usado para: - validar conflito final (overlap) - garantir unicidade do
 * protocolo
 */
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

	boolean existsByProtocolo(String protocolo);

	/**
	 * Agendamentos que se sobrepõem ao intervalo [from, to] e BLOQUEIAM agenda.
	 *
	 * Regra MVP: bloqueiam: - pendente - confirmado - em_atendimento
	 *
	 * Interseção: - inicio < to - fim > from
	 */
	@Query("""
			    select a
			    from Agendamento a
			    where a.funcionarioId = :funcionarioId
			      and a.status in ('pendente','confirmado','em_atendimento')
			      and a.inicio < :to
			      and a.fim > :from
			    order by a.inicio asc
			""")
	List<Agendamento> findBlockingOverlapping(@Param("funcionarioId") Long funcionarioId,
			@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	Optional<Agendamento> findByProtocolo(String protocolo);

	@Query("""
			    select a
			    from Agendamento a
			    where a.clienteId = :clienteId
			      and (:fromDt is null or a.inicio >= :fromDt)
			      and (:toDt is null or a.inicio < :toDt)
			      and (:statusList is null or a.status in :statusList)
			    order by a.inicio asc
			""")
	List<Agendamento> findByClienteFiltro(@Param("clienteId") Long clienteId, @Param("fromDt") LocalDateTime fromDt,
			@Param("toDt") LocalDateTime toDt, @Param("statusList") List<String> statusList);

	@Query("""
			    select a
			    from Agendamento a
			    where a.funcionarioId = :funcionarioId
			      and (:fromDt is null or a.inicio >= :fromDt)
			      and (:toDt is null or a.inicio < :toDt)
			      and (:statusList is null or a.status in :statusList)
			    order by a.inicio asc
			""")
	List<Agendamento> findByFuncionarioFiltro(@Param("funcionarioId") Long funcionarioId,
			@Param("fromDt") LocalDateTime fromDt, @Param("toDt") LocalDateTime toDt,
			@Param("statusList") List<String> statusList);
}
