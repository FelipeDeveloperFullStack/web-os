package br.com.finance.financeiro_pessoal.service.fin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.finance.financeiro_pessoal.domain.fin.ContaCaixa;
import br.com.finance.financeiro_pessoal.domain.fin.MovimentoCaixa;
import br.com.finance.financeiro_pessoal.domain.fin.SaldoFinanceiro;
import br.com.finance.financeiro_pessoal.domain.fin.type.TipoFinanceiro;
import br.com.finance.financeiro_pessoal.domain.fin.type.TipoOrigemMovimento;
import br.com.finance.financeiro_pessoal.repository.fin.SaldoFinanceiroRepository;
import br.com.finance.financeiro_pessoal.util.DateUtil;

@Service
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class SaldoFinanceiroServiceImpl implements SaldoFinanceiroService {

	@Autowired
	private SaldoFinanceiroRepository saldoFinanceiroRepository;
	
	@Autowired
	private ContaCaixaService contaCaixaService;
	
	@Override
	public SaldoFinanceiro calcularSaldoFinanceiro(MovimentoCaixa movimentoCaixa){
		ContaCaixa contaCaixa = contaCaixaService.procurarPeloID(movimentoCaixa.getContaCaixa().getId());
		BigDecimal saldoInicial = BigDecimal.ZERO;
		if(!contaCaixa.isPossuiMovimento()){
			saldoInicial = contaCaixa.getSaldoInicial();
			calcularResumoCaixa(saldoInicial, movimentoCaixa);
			atualizarContaCaixa(contaCaixa);
		}else{
			calcularResumoCaixa(verificarSeExisteSaldoFinalDiaAnterior(movimentoCaixa, movimentoCaixa.getContaCaixa().getSaldoInicial()),movimentoCaixa);
		}
		return obterSaldoFinanceiro(movimentoCaixa);
	}
	
	private BigDecimal verificarSeExisteSaldoFinalDiaAnterior(MovimentoCaixa movimentoCaixa, BigDecimal saldoInicial){
		BigDecimal saldo = findByDataMovimentoSaldoFinalDiaAnterior(DateUtil.asLocalDate(movimentoCaixa.getDataMovimento()), movimentoCaixa.getContaCaixa(), TipoFinanceiro.CAIXA);
		if(saldo == BigDecimal.ZERO){
			return saldoInicial;
		}
		return saldo;
	}
	
	private void calcularResumoCaixa(BigDecimal saldoFinalAnteriorOuSaldoInicialAtual, MovimentoCaixa movimentoCaixa){
		BigDecimal saldoInicial = saldoFinalAnteriorOuSaldoInicialAtual;
		BigDecimal totalEntrada = BigDecimal.ZERO;
		BigDecimal totalSaida = BigDecimal.ZERO;
		BigDecimal saldoOperacional = BigDecimal.ZERO;
		
		if(movimentoCaixa.getTipoOrigemMovimento() == TipoOrigemMovimento.RECEBIMENTO){
			totalEntrada = movimentoCaixa.getValorMovimento();
		}
		if(movimentoCaixa.getTipoOrigemMovimento() == TipoOrigemMovimento.PAGAMENTO){
			totalSaida = movimentoCaixa.getValorMovimento();
		}
		if(movimentoCaixa.getTipoOrigemMovimento() == TipoOrigemMovimento.LANCAMENTO){
			totalEntrada = movimentoCaixa.getValorMovimento();
		}
		if(movimentoCaixa.getTipoOrigemMovimento() == TipoOrigemMovimento.TRANSFERENCIA_PARA_ORIGEM){
			totalEntrada = movimentoCaixa.getValorMovimento();
		}
		if(movimentoCaixa.getTipoOrigemMovimento() == TipoOrigemMovimento.TRANSFERENCIA_PARA_DESTINO){
			totalSaida = movimentoCaixa.getValorMovimento();
		}
		
		SaldoFinanceiro saldoFinanceiro = obterSaldoFinanceiro(movimentoCaixa);
		
		if(saldoFinanceiro == null){
			saldoFinanceiro = new SaldoFinanceiro();
			saldoOperacional = calcularSaldoOperacional(saldoFinanceiro.getTotalEntrada().add(totalEntrada), saldoFinanceiro.getTotalSaida().add(totalSaida));
		}else{
			saldoOperacional = calcularSaldoOperacional(saldoFinanceiro.getTotalEntrada().add(totalEntrada), saldoFinanceiro.getTotalSaida().add(totalSaida));
		}
		
		salvar(setarSaldoFinanceiro(movimentoCaixa,calcularSaldoFinal(saldoOperacional, saldoInicial)
				,saldoInicial, saldoOperacional
				,TipoFinanceiro.CAIXA, totalEntrada, totalSaida));
		
	}
	
	private SaldoFinanceiro setarSaldoFinanceiro(MovimentoCaixa movimentoCaixa
			,BigDecimal saldoFinal, BigDecimal saldoInicial
			,BigDecimal saldoOperacional, TipoFinanceiro tipoFinanceiro
			,BigDecimal totalEntrada, BigDecimal totalSaida){
		
		SaldoFinanceiro saldoFinanceiro = obterSaldoFinanceiro(movimentoCaixa);
		
		if(saldoFinanceiro == null){
			saldoFinanceiro = new SaldoFinanceiro();
			return setarSaldoFinanceiro(saldoFinanceiro, movimentoCaixa, saldoFinal
					, saldoInicial, saldoOperacional
					, tipoFinanceiro, totalEntrada, totalSaida);
		}else{
			return setarSaldoFinanceiro(saldoFinanceiro, movimentoCaixa
					, saldoFinal, saldoInicial, saldoOperacional
					, tipoFinanceiro, totalEntrada, totalSaida);
		}
	}
	
	private SaldoFinanceiro setarSaldoFinanceiro(SaldoFinanceiro saldoFinanceiro,MovimentoCaixa movimentoCaixa, BigDecimal saldoFinal, BigDecimal saldoInicial
			,BigDecimal saldoOperacional, TipoFinanceiro tipoFinanceiro
			,BigDecimal totalEntrada, BigDecimal totalSaida){
		
		saldoFinanceiro.setDataMovimento(movimentoCaixa.getDataMovimento());
		saldoFinanceiro.setSaldoFinal(saldoFinal);
		saldoFinanceiro.setSaldoInicial(saldoInicial);
		saldoFinanceiro.setSaldoOperacional(saldoOperacional);
		saldoFinanceiro.setTipoFinanceiro(tipoFinanceiro);
		saldoFinanceiro.setTotalEntrada(saldoFinanceiro.getTotalEntrada().add(totalEntrada));
		saldoFinanceiro.setTotalSaida(saldoFinanceiro.getTotalSaida().add(totalSaida));
		saldoFinanceiro.setContaCaixa(movimentoCaixa.getContaCaixa());
		
		return saldoFinanceiro;
	}
	
	private SaldoFinanceiro obterSaldoFinanceiro(MovimentoCaixa movimentoCaixa){
		SaldoFinanceiro saldoFinanceiro = saldoFinanceiroRepository.findByDataMovimentoAndTipoFinanceiroAndContaCaixa
				(movimentoCaixa.getDataMovimento(), TipoFinanceiro.CAIXA, movimentoCaixa.getContaCaixa());
		return saldoFinanceiro;
	}
	
	
	@Override
	public BigDecimal calcularSaldoFinal(BigDecimal saldoOperacional, BigDecimal saldoInicial){
		return saldoOperacional.add(saldoInicial);
	}
	
	@Override
	public BigDecimal calcularSaldoOperacional(BigDecimal totalEntrada, BigDecimal totalSaida){
		return totalEntrada.subtract(totalSaida);
	}
	
	private void atualizarContaCaixa(ContaCaixa contaCaixa){
		contaCaixa.setPossuiMovimento(Boolean.TRUE);
		contaCaixaService.salvar(contaCaixa);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public SaldoFinanceiro salvar(SaldoFinanceiro saldoFinanceiro) {
		return saldoFinanceiroRepository.save(saldoFinanceiro);
	}

	@Override
	public List<SaldoFinanceiro> listarTodos() {
		return saldoFinanceiroRepository.findAll();
	}

	@Override
	public SaldoFinanceiro procurarPeloID(Long id) {
		return saldoFinanceiroRepository.findOne(id);
	}

	@Override
	public SaldoFinanceiro findByDataMovimentoAndTipoFinanceiroAndContaCaixa(Date dataMovimento, TipoFinanceiro tipoFinanceiro, ContaCaixa contaCaixa) {
		return saldoFinanceiroRepository.findByDataMovimentoAndTipoFinanceiroAndContaCaixa(dataMovimento, tipoFinanceiro, contaCaixa);
	}

	@Override
	public BigDecimal findByDataMovimentoSaldoFinalDiaAnterior(LocalDate dataMovimentoAnteriorSaldoFinal, ContaCaixa contaCaixa, TipoFinanceiro tipoFinanceiro) {
		List<SaldoFinanceiro> saldosPorContaCaixa = findByContaCaixa(contaCaixa);
		List<SaldoFinanceiro> saldosAnterioresDaDataMovimento = findByDataMovimentoBefore(DateUtil.asDate(dataMovimentoAnteriorSaldoFinal));
		SaldoFinanceiro saldo = saldosAnterioresDaDataMovimento.get(saldosAnterioresDaDataMovimento.size() - 1);
		if(saldo == null){
			return BigDecimal.ZERO;
		}else{
			return saldo.getSaldoFinal();
		}
	}

	@Override
	public List<SaldoFinanceiro> findByContaCaixa(ContaCaixa contaCaixa) {
		return saldoFinanceiroRepository.findByContaCaixa(contaCaixa);
	}

	@Override
	public List<SaldoFinanceiro> findByDataMovimentoBefore(Date dataMovimento) {
		return saldoFinanceiroRepository.findByDataMovimentoBefore(dataMovimento);
	}
	


}
