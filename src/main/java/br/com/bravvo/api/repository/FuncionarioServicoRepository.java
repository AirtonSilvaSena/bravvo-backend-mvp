package br.com.bravvo.api.repository;

import br.com.bravvo.api.entity.FuncionarioServico;
import br.com.bravvo.api.entity.FuncionarioServicoId;
import br.com.bravvo.api.enums.PerfilUser;
import br.com.bravvo.api.repository.projection.FuncionarioBasicProjection;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Repository da tabela funcionario_servicos.
 *
 * Usado para identificar quais serviços estão habilitados para o funcionário.
 *
 * Importante (multi-tenant):
 * - A tabela funcionario_servicos NÃO precisa ter estabelecimento_id.
 * - A validação do tenant é feita via join/exists na tabela estabelecimento_users (EstabelecimentoUser),
 *   garantindo que o funcionário pertence ao estabelecimento atual.
 */
public interface FuncionarioServicoRepository extends JpaRepository<FuncionarioServico, FuncionarioServicoId> {

    // =========================================================
    // Helpers básicos por funcionário (sem tenant)
    // =========================================================

    /**
     * Retorna os IDs dos serviços habilitados para o funcionário (sem filtrar tenant).
     * Útil para cenários internos onde o tenant já foi garantido antes.
     */
    @Query("""
            select fs.id.servicoId
            from FuncionarioServico fs
            where fs.id.funcionarioId = :funcionarioId
           """)
    List<Long> findServicoIdsByFuncionarioId(@Param("funcionarioId") Long funcionarioId);

    /**
     * Remove todos vínculos do funcionário (sem filtrar tenant).
     * Use com cuidado: prefira deleteAllByEstabelecimentoIdAndFuncionarioId quando estiver no fluxo multi-tenant.
     */
    @Modifying
    @Transactional
    @Query("""
            delete from FuncionarioServico fs
            where fs.id.funcionarioId = :funcionarioId
           """)
    void deleteAllByFuncionarioId(@Param("funcionarioId") Long funcionarioId);

    // =========================================================
    // Listagem de funcionários por serviço (multi-tenant)
    // =========================================================

    /**
     * Lista usuários (id + nome) que executam um serviço específico
     * NO CONTEXTO DO ESTABELECIMENTO (multi-tenant).
     *
     * Regras:
     * - existe vínculo em funcionario_servicos (fs)
     * - usuário (u) está ativo
     * - vínculo do usuário com o estabelecimento (estabelecimento_users) está ativo
     *
     * Observação:
     * - Não filtra mais por perfil (ADMIN/FUNCIONARIO/CLIENTE).
     * - Se futuramente isso gerar efeito colateral (ex.: ADMIN aparecendo), aí sim reintroduzimos filtro.
     */
    @Query("""
            select u.id as id, u.nome as nome
            from FuncionarioServico fs, User u, EstabelecimentoUser eu
            where fs.id.funcionarioId = u.id
              and eu.userId = u.id
              and eu.estabelecimentoId = :estabelecimentoId
              and fs.id.servicoId = :servicoId
              and eu.ativo = true
              and u.ativo = true
           """)
    List<FuncionarioBasicProjection> findUsuariosAtivosByServicoId(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("servicoId") Long servicoId
    );

    /**
     * Verifica rapidamente se existe vínculo funcionario-servico.
     * (Usa navegação correta pela EmbeddedId)
     */
    boolean existsByIdFuncionarioIdAndIdServicoId(Long funcionarioId, Long servicoId);

    // =========================================================
    // Multi-tenant: ler e apagar vínculos respeitando o tenant
    // =========================================================

    /**
     * Retorna os IDs dos serviços habilitados para o funcionário,
     * mas garantindo que o funcionário pertence ao estabelecimento informado.
     *
     * Importante:
     * - funcionario_servicos não precisa ter estabelecimento_id.
     * - a validação é feita verificando que existe vínculo ativo em estabelecimento_users.
     */
    @Query("""
            select fs.id.servicoId
            from FuncionarioServico fs, EstabelecimentoUser eu
            where fs.id.funcionarioId = :funcionarioId
              and eu.userId = :funcionarioId
              and eu.estabelecimentoId = :estabelecimentoId
              and eu.ativo = true
           """)
    List<Long> findServicoIdsByEstabelecimentoIdAndFuncionarioId(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("funcionarioId") Long funcionarioId
    );

    /**
     * Remove todos os vínculos funcionario-servico do funcionário,
     * MAS SOMENTE se ele pertencer ao estabelecimento informado (tenant).
     *
     * Observação:
     * - JPQL delete não faz join direto de forma simples.
     * - usamos exists() para garantir o tenant antes de deletar.
     */
    @Modifying
    @Transactional
    @Query("""
            delete from FuncionarioServico fs
            where fs.id.funcionarioId = :funcionarioId
              and exists (
                    select 1
                    from EstabelecimentoUser eu
                    where eu.userId = :funcionarioId
                      and eu.estabelecimentoId = :estabelecimentoId
                      and eu.ativo = true
              )
           """)
    void deleteAllByEstabelecimentoIdAndFuncionarioId(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("funcionarioId") Long funcionarioId
    );
    
    @Query("""
    	    select fs.id.funcionarioId
    	    from FuncionarioServico fs
    	    where fs.id.servicoId = :servicoId
    	      and fs.id.funcionarioId in :funcionarioIds
    	""")
    	List<Long> findFuncionarioIdsJaVinculados(
    	        @Param("servicoId") Long servicoId,
    	        @Param("funcionarioIds") List<Long> funcionarioIds
    	);

}