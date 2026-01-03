package br.com.bravvo.api.repository;

import br.com.bravvo.api.entity.Protocolo;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository da tabela protocolos.
 */
public interface ProtocoloRepository extends JpaRepository<Protocolo, Long> {

	/**
	 * Usado para evitar colisões de código.
	 */
	boolean existsByCodigo(String codigo);
}
