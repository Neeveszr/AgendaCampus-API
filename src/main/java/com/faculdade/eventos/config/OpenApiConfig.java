package com.faculdade.eventos.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI configurarDocumentacao() {
        // Os controllers referenciam estas respostas para evitar repetir o mesmo schema.
        Components componentes = new Components()
                .addSchemas("ErroApi", new Schema<>().type("object")
                        .addProperty("type", new Schema<>().type("string"))
                        .addProperty("title", new Schema<>().type("string"))
                        .addProperty("status", new Schema<>().type("integer"))
                        .addProperty("detail", new Schema<>().type("string"))
                        .addProperty("instance", new Schema<>().type("string"))
                        .addProperty("campos", new Schema<>().type("object")
                                .additionalProperties(new Schema<>().type("string"))))
                .addResponses("Erro400", respostaErro("Parâmetro, ordenação ou corpo inválido"))
                .addResponses("Erro404", respostaErro("Recurso não encontrado"))
                .addResponses("Erro409", respostaErro("Conflito com as regras de negócio ou vínculos existentes"))
                .addResponses("Erro500", respostaErro("Erro interno inesperado"));

        return new OpenAPI().components(componentes)
                .info(new Info()
                        .title("Agenda Campus API")
                        .version("1.0.0")
                        .description(
                                "API para gerenciamento de eventos, "
                                        + "participantes e inscrições, "
                                        + "com controle de vagas e prazos."
                        ));
    }

    private ApiResponse respostaErro(String descricao) {
        return new ApiResponse().description(descricao)
                .content(new Content().addMediaType("application/problem+json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErroApi"))));
    }
}
