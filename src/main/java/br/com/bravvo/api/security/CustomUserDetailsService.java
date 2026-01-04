package br.com.bravvo.api.security;

import br.com.bravvo.api.entity.User;
import br.com.bravvo.api.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
//Teste deploy automatico
import java.util.List;
//teste 2
/**
 * Classe responsável por ensinar o Spring Security a buscar um usuário no banco
 * de dados.
 *
 * O Spring chama automaticamente este método quando precisa autenticar alguém.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}
//teste
	/**
	 * Este método é chamado pelo Spring Security sempre que ele precisar carregar
	 * um usuário.
	 *
	 * Aqui usamos o EMAIL como identificador único (o mesmo que colocamos no JWT
	 * como subject).
	 */
	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

		// Busca o usuário no banco
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));

		// Defesa extra: usuário inativo não pode autenticar
		if (Boolean.FALSE.equals(user.getAtivo())) {
			throw new UsernameNotFoundException("Usuário inativo.");
		}

		/*
		 * Aqui devolvemos um UserDetails para o Spring.
		 *
		 * - username -> email - password -> senhaHash (já criptografada) - authorities
		 * -> perfis/roles do usuário
		 *
		 * IMPORTANTE: Toda role no Spring Security deve começar com "ROLE_"
		 */
		return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getSenhaHash(),
				List.of(new SimpleGrantedAuthority("ROLE_" + user.getPerfil().name())));
	}
}
