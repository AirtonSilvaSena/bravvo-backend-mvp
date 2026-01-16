package br.com.bravvo.api.controller;

import br.com.bravvo.api.dto.estabelecimento.EstabelecimentoConfirmEmailRequestDTO;
import br.com.bravvo.api.dto.estabelecimento.EstabelecimentoConfirmEmailResponseDTO;
import br.com.bravvo.api.dto.estabelecimento.EstabelecimentoPreRegisterRequestDTO;
import br.com.bravvo.api.dto.estabelecimento.EstabelecimentoPreRegisterResponseDTO;
import br.com.bravvo.api.service.EstabelecimentoOnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Onboarding Público - Estabelecimentos", description = "Pré-cadastro e confirmação de e-mail para criação do salão (SaaS).")
@RestController
@RequestMapping("/api/public/establecimentos")
public class PublicEstabelecimentoOnboardingController {

    private final EstabelecimentoOnboardingService service;

    public PublicEstabelecimentoOnboardingController(EstabelecimentoOnboardingService service) {
        this.service = service;
    }

    @Operation(
            summary = "Pré-cadastro do estabelecimento (envia código por e-mail)",
            description = """
                    Inicia o cadastro do salão (Admin), mas NÃO cria o salão nem o usuário ainda.
                    O sistema valida e-mail e slug, gera um código de confirmação (expira em ~15 min) e envia para o e-mail informado.
                    
                    Somente após confirmar o código (confirm-email) o salão e o usuário ADMIN serão criados.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Código enviado com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    name = "Sucesso",
                                    value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "success": true,
                                                "message": "Código enviado para o e-mail informado."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Erro de validação (campos inválidos)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Validação",
                                    value = """
                                            {
                                              "success": false,
                                              "message": "Erro de validação",
                                              "errors": [
                                                { "field": "email", "message": "deve ser um e-mail válido" }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "409", description = "Conflito (e-mail ou slug já em uso)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Conflito",
                                    value = """
                                            {
                                              "success": false,
                                              "message": "E-mail já cadastrado."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "500", description = "Erro interno (ex.: falha ao enviar e-mail)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Erro interno",
                                    value = """
                                            {
                                              "success": false,
                                              "message": "An error occurred while processing your request."
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/pre-register")
    public ResponseEntity<?> preRegister(
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Dados do pré-cadastro do salão (Admin).",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Exemplo",
                                    value = """
                                            {
                                              "nome": "Bravvo Admin",
                                              "email": "admin@bravvo.com",
                                              "telefone": "119974086769",
                                              "senha": "Senha@123",
                                              "slug": "bravvo-admin"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody EstabelecimentoPreRegisterRequestDTO dto
    ) {
        service.preRegister(dto);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", new EstabelecimentoPreRegisterResponseDTO("Código enviado para o e-mail informado.")
        ));
    }

    @Operation(
            summary = "Confirmação do e-mail (finaliza cadastro e cria salão + admin)",
            description = """
                    Confirma o código enviado por e-mail e FINALIZA o cadastro:
                    - Cria o registro do salão (saloes)
                    - Cria o usuário ADMIN (users)
                    - Inicia o TRIAL de 14 dias (trial_ends_at = now + 14d)
                    - Define o owner do salão
                    
                    Se o código estiver inválido/expirado, o cadastro não é finalizado.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "E-mail confirmado e cadastro finalizado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    name = "Sucesso",
                                    value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "success": true,
                                                "message": "E-mail confirmado. Salão criado com TRIAL de 14 dias."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Erro de validação (campos inválidos)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Validação",
                                    value = """
                                            {
                                              "success": false,
                                              "message": "Erro de validação",
                                              "errors": [
                                                { "field": "codigo", "message": "tamanho deve ser entre 4 e 10" }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "409", description = "Conflito (slug/email já ficaram indisponíveis)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Conflito",
                                    value = """
                                            {
                                              "success": false,
                                              "message": "Slug já está em uso."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "500", description = "Erro interno",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Erro interno",
                                    value = """
                                            {
                                              "success": false,
                                              "message": "An error occurred while processing your request."
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/confirm-email")
    public ResponseEntity<?> confirmEmail(
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Dados para confirmar o código recebido por e-mail.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Exemplo",
                                    value = """
                                            {
                                              "email": "admin@bravvo.com",
                                              "codigo": "123456"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody EstabelecimentoConfirmEmailRequestDTO dto
    ) {
        service.confirmEmail(dto);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", new EstabelecimentoConfirmEmailResponseDTO("E-mail confirmado. Salão criado com TRIAL de 14 dias.")
        ));
    }
}
