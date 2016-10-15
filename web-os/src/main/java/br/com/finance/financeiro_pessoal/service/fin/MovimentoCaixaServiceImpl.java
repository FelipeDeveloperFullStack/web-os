package br.com.finance.financeiro_pessoal.service.fin;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.finance.financeiro_pessoal.domain.fin.MovimentoCaixa;
import br.com.finance.financeiro_pessoal.repository.fin.MovimentoCaixaRepository;

@Service
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class MovimentoCaixaServiceImpl implements MovimentoCaixaService{

	@Autowired
	private MovimentoCaixaRepository movimentoCaixaRepository;
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public MovimentoCaixa salvar(MovimentoCaixa movimentoCaixa) {
		return movimentoCaixaRepository.save(movimentoCaixa);
	}

	@Override
	public List<MovimentoCaixa> listarTodos() {
		return movimentoCaixaRepository.findAll();
	}

	@Override
	public MovimentoCaixa procurarPeloID(Long id) {
		return movimentoCaixaRepository.findOne(id);
	}

}
