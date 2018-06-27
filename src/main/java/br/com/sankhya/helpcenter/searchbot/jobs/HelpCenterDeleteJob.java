package br.com.sankhya.helpcenter.searchbot.jobs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.com.sankhya.helpcenter.searchbot.elasticsearch.ElasticSearchClient;
import br.com.sankhya.helpcenter.searchbot.kayako.KayakoClient;

public class HelpCenterDeleteJob {

	private Integer	quantidadeExcluida	= 0;

	private Logger	logger				= LogManager.getLogger(HelpCenterDeleteJob.class);

	public void deleteExcludedArticles() throws ClientProtocolException, IOException {
		ElasticSearchClient elasticClient = new ElasticSearchClient();
		KayakoClient kayakoClient = new KayakoClient();

		List<Integer> idsFromElastic = new ArrayList<>();
		List<Integer> idsFromKayako = new ArrayList<>();

		idsFromElastic = elasticClient.getAllIds();
		idsFromKayako = kayakoClient.getAllArticlesIds();

		deleteArticles(idsFromElastic, idsFromKayako, elasticClient);
		logResults();
		kayakoClient.closeClient();

	}

	private void deleteArticles(List<Integer> idsFromElastic, List<Integer> idsFromKayako, ElasticSearchClient elasticClient) throws ClientProtocolException, IOException {
		logger.info("Comparando base de dados do ElasticSearch com a base do Kayako...");
		HashMap<Integer, Integer> kayakoMap = new HashMap<>();

		for (Integer id : idsFromKayako) {
			kayakoMap.put(id, 0);
		}

		for (Integer id : idsFromElastic) {
			if (!kayakoMap.containsKey(id)) {
				elasticClient.deleteArticle(id.toString());
				quantidadeExcluida++;
				logger.debug("ARTIGO EXCLUÍDO\n{\n id: " + id + "\n}");
			}
		}
		logger.info("Comparação entre bases de dados concluída!");
	}

	private void logResults() {
		logger.info("************************************************************");
		logger.info("QUANTIDADE DE ARTIGOS EXCLUÍDOS: " + quantidadeExcluida);
		logger.info("************************************************************");
	}

}
