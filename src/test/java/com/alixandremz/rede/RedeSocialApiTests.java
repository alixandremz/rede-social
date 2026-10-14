package com.alixandremz.rede;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.alixandremz.rede.repository.CurtidaRepository;
import com.alixandremz.rede.repository.PostRepository;
import com.alixandremz.rede.repository.SeguidorRepository;
import com.alixandremz.rede.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
class RedeSocialApiTests {

    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private PostRepository posts;

    @Autowired
    private CurtidaRepository curtidas;

    @Autowired
    private SeguidorRepository seguidores;

    @BeforeEach
    void limpar() {
        seguidores.deleteAll();
        curtidas.deleteAll();
        posts.deleteAll();
        usuarios.deleteAll();
    }

    private long extrairId(String resposta) {
        Matcher matcher = ID.matcher(resposta);
        matcher.find();
        return Long.parseLong(matcher.group(1));
    }

    private long criarUsuario(String nome) throws Exception {
        String resposta = mvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return extrairId(resposta);
    }

    private long criarPost(long autorId, String conteudo) throws Exception {
        String resposta = mvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autorId\":" + autorId + ",\"conteudo\":\"" + conteudo + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return extrairId(resposta);
    }

    private void seguir(long seguidorId, long alvoId) throws Exception {
        mvc.perform(post("/usuarios/{id}/seguindo/{alvo}", seguidorId, alvoId))
                .andExpect(status().isNoContent());
    }

    private void curtir(long postId, long usuarioId) throws Exception {
        mvc.perform(post("/posts/{id}/curtidas", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + usuarioId + "}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void criaUsuarioComStatus201() throws Exception {
        mvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.nome").value("Ana"));
    }

    @Test
    void buscaUsuarioInexistenteRetorna404() throws Exception {
        mvc.perform(get("/usuarios/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void usuarioDetalheTemContagens() throws Exception {
        long ana = criarUsuario("Ana");
        long bruno = criarUsuario("Bruno");
        long carla = criarUsuario("Carla");
        seguir(ana, bruno);
        seguir(ana, carla);
        seguir(bruno, ana);
        criarPost(ana, "primeiro post");

        mvc.perform(get("/usuarios/{id}", ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seguindo").value(2))
                .andExpect(jsonPath("$.seguidores").value(1))
                .andExpect(jsonPath("$.posts").value(1));
    }

    @Test
    void naoSegueASiMesmo() throws Exception {
        long ana = criarUsuario("Ana");
        mvc.perform(post("/usuarios/{id}/seguindo/{alvo}", ana, ana))
                .andExpect(status().isConflict());
    }

    @Test
    void naoSegueDuasVezes() throws Exception {
        long ana = criarUsuario("Ana");
        long bruno = criarUsuario("Bruno");
        seguir(ana, bruno);
        mvc.perform(post("/usuarios/{id}/seguindo/{alvo}", ana, bruno))
                .andExpect(status().isConflict());
    }

    @Test
    void deixaDeSeguir() throws Exception {
        long ana = criarUsuario("Ana");
        long bruno = criarUsuario("Bruno");
        seguir(ana, bruno);

        mvc.perform(delete("/usuarios/{id}/seguindo/{alvo}", ana, bruno))
                .andExpect(status().isNoContent());
        mvc.perform(get("/usuarios/{id}", bruno))
                .andExpect(jsonPath("$.seguidores").value(0));
    }

    @Test
    void deixaDeSeguirQuemNaoSegueRetorna404() throws Exception {
        long ana = criarUsuario("Ana");
        long bruno = criarUsuario("Bruno");
        mvc.perform(delete("/usuarios/{id}/seguindo/{alvo}", ana, bruno))
                .andExpect(status().isNotFound());
    }

    @Test
    void curtirAumentaContagem() throws Exception {
        long autor = criarUsuario("Autor");
        long leitor = criarUsuario("Leitor");
        long postId = criarPost(autor, "post interessante");

        curtir(postId, leitor);

        mvc.perform(get("/posts/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCurtidas").value(1));
    }

    @Test
    void naoCurtiDuasVezes() throws Exception {
        long autor = criarUsuario("Autor");
        long leitor = criarUsuario("Leitor");
        long postId = criarPost(autor, "post");
        curtir(postId, leitor);

        mvc.perform(post("/posts/{id}/curtidas", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + leitor + "}"))
                .andExpect(status().isConflict());
    }

    @Test
    void descurtir() throws Exception {
        long autor = criarUsuario("Autor");
        long leitor = criarUsuario("Leitor");
        long postId = criarPost(autor, "post");
        curtir(postId, leitor);

        mvc.perform(delete("/posts/{id}/curtidas/{usuarioId}", postId, leitor))
                .andExpect(status().isNoContent());
        mvc.perform(get("/posts/{id}", postId))
                .andExpect(jsonPath("$.totalCurtidas").value(0));
    }

    @Test
    void feedMostraPostsDeQuemSegue() throws Exception {
        long ana = criarUsuario("Ana");
        long bruno = criarUsuario("Bruno");
        seguir(ana, bruno);
        criarPost(bruno, "post do bruno");
        criarPost(ana, "post da ana");

        mvc.perform(get("/usuarios/{id}/feed", ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.conteudo[0].autorNome").value("Bruno"))
                .andExpect(jsonPath("$.conteudo[0].conteudo").value("post do bruno"));
    }

    @Test
    void popularesOrdenaPorCurtidas() throws Exception {
        long u1 = criarUsuario("U1");
        long u2 = criarUsuario("U2");
        long u3 = criarUsuario("U3");
        long postador = criarPost(u1, "menos curtido");
        long popular = criarPost(u2, "mais curtido");
        curtir(popular, u1);
        curtir(popular, u3);
        curtir(postador, u2);

        mvc.perform(get("/posts/populares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].conteudo").value("mais curtido"));
    }

    @Test
    void listaPostsPaginado() throws Exception {
        long autor = criarUsuario("Autor");
        criarPost(autor, "post 1");
        criarPost(autor, "post 2");
        criarPost(autor, "post 3");

        mvc.perform(get("/posts").param("tamanho", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.conteudo.length()").value(2));
    }

    @Test
    void criarPostComUsuarioInexistenteRetorna404() throws Exception {
        mvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autorId\":9999,\"conteudo\":\"texto\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void conteudoEmBrancoRetorna400() throws Exception {
        long autor = criarUsuario("Autor");
        mvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autorId\":" + autor + ",\"conteudo\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.conteudo").exists());
    }
}
