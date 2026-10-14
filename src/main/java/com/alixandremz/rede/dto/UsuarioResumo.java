package com.alixandremz.rede.dto;

import com.alixandremz.rede.model.Usuario;

public record UsuarioResumo(Long id, String nome) {

    public static UsuarioResumo de(Usuario usuario) {
        return new UsuarioResumo(usuario.getId(), usuario.getNome());
    }
}
