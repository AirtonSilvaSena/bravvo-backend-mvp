package br.com.bravvo.api.security;

import br.com.bravvo.api.entity.EstabelecimentoUser;
import br.com.bravvo.api.entity.User;
import br.com.bravvo.api.repository.EstabelecimentoUserRepository;
import br.com.bravvo.api.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Ensina o Spring Security a buscar o usuário no banco.
 *
 * Padrão:
 * - subject do JWT = email
 * - authorities = ROLE_<PERFIL>
 *
 * Multi-tenant:
 * - perfil vem do vínculo estabelecimento_users (fonte de verdade)
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final EstabelecimentoUserRepository estabelecimentoUserRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    EstabelecimentoUserRepository estabelecimentoUserRepository) {
        this.userRepository = userRepository;
        this.estabelecimentoUserRepository = estabelecimentoUserRepository;
    }

    /**
     * Mantido para compatibilidade (se algum fluxo ainda usar).
     * ATENÇÃO: em cenário com e-mail repetido em tenants diferentes, findByEmail pode ser ambíguo.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));

        if (Boolean.FALSE.equals(user.getAtivo())) {
            throw new UsernameNotFoundException("Usuário inativo.");
        }

        // Fallback: usa perfil do user enquanto não houver estabelecimentoId para resolver vínculo.
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getSenhaHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getPerfil().name()))
        );
    }

    /**
     * Multi-tenant: carrega por e-mail + estabelecimento_id.
     * Aqui garantimos:
     * - usuário ativo
     * - vínculo ativo
     * - authority baseada no perfil do vínculo
     */
    public UserDetails loadUserByEmailAndEstabelecimentoId(String email, Long estabelecimentoId) throws UsernameNotFoundException {

        User user = userRepository.findByEstabelecimentoIdAndEmail(estabelecimentoId, email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado neste estabelecimento."));

        if (Boolean.FALSE.equals(user.getAtivo())) {
            throw new UsernameNotFoundException("Usuário inativo.");
        }

        EstabelecimentoUser link = estabelecimentoUserRepository
                .findByEstabelecimentoIdAndUserId(estabelecimentoId, user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Vínculo do usuário com o estabelecimento não encontrado."));

        if (Boolean.FALSE.equals(link.getAtivo())) {
            throw new UsernameNotFoundException("Usuário sem permissão (vínculo inativo).");
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getSenhaHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + link.getPerfil().name()))
        );
    }
}