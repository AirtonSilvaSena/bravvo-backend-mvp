package br.com.bravvo.api.dto.user;

import br.com.bravvo.api.enums.PerfilUser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UserUpdateRequestDTO {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 120, message = "Nome deve ter entre 2 e 120 caracteres")
    private String nome;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Size(max = 180, message = "Email deve ter no máximo 180 caracteres")
    private String email;

    @Size(max = 30, message = "Telefone deve ter no máximo 30 caracteres")
    private String telefone;

    @Size(min = 6, max = 72, message = "Senha deve ter entre 6 e 72 caracteres")
    private String senha;

    @NotNull(message = "Perfil é obrigatório")
    private PerfilUser perfil;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public PerfilUser getPerfil() { return perfil; }
    public void setPerfil(PerfilUser perfil) { this.perfil = perfil; }
}
