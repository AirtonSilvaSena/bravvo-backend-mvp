package br.com.bravvo.api.repository;

import br.com.bravvo.api.entity.Estabelecimentos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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
    
    /**
     * Lista estabelecimentos associados ao user, considerando apenas vínculo ativo em estabelecimento_users.
     *
     * IMPORTANTE:
     * - Estabelecimentos NÃO possui coluna/atributo "ativo" no MVP.
     * - Quem controla ativo/inativo é o vínculo EstabelecimentoUser (estabelecimento_users.ativo).
     */
    @Query("""
        select e
        from Estabelecimentos e
        join EstabelecimentoUser eu on eu.estabelecimentoId = e.id
        where eu.userId = :userId
          and eu.ativo = true
    """)
    List<Estabelecimentos> findAllAtivosByUserId(@Param("userId") Long userId);
    
    /**
     * Lista estabelecimentos ativos (ou todos, conforme você decidir) associados ao e-mail do usuário,
     * usando o vínculo estabelecimento_users (multi-tenant).
     *
     * Observação:
     * - Não usa users.estabelecimento_id (porque já foi refatorado para vínculo).
     * - Faz match case-insensitive no e-mail.
     */
    @Query("""
        select distinct e
        from Estabelecimentos e
        join EstabelecimentoUser eu on eu.estabelecimentoId = e.id
        join User u on u.id = eu.userId
        where lower(u.email) = lower(:email)
          and eu.ativo = true
    """)
    List<Estabelecimentos> findAllByUserEmailViaLink(@Param("email") String email);
    
    /**
     * Retorna IDs de usuários que possuem vínculo ATIVO com o estabelecimento.
     * (Sem filtrar perfil — admin também entra.)
     */
    @Query("""
        select eu.userId
        from EstabelecimentoUser eu
        where eu.estabelecimentoId = :estabelecimentoId
          and eu.ativo = true
    """)
    List<Long> findActiveUserIdsByEstabelecimentoId(@Param("estabelecimentoId") Long estabelecimentoId);
}