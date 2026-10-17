package com.example.api_docker.infra.batch.validator;

import com.example.api_docker.infra.batch.descriptor.BatchJobDescriptorProvider;
import com.example.api_docker.infra.batch.descriptor.JobDescriptor;
import com.example.api_docker.domain.batch.JobType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Testes do Validador de Integridade de Jobs do Spring Batch")
class BatchJobDescriptorValidatorTest {

    @Test
    @DisplayName("Deve passar na validação quando todos os jobs possuem BatchJobDescriptorProvider associado")
    void shouldPassValidationWhenAllJobsHaveDescriptor() {
        Job job1 = mock(Job.class);
        when(job1.getName()).thenReturn("collectorJob");

        Job job2 = mock(Job.class);
        when(job2.getName()).thenReturn("calculatorJob");

        BatchJobDescriptorProvider provider1 = () -> new JobDescriptor("collectorJob", JobType.COLLECTOR);
        BatchJobDescriptorProvider provider2 = () -> new JobDescriptor("calculatorJob", JobType.CALCULATOR);

        BatchJobDescriptorValidator validator = new BatchJobDescriptorValidator(
                List.of(job1, job2),
                List.of(provider1, provider2)
        );

        assertThatCode(validator::afterSingletonsInstantiated)
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException quando um job não possuir BatchJobDescriptorProvider")
    void shouldThrowExceptionWhenJobHasNoDescriptor() {
        Job job1 = mock(Job.class);
        when(job1.getName()).thenReturn("collectorJob");

        Job orphanJob = mock(Job.class);
        when(orphanJob.getName()).thenReturn("novoJobSemDescritor");

        BatchJobDescriptorProvider provider1 = () -> new JobDescriptor("collectorJob", JobType.COLLECTOR);

        BatchJobDescriptorValidator validator = new BatchJobDescriptorValidator(
                List.of(job1, orphanJob),
                List.of(provider1)
        );

        assertThatThrownBy(validator::afterSingletonsInstantiated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("VIOLAÇÃO DE CONTRATO BATCH")
                .hasMessageContaining("novoJobSemDescritor");
    }

    @Test
    @DisplayName("Deve passar na validação quando não houver jobs registrados")
    void shouldPassValidationWhenNoJobsRegistered() {
        BatchJobDescriptorValidator validator = new BatchJobDescriptorValidator(
                List.of(),
                List.of()
        );

        assertThatCode(validator::afterSingletonsInstantiated)
                .doesNotThrowAnyException();
    }
}
