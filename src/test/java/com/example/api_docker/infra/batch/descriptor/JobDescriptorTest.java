package com.example.api_docker.infra.batch.descriptor;

import com.example.api_docker.domain.batch.JobType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Unitários do JobDescriptor")
class JobDescriptorTest {

    @Test
    @DisplayName("Deve criar JobDescriptor sem dependências via construtor simplificado")
    void shouldCreateJobDescriptorWithoutDependencies() {
        JobDescriptor descriptor = new JobDescriptor("collectorJob", JobType.COLLECTOR);

        assertEquals("collectorJob", descriptor.jobName());
        assertEquals(JobType.COLLECTOR, descriptor.type());
        assertTrue(descriptor.dependsOnJobNames().isEmpty());
        assertFalse(descriptor.hasDependencies());
    }

    @Test
    @DisplayName("Deve criar JobDescriptor com dependências e reportar hasDependencies() como true")
    void shouldCreateJobDescriptorWithDependencies() {
        List<String> dependencias = List.of("coletorA", "coletorB");
        JobDescriptor descriptor = new JobDescriptor("calculatorJob", JobType.CALCULATOR, dependencias);

        assertEquals("calculatorJob", descriptor.jobName());
        assertEquals(JobType.CALCULATOR, descriptor.type());
        assertEquals(2, descriptor.dependsOnJobNames().size());
        assertTrue(descriptor.hasDependencies());
    }

    @Test
    @DisplayName("Deve garantir a imutabilidade defensiva da lista de dependências")
    void shouldEnsureDefensiveCopyOfDependencies() {
        List<String> listaMutavel = new ArrayList<>();
        listaMutavel.add("coletorA");

        JobDescriptor descriptor = new JobDescriptor("calculatorJob", JobType.CALCULATOR, listaMutavel);

        // Modificação na lista original externa não deve impactar o descriptor
        listaMutavel.add("coletorB");
        assertEquals(1, descriptor.dependsOnJobNames().size());

        // A lista interna deve ser não-modificável
        assertThrows(UnsupportedOperationException.class, () ->
                descriptor.dependsOnJobNames().add("coletorC")
        );
    }

    @Test
    @DisplayName("Deve lançar NullPointerException caso jobName ou type sejam nulos")
    void shouldThrowExceptionWhenMandatoryFieldsAreNull() {
        assertThrows(NullPointerException.class, () ->
                new JobDescriptor(null, JobType.COLLECTOR)
        );

        assertThrows(NullPointerException.class, () ->
                new JobDescriptor("jobName", null)
        );
    }
}
