package com.itriad.parkinglot.services;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.itriad.parkinglot.domain.RegistroVeiculo;
import com.itriad.parkinglot.domain.Veiculo;
import com.itriad.parkinglot.dto.PeriodoDataDTO;
import com.itriad.parkinglot.dto.RelatorioDTO;
import com.itriad.parkinglot.repositories.RegistroVeiculoRepository;
import com.itriad.parkinglot.services.calculoPreco.CalculoPreco;
import com.itriad.parkinglot.services.calculoPreco.RegraFDS;
import com.itriad.parkinglot.services.calculoPreco.RegraSemanaManha;
import com.itriad.parkinglot.services.calculoPreco.RegraSemanaTarde;
import com.itriad.parkinglot.services.validators.EntradaVeiculoAtivoValidator;
import com.itriad.parkinglot.services.validators.EntradaVeiculoHorarioValidator;
import com.itriad.parkinglot.services.validators.RegistroVeiculoValidator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RegistroVeiculoService {
    @Autowired
    RegistroVeiculoRepository registroRepository;

    private Double calculaValorAPagar(RegistroVeiculo registro) {
        CalculoPreco cPreco = new RegraFDS();
        cPreco.link(new RegraSemanaTarde()).link(new RegraSemanaManha());
        return cPreco.executaCalculo(registro.getEntrada(), new Date());
    }

    public List<Veiculo> buscaVeiculosAtivos() {
        return registroRepository.findRegistrosWithActiveVeiculos()
            .stream().map((registro) -> registro.getVeiculo()).collect(Collectors.toList());
    }

    public void registrarVeiculo(Veiculo veiculo) {
        RegistroVeiculo registro = new RegistroVeiculo();
        veiculo.setPlaca(veiculo.getPlaca().toUpperCase());
        registro.setVeiculo(veiculo);
        registro.setEntrada(new Date());

        RegistroVeiculoValidator validator = new EntradaVeiculoAtivoValidator(registroRepository);
        validator.link(new EntradaVeiculoHorarioValidator());
        validator.executaValidacao(registro);

        registroRepository.save(registro);
    }

    public RegistroVeiculo registrarSaidaVeiculo(String placa) {
        RegistroVeiculo registro = registroRepository.findRegistroWithActiveVeiculoByPlaca(placa);
        registro.setSaida(new Date());
        registro.setValorPago(calculaValorAPagar(registro));
        return registroRepository.save(registro);
    }

    public Double verificaValorAPagar(String placa) {
        RegistroVeiculo registro = registroRepository.findRegistroWithActiveVeiculoByPlaca(placa);
        return calculaValorAPagar(registro);
    }

    public List<RelatorioDTO> buscaDadosRelatorio(PeriodoDataDTO periodo) {

        Calendar cal = Calendar.getInstance();
        cal.setTime(periodo.getFim());
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);

        return registroRepository.generateReportBetween(periodo.getInicio(), cal.getTime());
    }
}