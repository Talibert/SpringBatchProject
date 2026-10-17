package com.example.api_docker.infra.controller.batch;

import com.example.api_docker.infra.batch.descriptor.JobDescriptor;
import com.example.api_docker.infra.batch.service.BatchJobLauncherService;
import com.example.api_docker.infra.batch.service.BatchOrchestratorService;
import com.example.api_docker.infra.batch.service.OrchestratedJobTriggerResult;
import com.example.api_docker.infra.controller.batch.request.TriggerJobRequest;
import com.example.api_docker.infra.controller.batch.response.JobExecutionDetailResponse;
import com.example.api_docker.infra.controller.batch.response.JobExecutionResponse;
import com.example.api_docker.infra.controller.batch.response.RunningJobResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.batch.core.JobExecution;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para operação e monitoramento de Jobs do Spring Batch.
 * <p>
 * O endpoint de execução delega o processamento para um pool de threads dedicado
 * e libera a thread HTTP imediatamente com o status {@code 202 Accepted}.
 */
@Tag(name = "Batch Jobs", description = "Endpoints de solicitação, catálogo e monitoramento de execução de jobs Spring Batch")
@SecurityRequirement(name = "bearerAuth")
@RestController
@AllArgsConstructor
@RequestMapping("/batch/jobs")
public class BatchJobController {

    private final BatchOrchestratorService orchestratorService;
    private final BatchJobLauncherService jobLauncherService;

    @Operation(
            summary = "Listar catálogo de jobs disponíveis",
            description = "Retorna todos os jobs registrados na aplicação, seus tipos e grafo de dependências."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Catálogo retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping
    public ResponseEntity<List<JobDescriptor>> listAvailableJobs() {
        return ResponseEntity.ok(orchestratorService.getAvailableDescriptors());
    }

    @Operation(
            summary = "Listar todos os jobs em execução no momento",
            description = "Retorna todos os jobs que estão atualmente com status STARTING ou STARTED no Spring Batch com métricas de tempo decorrido."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de jobs em execução retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping("/running")
    public ResponseEntity<List<RunningJobResponse>> listRunningJobs() {
        List<JobExecution> running = orchestratorService.getRunningJobExecutions();
        List<RunningJobResponse> response = running.stream()
                .map(RunningJobResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Solicitar execução de um job em background",
            description = "Dispara a execução do job especificado para a data de referência informada. " +
                    "A thread HTTP é liberada imediatamente com status 202 Accepted. Caso o job seja um calculador com dependências pendentes, " +
                    "os coletores necessários são acionados em segundo plano e a resposta informa quais dependências foram disparadas."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Execução aceita e agendada em background"),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos ou dataReferencia incorreta"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @PostMapping("/{jobName}/run")
    public ResponseEntity<JobExecutionResponse> triggerJob(
            @Parameter(description = "Nome do job a ser executado", example = "collectorJob")
            @PathVariable String jobName,
            @Valid @RequestBody TriggerJobRequest request
    ) throws Exception {
        OrchestratedJobTriggerResult result = orchestratorService.triggerJob(
                jobName,
                request.dataReferencia(),
                request.parametros()
        );

        Long executionId = result.jobExecution() != null ? result.jobExecution().getId() : null;

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(JobExecutionResponse.of(
                        result.jobName(),
                        result.dataReferencia(),
                        result.startedImmediately(),
                        result.triggeredDependencies(),
                        executionId,
                        result.message()
                ));
    }

    @Operation(
            summary = "Consultar status de uma execução de job",
            description = "Recupera os detalhes de status (STARTING, STARTED, COMPLETED, FAILED) e duração de uma execução pelo ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Execução encontrada"),
            @ApiResponse(responseCode = "404", description = "Execução não encontrada"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping("/executions/{executionId}")
    public ResponseEntity<JobExecutionDetailResponse> getExecution(
            @Parameter(description = "ID físico da execução no Spring Batch", example = "105")
            @PathVariable Long executionId
    ) {
        JobExecution execution = jobLauncherService.getJobExecution(executionId);
        if (execution == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(JobExecutionDetailResponse.from(execution));
    }
}
