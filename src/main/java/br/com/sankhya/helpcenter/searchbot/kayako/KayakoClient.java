package br.com.sankhya.helpcenter.searchbot.kayako;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.sankhya.helpcenter.searchbot.utils.ArticleData;
import br.com.sankhya.helpcenter.searchbot.utils.EmailSenderUtil;
import br.com.sankhya.helpcenter.searchbot.utils.ModelDataUtil;
import br.com.sankhya.helpcenter.searchbot.utils.ModelDataUtil.ResponseType;

public class KayakoClient {

	private static final String	APIURL					= "https://jiva.kayako.com/api/v1";
	private static final String	CATEGORYURL				= APIURL + "/categories?include=*&fields=id&limit=100";
	private static final String	SECTIONSURL				= APIURL + "/sections.json?include=*&limit=100&fields=id,visibility&category_ids=";
	private static final String	ARTICLEURL				= APIURL + "/articles.json?include=*&filter=PUBLISHED&limit=10000&fields=id,titles,contents,updated_at,helpcenter_url,keywords,status,section,upvote_count,views&section_id=";
	private static final String	ARTICLEVIEWSURL			= APIURL + "/articles.json?include=*&filter=PUBLISHED&limit=10000&fields=id,upvote_count,views&section_id=";
	private static final String	ARTICLEIDANDLASTUPDATE	= APIURL + "/articles.json?filter=PUBLISHED&limit=10000&fields=id,updated_at&section_id=";
	private static final String	ARTICLEID				= APIURL + "/articles.json?filter=PUBLISHED&limit=10000&fields=id&section_id=";

	private CloseableHttpClient	httpclient				= HttpClients.createDefault();
	private Logger				logger					= LogManager.getLogger(KayakoClient.class);

	/**
	 * 
	 * Pega o ID de todas as categorias do Kayako.
	 * 
	 * @return Lista com os IDs.
	 * @throws IOException
	 */
	public List<Integer> getAllCategoryIds() throws IOException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(CATEGORYURL);
		httpGet.addHeader("Content-Type", "application/json");

