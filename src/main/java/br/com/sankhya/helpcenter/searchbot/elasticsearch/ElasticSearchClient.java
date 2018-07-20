package br.com.sankhya.helpcenter.searchbot.elasticsearch;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.sankhya.helpcenter.searchbot.utils.AWSRequestSigningApacheInterceptor;
import br.com.sankhya.helpcenter.searchbot.utils.ArticleData;
import br.com.sankhya.helpcenter.searchbot.utils.ModelDataUtil;
import br.com.sankhya.helpcenter.searchbot.utils.ModelDataUtil.ResponseType;

/**
 * 
 * @author Nilson Neto
 * 
 * Classe responsavel por fazer a comunicacao e as operacoes
 * entre o sistema e o servidor do ElasticSearch.
 *
 */
public class ElasticSearchClient {

	private static final String					ESENDPOINT			= "https://search-jiva-k5qrjushn4sgnqers2tkxvsoh4.us-east-1.es.amazonaws.com";
	private static final String					SERVICENAME			= "es";
	private static final String					REGION				= "us-east-1";
	private static final String					ELASTICURL			= ESENDPOINT + "/jiva/helpcenter/_search?&size=10000";

	private static DateFormat					yyyyMMddHHmmss		= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	static {
		yyyyMMddHHmmss.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private static final AWSCredentialsProvider	credentialsProvider	= new DefaultAWSCredentialsProviderChain();

	private final static Logger					logger				= LogManager.getLogger(ElasticSearchClient.class);
	private static Map<String, Object>			arquivoMAP			= new HashMap<>();
	private static RestHighLevelClient			client				= buildClient(SERVICENAME, REGION);

	/**
	 * 
	 * Pega a quantidade total de artigos indexados no servidor do ElastiSearc.
	 * 
	 * @return Quantidade total de artigos
	 * @throws ParseException
	 * @throws IOException
	 */
	public Integer getTotalOfArticles() throws ParseException, IOException {

		CloseableHttpClient httpclient = HttpClients.createDefault();

		HttpGet httpGet = new HttpGet(ELASTICURL);
		httpGet.addHeader("Content-Type", "application/json");

		CloseableHttpResponse response = httpclient.execute(httpGet);

		int status = response.getStatusLine().getStatusCode();
		if (status >= 200 && status < 300) {
			String responseBody = EntityUtils.toString(response.getEntity());

			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(responseBody);

			return root.get("hits").get("total").asInt();
		} else {
			throw new ClientProtocolException("[ElasticSearchClient: getTotalOfArticles] - Status inesperado: " + status);
		}

	}

	/**
	 * 
	 * Pega todos os artigos indexados na base de dados do ElasticSearch.
	 * 
	 * @return Lista com todos os artigos.
	 * @throws IOException
	 */
	public List<ArticleData> getAllArticles() throws IOException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {
			HttpGet httpGet = new HttpGet(ELASTICURL);
			httpGet.addHeader("Content-Type", "application/json");

			return ModelDataUtil.convertToArticleData(httpclient.execute(httpGet), ResponseType.ELASTIC_SEARCH);
		} finally {
			httpclient.close();
		}
	}

	/**
	 * Indexa o artigo recebido no ElasticSearch, tendo como ID o parametro informado.
	 * 
	 * @param article
	 * @param id
	 */
	public void indexArticle(ArticleData article, String id) {
		try {

			IndexRequest request = new IndexRequest("jiva", "helpcenter", id);
			arquivoMAP.put("id", article.getId());
			arquivoMAP.put("title", article.getTitle());
			arquivoMAP.put("search_content", article.getSearchContent());
			arquivoMAP.put("last_update", article.getUpdatedAt());
			arquivoMAP.put("helpcenter_url", article.getLink());
			arquivoMAP.put("section_name", article.getSectionName());
			arquivoMAP.put("views", article.getViews());
			arquivoMAP.put("upvote_count", article.getUpvoteCount());
			arquivoMAP.put("keywords", article.getKeywords());

			String searchContent = article.getSearchContent();

			if (searchContent != null) {
				searchContent = (searchContent.length() > 200) ? searchContent.substring(0, 200).concat("...") : searchContent;
				arquivoMAP.put("summary", searchContent);
			}

			request.source(arquivoMAP, XContentType.JSON);
			client.index(request).getResult();

		} catch (Exception e) {
			logger.error("[ElasticSearchClient: index] Não foi possível indexar arquivo no ElasticSearch. Causa: ", e);
		}
	}

	/**
	 * 
	 * Deleta do indice, caso exista, o artigo com o ID informado.
	 * 
	 * @param articleId
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public void deleteArticle(String articleId) throws ClientProtocolException, IOException {
		DeleteRequest delete = new DeleteRequest("jiva", "helpcenter", articleId);
		client.delete(delete).getResult();
	}

	/**
	 * Atualiza o artigo da base de dados do ElasticSearch que possui o Id informado como parametro.
	 * 
	 * @param article
	 * @param id
	 * @throws IOException
	 */
	public void updateArticle(ArticleData article, String id) throws IOException {

		if (id != null) {
			UpdateRequest request = new UpdateRequest("jiva", "helpcenter", id);

			arquivoMAP.put("id", article.getId());
			arquivoMAP.put("title", article.getTitle());
			arquivoMAP.put("search_content", article.getSearchContent());
			arquivoMAP.put("last_update", article.getUpdatedAt());
			arquivoMAP.put("helpcenter_url", article.getLink());
			arquivoMAP.put("section_name", article.getSectionName());
			arquivoMAP.put("upvote_count", article.getUpvoteCount());
			arquivoMAP.put("keywords", article.getKeywords());

			String searchContent = article.getSearchContent();

			if (searchContent != null) {
				searchContent = (searchContent.length() > 200) ? searchContent.substring(0, 200).concat("...") : searchContent;
				arquivoMAP.put("summary", searchContent);
			}

			request.doc(arquivoMAP, XContentType.JSON);
			client.update(request);

		} else {
			logger.error("[ElasticSearchClient: updateArticle] - Id do artigo não foi especificado!");
		}
	}

