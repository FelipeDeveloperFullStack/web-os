package br.com.finance.financeiro_pessoal.service.fin;

import java.util.Date;

import br.com.finance.financeiro_pessoal.domain.fin.SaldoFinanceiro;
import br.com.finance.financeiro_pessoal.domain.fin.type.TipoFinanceiro;
import br.com.finance.financeiro_pessoal.service.GenericService;

public interface SaldoFinanceiroService extends GenericService<SaldoFinanceiro>{

	SaldoFinanceiro findByDataMovimentoAndTipoFinanceiro(Date dataMovimento, TipoFinanceiro tipoFinanceiro);
}