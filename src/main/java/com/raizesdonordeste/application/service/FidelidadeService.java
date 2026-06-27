package com.raizesdonordeste.application.service;

import com.raizesdonordeste.api.dto.fidelidade.FidelidadeResgateDTO;
import com.raizesdonordeste.api.dto.fidelidade.MovimentoFidelidadeResponseDTO;
import com.raizesdonordeste.api.dto.fidelidade.SaldoFidelidadeResponseDTO;
import com.raizesdonordeste.api.exception.BusinessException;
import com.raizesdonordeste.domain.enums.TipoMovimentoFidelidade;
import com.raizesdonordeste.domain.model.Fidelidade;
import com.raizesdonordeste.domain.model.MovimentoFidelidade;
import com.raizesdonordeste.domain.model.Usuario;
import com.raizesdonordeste.infrastructure.repository.FidelidadeRepository;
import com.raizesdonordeste.infrastructure.repository.MovimentoFidelidadeRepository;
import com.raizesdonordeste.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FidelidadeService {

    public static final int PONTOS_MINIMOS_RESGATE = 50;
    public static final BigDecimal VALOR_POR_PONTO = new BigDecimal("0.05");

    private final FidelidadeRepository fidelidadeRepository;
    private final MovimentoFidelidadeRepository movimentoRepository;
    private final UsuarioService usuarioService;
    private final AuditoriaService auditoriaService;

    public SaldoFidelidadeResponseDTO consultarSaldo() {
        Usuario usuario = usuarioService.buscarEntidadePorEmail(SecurityUtils.getEmailAutenticado());
        validarConsentimentoLgpd(usuario);
        Fidelidade fidelidade = buscarPorUsuarioAutenticado();
        return SaldoFidelidadeResponseDTO.builder()
                .saldoPontos(fidelidade.getSaldoPontos())
                .build();
    }

    public List<MovimentoFidelidadeResponseDTO> consultarHistorico() {
        Usuario usuario = usuarioService.buscarEntidadePorEmail(SecurityUtils.getEmailAutenticado());
        validarConsentimentoLgpd(usuario);
        Fidelidade fidelidade = buscarPorUsuarioAutenticado();
        return movimentoRepository.findByFidelidadeIdOrderByDataMovimentoDesc(fidelidade.getId())
                .stream()
                .map(m -> MovimentoFidelidadeResponseDTO.builder()
                        .tipo(m.getTipo())
                        .pontos(m.getPontos())
                        .descricao(m.getDescricao())
                        .dataMovimento(m.getDataMovimento())
                        .build())
                .toList();
    }

    @Transactional
    public SaldoFidelidadeResponseDTO resgatarPontos(FidelidadeResgateDTO dto) {
        Usuario usuario = usuarioService.buscarEntidadePorEmail(SecurityUtils.getEmailAutenticado());
        resgatarPontos(usuario, dto.getPontos());
        return consultarSaldo();
    }

    @Transactional
    public BigDecimal aplicarResgate(Usuario cliente, int pontos) {
        resgatarPontos(cliente, pontos);
        return VALOR_POR_PONTO.multiply(BigDecimal.valueOf(pontos));
    }

    @Transactional
    public void acumularPontos(Usuario cliente, BigDecimal valorPedido) {
        int pontos = valorPedido.intValue();
        if (pontos <= 0) {
            return;
        }

        Fidelidade fidelidade = buscarOuCriar(cliente);
        fidelidade.setSaldoPontos(fidelidade.getSaldoPontos() + pontos);
        fidelidadeRepository.save(fidelidade);

        registrarMovimento(fidelidade, TipoMovimentoFidelidade.ACUMULO, pontos,
                "Acúmulo por pagamento aprovado");
    }

    @Transactional
    public void estornarResgate(Usuario cliente, int pontos) {
        if (pontos <= 0) {
            return;
        }

        Fidelidade fidelidade = buscarOuCriar(cliente);
        fidelidade.setSaldoPontos(fidelidade.getSaldoPontos() + pontos);
        fidelidadeRepository.save(fidelidade);

        registrarMovimento(fidelidade, TipoMovimentoFidelidade.ACUMULO, pontos,
                "Estorno de resgate por pagamento recusado");
    }

    private void resgatarPontos(Usuario cliente, int pontos) {
        validarConsentimentoLgpd(cliente);

        if (pontos < PONTOS_MINIMOS_RESGATE) {
            throw new BusinessException("Resgate mínimo de " + PONTOS_MINIMOS_RESGATE + " pontos.");
        }

        Fidelidade fidelidade = buscarOuCriar(cliente);

        if (fidelidade.getSaldoPontos() < pontos) {
            throw new BusinessException("Saldo de pontos insuficiente.");
        }

        fidelidade.setSaldoPontos(fidelidade.getSaldoPontos() - pontos);
        fidelidadeRepository.save(fidelidade);

        registrarMovimento(fidelidade, TipoMovimentoFidelidade.RESGATE, pontos,
                "Resgate de pontos");

        auditoriaService.registrar(cliente.getEmail(), "RESGATE_FIDELIDADE",
                pontos + " pontos resgatados");
    }

    private Fidelidade buscarPorUsuarioAutenticado() {
        Usuario usuario = usuarioService.buscarEntidadePorEmail(SecurityUtils.getEmailAutenticado());
        return buscarOuCriar(usuario);
    }

    private void validarConsentimentoLgpd(Usuario usuario) {
        if (!Boolean.TRUE.equals(usuario.getConsentimentoLgpd())) {
            throw new BusinessException("Consentimento LGPD necessário para usar o programa de fidelidade.");
        }
    }

    private Fidelidade buscarOuCriar(Usuario usuario) {
        return fidelidadeRepository.findByUsuarioId(usuario.getId())
                .orElseGet(() -> fidelidadeRepository.save(Fidelidade.builder()
                        .usuario(usuario)
                        .saldoPontos(0)
                        .build()));
    }

    private void registrarMovimento(Fidelidade fidelidade, TipoMovimentoFidelidade tipo,
                                    int pontos, String descricao) {
        movimentoRepository.save(MovimentoFidelidade.builder()
                .fidelidade(fidelidade)
                .tipo(tipo)
                .pontos(pontos)
                .descricao(descricao)
                .build());
    }
}
