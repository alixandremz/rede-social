package com.alixandremz.rede.dto;

import java.time.Instant;

import com.alixandremz.rede.model.Post;

public record PostResumo(
        Long id,
        Long autorId,
        String autorNome,
        String conteudo,
        Instant criadoEm,
        int totalCurtidas
) {
    public static PostResumo de(Post post) {
        return new PostResumo(
                post.getId(),
                post.getAutor().getId(),
                post.getAutor().getNome(),
                post.getConteudo(),
                post.getCriadoEm(),
                post.getCurtidas().size()
        );
    }
}
