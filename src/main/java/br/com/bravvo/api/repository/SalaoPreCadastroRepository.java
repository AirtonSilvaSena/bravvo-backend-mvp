package br.com.bravvo.api.repository;

import br.com.bravvo.api.entity.EstabelecimentosPreCadastro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalaoPreCadastroRepository extends JpaRepository<EstabelecimentosPreCadastro, Long> {
    Optional<EstabelecimentosPreCadastro> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsBySlug(String slug);
    void deleteByEmail(String email);
}