		return processIdListResponse(httpclient.execute(httpGet));
	}

	/**
	 * 
	 * Pega os IDs das secoes do Kayako.
	 * 
	 * @return Lista com os IDs.
	 * @throws IOException
	 */
	public List<Integer> getAllSectionIds() throws IOException {
		List<Integer> categoryIds = getAllCategoryIds();

		HttpGet httpGet = new HttpGet(SECTIONSURL + categoryIds.toString().replace("[", "").replace(" ", "").replace("]", ""));
		httpGet.addHeader("Content-Type", "application/json");
		return processIdListOfSections(httpclient.execute(httpGet));
	}

	private List<Integer> processIdListResponse(HttpResponse response) throws IOException {
		if (response == null) {
			IllegalArgumentException e = new IllegalArgumentException("Reponse nao pode ser nulo.");
			logger.error("[processIdListResponse] - Erro ao processar a lista com os ids. Causa: ", e);
			EmailSenderUtil.sendEmail(e.getMessage());
		}

		int status = response.getStatusLine().getStatusCode();

		if (status >= 200 && status < 300) {
			String responseBody = EntityUtils.toString(response.getEntity());

			ObjectMapper mapper = new ObjectMapper();
			JsonNode data = mapper.readTree(responseBody).path("data");

			List<Integer> ids = new ArrayList<>();

			data.forEach(c -> {
				ids.add(c.get("id").asInt());
			});
			return ids;
		}
		throw new ClientProtocolException("Unexpected response status: " + status);
	}

	private List<Integer> processIdListOfSections(HttpResponse response) throws IOException {
		if (response == null) {
			IllegalArgumentException e = new IllegalArgumentException("Reponse nao pode ser nulo.");
			logger.error("[processIdListResponse] - Erro ao processar a lista com os ids das sections. Causa: ", e);
			EmailSenderUtil.sendEmail(e.getMessage());
		}

		int status = response.getStatusLine().getStatusCode();

		if (status >= 200 && status < 300) {
			String responseBody = EntityUtils.toString(response.getEntity());

			ObjectMapper mapper = new ObjectMapper();
			JsonNode data = mapper.readTree(responseBody).path("data");

			List<Integer> ids = new ArrayList<>();

			data.forEach(c -> {
				String visibility = c.get("visibility").textValue();
				if (visibility.equals("PUBLIC")) {
					ids.add(c.get("id").asInt());
				}
			});
			return ids;
		}
		throw new ClientProtocolException("Unexpected response status: " + status);
	}

	/**
	 * 
	 * Pega os artigos de uma determinada secao.
	 * 
	 * @param sectionId
	 * 
	 * @return Lista com os artigos.
	 * @throws IOException
	 */
	public List<ArticleData> getArticles(Integer sectionId) throws IOException {
		if (sectionId == null) {
			IllegalArgumentException e = new IllegalArgumentException("Section ID nao pode ser nulo.");
			logger.error("[getArticles] - Não foi possíverl pegar os artigos do Kayako. Causa: ", e);
			EmailSenderUtil.sendEmail(e.getMessage());
		}

		String url = (ARTICLEURL + sectionId);
		HttpGet httpGet = new HttpGet(url);
		httpGet.addHeader("Content-Type", "application/json");

		return ModelDataUtil.convertToArticleData(httpclient.execute(httpGet), ResponseType.KAYAKO);
	}

	/**
	 * 
	 * Pega todos os artigos do Kayako.
	 * 
	 * @return lista com os artigos.
	 * @throws IOException
	 */
	public List<ArticleData> getAllArticles() throws IOException {
		List<ArticleData> articlesList = new ArrayList<>();
		List<Integer> sectionIds = getAllSectionIds();
		for (Integer section : sectionIds) {
			articlesList.addAll(getArticles(section));
		}
		return articlesList;
	}

	/**
	 * 
	 * Pega os artigos novos e atualizados recentemente.
	 * 
	 * @return Lista com os artigos.
	 * @throws IOException
	 */
	public List<ArticleData> getUpdatedAndNewArticles() throws IOException {
		List<ArticleData> newArticles = new ArrayList<>();
		DateTime yesterday = new DateTime(new DateTime().minusDays(2));
		for (Map.Entry<Integer, Date> article : getIdAndDateOfUpdatedArticles().entrySet()) {
			DateTime lastUpdate = new DateTime(article.getValue());
			if (lastUpdate.isAfter(yesterday)) {
				newArticles.add(getArticle(article.getKey()));
			}
		}

		return newArticles;
	}

	/**
	 * 
	 * Pega o ID e e data da ultima atualizacao dos artigos atualizados recentemente.
	 * 
	 * @return HashMap com os IDs e data de atualizacao.
	 * @throws IOException
	 */
	public HashMap<Integer, Date> getIdAndDateOfUpdatedArticles() throws IOException {
		HashMap<Integer, Date> idsAndDatesBySection = new HashMap<>();
		HashMap<Integer, Date> updatedIdsAndDates = new HashMap<>();

		DateTime yesterday = new DateTime(new DateTime().minusDays(2));

		for (Integer sectionId : getAllSectionIds()) {
			idsAndDatesBySection = getIdAndDateOfArticlesBySection(sectionId);

			for (Map.Entry<Integer, Date> articleMap : idsAndDatesBySection.entrySet()) {

				DateTime lastUpdate = new DateTime(articleMap.getValue());
				if (lastUpdate.isAfter(yesterday)) {
					updatedIdsAndDates.put(articleMap.getKey(), articleMap.getValue());
				}
			}
		}
		return updatedIdsAndDates;
	}

	/**
	 * 
	 * Pega o ID e data da ultima atualizacao de todos os artigos de uma secao.
	 * 
	 * @param sectionId
	 * @return HashMap com o ID e data de atualizacao.
	 * @throws IOException
	 */
	public HashMap<Integer, Date> getIdAndDateOfArticlesBySection(Integer sectionId) throws IOException {
		if (sectionId == null) {
			IllegalArgumentException e = new IllegalArgumentException("Section ID nao pode ser nulo.");
			logger.error("[getIdAndDateOfArticlesOfASection] - Não foi possíverl pegar os artigos do Kayako. Causa: ", e);
			EmailSenderUtil.sendEmail(e.getMessage());
		}

		HashMap<Integer, Date> idsAndDates = new HashMap<>();
		String url = (ARTICLEIDANDLASTUPDATE + sectionId);
		HttpGet httpGet = new HttpGet(url);
		httpGet.addHeader("Content-Type", "application/json");

		CloseableHttpResponse response = httpclient.execute(httpGet);
		int status = response.getStatusLine().getStatusCode();

		if (status >= 200 && status < 300) {
			String responseBody = EntityUtils.toString(response.getEntity());

			ObjectMapper mapper = new ObjectMapper();
			JsonNode data = mapper.readTree(responseBody).path("data");

			data.forEach(hit -> {
				Integer id = hit.get("id").asInt();
				String dataString = hit.get("updated_at").textValue().replaceAll("\\+00:00", "");
				Date dataDeAtualização = ModelDataUtil.convertDate(dataString);
				idsAndDates.put(id, dataDeAtualização);
			});
		}
		return idsAndDates;
	}

	/**
	 * 
	 * Pega um artigo com o ID especificado.
	 * 
	 * @param id
	 * @return
	 * @throws IOException
	 */
	public ArticleData getArticle(Integer id) throws IOException {
		String url = (APIURL + "/articles/" + id + ".json?include=*&fields=id,titles,contents,updated_at,helpcenter_url,status,section");

		HttpGet httpGet = new HttpGet(url);
		httpGet.addHeader("Content-Type", "application/json");

		return ModelDataUtil.convertToSingleArticle(httpclient.execute(httpGet));
	}

	/**
	 * 
	 * Pega o ID de todos os artigos do Kayako.
	 * 
	 * @return Lista com os IDs.
	 * @throws IOException
	 */
	public List<Integer> getAllArticlesIds() throws IOException {
		List<Integer> articlesIdsList = new ArrayList<>();
		List<Integer> sectionIds = getAllSectionIds();

		for (Integer section : sectionIds) {
			articlesIdsList.addAll(getArticlesIdsBySection(section));
		}
		return articlesIdsList;
	}

	/**
	 * 
	 * Pega o ID dos artigos de uma determinada secao do kayako.
	 * 
	 * @param sectionId
	 * 
	 * @return Lista com os IDs.
	 * 
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private List<Integer> getArticlesIdsBySection(Integer sectionId) throws ClientProtocolException, IOException {
		String url = (ARTICLEID + sectionId);
		HttpGet httpGet = new HttpGet(url);
		httpGet.addHeader("Content-Type", "application/json");

		return processIdListResponse(httpclient.execute(httpGet));
	}

	/**
	 * 
	 * Fecha a conexao do Client com o servidor do Kayako.
	 * 
	 * @throws IOException
	 */
	public void closeClient() throws IOException {
		httpclient.close();
	}

	/**
	 * 
	 * Pega as views e os upvotes de todos os artigos do Kayako.
	 * 
	 * @return
	 * @throws IOException
	 */
	public List<ArticleData> getAllArticlesViewsAndUpvote() throws IOException {

		List<ArticleData> viewsAndUpvote = new ArrayList<>();
		List<Integer> sectionIds = getAllSectionIds();

		for (Integer id : sectionIds) {
			viewsAndUpvote.addAll(getArticlesViewsAndUpvoteBySection(id));
		}

		return viewsAndUpvote;
	}

	/**
	 * 
	 * Pega as views e os upvotes dos artigos de uma determinada secao.
	 * 
	 * @param sectionId
	 * @return Lsiat com o ID, views e upvote.
	 * @throws IOException
	 */
	public List<ArticleData> getArticlesViewsAndUpvoteBySection(Integer sectionId) throws IOException {
		if (sectionId == null) {
			IllegalArgumentException e = new IllegalArgumentException("Section ID nao pode ser nulo.");
			logger.error("[getArticles] - Não foi possíverl pegar os artigos do Kayako. Causa: ", e);
			EmailSenderUtil.sendEmail(e.getMessage());
		}

		String url = (ARTICLEVIEWSURL + sectionId);
		HttpGet httpGet = new HttpGet(url);
		httpGet.addHeader("Content-Type", "application/json");

		return ModelDataUtil.convertToArticleData(httpclient.execute(httpGet), ResponseType.KAYAKO);
	}
}
