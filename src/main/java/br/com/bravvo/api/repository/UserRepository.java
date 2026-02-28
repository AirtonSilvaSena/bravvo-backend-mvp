package br.com.bravvo.api.repository;

import br.com.bravvo.api.entity.EstabelecimentoUser;
import br.com.bravvo.api.entity.User;
import br.com.bravvo.api.enums.PerfilUser;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca global por email.
     * Atenção: em multi-tenant real, email pode repetir.
     * Se seu DB tiver unique(email), então Optional faz sentido.
     * Se não tiver, troque para List<User>.
     */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Busca o User no contexto do tenant através do vínculo.
     * Isso substitui findByEstabelecimentoIdAndEmail (que não existe mais).
     */
    @Query("""
        select u
        from User u
        join EstabelecimentoUser eu on eu.userId = u.id
        where eu.estabelecimentoId = :estabelecimentoId
          and lower(u.email) = lower(:email)
    """)
    Optional<User> findByEstabelecimentoIdAndEmailViaLink(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("email") String email
    );

    /**
     * Listagem paginada no contexto do tenant.
     * - filtro por perfil vem do vínculo (eu.perfil)
     * - filtro por ativo do user e do vínculo podem ser aplicados
     * - busca por nome/email/telefone do user
     */
    @Query("""
        select u
        from User u
        join EstabelecimentoUser eu on eu.userId = u.id
        where eu.estabelecimentoId = :estabelecimentoId
          and (:perfil is null or eu.perfil = :perfil)
          and (:ativo is null or u.ativo = :ativo)
          and (:q is null or :q = '' or
               lower(u.nome) like lower(concat('%', :q, '%')) or
               lower(u.email) like lower(concat('%', :q, '%')) or
               lower(u.telefone) like lower(concat('%', :q, '%'))
          )
    """)
    Page<User> searchByTenant(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("perfil") PerfilUser perfil,
            @Param("ativo") Boolean ativo,
            @Param("q") String q,
            Pageable pageable
    );
    
   
}