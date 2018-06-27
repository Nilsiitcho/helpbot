package br.com.sankhya.helpcenter.searchbot.utils;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ModelDataUtil {

	private static DateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	static {
		yyyyMMddHHmmss.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	public enum ResponseType {
		KAYAKO, ELASTIC_SEARCH
	}

	private final static Logger logger = LogManager.getLogger(ModelDataUtil.class);

	public static List<ArticleData> convertToArticleData(HttpResponse response, ResponseType type) throws IOException {

		if (checkStatus(response)) {
			String responseBody = EntityUtils.toString(response.getEntity());

			ObjectMapper mapper = new ObjectMapper();
			JsonNode data = null;

			if (type == ResponseType.KAYAKO) {
				data = mapper.readTree(responseBody).path("data");
			} else if (type == ResponseType.ELASTIC_SEARCH) {
				data = mapper.readTree(responseBody).path("hits").path("hits");
			} else {
				throw new IllegalArgumentException("Tipo de response type desconhecido.");
			}

			return Arrays.asList(mapper.treeToValue(data, ArticleData[].class));
		} else {
			return null;
		}

	}

	public static Date convertData(String dataString) {

		Date dataDeAtualização = new Date();
		try {
			dataDeAtualização = yyyyMMddHHmmss.parse(dataString);
		} catch (java.text.ParseException e) {
			logger.error("Não foipossível pegar os artigos do ElasticSearch. Causa: ", e);
			EmailSenderUtil.reportErrorViaEmail(e.getMessage());
		}
		return dataDeAtualização;
	}

	public static ArticleData convertToSingleArticle(HttpResponse response) throws ParseException, IOException {
		if (checkStatus(response)) {
			String responseBody = EntityUtils.toString(response.getEntity());
			ObjectMapper mapper = new ObjectMapper();
			JsonNode data = null;
			data = mapper.readTree(responseBody).path("data");

			return mapper.treeToValue(data, ArticleData.class);
		} else {
			return null;
		}

	}

	private static boolean checkStatus(HttpResponse response) {
		if (response == null) {
			throw new IllegalArgumentException("Reponse nao pode ser nulo.");
		}
		int status = response.getStatusLine().getStatusCode();
		if (status >= 200 && status < 300) {
			return true;
		} else {
			logger.error(new ClientProtocolException("Unexpected response status: " + status));
			return false;
		}
	}

}