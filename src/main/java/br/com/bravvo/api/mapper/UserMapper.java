package br.com.bravvo.api.mapper;

import br.com.bravvo.api.dto.user.UserCreateRequestDTO;
import br.com.bravvo.api.dto.user.UserResponseDTO;
import br.com.bravvo.api.dto.user.UserUpdateRequestDTO;
import br.com.bravvo.api.entity.User;

public class UserMapper {

    private UserMapper() {}

    /**
     * DTO (create) -> Entity
     * Observação: NÃO seta senhaHash aqui.
     * A senha deve ser hasheada no Service e aplicada via setSenhaHash().
     */
    public static User toEntity(UserCreateRequestDTO dto) {
        if (dto == null) return null;

        User user = new User();
        user.setNome(dto.getNome());
        user.setEmail(dto.getEmail());
        user.setTelefone(dto.getTelefone());
        user.setPerfil(dto.getPerfil());
        user.setAtivo(true);

        return user;
    }

    /**
     * Atualiza uma Entity existente com dados do DTO (update).
     * Observação: senha (se existir) deve ser hasheada no Service e aplicada via setSenhaHash().
     */
    public static void updateEntity(User user, UserUpdateRequestDTO dto) {
        if (user == null || dto == null) return;

        user.setNome(dto.getNome());
        user.setEmail(dto.getEmail());
        user.setTelefone(dto.getTelefone());
        user.setPerfil(dto.getPerfil());
        // ativo não mexe aqui (regra do negócio: inativar é endpoint/ação específica)
    }

    /**
     * Entity -> DTO (response)
     * Nunca expõe senhaHash.
     */
    public static UserResponseDTO toResponse(User user) {
        if (user == null) return null;

        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setNome(user.getNome());
        dto.setEmail(user.getEmail());
        dto.setTelefone(user.getTelefone());
        dto.setPerfil(user.getPerfil());
        dto.setAtivo(user.getAtivo());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());

        return dto;
    }
}
