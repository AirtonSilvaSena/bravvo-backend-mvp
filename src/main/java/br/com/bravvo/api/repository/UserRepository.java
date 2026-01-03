package br.com.bravvo.api.repository;

import br.com.bravvo.api.entity.User;
import br.com.bravvo.api.enums.PerfilUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	// =========================
	// Métodos já existentes
	// =========================

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	// =========================
	// Listagem paginada com filtros e busca
	// =========================

	@Query("""
			SELECT u
			FROM User u
			WHERE (:perfil IS NULL OR u.perfil = :perfil)
			  AND (:ativo IS NULL OR u.ativo = :ativo)
			  AND (
			        :q IS NULL OR :q = '' OR
			        LOWER(u.nome) LIKE LOWER(CONCAT('%', :q, '%')) OR
			        LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) OR
			        LOWER(COALESCE(u.telefone, '')) LIKE LOWER(CONCAT('%', :q, '%'))
			  )
			""")
	Page<User> search(@Param("perfil") PerfilUser perfil, @Param("ativo") Boolean ativo, @Param("q") String q,
			Pageable pageable);

	/**
	 * Lista somente CLIENTES ativos, com busca opcional (q) em nome/email/telefone.
	 *
	 * Observação: - 'q' pode ser null -> sem filtro. - Busca é "contains" (like
	 * %q%).
	 */
	@Query("""
			    select u
			    from User u
			    where u.perfil = :perfil
			      and u.ativo = true
			      and (
			           :q is null
			           or lower(u.nome) like lower(concat('%', :q, '%'))
			           or (u.email is not null and lower(u.email) like lower(concat('%', :q, '%')))
			           or (u.telefone is not null and lower(u.telefone) like lower(concat('%', :q, '%')))
			      )
			""")
	Page<User> searchAtivosByPerfilAndQ(@Param("perfil") PerfilUser perfil, @Param("q") String q, Pageable pageable);
}
