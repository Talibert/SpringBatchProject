package com.example.api_docker.infra.batch.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.TaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("Testes da Configuração Condicional de JobLaunchers")
class BatchJobLauncherConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(BatchJobLauncherConfig.class)
            .withBean(JobRepository.class, () -> mock(JobRepository.class));

    @Test
    @DisplayName("Deve carregar asyncJobLauncher e batchTaskExecutor quando async estiver ativado (padrão)")
    void shouldLoadAsyncLauncherWhenAsyncEnabled() {
        contextRunner
                .withPropertyValues("spring.batch.job.async.enabled=true")
                .run(context -> {
                    assertThat(context).hasBean("asyncJobLauncher");
                    assertThat(context).hasBean("batchTaskExecutor");
                    assertThat(context).doesNotHaveBean("syncJobLauncher");

                    JobLauncher launcher = context.getBean(JobLauncher.class);
                    assertThat(launcher).isNotNull();

                    TaskExecutor executor = context.getBean(TaskExecutor.class);
                    assertThat(executor).isNotNull();
                });
    }

    @Test
    @DisplayName("Deve carregar syncJobLauncher e NÃO instanciar thread pool quando async estiver desativado")
    void shouldLoadSyncLauncherWhenAsyncDisabled() {
        contextRunner
                .withPropertyValues("spring.batch.job.async.enabled=false")
                .run(context -> {
                    assertThat(context).hasBean("syncJobLauncher");
                    assertThat(context).doesNotHaveBean("asyncJobLauncher");
                    assertThat(context).doesNotHaveBean("batchTaskExecutor");

                    JobLauncher launcher = context.getBean(JobLauncher.class);
                    assertThat(launcher).isNotNull();
                });
    }
}
