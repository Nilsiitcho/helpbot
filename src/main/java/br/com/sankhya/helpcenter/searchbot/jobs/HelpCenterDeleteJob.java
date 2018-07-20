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

/**
 * 
 * @author Nilson Neto
 * 
 * Classe responsavel por realizar o job de deletar do ElasticSearch
 * os artigos que foram excluidos da base de dados do Kayako.
 *
 */
public class HelpCenterDeleteJob {

	private Integer	quantidadeExcluida	= 0;

	private Logger	logger				= LogManager.getLogger(HelpCenterDeleteJob.class);


	/**
	 * 
	 * Inicializa os clients e parametros necessarios para realizacao do Job.
	 * 
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public void start() throws ClientProtocolException, IOException {
		ElasticSearchClient elasticClient = new ElasticSearchClient();
		KayakoClient kayakoClient = new KayakoClient();

		List<Integer> idsFromElastic = new ArrayList<>();
		List<Integer> idsFromKayako = new ArrayList<>();

		idsFromElastic = elasticClient.getAllIds();
		idsFromKayako = kayakoClient.getAllArticlesIds();

		deleteArticles(idsFromElastic, idsFromKayako, elasticClient);
		logResults();
		//Necessario fechar o client do Kayako externamente.
		kayakoClient.closeClient();

	}

	/**
	 * 
	 * Metodo que realiza de fato a exclusao dos artigos no elasticSearch.
	 * 
	 * @param elasticArticlesIds lista de IDs de todos os artigos do ElasticSearch.
	 * @param kayakoArticlesIds Lista de IDs de todos os artigos do Kayako.
	 * @param elasticSearchClient
	 * 
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private void deleteArticles(List<Integer> elasticArticlesIds, List<Integer> kayakoArticlesIds, ElasticSearchClient elasticSearchClient) throws ClientProtocolException, IOException {
		logger.info("Comparando base de dados do ElasticSearch com a base do Kayako...");
		HashMap<Integer, Integer> kayakoMap = new HashMap<>();

		for (Integer id : kayakoArticlesIds) {
			kayakoMap.put(id, 0);
		}

		for (Integer id : elasticArticlesIds) {
			if (!kayakoMap.containsKey(id)) {
				elasticSearchClient.deleteArticle(id.toString());
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
