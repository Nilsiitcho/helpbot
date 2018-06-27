package testes;

import java.io.IOException;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class logInElasticCloud {
	
	private static boolean isLoged() throws ParseException, IOException {

		String encoding = "bmlsc29ubmV0bzpMdWNpQG5lMTIz";

		CloseableHttpClient httpclient = HttpClients.createDefault();

		HttpGet httpGet = new HttpGet("https://d91cfb626e17469391e3b9cb25f880d1.us-east-1.aws.found.io:9243/jiva/helpcenter/_search");

		httpGet.addHeader("Content-Type", "application/json");
		httpGet.setHeader("Authorization", "Basic " + encoding);

		CloseableHttpResponse response = httpclient.execute(httpGet);

		int status = response.getStatusLine().getStatusCode();

		if (status >= 200 && status < 300) {

			return true;

		} else {
			throw new ClientProtocolException("Status inesperado: " + status);
		}
	}

	public static void main(String[] args) throws ParseException, IOException {

		if (isLoged()) {
			System.out.println("Logado com sucesso!");
		} else {
			System.out.println("Não foi possível realizar o login!");
		}
	}	


}
