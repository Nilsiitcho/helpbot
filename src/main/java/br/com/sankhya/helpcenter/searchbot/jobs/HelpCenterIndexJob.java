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
import org.springframework.stereotype.Component;

import br.com.sankhya.helpcenter.searchbot.elasticsearch.ElasticSearchClient;
import br.com.sankhya.helpcenter.searchbot.kayako.KayakoClient;
import br.com.sankhya.helpcenter.searchbot.utils.ArticleData;

@Component
public class HelpCenterIndexJob {

	private Integer	quantidadeInserida		= 0;
	private Integer	quantidadeAtualizada	= 0;

	private Logger	logger					= LogManager.getLogger(HelpCenterIndexJob.class);

	public void indexArticlesFromKayakoInElastic() throws Exception {
		KayakoClient kayakoClient = new KayakoClient();

		if (elasticDatabaseIsEmpty()) {
			logger.info("Inserindo base de dados do Kayako no ElasticSearch...");
			List<ArticleData> articlesFromKayako = kayakoClient.getAllArticles();
			indexForFirstTime(articlesFromKayako);
			logger.info("Base de dados inserida.");
		} else {
			logger.info("Verificando se existem artigos novos ou atualizados...");
			HashMap<Integer, Date> newAndUpdatedArticlesIds = kayakoClient.getIdAndDateOfUpdatedArticles();
			if (!newAndUpdatedArticlesIds.isEmpty()) {
				indexWhenElasticIsNotEmpty(newAndUpdatedArticlesIds);
			}
			logger.info("Atualizando a quantidade de views e upvote_count!");
			List<ArticleData> viewsAndUpvotes = kayakoClient.getAllArticlesViewsAndUpvote();
			if (viewsAndUpvotes != null && !viewsAndUpvotes.isEmpty()) {
				updateViewsAndUpVotes(viewsAndUpvotes);
			}
		}

		kayakoClient.closeClient();
		logResults();
	}

	private void updateViewsAndUpVotes(List<ArticleData> viewsAndUpVotes) throws Exception {
		ElasticSearchClient elasticClient = new ElasticSearchClient();
		List<ArticleData> elasticArticles = elasticClient.getAllViewsAndUpvotes();

		for (ArticleData kayako : viewsAndUpVotes) {
			for (ArticleData elastic : elasticArticles) {
				if (kayako.getId().equals(elastic.getId())) {
					if (!kayako.getViews().equals(elastic.getViews()) || !kayako.getUpvoteCount().equals(elastic.getUpvoteCount())) {
						elasticClient.upDateViews(kayako.getId(), kayako.getViews(), kayako.getUpvoteCount());
					}
				}
			}
		}

	}

	private void indexWhenElasticIsNotEmpty(HashMap<Integer, Date> newArticles) throws ClientProtocolException, IOException, Exception {
		ElasticSearchClient elasticSearchClient = new ElasticSearchClient();
		HashMap<Integer, Date> elasticMap = new HashMap<>();

		for (Map.Entry<Integer, Date> mapArticle : newArticles.entrySet()) {

			Date lastUpdate;
			lastUpdate = elasticSearchClient.getDateFromArticleToCompare(mapArticle.getKey().toString());
			if (lastUpdate != null) {
				elasticMap.put(mapArticle.getKey(), lastUpdate);
			}

		}
		compareArticlesDates(newArticles, elasticMap);
	}

	private void compareArticlesDates(HashMap<Integer, Date> kayakoIdsMap, HashMap<Integer, Date> elasticMap) throws IOException {
		ElasticSearchClient elasticClient = new ElasticSearchClient();
		KayakoClient kayakoClient = new KayakoClient();

		for (Map.Entry<Integer, Date> articleKayako : kayakoIdsMap.entrySet()) {
			if (elasticMap.containsKey(articleKayako.getKey())) {
				if (elasticMap.get(articleKayako.getKey()).before(articleKayako.getValue())) {
					ArticleData articleUpdated = kayakoClient.getArticleById(articleKayako.getKey());

					elasticClient.updateArticle(articleUpdated, articleUpdated.getId().toString());
					quantidadeAtualizada++;
					logger.debug("Arquivo atualizado. Id: " + articleUpdated.getId());
				}
			} else {
				ArticleData articleUpdated = kayakoClient.getArticleById(articleKayako.getKey());

				elasticClient.index(articleUpdated, articleUpdated.getId().toString());
				quantidadeInserida++;
				logger.debug("Artigo Inserido: " + articleUpdated.getId());
			}
		}
	}

	private void indexForFirstTime(List<ArticleData> articlesFromKayako) {
		ElasticSearchClient elasticClient = new ElasticSearchClient();
		for (ArticleData article : articlesFromKayako) {
			elasticClient.index(article, article.getId().toString());
			quantidadeInserida++;
		}
	}

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
