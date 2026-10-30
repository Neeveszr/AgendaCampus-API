package com.faculdade.eventos.controller;

import com.faculdade.eventos.dto.EventoRequest;
import com.faculdade.eventos.dto.EventoResponse;
import com.faculdade.eventos.dto.PaginaResponse;
import com.faculdade.eventos.service.EventoService;

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


// Recebe as requisições HTTP de /eventos e repassa ao EventoService, onde ficam as regras de negócio.
@RestController
@RequestMapping("/eventos")
@Tag(name = "Eventos", description = "Gerenciamento de eventos, prazos e disponibilidade de vagas")
// Erros que qualquer endpoint desta classe pode devolver (documentados no OpenApiConfig).
@ApiResponses({
        @ApiResponse(responseCode = "400", ref = "#/components/responses/Erro400"),
        @ApiResponse(responseCode = "500", ref = "#/components/responses/Erro500")
})
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    // POST /eventos
    // Cadastra um evento. Responde 201 com o evento criado e o endereço dele no cabeçalho Location.
    @Operation(
            summary = "Cadastrar evento",
            description = "Cadastra um evento com início futuro, capacidade positiva e prazo de inscrição "
                    + "anterior ou igual ao início."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Evento cadastrado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Erro409")
    })
    @PostMapping
    public ResponseEntity<EventoResponse> cadastrar(
            @Valid @RequestBody EventoRequest evento
    ) {
        EventoResponse salvo = eventoService.cadastrar(evento);

        return ResponseEntity.created(LocalizacaoRecurso.doId(salvo.id())).body(salvo);
    }

    // GET /eventos?nome=&local=&page=&size=&sort=
    // Lista os eventos em páginas. Os filtros são opcionais e podem ser combinados.
    @Operation(
            summary = "Listar eventos",
            description = "Lista com paginação e filtros combináveis por parte do nome e do local, sem "
                    + "distinção de maiúsculas e minúsculas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta realizada", useReturnTypeSchema = true)
    })
    @GetMapping
    public ResponseEntity<PaginaResponse<EventoResponse>> listar(
            @RequestParam(defaultValue = "") String nome,
            @RequestParam(defaultValue = "") String local,
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(eventoService.listar(nome, local, pageable));
    }

    // GET /eventos/{id}
    // Retorna um evento com as vagas disponíveis; 404 se o ID não existir.
    @Operation(
            summary = "Buscar evento pelo ID",
            description = "Retorna os dados do evento e as vagas disponíveis, calculadas descontando as "
                    + "inscrições confirmadas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento encontrado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/Erro404")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EventoResponse> buscarPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(eventoService.buscarPorId(id));
    }

    // PUT /eventos/{id}
    // Substitui todos os dados do evento, respeitando as regras de prazo e capacidade.
    @Operation(
            summary = "Atualizar evento",
            description = "Atualiza os dados completos do evento. A capacidade não pode ficar abaixo das "
                    + "inscrições confirmadas, o prazo não pode ultrapassar o início e uma nova data de "
                    + "início deve ser futura."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento atualizado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/Erro404"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Erro409")
    })
    @PutMapping("/{id}")
    public ResponseEntity<EventoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody EventoRequest evento
    ) {
        return ResponseEntity.ok(eventoService.atualizar(id, evento));
    }

    // DELETE /eventos/{id}
    // Exclui o evento se ele não tiver inscrições. Responde 204, sem corpo.
    @Operation(
            summary = "Excluir evento",
            description = "Exclui somente eventos sem inscrições vinculadas, incluindo canceladas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Evento excluído", content = @Content),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/Erro404"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Erro409")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        eventoService.excluir(id);

        return ResponseEntity.noContent().build();
    }
}
