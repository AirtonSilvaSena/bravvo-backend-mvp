package br.com.bravvo.api.repository.projection;

/**
 * Projection mínima para retornar funcionário no catálogo público. Segurança:
 * não expõe email/telefone.
 */
public interface FuncionarioBasicProjection {
	Long getId();

	String getNome();
}
