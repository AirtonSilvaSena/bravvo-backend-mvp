package br.com.bravvo.api.repository;

import br.com.bravvo.api.entity.EstabelecimentoUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositório do vínculo Estabelecimento x Usuário.
 * Fonte de verdade do perfil por tenant.
 */
public interface EstabelecimentoUserRepository extends JpaRepository<EstabelecimentoUser, Long> {

    Optional<EstabelecimentoUser> findByEstabelecimentoIdAndUserId(Long estabelecimentoId, Long userId);

    boolean existsByEstabelecimentoIdAndUserId(Long estabelecimentoId, Long userId);
}