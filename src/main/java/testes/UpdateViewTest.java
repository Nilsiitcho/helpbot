package testes;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class UpdateViewTest {
	
	public void doUpdateArticleViews(String id) {
		String url = "https://search-jiva-k5qrjushn4sgnqers2tkxvsoh4.us-east-1.es.amazonaws.com/jiva/helpcenter/" + id;

		GetMethod searchRequest = new GetMethod(url);
		try {
			searchRequest.addRequestHeader("Content-Type", "application/json; charset=utf-8");

			HttpClient hc = new HttpClient();
			hc.executeMethod(searchRequest);

			String response = null;
			JsonObject arquivoJson;

			if (searchRequest.getStatusCode() == 200 || searchRequest.getStatusCode() == 201) {
				response = new String(searchRequest.getResponseBody(), "utf-8");
				arquivoJson = processArticleData(response);
				addOneView(arquivoJson, id);

			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (searchRequest != null) {
				searchRequest.releaseConnection();
			}
		}

	}

	private JsonObject processArticleData(String response) {
		JsonObject source = new JsonParser().parse(response).getAsJsonObject().get("_source").getAsJsonObject();

		JsonObject arquivoMAP = new JsonObject();

		arquivoMAP.addProperty("id", source.get("id").getAsString());
		arquivoMAP.addProperty("title", source.get("title").getAsString());
		arquivoMAP.addProperty("search_content", source.get("search_content").getAsString());
		arquivoMAP.addProperty("summary", source.get("search_content").getAsString());
		arquivoMAP.addProperty("last_update", source.get("last_update").getAsString());
		arquivoMAP.addProperty("helpcenter_url", source.get("helpcenter_url").getAsString());
		arquivoMAP.addProperty("section_name", source.get("section_name").getAsString());
		arquivoMAP.addProperty("upvote_count", source.get("upvote_count").getAsNumber().intValue());

		if (source.get("keywords") != null) {
			arquivoMAP.addProperty("keywords", source.get("keywords").getAsString());
		} else {
			arquivoMAP.addProperty("keywords", "");
		}

		Integer oldViews = source.get("views").getAsNumber().intValue();
		arquivoMAP.addProperty("views", (oldViews + 1));

		return arquivoMAP;
	}

	private void addOneView(JsonObject article, String id) {
		String url = "https://search-jiva-k5qrjushn4sgnqers2tkxvsoh4.us-east-1.es.amazonaws.com/jiva/helpcenter/" + id;

		PutMethod searchRequest = new PutMethod(url);

		try {

			searchRequest.setRequestEntity(new StringRequestEntity(article.toString()));
			searchRequest.addRequestHeader("Content-Type", "application/json; charset=utf-8");

			HttpClient hc = new HttpClient();
			hc.executeMethod(searchRequest);

			if (searchRequest.getStatusCode() == 200 || searchRequest.getStatusCode() == 201) {
				System.out.println("Views atualizadas!");
			} else {
				System.out.println("Status inexperado: " + searchRequest.getStatusCode());
			}

			System.out.println(searchRequest.getResponseBodyAsString());

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (searchRequest != null) {
				searchRequest.releaseConnection();
			}
		}

	}
	
	public static void main(String[] args) {
		
		UpdateViewTest teste = new UpdateViewTest();
		teste.doUpdateArticleViews("11572");
	}

}
