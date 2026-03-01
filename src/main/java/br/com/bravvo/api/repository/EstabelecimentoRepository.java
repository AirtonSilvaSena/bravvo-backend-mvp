package br.com.bravvo.api.repository;

import br.com.bravvo.api.entity.Estabelecimentos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository de Estabelecimentos.
 *
 * Observação:
 * - Para público (slug), usamos métodos seguros que retornam apenas o id quando necessário.
 */
public interface EstabelecimentoRepository extends JpaRepository<Estabelecimentos, Long> {

    boolean existsBySlug(String slug);

    Optional<Estabelecimentos> findBySlug(String slug);

    Optional<Estabelecimentos> findByOwnerUserId(Long ownerUserId);

    /**
     * Retorna apenas o id do estabelecimento pelo slug.
     * Útil para endpoints públicos para evitar carregar a entidade completa.
     */
    @Query("select e.id from Estabelecimentos e where e.slug = :slug")
    Optional<Long> findIdBySlug(@Param("slug") String slug);
}