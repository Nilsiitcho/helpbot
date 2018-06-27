package br.com.sankhya.helpcenter.searchbot.robot;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.sankhya.helpcenter.searchbot.jobs.HelpCenterDeleteJob;
import br.com.sankhya.helpcenter.searchbot.jobs.HelpCenterIndexJob;
import br.com.sankhya.helpcenter.searchbot.utils.EmailSenderUtil;

@Component
public class HelpcenterRobot {

	final Logger logger = LogManager.getLogger(HelpcenterRobot.class);

	@Scheduled(fixedDelay = 24 * 60 * 60 * 1000)
	public void executaTarefas() {
		logger.info("Iniciando tarefa...");

		try {

			HelpCenterIndexJob helpIndex = new HelpCenterIndexJob();
			HelpCenterDeleteJob helpDelete = new HelpCenterDeleteJob();

			try {
				helpIndex.indexArticlesFromKayakoInElastic();
			} catch (Exception e) {
				logger.error("[indexArticlesFromKayakoInElastic] - Erro ao indexar arqui vos no ElasticSearch. Causa: ", e);
			}

			helpDelete.deleteExcludedArticles();

		} catch (IOException e) {
			logger.error("Não foi possível concluir as tarefas. Causa: ", e);
			EmailSenderUtil.reportErrorViaEmail(e.getMessage());
		}
		logger.info("Programa finalizado!");

		System.gc();
	}

}
