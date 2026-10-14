package com.alixandremz.rede.dto;

import jakarta.validation.constraints.NotNull;

public record CurtidaRequest(
        @NotNull(message = "o id do usuário é obrigatório")
        Long usuarioId
) {
}
