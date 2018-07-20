package br.com.sankhya.helpcenter.searchbot.robot;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.sankhya.helpcenter.searchbot.jobs.HelpCenterDeleteJob;
import br.com.sankhya.helpcenter.searchbot.jobs.HelpCenterIndexJob;
import br.com.sankhya.helpcenter.searchbot.utils.EmailSenderUtil;

/**
 * 
 * @author Nilson Neto
 * 
 * Classe responsavel por orquestrar a execucao das jobs do robo.
 *
 */
@Component
public class HelpcenterRobot {

	final Logger	logger	= LogManager.getLogger(HelpcenterRobot.class);

	//	@Scheduled(fixedDelay = 24 * 60 * 60 * 1000)
	@Scheduled(cron = "0 0 21 * * MON-FRI")
	public void executeTasks() {
		logger.info("Iniciando tarefa...");
		try {

			HelpCenterIndexJob helpIndex = new HelpCenterIndexJob();
			HelpCenterDeleteJob helpDelete = new HelpCenterDeleteJob();

			helpIndex.start();

			helpDelete.start();

		} catch (Exception e) {
			logger.error("Não foi possível concluir as tarefas. Causa: ", e);
			EmailSenderUtil.sendEmail(e.getMessage());
			logger.info("Programa finalizado!");
		}
		System.gc();
	}

}
