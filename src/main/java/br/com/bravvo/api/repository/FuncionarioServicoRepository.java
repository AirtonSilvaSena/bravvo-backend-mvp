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
 */
public interface FuncionarioServicoRepository extends JpaRepository<FuncionarioServico, FuncionarioServicoId> {

    /**
     * Retorna os IDs dos serviços habilitados para o funcionário.
     * Isso nos permite marcar checkbox no front.
     */
    @Query("""
            select fs.id.servicoId
            from FuncionarioServico fs
            where fs.id.funcionarioId = :funcionarioId
           """)
    List<Long> findServicoIdsByFuncionarioId(@Param("funcionarioId") Long funcionarioId);

    /**
     * Remove todos vínculos do funcionário.
     * Usado para sincronizar de forma simples e consistente.
     */
    @Modifying
    @Transactional
    @Query("delete from FuncionarioServico fs where fs.id.funcionarioId = :funcionarioId")
    void deleteAllByFuncionarioId(@Param("funcionarioId") Long funcionarioId);

    /**
     * Lista funcionários (id + nome) que executam um serviço específico
     * NO CONTEXTO DO ESTABELECIMENTO (multi-tenant).
     *
     * Regras:
     * - existe vínculo em funcionario_servicos (fs)
     * - usuário (u) está ativo
     * - vínculo do usuário com o estabelecimento (estabelecimento_users) está ativo
     * - perfil do vínculo deve ser FUNCIONARIO (ou o PerfilUser passado)
     *
     * Observação:
     * - perfil NÃO é mais lido de User, e sim de estabelecimento_users.
     * - por isso, a query faz join manual com EstabelecimentoUser (eu).
     */
    @Query("""
            select u.id as id, u.nome as nome
            from FuncionarioServico fs, User u, EstabelecimentoUser eu
            where fs.id.funcionarioId = u.id
              and eu.userId = u.id
              and eu.estabelecimentoId = :estabelecimentoId
              and fs.id.servicoId = :servicoId
              and eu.perfil = :perfil
              and eu.ativo = true
              and u.ativo = true
           """)
    List<FuncionarioBasicProjection> findFuncionariosAtivosByServicoId(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("servicoId") Long servicoId,
            @Param("perfil") PerfilUser perfil
    );

    boolean existsByIdFuncionarioIdAndIdServicoId(Long funcionarioId, Long servicoId);
}