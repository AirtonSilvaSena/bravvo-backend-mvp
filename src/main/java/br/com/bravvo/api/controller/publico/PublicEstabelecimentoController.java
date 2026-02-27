package br.com.bravvo.api.controller.publico;

import br.com.bravvo.api.dto.estabelecimento.PublicEstabelecimentoResolveResponseDTO;
import br.com.bravvo.api.service.PublicEstabelecimentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public/estabelecimentos")
@Tag(name = "Público - Estabelecimentos")
public class PublicEstabelecimentoController {

    private final PublicEstabelecimentoService publicEstabelecimentoService;

    public PublicEstabelecimentoController(PublicEstabelecimentoService publicEstabelecimentoService) {
        this.publicEstabelecimentoService = publicEstabelecimentoService;
    }

    @GetMapping("/{slug}")
    @Operation(
            summary = "Resolver estabelecimento por slug",
            description = "Retorna branding e status do estabelecimento (para tela de login e navegação pública)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estabelecimento encontrado"),
            @ApiResponse(responseCode = "404", description = "Estabelecimento não encontrado")
    })
    public ResponseEntity<Map<String, Object>> resolveBySlug(@PathVariable String slug) {
        PublicEstabelecimentoResolveResponseDTO dto = publicEstabelecimentoService.resolveBySlug(slug);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", dto
        ));
    }

    @GetMapping("/{slug}/logo")
    @Operation(
            summary = "Obter logo do estabelecimento por slug",
            description = "Retorna o arquivo do logo (PNG/JPG) para uso público (ex.: tela de login)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logo encontrada"),
            @ApiResponse(responseCode = "404", description = "Logo não encontrada / estabelecimento não encontrado")
    })
    public ResponseEntity<byte[]> getLogoBySlug(@PathVariable String slug) {
        return publicEstabelecimentoService.getLogoBySlug(slug);
    }
}