package com.faculdade.eventos.controller;

import com.faculdade.eventos.dto.InscricaoRequest;
import com.faculdade.eventos.dto.InscricaoResponse;
import com.faculdade.eventos.dto.InscricaoStatusRequest;
import com.faculdade.eventos.dto.PaginaResponse;
import com.faculdade.eventos.model.StatusInscricao;
import com.faculdade.eventos.service.InscricaoService;

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

// Recebe as requisições HTTP de /inscricoes e repassa ao InscricaoService, onde ficam as regras de vagas e prazos.
@RestController
@RequestMapping("/inscricoes")
@Tag(name = "Inscrições", description = "Inscrição de participantes em eventos, cancelamento e reativação")
// Erros que qualquer endpoint desta classe pode devolver (documentados no OpenApiConfig).
@ApiResponses({
        @ApiResponse(responseCode = "400", ref = "#/components/responses/Erro400"),
        @ApiResponse(responseCode = "500", ref = "#/components/responses/Erro500")
})
public class InscricaoController {

    private final InscricaoService inscricaoService;

    public InscricaoController(InscricaoService inscricaoService) {
        this.inscricaoService = inscricaoService;
    }

    // POST /inscricoes
    // Inscreve um participante em um evento. O corpo traz só os IDs; a inscrição sempre nasce CONFIRMADA.
    @Operation(
            summary = "Criar inscrição",
            description = "Cria uma inscrição CONFIRMADA. Corpo: { \"eventoId\": 1, \"participanteId\": 3 }. "
                    + "O evento e o participante devem existir; a inscrição exige prazo aberto, evento não iniciado e "
                    + "vaga disponível. Não permite duplicidade, mesmo se a inscrição "
                    + "anterior estiver cancelada; nesse caso, use a atualização de status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inscrição criada", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/Erro404"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Erro409")
    })
    @PostMapping
    public ResponseEntity<InscricaoResponse> cadastrar(
            @Valid @RequestBody InscricaoRequest inscricao
    ) {
        InscricaoResponse salva = inscricaoService.cadastrar(inscricao);

        return ResponseEntity.created(LocalizacaoRecurso.doId(salva.id())).body(salva);
    }

    // GET /inscricoes?eventoId=&status=&page=&size=&sort=
    // Lista as inscrições em páginas. Os filtros são opcionais e podem ser combinados.
    @Operation(
            summary = "Listar inscrições",
            description = "Lista com paginação e filtros combináveis por eventoId e status "
                    + "(CONFIRMADA ou CANCELADA). Sem filtros, retorna todas as inscrições."
    )
    @ApiResponse(responseCode = "200", description = "Consulta realizada", useReturnTypeSchema = true)
    @GetMapping
    public ResponseEntity<PaginaResponse<InscricaoResponse>> listar(
            @RequestParam(required = false) Long eventoId,
            @RequestParam(required = false) StatusInscricao status,
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(inscricaoService.listar(eventoId, status, pageable));
    }

    // GET /inscricoes/{id}
    // Retorna uma inscrição com o evento e o participante; 404 se o ID não existir.
    @Operation(
            summary = "Buscar inscrição pelo ID",
            description = "Retorna a inscrição com os dados de identificação do evento e do participante."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inscrição encontrada", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/Erro404")
    })
    @GetMapping("/{id}")
    public ResponseEntity<InscricaoResponse> buscarPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(inscricaoService.buscarPorId(id));
    }

    // PUT /inscricoes/{id}
    // Muda só o status: CANCELADA libera a vaga, CONFIRMADA reativa (se ainda houver vaga e prazo).
    @Operation(
            summary = "Cancelar ou reativar inscrição",
            description = "Altera apenas o status. Corpo: { \"status\": \"CANCELADA\" } ou { \"status\": \"CONFIRMADA\" }. Cancelar exige evento ainda não iniciado e libera a vaga. "
                    + "Reativar exige prazo aberto, início futuro e vaga disponível. Repetir o status atual "
                    + "mantém a inscrição sem alterações."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status atualizado ou já correspondente ao solicitado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/Erro404"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Erro409")
    })
    @PutMapping("/{id}")
    public ResponseEntity<InscricaoResponse> atualizarStatus(
            @PathVariable Long id,
            @Valid @RequestBody InscricaoStatusRequest dados
    ) {
        return ResponseEntity.ok(inscricaoService.atualizarStatus(id, dados.status()));
    }

    // DELETE /inscricoes/{id}
    // Apaga uma inscrição que já está cancelada. Responde 204, sem corpo.
    @Operation(
            summary = "Excluir inscrição",
            description = "Exclui fisicamente somente inscrições CANCELADAS. Para preservar o histórico, "
                    + "prefira cancelar pela atualização de status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Inscrição excluída", content = @Content),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/Erro404"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Erro409")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        inscricaoService.excluir(id);

        return ResponseEntity.noContent().build();
    }
}
