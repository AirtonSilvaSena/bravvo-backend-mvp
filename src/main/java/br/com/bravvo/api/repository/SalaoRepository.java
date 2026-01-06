package br.com.bravvo.api.repository;

import br.com.bravvo.api.entity.Salao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalaoRepository extends JpaRepository<Salao, Long> {
	boolean existsBySlug(String slug);

	Optional<Salao> findBySlug(String slug);
}
