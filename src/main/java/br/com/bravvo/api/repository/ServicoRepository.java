package br.com.bravvo.api.repository;

import br.com.bravvo.api.entity.Servico;
import br.com.bravvo.api.enums.StatusServico;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ServicoRepository extends JpaRepository<Servico, Long> {
	
	boolean existsByNomeIgnoreCase(String nome);

	boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);


    /**
     * Listagem paginada com filtros e busca.
     *
     * Regras:
     * - status (opcional): filtra por ATIVO/INATIVO quando informado
     * - search (opcional): busca apenas pelo NOME (case-insensitive)
     *
     * Observação:
     * - Se search vier null/"" não filtra por nome.
     */
    @Query("""
        SELECT s
        FROM Servico s
        WHERE (:status IS NULL OR s.status = :status)
          AND (
                :search IS NULL OR :search = '' OR
                LOWER(s.nome) LIKE LOWER(CONCAT('%', :search, '%'))
          )
        """)
    Page<Servico> search(
            @Param("status") StatusServico status,
            @Param("search") String search,
            Pageable pageable
    );
    
 
    @Query("select s from Servico s where s.status = br.com.bravvo.api.enums.StatusServico.ATIVO")
    List<Servico> findAllAtivos();
    
    @Query("""
    	    select s.id
    	    from Servico s
    	    where s.status = br.com.bravvo.api.enums.StatusServico.ATIVO
    	      and s.id in :ids
    	""")
    	List<Long> findActiveIdsByIds(@Param("ids") List<Long> ids);
}
