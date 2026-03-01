package br.com.bravvo.api.repository;

import br.com.bravvo.api.entity.Agendamento;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    boolean existsByEstabelecimentoIdAndProtocolo(Long estabelecimentoId, String protocolo);

    Optional<Agendamento> findByEstabelecimentoIdAndProtocolo(Long estabelecimentoId, String protocolo);

    @Query("""
        select a
        from Agendamento a
        where a.estabelecimentoId = :estabelecimentoId
          and a.funcionarioId = :funcionarioId
          and a.status in ('pendente','confirmado','em_atendimento')
          and a.inicio < :to
          and a.fim > :from
        order by a.inicio asc
    """)
    List<Agendamento> findBlockingOverlapping(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("funcionarioId") Long funcionarioId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
        select a
        from Agendamento a
        where a.estabelecimentoId = :estabelecimentoId
          and a.clienteId = :clienteId
          and (:fromDt is null or a.inicio >= :fromDt)
          and (:toDt is null or a.inicio < :toDt)
          and (:statusList is null or a.status in :statusList)
        order by a.inicio asc
    """)
    List<Agendamento> findByClienteFiltro(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("clienteId") Long clienteId,
            @Param("fromDt") LocalDateTime fromDt,
            @Param("toDt") LocalDateTime toDt,
            @Param("statusList") List<String> statusList
    );

    @Query("""
        select a
        from Agendamento a
        where a.estabelecimentoId = :estabelecimentoId
          and a.funcionarioId = :funcionarioId
          and (:fromDt is null or a.inicio >= :fromDt)
          and (:toDt is null or a.inicio < :toDt)
          and (:statusList is null or a.status in :statusList)
        order by a.inicio asc
    """)
    List<Agendamento> findByFuncionarioFiltro(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("funcionarioId") Long funcionarioId,
            @Param("fromDt") LocalDateTime fromDt,
            @Param("toDt") LocalDateTime toDt,
            @Param("statusList") List<String> statusList
    );

    Optional<Agendamento> findByIdAndEstabelecimentoId(Long id, Long estabelecimentoId);
}