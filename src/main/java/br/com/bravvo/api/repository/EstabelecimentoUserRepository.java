package br.com.bravvo.api.repository;

import br.com.bravvo.api.entity.EstabelecimentoUser;
import br.com.bravvo.api.entity.User;
import br.com.bravvo.api.enums.PerfilUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EstabelecimentoUserRepository extends JpaRepository<EstabelecimentoUser, Long> {

	Optional<EstabelecimentoUser> findByEstabelecimentoIdAndUserId(Long estabelecimentoId, Long userId);

	boolean existsByEstabelecimentoIdAndUserId(Long estabelecimentoId, Long userId);

	/**
	 * Busca vínculo ATIVO pelo email do usuário (join users). Fonte de verdade do
	 * tenant + perfil.
	 */
	@Query("""
			    select eu
			    from EstabelecimentoUser eu
			    join User u on u.id = eu.userId
			    where eu.estabelecimentoId = :estabelecimentoId
			      and lower(u.email) = lower(:email)
			""")
	Optional<EstabelecimentoUser> findByEstabelecimentoIdAndUserEmail(
			@Param("estabelecimentoId") Long estabelecimentoId, @Param("email") String email);

	@Query("""
			    select eu
			    from EstabelecimentoUser eu
			    join User u on u.id = eu.userId
			    where eu.estabelecimentoId = :estabelecimentoId
			      and lower(u.email) = lower(:email)
			      and eu.ativo = true
			""")
	Optional<EstabelecimentoUser> findAtivoByEstabelecimentoIdAndUserEmail(
			@Param("estabelecimentoId") Long estabelecimentoId, @Param("email") String email);

	/**
	 * Útil para listagens: filtra vínculos por perfil (tenant).
	 */
	@Query("""
			    select eu
			    from EstabelecimentoUser eu
			    where eu.estabelecimentoId = :estabelecimentoId
			      and (:perfil is null or eu.perfil = :perfil)
			      and (:ativo is null or eu.ativo = :ativo)
			""")
	Page<EstabelecimentoUser> searchLinks(@Param("estabelecimentoId") Long estabelecimentoId,
			@Param("perfil") PerfilUser perfil, @Param("ativo") Boolean ativo, Pageable pageable);

	@Query("""
			    select eu
			    from EstabelecimentoUser eu
			    join User u on u.id = eu.userId
			    where eu.estabelecimentoId = :estabelecimentoId
			      and u.email = :email
			""")
	Optional<EstabelecimentoUser> findByEstabelecimentoIdAndEmail(Long estabelecimentoId, String email);

	@Query("""
			select u
			from EstabelecimentoUser eu
			join User u on u.id = eu.userId
			where eu.estabelecimentoId = :estabelecimentoId
			  and (:perfil is null or eu.perfil = :perfil)
			  and (:ativo is null or eu.ativo = :ativo)
			  and (
			        :q is null or :q = '' or
			        lower(u.nome) like lower(concat('%', :q, '%')) or
			        lower(u.email) like lower(concat('%', :q, '%')) or
			        (u.telefone is not null and u.telefone like concat('%', :q, '%'))
			      )
			""")
	Page<User> searchUsersByTenant(Long estabelecimentoId, PerfilUser perfil, Boolean ativo, String q,
			Pageable pageable);
	
	Optional<EstabelecimentoUser> findByEstabelecimentoIdAndUser_Id(Long estabelecimentoId, Long userId);
}