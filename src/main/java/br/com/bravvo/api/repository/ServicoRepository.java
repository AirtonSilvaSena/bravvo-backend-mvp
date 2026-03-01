package br.com.bravvo.api.repository;

import br.com.bravvo.api.entity.Servico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    // =========================================
    // Duplicidade de nome (tenant-safe)
    // =========================================
    boolean existsByEstabelecimentoIdAndNomeIgnoreCase(Long estabelecimentoId, String nome);

    boolean existsByEstabelecimentoIdAndNomeIgnoreCaseAndIdNot(Long estabelecimentoId, String nome, Long id);

    // =========================================
    // Search paginado (status agora é String)
    // status esperado: "ativo" | "inativo" | null
    // =========================================
    @Query("""
        SELECT s
        FROM Servico s
        WHERE s.estabelecimentoId = :estabelecimentoId
          AND (:status IS NULL OR LOWER(s.status) = LOWER(:status))
          AND (:search IS NULL OR :search = '' OR LOWER(s.nome) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<Servico> search(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable
    );

    // =========================================
    // Ativos (status = "ativo")
    // =========================================
    @Query("""
        select s
        from Servico s
        where s.estabelecimentoId = :estabelecimentoId
          and LOWER(s.status) = 'ativo'
        """)
    List<Servico> findAllAtivos(@Param("estabelecimentoId") Long estabelecimentoId);

    @Query("""
        select s.id
        from Servico s
        where s.estabelecimentoId = :estabelecimentoId
          and LOWER(s.status) = 'ativo'
          and s.id in :ids
        """)
    List<Long> findActiveIdsByIds(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("ids") List<Long> ids
    );

    // =========================================
    // Tenant-safe findById
    // =========================================
    @Query("""
        select s
        from Servico s
        where s.estabelecimentoId = :estabelecimentoId
          and s.id = :id
        """)
    Optional<Servico> findByIdAndEstabelecimentoId(
            @Param("id") Long id,
            @Param("estabelecimentoId") Long estabelecimentoId
    );

    // =========================================
    // ✅ Mantendo os “atalhos” que você tentou criar no fim,
    // mas corrigindo para realmente funcionar e não conflitar.
    // =========================================

    /**
     * Alias do findAllAtivos(...) para o catálogo público.
     * Mantive o nome que você já usa no service.
     */
    default List<Servico> findAllAtivosByEstabelecimentoId(Long estabelecimentoId) {
        return findAllAtivos(estabelecimentoId);
    }

    /**
     * Alias do findActiveIdsByIds(...).
     * Mantive o nome que você já usa em outros services.
     */
    default List<Long> findActiveIdsByEstabelecimentoIdAndIds(Long estabelecimentoId, List<Long> ids) {
        return findActiveIdsByIds(estabelecimentoId, ids);
    }
    
    boolean existsByIdAndEstabelecimentoId(Long id, Long estabelecimentoId);
}