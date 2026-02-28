package br.com.bravvo.api.security;

import br.com.bravvo.api.entity.User;
import br.com.bravvo.api.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Classe responsável por ensinar o Spring Security a buscar um usuário no banco.
 *
 * Padrão:
 * - subject do JWT = email
 * - authorities = ROLE_<PERFIL>
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Carrega por e-mail (padrão atual).
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));

        if (Boolean.FALSE.equals(user.getAtivo())) {
            throw new UsernameNotFoundException("Usuário inativo.");
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getSenhaHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getPerfil().name()))
        );
    }

    /**
     * Multi-tenant: carrega por e-mail + salao_id.
     * Isso garante que o token só autentica o usuário dentro do salão correto.
     */
    public UserDetails loadUserByEmailAndEstabelecimentoId(String email, Long salaoId) throws UsernameNotFoundException {

        User user = userRepository.findByEmailAndEstabelecimentoId(email, salaoId)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));

        if (Boolean.FALSE.equals(user.getAtivo())) {
            throw new UsernameNotFoundException("Usuário inativo.");
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getSenhaHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getPerfil().name()))
        );
    }
}