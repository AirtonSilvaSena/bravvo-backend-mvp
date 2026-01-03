package br.com.bravvo.api.controller;

import br.com.bravvo.api.dto.common.PagedResponseDTO;
import br.com.bravvo.api.dto.user.UserCreateRequestDTO;
import br.com.bravvo.api.dto.user.UserResponseDTO;
import br.com.bravvo.api.dto.user.UserUpdateRequestDTO;
import br.com.bravvo.api.enums.PerfilUser;
import br.com.bravvo.api.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Swagger imports
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller responsável pelo gerenciamento de usuários do sistema.
 *
 * Responsabilidades desta classe:
 * - Expor endpoints REST relacionados a usuários
 * - Validar dados de entrada (DTOs)
 * - Delegar regras de negócio para o UserService
 *
 * IMPORTANTE:
 * - Este controller NÃO contém lógica de negócio
 * - Toda regra fica concentrada na camada de service
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Gerenciamento de usuários do sistema")
public class UserController {

    /**
     * Serviço responsável pelas regras de negócio de usuário.
     */
    private final UserService userService;

    /**
     * Construtor com injeção de dependência do UserService.
     *
     * @param userService serviço de usuário
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Cria um novo usuário no sistema.
     *
     * Endpoint:
     * POST /api/users
     *
     * @param dto dados do usuário a ser criado
     * @return dados do usuário criado
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Criar usuário",
        description = "Cria um novo usuário no sistema"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "409", description = "E-mail já cadastrado"),
        @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    public UserResponseDTO create(@Valid @RequestBody UserCreateRequestDTO dto) {
        return userService.create(dto);
    }

    /**
     * Busca um usuário pelo ID.
     *
     * Endpoint:
     * GET /api/users/{id}
     *
     * @param id identificador do usuário
     * @return dados do usuário encontrado
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Buscar usuário por ID",
        description = "Retorna os dados de um usuário pelo ID"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
        @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    public UserResponseDTO getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    /**
     * Lista usuários com paginação, filtros e busca.
     *
     * Endpoint:
     * GET /api/users
     *
     * Query params:
     * - page  (opcional, default=1)
     * - limit (opcional, default=10)
     * - perfil (opcional: CLIENTE|FUNCIONARIO) -> ADMIN pode filtrar; FUNCIONARIO fica restrito a CLIENTE
     * - ativo  (opcional: true|false)
     * - q (opcional): busca por nome/email/telefone
     *
     * @return resposta paginada {page, limit, total, pages, items}
     */
    @GetMapping
    @Operation(
        summary = "Listar usuários (paginado)",
        description = "Lista usuários com paginação, filtro por perfil e busca por nome/email/telefone."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista paginada retornada com sucesso"),
        @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    public PagedResponseDTO<UserResponseDTO> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) PerfilUser perfil,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) String q
    ) {
        return userService.listPaged(page, limit, perfil, ativo, q);
    }

    /**
     * Atualiza os dados de um usuário existente.
     *
     * Endpoint:
     * PUT /api/users/{id}
     *
     * @param id identificador do usuário
     * @param dto dados atualizados do usuário
     * @return dados do usuário atualizado
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Atualizar usuário",
        description = "Atualiza os dados de um usuário existente"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
        @ApiResponse(responseCode = "409", description = "E-mail já cadastrado"),
        @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    public UserResponseDTO update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequestDTO dto
    ) {
        return userService.update(id, dto);
    }

    /**
     * Inativa um usuário do sistema (soft delete).
     *
     * Endpoint:
     * DELETE /api/users/{id}
     *
     * Observação:
     * - O usuário não é removido fisicamente do banco
     * - Apenas é marcado como inativo
     *
     * @param id identificador do usuário
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Inativar usuário",
        description = "Inativa um usuário (soft delete)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Usuário inativado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
        @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    public void inactivate(@PathVariable Long id) {
        userService.inactivate(id);
    }
}
