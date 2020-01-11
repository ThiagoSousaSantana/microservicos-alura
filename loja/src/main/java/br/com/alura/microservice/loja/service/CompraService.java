package br.com.alura.microservice.loja.service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import br.com.alura.microservice.loja.client.FornecedorClient;
import br.com.alura.microservice.loja.client.TransportadorClient;
import br.com.alura.microservice.loja.dto.CompraDTO;
import br.com.alura.microservice.loja.dto.InfoEntregaDTO;
import br.com.alura.microservice.loja.model.Compra;
import br.com.alura.microservice.loja.model.CompraState;
import br.com.alura.microservice.loja.repositories.CompraRepository;
import javassist.tools.rmi.ObjectNotFoundException;

@Service
public class CompraService {
	
	@Autowired
	private FornecedorClient fornecedorClient;
	
	@Autowired
	private CompraRepository repository;
	
	@Autowired
	private TransportadorClient transportadorClient;
	
	@HystrixCommand(fallbackMethod = "realizaCompraFallback",
			threadPoolKey = "realizaCompraPool")
	public Compra realizaCompra(CompraDTO compra) {
		
		var compraSalva = new Compra();
		compraSalva.setState(CompraState.RECEBIDO);
		compraSalva.setEnderecoDestino(compra.getEndereco().toString());
		repository.save(compraSalva);
		compra.setCompraId(compraSalva.getId());
		
		var infoFornecedor = fornecedorClient.getInfoPorEstado(compra.getEndereco().getEstado());
		
		var pedido = fornecedorClient.realizaPedido(compra.getItens());
		
		compraSalva.setState(CompraState.PEDIDO_REALIZADO);
		compraSalva.setPedidoId(pedido.getId());
		compraSalva.setTempoDePreparo(pedido.getTempoDePreparo());
		repository.save(compraSalva);
		
		var entregaDTO = new InfoEntregaDTO();
		entregaDTO.setPedidoId(pedido.getId());
		entregaDTO.setDataParaEntrega(LocalDate.now().plusDays(pedido.getTempoDePreparo()));
		entregaDTO.setEnderecoOrigem(infoFornecedor.getEndereco());
		entregaDTO.setEnderecoDestino(compra.getEndereco().toString());
		
		var voucher = transportadorClient.reservaEntrega(entregaDTO);
		compraSalva.setDataParaEntrega(voucher.getPrevisaoParaEntrega()); 
		compraSalva.setVoucher(voucher.getNumero());
		compraSalva.setState(CompraState.ENTREGA_RESERVADA);
		
		repository.save(compraSalva);
		
		return compraSalva;
	}
	
	public Compra realizaCompraFallback(CompraDTO compra) {
		if (compra.getCompraId() != null) {
			return repository.findById(compra.getCompraId()).get();
		}
		
		Compra compraFallback = new Compra();
		compraFallback.setEnderecoDestino(compra.getEndereco().toString());
		return compraFallback;
	}

	@HystrixCommand(threadPoolKey = "buscaPorIdPool")
	public Compra buscaPorId(Long id) throws ObjectNotFoundException {
		return repository.findById(id).orElseThrow(() -> new ObjectNotFoundException("Compra"));
	}
	
}
