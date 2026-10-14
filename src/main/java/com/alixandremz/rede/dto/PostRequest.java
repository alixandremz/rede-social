package com.alixandremz.rede.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostRequest(
        @NotNull(message = "o id do autor é obrigatório")
        Long autorId,

        @NotBlank(message = "o conteúdo é obrigatório")
        @Size(max = 2000, message = "o conteúdo deve ter no máximo 2000 caracteres")
        String conteudo
) {
}
