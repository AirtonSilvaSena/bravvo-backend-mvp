package br.com.bravvo.api.security;

import br.com.bravvo.api.entity.EstabelecimentoUser;
import br.com.bravvo.api.entity.User;
import br.com.bravvo.api.repository.EstabelecimentoUserRepository;
import br.com.bravvo.api.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final EstabelecimentoUserRepository estabelecimentoUserRepository;

    public CustomUserDetailsService(
            UserRepository userRepository,
            EstabelecimentoUserRepository estabelecimentoUserRepository
    ) {
        this.userRepository = userRepository;
        this.estabelecimentoUserRepository = estabelecimentoUserRepository;
    }

    /**
     * Mantido para compatibilidade, mas NÃO deve ser usado para fluxo multi-tenant.
     * (Em multi-tenant real, email pode existir em mais de um tenant).
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));

        if (Boolean.FALSE.equals(user.getAtivo())) {
            throw new UsernameNotFoundException("Usuário inativo.");
        }

        // Sem tenant não dá pra definir o perfil do vínculo corretamente.
        // Mantém um fallback mínimo.
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getSenhaHash(),
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))
        );
    }

    /**
     * Multi-tenant: carrega por e-mail + estabelecimentoId.
     * - user vem por join do vínculo
     * - authority vem do perfil do vínculo
     */
    public UserDetails loadUserByEmailAndEstabelecimentoId(String email, Long estabelecimentoId)
            throws UsernameNotFoundException {

        User user = userRepository.findByEstabelecimentoIdAndEmailViaLink(estabelecimentoId, email)
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