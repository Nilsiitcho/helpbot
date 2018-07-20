package br.com.sankhya.helpcenter.searchbot.jobs;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.com.sankhya.helpcenter.searchbot.elasticsearch.ElasticSearchClient;
import br.com.sankhya.helpcenter.searchbot.kayako.KayakoClient;
import br.com.sankhya.helpcenter.searchbot.utils.ArticleData;

/**
 * 
 * @author Nilson Neto
 * 
 * Classe responsavel por realizar o Job de alterar e incluir
 * os novos artigos do Kayako, no ElasticSearch.
 *
 */
public class HelpCenterIndexJob {

	private Integer	quantidadeInserida		= 0;
	private Integer	quantidadeAtualizada	= 0;

	private Logger	logger					= LogManager.getLogger(HelpCenterIndexJob.class);

	/**
	 * 
	 * Realiza as configuracoes iniciais necessarias
	 * e inicia a execucao do Job.
	 * 
	 * @throws Exception
	 */
	public void start() throws Exception {
		KayakoClient kayakoClient = new KayakoClient();

		if (elasticDatabaseIsEmpty()) {
			logger.info("Inserindo base de dados do Kayako no ElasticSearch...");
			List<ArticleData> articlesFromKayako = kayakoClient.getAllArticles();
			firstIndexation(articlesFromKayako);
			logger.info("Base de dados inserida.");
		} else {
			logger.info("Verificando se existem artigos novos ou atualizados...");
			HashMap<Integer, Date> newAndUpdatedArticlesIds = kayakoClient.getIdAndDateOfUpdatedArticles();
			if (!newAndUpdatedArticlesIds.isEmpty()) {
				prepareToIndexArticles(newAndUpdatedArticlesIds);
			}
		}
		//Eh necessario fechar o client do Kayako externamente.
		kayakoClient.closeClient();
		logResults();
	}

	/**
	 * 
	 * Monta uma lista com os artigos que foram atualizados recentemente
	 * e solicita a indexacao dos artigos novos.
	 * 
	 * @param newArticles Hash com o ID e as datas dos artigos novos.
	 * 
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws Exception
	 */
	private void prepareToIndexArticles(HashMap<Integer, Date> newArticles) throws ClientProtocolException, IOException, Exception {
		ElasticSearchClient elasticSearchClient = new ElasticSearchClient();
		HashMap<Integer, Date> elasticMap = new HashMap<>();

		for (Map.Entry<Integer, Date> mapArticle : newArticles.entrySet()) {
			Date lastUpdate;
			lastUpdate = elasticSearchClient.getDateFromArticle(mapArticle.getKey().toString());
			if (lastUpdate != null) {
				elasticMap.put(mapArticle.getKey(), lastUpdate);
			} else if (lastUpdate == null) {
				indexArticle(elasticSearchClient, mapArticle);
			}
		}
		compareArticlesDates(newArticles, elasticMap);
	}

	/**
	 * 
	 * Realiza de fato a indexacao de um artigo no ElasticSearch.
	 * 
	 * @param mapArticle HashMap com o ID e a data da ultima atualizacao
	 * do artigo a ser indexado.
	 * 
	 * @param elasticSearchClient
	 * @throws IOException
	 */
	private void indexArticle(ElasticSearchClient elasticSearchClient, Map.Entry<Integer, Date> mapArticle) throws IOException {
		KayakoClient kayakoClient = new KayakoClient();
		ArticleData articleUpdated = kayakoClient.getArticle(mapArticle.getKey());
		elasticSearchClient.indexArticle(articleUpdated, articleUpdated.getId().toString());
		quantidadeInserida++;
	}

	/**
	 * 
	 * Compara as data dos artigos mais recentes do Kayako, com seu espelho no ElasticSearch.
	 * 
	 * @param kayakoIdsMap HashMap com os IDs e datas do Kayako.
	 * @param elasticMap HashMap com os IDs e datas do Kayako.
	 * 
	 * @throws IOException
	 */
	private void compareArticlesDates(HashMap<Integer, Date> kayakoIdsMap, HashMap<Integer, Date> elasticMap) throws IOException {
		ElasticSearchClient elasticClient = new ElasticSearchClient();
		KayakoClient kayakoClient = new KayakoClient();

		for (Map.Entry<Integer, Date> articleKayako : kayakoIdsMap.entrySet()) {
			if (elasticMap.containsKey(articleKayako.getKey())) {
				if (elasticMap.get(articleKayako.getKey()).before(articleKayako.getValue())) {
					ArticleData articleUpdated = kayakoClient.getArticle(articleKayako.getKey());

					elasticClient.updateArticle(articleUpdated, articleUpdated.getId().toString());
					quantidadeAtualizada++;
					logger.debug("Arquivo atualizado. Id: " + articleUpdated.getId());
				}
			}
		}
	}

	/**
	 * 
	 * Realiza a indexacao dos artigos do Kayako, na base de dados do ElasticSearch,
	 * quando o ElasticSearch esta vazio.
	 * 
	 * @param articlesFromKayako Lista de artigos do Kayako.
	 */
	private void firstIndexation(List<ArticleData> articlesFromKayako) {
		ElasticSearchClient elasticClient = new ElasticSearchClient();
		for (ArticleData article : articlesFromKayako) {
			elasticClient.indexArticle(article, article.getId().toString());
			quantidadeInserida++;
		}
	}

	/**
	 * 
	 * Verifica se a base de dados do ElasticSearch esta vazia. 
	 * 
	 * @return
	 * @throws ParseException
	 * @throws IOException
	 */
	private boolean elasticDatabaseIsEmpty() throws ParseException, IOException {
		ElasticSearchClient elasticClient = new ElasticSearchClient();
		Integer totalArticles = elasticClient.getTotalOfArticles();

		if (totalArticles == null || totalArticles == 0) {
			return true;
		}
		return false;
	}

	private void logResults() {
		logger.info("************************************************************");
		logger.info(" QUANTIDADE DE ARTIGOS INDEXADOS: " + quantidadeInserida);
		logger.info(" QUANTIDADE DE ARTIGOS ATUALIZADOS: " + quantidadeAtualizada);
		logger.info("************************************************************");
	}

}
