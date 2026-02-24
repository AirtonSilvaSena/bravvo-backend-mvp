package br.com.bravvo.api.repository;

import br.com.bravvo.api.entity.EstabelecimentosPreCadastro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstabelecimentoPreCadastroRepository extends JpaRepository<EstabelecimentosPreCadastro, Long> {
    Optional<EstabelecimentosPreCadastro> findByEmail(String email);
    Optional<EstabelecimentosPreCadastro> findBySlug(String slug); 
    
    boolean existsByEmail(String email);
    boolean existsBySlug(String slug);
    void deleteByEmail(String email);
    
    Optional<EstabelecimentosPreCadastro> findByTelefone(String telefone);
    boolean existsByTelefone(String telefone); // opcional
}
