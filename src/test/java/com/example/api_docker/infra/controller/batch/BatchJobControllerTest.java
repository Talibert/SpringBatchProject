package com.example.api_docker.infra.controller.batch;

import com.example.api_docker.ControllerAbstractTests;
import com.example.api_docker.infra.batch.descriptor.JobDescriptor;
import com.example.api_docker.infra.batch.descriptor.JobType;
import com.example.api_docker.infra.batch.service.BatchJobLauncherService;
import com.example.api_docker.infra.batch.service.BatchOrchestratorService;
import com.example.api_docker.infra.batch.service.OrchestratedJobTriggerResult;
import com.example.api_docker.infra.controller.batch.request.TriggerJobRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Testes do BatchJobController")
@WebMvcTest(BatchJobController.class)
class BatchJobControllerTest extends ControllerAbstractTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BatchOrchestratorService orchestratorService;

    @MockitoBean
    private BatchJobLauncherService jobLauncherService;

    @Test
    @DisplayName("POST /batch/jobs/{jobName}/run: Deve retornar 202 Accepted indicando startedImmediately=true")
    void shouldReturn202AcceptedWhenTriggeringJobWithValidData() throws Exception {
        String jobName = "collectorJob";
        String dataRef = "2026-10-17";
        TriggerJobRequest request = new TriggerJobRequest(dataRef, Map.of("origem", "bacen"));

        JobInstance instance = new JobInstance(1L, jobName);
        JobExecution mockExecution = new JobExecution(instance, 105L, new JobParametersBuilder().toJobParameters());
        mockExecution.setStatus(BatchStatus.STARTING);

        OrchestratedJobTriggerResult triggerResult = new OrchestratedJobTriggerResult(
                jobName,
                dataRef,
                mockExecution,
                true,
                List.of(),
                "Execução do job solicitada com sucesso."
        );

        when(orchestratorService.triggerJob(eq(jobName), eq(dataRef), any()))
                .thenReturn(triggerResult);

        mockMvc.perform(post("/batch/jobs/{jobName}/run", jobName)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + tokenDoUser))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobName").value(jobName))
                .andExpect(jsonPath("$.dataReferencia").value(dataRef))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.startedImmediately").value(true))
                .andExpect(jsonPath("$.pendingDependencies").isEmpty())
                .andExpect(jsonPath("$.executionId").value(105))
                .andExpect(jsonPath("$.mensagem").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("POST /batch/jobs/{jobName}/run: Deve retornar 202 Accepted com startedImmediately=false e lista de dependências disparadas")
    void shouldReturn202AcceptedWhenExecutionIdIsNullAndDependenciesTriggered() throws Exception {
        String jobName = "calculatorJob";
        String dataRef = "2026-10-17";
        TriggerJobRequest request = new TriggerJobRequest(dataRef, Map.of());

        OrchestratedJobTriggerResult triggerResult = new OrchestratedJobTriggerResult(
                jobName,
                dataRef,
                null,
                false,
                List.of("collectorJob"),
                "Solicitação aceita. O calculador possui 1 coleta(s) pendente(s)."
        );

        when(orchestratorService.triggerJob(eq(jobName), eq(dataRef), any()))
                .thenReturn(triggerResult);

        mockMvc.perform(post("/batch/jobs/{jobName}/run", jobName)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + tokenDoUser))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobName").value(jobName))
                .andExpect(jsonPath("$.dataReferencia").value(dataRef))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.startedImmediately").value(false))
                .andExpect(jsonPath("$.pendingDependencies[0]").value("collectorJob"))
                .andExpect(jsonPath("$.executionId").doesNotExist());
    }

    @Test
    @DisplayName("POST /batch/jobs/{jobName}/run: Deve retornar 400 Bad Request quando dataReferencia não for informada")
    void shouldReturn400BadRequestWhenDataReferenciaIsMissing() throws Exception {
        String invalidJson = "{\"parametros\": {}}";

        mockMvc.perform(post("/batch/jobs/collectorJob/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson)
                        .header("Authorization", "Bearer " + tokenDoUser))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /batch/jobs/{jobName}/run: Deve retornar 400 Bad Request quando dataReferencia estiver em formato inválido")
    void shouldReturn400BadRequestWhenDataReferenciaFormatIsInvalid() throws Exception {
        String invalidJson = "{\"dataReferencia\": \"17/10/2026\"}";

        mockMvc.perform(post("/batch/jobs/collectorJob/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson)
                        .header("Authorization", "Bearer " + tokenDoUser))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /batch/jobs/{jobName}/run: Deve retornar 400 Bad Request quando o job não existir no catálogo")
    void shouldReturn400BadRequestWhenJobNameNotFound() throws Exception {
        String jobInexistente = "jobFantasma";
        String dataRef = "2026-10-17";
        TriggerJobRequest request = new TriggerJobRequest(dataRef, Map.of());

        when(orchestratorService.triggerJob(eq(jobInexistente), eq(dataRef), any()))
                .thenThrow(new IllegalArgumentException("Job não encontrado no catálogo registrado: " + jobInexistente));

        mockMvc.perform(post("/batch/jobs/{jobName}/run", jobInexistente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + tokenDoUser))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("Job não encontrado no catálogo registrado: jobFantasma"));
    }

    @Test
    @DisplayName("POST /batch/jobs/{jobName}/run: Deve retornar 401 Unauthorized quando não enviar token JWT")
    void shouldReturn401UnauthorizedWhenTokenMissing() throws Exception {
        TriggerJobRequest request = new TriggerJobRequest("2026-10-17", Map.of());

        mockMvc.perform(post("/batch/jobs/collectorJob/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /batch/jobs: Deve retornar catálogo de jobs disponíveis")
    void shouldReturnAvailableJobsList() throws Exception {
        JobDescriptor d1 = new JobDescriptor("collectorJob", JobType.COLLECTOR);
        JobDescriptor d2 = new JobDescriptor("calculatorJob", JobType.CALCULATOR, List.of("collectorJob"));

        when(orchestratorService.getAvailableDescriptors()).thenReturn(List.of(d1, d2));

        mockMvc.perform(get("/batch/jobs")
                        .header("Authorization", "Bearer " + tokenDoUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].jobName").value("collectorJob"))
                .andExpect(jsonPath("$[0].type").value("COLLECTOR"))
                .andExpect(jsonPath("$[1].jobName").value("calculatorJob"))
                .andExpect(jsonPath("$[1].type").value("CALCULATOR"))
                .andExpect(jsonPath("$[1].dependsOnJobNames[0]").value("collectorJob"));
    }

    @Test
    @DisplayName("GET /batch/jobs/running: Deve retornar lista de jobs em execução com métricas")
    void shouldReturnRunningJobsList() throws Exception {
        JobInstance instance = new JobInstance(1L, "collectorJob");
        JobExecution execution = new JobExecution(instance, 105L,
                new JobParametersBuilder().addString("dataReferencia", "2026-10-17").toJobParameters());
        execution.setStatus(BatchStatus.STARTED);
        execution.setStartTime(LocalDateTime.now().minusSeconds(15));

        StepExecution stepExecution = execution.createStepExecution("collectorStep");
        stepExecution.setStatus(BatchStatus.STARTED);
        stepExecution.setReadCount(50);
        stepExecution.setWriteCount(50);

        when(orchestratorService.getRunningJobExecutions()).thenReturn(List.of(execution));

        mockMvc.perform(get("/batch/jobs/running")
                        .header("Authorization", "Bearer " + tokenDoUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].executionId").value(105))
                .andExpect(jsonPath("$[0].jobName").value("collectorJob"))
                .andExpect(jsonPath("$[0].dataReferencia").value("2026-10-17"))
                .andExpect(jsonPath("$[0].status").value("STARTED"))
                .andExpect(jsonPath("$[0].durationSeconds").exists())
                .andExpect(jsonPath("$[0].activeSteps[0].stepName").value("collectorStep"))
                .andExpect(jsonPath("$[0].activeSteps[0].readCount").value(50))
                .andExpect(jsonPath("$[0].activeSteps[0].writeCount").value(50));
    }

    @Test
    @DisplayName("GET /batch/jobs/running: Deve retornar array vazio quando não houver jobs em execução")
    void shouldReturnEmptyListWhenNoJobsRunning() throws Exception {
        when(orchestratorService.getRunningJobExecutions()).thenReturn(List.of());

        mockMvc.perform(get("/batch/jobs/running")
                        .header("Authorization", "Bearer " + tokenDoUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /batch/jobs/executions/{id}: Deve retornar detalhes da execução existente")
    void shouldReturnExecutionDetailsWhenFound() throws Exception {
        Long executionId = 105L;
        JobInstance instance = new JobInstance(1L, "collectorJob");
        JobExecution mockExecution = new JobExecution(instance, executionId, new JobParametersBuilder().toJobParameters());
        mockExecution.setStatus(BatchStatus.COMPLETED);
        mockExecution.setExitStatus(ExitStatus.COMPLETED);
        mockExecution.setStartTime(LocalDateTime.now().minusSeconds(10));
        mockExecution.setEndTime(LocalDateTime.now());

        when(jobLauncherService.getJobExecution(executionId)).thenReturn(mockExecution);

        mockMvc.perform(get("/batch/jobs/executions/{executionId}", executionId)
                        .header("Authorization", "Bearer " + tokenDoUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(105))
                .andExpect(jsonPath("$.jobName").value("collectorJob"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.exitCode").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /batch/jobs/executions/{id}: Deve retornar 404 Not Found quando a execução não existir")
    void shouldReturn404WhenExecutionNotFound() throws Exception {
        when(jobLauncherService.getJobExecution(999L)).thenReturn(null);

        mockMvc.perform(get("/batch/jobs/executions/999")
                        .header("Authorization", "Bearer " + tokenDoUser))
                .andExpect(status().isNotFound());
    }
}
