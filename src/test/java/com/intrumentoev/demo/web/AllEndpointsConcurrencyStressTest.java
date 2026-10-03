package com.intrumentoev.demo.web;

import com.intrumentoev.demo.service.service.auth.ServerSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class AllEndpointsConcurrencyStressTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServerSessionManager serverSessionManager;

    @BeforeEach
    void setUp() {
        serverSessionManager.setLoggedIn(true, "stress.admin@banco.com", 1L);
    }

    @Test
    @DisplayName("Estrés Concurrente: 25 hilos simultáneos a GET /v1/auth/session-status")
    void testConcurrentSessionStatusRequests() throws InterruptedException, ExecutionException {
        int threads = 25;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startGate.await(); // Esperar señal para disparar todos los hilos al mismo instante
                    MvcResult result = mockMvc.perform(get("/v1/auth/session-status"))
                            .andReturn();
                    if (result.getResponse().getStatus() == 200) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    endGate.countDown();
                }
                return null;
            }));
        }

        startGate.countDown(); // ¡Disparar todas las peticiones al mismo tiempo!
        boolean completed = endGate.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).as("Todas las peticiones concurrentes deben completarse en el tiempo límite").isTrue();
        assertThat(failureCount.get()).as("Ninguna petición debe fallar por concurrencia").isZero();
        assertThat(successCount.get()).as("Todas las peticiones deben devolver 200 OK").isEqualTo(threads);
    }

    @Test
    @DisplayName("Estrés Concurrente: 25 hilos simultáneos a GET /v1/catalogos/generos")
    void testConcurrentCatalogosRequests() throws InterruptedException {
        int threads = 25;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    MvcResult result = mockMvc.perform(get("/v1/catalogos/generos")).andReturn();
                    if (result.getResponse().getStatus() == 200) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        endGate.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(threads);
    }

    @Test
    @DisplayName("Estrés Concurrente de Validación: 20 peticiones simultáneas con tipos inválidos")
    void testConcurrentInvalidTypeRequestsDoNotCrashServer() throws InterruptedException {
        int threads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threads);
        AtomicInteger badRequestCount = new AtomicInteger(0);
        AtomicInteger serverErrorCount = new AtomicInteger(0);

        String invalidJson = """
            {
              "monthlyIncome": "texto-invalido",
              "initialBalance": "texto"
            }
            """;

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    MvcResult result = mockMvc.perform(post("/v1/clientes/onboarding")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(invalidJson))
                            .andReturn();

                    int status = result.getResponse().getStatus();
                    if (status == 400) {
                        badRequestCount.incrementAndGet();
                    } else if (status >= 500) {
                        serverErrorCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        endGate.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(serverErrorCount.get()).as("Ninguna petición debe causar error 500 bajo ráfaga concurrente").isZero();
        assertThat(badRequestCount.get()).as("Todas las peticiones con tipos inválidos deben responder 400").isEqualTo(threads);
    }
}