	/**
	 * 
	 * Metodo que cria e retorna uma instancia do Client para comunicacao com o servidor 
	 * do ElasticSearch.
	 * 
	 * @param serviceName
	 * @param region
	 * @return Client EelasticSearch
	 */
	public static RestHighLevelClient buildClient(String serviceName, String region) {

		AWS4Signer signer = new AWS4Signer();
		signer.setServiceName(serviceName);
		signer.setRegionName(region);
		HttpRequestInterceptor interceptor = new AWSRequestSigningApacheInterceptor(serviceName, signer, credentialsProvider);
		RestClientBuilder builder = RestClient.builder(HttpHost.create(ESENDPOINT)).setHttpClientConfigCallback(hacb -> hacb.addInterceptorLast(interceptor));

		return new RestHighLevelClient(builder);
	}

	/**
	 * 
	 * Pega a data da ultima atualizacao do artigo com o id informado/
	 * 
	 * @param id
	 * @return Data da ultima atualizacao.
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws java.text.ParseException
	 */
	public Date getDateFromArticle(String id) throws ClientProtocolException, IOException, java.text.ParseException {

		String url = ESENDPOINT + "/jiva/helpcenter/" + id + "/?_source_include=last_update";

		CloseableHttpClient httpclient = HttpClients.createDefault();

		HttpGet httpGet = new HttpGet(url);
		httpGet.addHeader("Content-Type", "application/json");

		CloseableHttpResponse response = httpclient.execute(httpGet);

		int status = response.getStatusLine().getStatusCode();

		if (status >= 200 && status < 300) {
			String responseBody = EntityUtils.toString(response.getEntity());

			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(responseBody);

			String dataString = root.get("_source").get("last_update").asText();
			String found = root.get("found").asText();

			if (found.equals("true")) {
				Date dataDeAtualização = new Date();
				dataDeAtualização = yyyyMMddHHmmss.parse(dataString);

				return dataDeAtualização;
			} else
				return null;

		} else if (status >= 404) {
			return null;
		} else {
			throw new ClientProtocolException("Status inesperado: " + status);
		}
	}

	/**
	 * 
	 * Pega os IDs de todos os artigos indexados no ElasticSearch.
	 * 
	 * @return Lista com os IDs dos artigos.
	 * 
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public List<Integer> getAllIds() throws ClientProtocolException, IOException {

		String url = ESENDPOINT + "/jiva/helpcenter/_search?_source_include=.id&size=10000";
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(url);
		httpGet.addHeader("Content-Type", "application/json");

		CloseableHttpResponse response = httpclient.execute(httpGet);
		int status = response.getStatusLine().getStatusCode();

		if (status >= 200 && status < 300) {
			List<Integer> ids = new ArrayList<>();

			String responseBody = EntityUtils.toString(response.getEntity());

			ObjectMapper mapper = new ObjectMapper();
			JsonNode data = mapper.readTree(responseBody).path("hits").path("hits");

			data.forEach(hit -> {
				ids.add(hit.get("_id").asInt());
			});

			return ids;
		} else {
			throw new ClientProtocolException("[ElasticSearchClient: getDateFromArticleToCompare] - Status inesperado: " + status);
		}
	}

	/**
	 * 
	 * Pega o numero de views e votos positivos de todos os artigos.
	 * 
	 * @return Lista com as views e votod positivos de cada artigo.
	 * 
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public List<ArticleData> getAllViewsAndUpvotes() throws ClientProtocolException, IOException {

		String url = ESENDPOINT + "/jiva/helpcenter/_search?_source_include=.id,views,upvote_count&size=10000";
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(url);
		httpGet.addHeader("Content-Type", "application/json");

		CloseableHttpResponse response = httpclient.execute(httpGet);
		int status = response.getStatusLine().getStatusCode();

		if (status >= 200 && status < 300) {
			List<ArticleData> articlesList = new ArrayList<>();

			String responseBody = EntityUtils.toString(response.getEntity());

			ObjectMapper mapper = new ObjectMapper();
			JsonNode data = mapper.readTree(responseBody).path("hits").path("hits");

			data.forEach(hit -> {
				ArticleData article = new ArticleData();
				article.setId(hit.get("_id").asInt());
				article.setViews(hit.get("_source").get("views").asInt());
				article.setUpvoteCount(hit.get("_source").get("upvote_count").asInt());
				articlesList.add(article);
			});

			return articlesList;
		} else {
			throw new ClientProtocolException("[ElasticSearchClient: getDateFromArticleToCompare] - Status inesperado: " + status);
		}
	}

	/**
	 * 
	 * Atualiza o numero de views e votos positivos de um determinado artigo.
	 * 
	 * @param id
	 * @param views
	 * @param upvoteCount
	 * @throws IOException
	 */
	public void updateViewsAndUpvotes(Integer id, Integer views, Integer upvoteCount) throws IOException {

		if (id != null) {
			UpdateRequest request = new UpdateRequest("jiva", "helpcenter", id.toString());

			arquivoMAP.put("id", id);
			arquivoMAP.put("views", views);
			arquivoMAP.put("upvote_count", upvoteCount);

			request.doc(arquivoMAP, XContentType.JSON);
			client.update(request);

		} else {
			logger.error("[ElasticSearchClient: updateArticle] - Id do artigo não foi especificado!");
		}
	}
}
