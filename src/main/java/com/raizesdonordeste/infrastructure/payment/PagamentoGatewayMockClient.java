package com.raizesdonordeste.infrastructure.payment;

import com.raizesdonordeste.api.exception.PagamentoGatewayIndisponivelException;
import com.raizesdonordeste.domain.model.Pedido;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PagamentoGatewayMockClient {

    private final ThreadLocal<Integer> falhasTransitoriasNoRetry = new ThreadLocal<>();

    @Value("${pagamento.gateway.latencia-ms:50}")
    private long latenciaMs;

    @Value("${pagamento.gateway.simular-falha-transitoria:false}")
    private boolean simularFalhaTransitoria;

    @Value("${pagamento.gateway.falhas-transitorias-antes-sucesso:2}")
    private int falhasTransitoriasAntesSucesso;

    @Retry(name = "pagamentoGateway")
    @CircuitBreaker(name = "pagamentoGateway", fallbackMethod = "fallbackEnviarSolicitacao")
    public void enviarSolicitacao(Pedido pedido, String transactionId) {
        simularLatenciaExterna();

        if (simularFalhaTransitoria) {
            int falhas = falhasTransitoriasNoRetry.get() == null ? 0 : falhasTransitoriasNoRetry.get();
            if (falhas < falhasTransitoriasAntesSucesso) {
                falhasTransitoriasNoRetry.set(falhas + 1);
                throw new PagamentoGatewayIndisponivelException(
                        "Falha transitoria simulada no gateway de pagamento.");
            }
            falhasTransitoriasNoRetry.remove();
        }

        log.info("Gateway mock: solicitacao registrada pedido={} transactionId={} canal={}",
                pedido.getId(), transactionId, pedido.getCanalPedido());
    }

    @SuppressWarnings("unused")
    private void fallbackEnviarSolicitacao(Pedido pedido, String transactionId, Throwable ex) {
        falhasTransitoriasNoRetry.remove();
        throw new PagamentoGatewayIndisponivelException(
                "Gateway de pagamento temporariamente indisponivel. Tente novamente em instantes.");
    }

    private void simularLatenciaExterna() {
        if (latenciaMs <= 0) {
            return;
        }
        try {
            Thread.sleep(latenciaMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PagamentoGatewayIndisponivelException("Interrupcao ao contactar gateway de pagamento.");
        }
    }
}
