package testes;

import java.io.IOException;
import java.util.Collections;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;

import br.com.sankhya.helpcenter.searchbot.utils.AWSRequestSigningApacheInterceptor;

public class AmazonElasticsearchServiceSample {
	private static String				serviceName			= "es";
	private static String				region				= "us-east-1";
	private static String				aesEndpoint			= "https://search-jivahelpcenter-yongp25azh7i6extjgicavdy7e.us-east-1.es.amazonaws.com";

	private static String				sampleDocument		= "{" + "\"title\":\"Walk the Line\"," + "\"director\":\"James Mangold\"," + "\"year\":\"2005\"}";
	private static String				indexingPath		= "/jiva/helpcenter";

	static final AWSCredentialsProvider	credentialsProvider	= new DefaultAWSCredentialsProviderChain();
	
	public static void main(String[] args) throws IOException {


		RestClient esClient = esClient(serviceName, region);
		HttpEntity entity;
		//for
			entity = new NStringEntity(sampleDocument, ContentType.APPLICATION_JSON);
			Response response = esClient.performRequest("POST", indexingPath, Collections.emptyMap(), entity);
		System.out.println(response.toString());

	}

	// Adds the interceptor to the ES REST client
	public static RestClient esClient(String serviceName, String region) {
		AWS4Signer signer = new AWS4Signer();
		signer.setServiceName(serviceName);
		signer.setRegionName(region);
		HttpRequestInterceptor interceptor = new AWSRequestSigningApacheInterceptor(serviceName, signer, credentialsProvider);
		return RestClient.builder(HttpHost.create(aesEndpoint)).setHttpClientConfigCallback(hacb -> hacb.addInterceptorLast(interceptor)).build();
	}
}
