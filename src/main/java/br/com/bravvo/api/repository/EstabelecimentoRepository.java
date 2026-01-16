package br.com.bravvo.api.repository;

import br.com.bravvo.api.entity.Estabelecimentos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstabelecimentoRepository extends JpaRepository<Estabelecimentos, Long> {
	boolean existsBySlug(String slug);

	Optional<Estabelecimentos> findBySlug(String slug);
}
