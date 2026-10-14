package com.alixandremz.rede.dto;

import java.time.Instant;

import com.alixandremz.rede.model.Usuario;

public record UsuarioDetalhe(
        Long id,
        String nome,
        Instant criadoEm,
        long seguidores,
        long seguindo,
        long posts
) {
    public static UsuarioDetalhe de(Usuario usuario, long seguidores, long seguindo, long posts) {
        return new UsuarioDetalhe(
                usuario.getId(),
                usuario.getNome(),
                usuario.getCriadoEm(),
                seguidores,
                seguindo,
                posts
        );
    }
}
