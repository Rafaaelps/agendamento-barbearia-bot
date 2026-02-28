package com.barber.agendamento_bot.api;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;


@SpringBootApplication
@EnableScheduling
public class AgendamentoBotApplication {

	// faz o docker utilizar horario do brasil
	@PostConstruct
	public void ajustarFusoHorario() {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
		System.out.println("⏰ Relógio do Robô ajustado para: " + TimeZone.getDefault().getID());
	}

	public static void main(String[] args) {
		SpringApplication.run(AgendamentoBotApplication.class, args);
	}


}
