package testes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.fasterxml.jackson.core.JsonProcessingException;

import br.com.sankhya.helpcenter.searchbot.utils.AWSRequestSigningApacheInterceptor;

public class InsereArquivoParaRoboRemover{
	private static String				serviceName			= "es";
	private static String				region				= "us-east-1";
	private static String				aesEndpoint			= "https://search-jiva-k5qrjushn4sgnqers2tkxvsoh4.us-east-1.es.amazonaws.com";

	static final AWSCredentialsProvider	credentialsProvider	= new DefaultAWSCredentialsProviderChain();
	
	private static Map<String, Object>	arquivoMAP	= new HashMap<>();
	private static RestHighLevelClient client = buildClient(serviceName, region);
	
	public static void main(String[] args) throws IOException {

		index();
	}
	
	private static void index() {
		try {
			
			IndexRequest request = new IndexRequest("jiva", "helpcenter", "9999");
			arquivoMAP.put("id", "9999");
			arquivoMAP.put("title", "Teste de inserção");
			arquivoMAP.put("content", "Testando a inserção de artigos no Amazon ElasticSearch Service");
			
			arquivoMAP.put("last_update", "2018-06-14T17:03:09.000Z");
			arquivoMAP.put("helpcenter_url", "www-google-com-br");
			arquivoMAP.put("section_name", "testes");
			arquivoMAP.put("section_visibility", "PUBLIC");
			request.source(arquivoMAP, XContentType.JSON);
			
			client.index(request);
			System.out.println("ARTIGO INDEXADO\n{\n id: 9999 ,\n title: Teste de inserção\n}");

		} catch (JsonProcessingException e) {
			System.out.println("Não foi possível indexar arquivo no ElasticSearch. Causa: " + e);
		} catch (IOException e) {
			System.out.println("Não foi possível indexar arquivo no ElasticSearch. Causa: " + e);
		}
	}

	public static RestHighLevelClient buildClient(String serviceName, String region) {
		AWS4Signer signer = new AWS4Signer();
		signer.setServiceName(serviceName);
		signer.setRegionName(region);
		HttpRequestInterceptor interceptor = new AWSRequestSigningApacheInterceptor(serviceName, signer, credentialsProvider);
		RestClientBuilder builder = RestClient.builder(HttpHost.create(aesEndpoint)).setHttpClientConfigCallback(hacb -> hacb.addInterceptorLast(interceptor));
		return new RestHighLevelClient(builder);
	}
}
