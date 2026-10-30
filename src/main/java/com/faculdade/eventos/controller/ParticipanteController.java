package com.faculdade.eventos.controller;

import com.faculdade.eventos.dto.ParticipanteRequest;
import com.faculdade.eventos.dto.ParticipanteResponse;
import com.faculdade.eventos.dto.PaginaResponse;
import com.faculdade.eventos.service.ParticipanteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Recebe as requisições HTTP de /participantes e repassa ao ParticipanteService, onde ficam as regras de negócio.
@RestController
@RequestMapping("/participantes")
@Tag(name = "Participantes", description = "Cadastro, consulta, atualização e exclusão de participantes")
// Erros que qualquer endpoint desta classe pode devolver (documentados no OpenApiConfig).
@ApiResponses({
        @ApiResponse(responseCode = "400", ref = "#/components/responses/Erro400"),
        @ApiResponse(responseCode = "500", ref = "#/components/responses/Erro500")
})
public class ParticipanteController {

    private final ParticipanteService participanteService;

    public ParticipanteController(ParticipanteService participanteService) {
        this.participanteService = participanteService;
    }

    // POST /participantes
    // Cadastra um participante. Responde 201 com o participante criado e o endereço dele no cabeçalho Location.
    @Operation(
            summary = "Cadastrar participante",
            description = "Cadastra um participante com e-mail único. O e-mail é convertido para minúsculas e "
                    + "tem os espaços das extremidades removidos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Participante cadastrado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Erro409")
    })
    @PostMapping
    public ResponseEntity<ParticipanteResponse> cadastrar(
            @Valid @RequestBody ParticipanteRequest participante
    ) {
        ParticipanteResponse salvo = participanteService.cadastrar(participante);

        return ResponseEntity.created(LocalizacaoRecurso.doId(salvo.id())).body(salvo);
    }

    // GET /participantes?nome=&email=&page=&size=&sort=
    // Lista os participantes em páginas. Os filtros são opcionais e podem ser combinados.
    @Operation(
            summary = "Listar participantes",
            description = "Lista participantes com paginação. Permite filtrar por parte do nome, ignorando "
                    + "maiúsculas e minúsculas, e por parte do e-mail. Quando combinados, os dois filtros "
                    + "devem ser atendidos."
    )
    @ApiResponse(responseCode = "200", description = "Consulta realizada", useReturnTypeSchema = true)
    @GetMapping
    public ResponseEntity<PaginaResponse<ParticipanteResponse>> listar(
            @RequestParam(defaultValue = "") String nome,
            @RequestParam(defaultValue = "") String email,
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(participanteService.listar(nome, email, pageable));
    }

    // GET /participantes/{id}
    // Retorna um participante; 404 se o ID não existir.
    @Operation(summary = "Buscar participante pelo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participante encontrado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/Erro404")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ParticipanteResponse> buscarPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(participanteService.buscarPorId(id));
    }

    // PUT /participantes/{id}
    // Substitui nome e e-mail; 409 se o e-mail já pertencer a outro participante.
    @Operation(
            summary = "Atualizar participante",
            description = "Atualiza nome e e-mail. O e-mail informado não pode pertencer a outro participante."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participante atualizado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/Erro404"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Erro409")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ParticipanteResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ParticipanteRequest participante
    ) {
        return ResponseEntity.ok(participanteService.atualizar(id, participante));
    }

    // DELETE /participantes/{id}
    // Exclui o participante se ele não tiver inscrições. Responde 204, sem corpo.
    @Operation(
            summary = "Excluir participante",
            description = "Exclui um participante somente se ele não possuir inscrições vinculadas, incluindo "
                    + "canceladas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Participante excluído", content = @Content),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/Erro404"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Erro409")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        participanteService.excluir(id);

        return ResponseEntity.noContent().build();
    }
}
