package br.com.bravvo.api.controller.admin;

import br.com.bravvo.api.service.EstabelecimentoLogoAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/estabelecimento/me")
public class EstabelecimentoLogoAdminController {

    private final EstabelecimentoLogoAdminService logoService;

    public EstabelecimentoLogoAdminController(EstabelecimentoLogoAdminService logoService) {
        this.logoService = logoService;
    }

    @PutMapping("/logo")
    public ResponseEntity<Map<String, Object>> uploadLogo(@RequestParam("file") MultipartFile file) {
        logoService.uploadLogo(file);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/logo")
    public ResponseEntity<byte[]> getLogo() {
        return logoService.getLogoResponse();
    }
}