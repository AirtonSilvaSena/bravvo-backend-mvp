package br.com.bravvo.api.mapper;

import br.com.bravvo.api.dto.user.UserCreateRequestDTO;
import br.com.bravvo.api.dto.user.UserResponseDTO;
import br.com.bravvo.api.dto.user.UserUpdateRequestDTO;
import br.com.bravvo.api.entity.User;

/**
 * Mapper responsável apenas pelos dados da entidade User.
 *
 * ⚠️ IMPORTANTE:
 * - Perfil NÃO pertence mais à entidade User.
 * - Perfil agora é responsabilidade do vínculo EstabelecimentoUser.
 * - Senha nunca é manipulada aqui (somente no Service).
 */
public class UserMapper {

    private UserMapper() {}

    /**
     * DTO (create) -> Entity
     * Não seta senhaHash.
     * Não seta perfil (agora pertence ao vínculo).
     */
    public static User toEntity(UserCreateRequestDTO dto) {
        if (dto == null) return null;

        User user = new User();
        user.setNome(dto.getNome());
        user.setEmail(dto.getEmail());
        user.setTelefone(dto.getTelefone());
        user.setAtivo(true);

        return user;
    }

    /**
     * Atualiza entidade existente.
     * Não altera perfil.
     * Não altera ativo.
     */
    public static void updateEntity(User user, UserUpdateRequestDTO dto) {
        if (user == null || dto == null) return;

        user.setNome(dto.getNome());
        user.setEmail(dto.getEmail());
        user.setTelefone(dto.getTelefone());
    }

    /**
     * Entity -> Response DTO
     * Perfil NÃO é retornado aqui.
     * O perfil deve vir do vínculo (EstabelecimentoUser).
     */
    public static UserResponseDTO toResponse(User user) {
        if (user == null) return null;

        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setNome(user.getNome());
        dto.setEmail(user.getEmail());
        dto.setTelefone(user.getTelefone());
        dto.setAtivo(user.getAtivo());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());

        return dto;
    }
}