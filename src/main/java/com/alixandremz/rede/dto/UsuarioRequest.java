package com.alixandremz.rede.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank(message = "o nome é obrigatório")
        @Size(max = 80, message = "o nome deve ter no máximo 80 caracteres")
        String nome
) {
}
