package br.com.sankhya.helpcenter.searchbot.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

//TODO: apagar campos desnecessários
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArticleData {

	private static DateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	static {
		yyyyMMddHHmmss.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	@JsonProperty("id")
	private Integer	id;

	@JsonProperty("_id")
	private String	idElastic;

	private String	title;
	private String	content;

	private Date	updatedAt;

	@JsonProperty("helpcenter_url")
	private String	link;

	private String	sectionName;

	private String	sectionVisibility;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getSectionName() {
		return sectionName;
	}

	public void setSectionName(String sectionName) {
		this.sectionName = sectionName;
	}

	public String getSectionVisibility() {
		return sectionVisibility;
	}

	public void setSectionVisibility(String visibility) {
		this.sectionVisibility = visibility;
	}

	public String getIdElastic() {
		return idElastic;
	}

	public void setIdElastic(String idElastic) {
		this.idElastic = idElastic;
	}

	@JsonProperty("updated_at")
	private void formatDate(String dataString) throws ParseException {
		dataString = dataString.replaceAll("\\+00:00", "");
		this.updatedAt = yyyyMMddHHmmss.parse(dataString);
	}

	@JsonProperty("titles")
	private void unpackTitleFromJson(ArrayList<JsonNode> titles) {
		JsonNode jsonTitle = titles.get(0);
		this.title = jsonTitle.get("translation").textValue();
	}

	@JsonProperty("section")
	private void unpackSectionFromJson(JsonNode section) {
		JsonNode json = section.get("titles");
		String sourceSectionName = json.get(0).get("translation").textValue();

		this.sectionName = sourceSectionName.replaceAll(" ", "-").toLowerCase();
		this.sectionVisibility = section.get("visibility").textValue();
	}

	@JsonProperty("contents")
	private void unpackContentFromJson(ArrayList<JsonNode> contents) {
		JsonNode jsonContent = contents.get(0);
		String contentStringRaw = jsonContent.get("translation").textValue();
		Document contentRaw = Jsoup.parse(contentStringRaw, "UTF-8");

		Elements contentElements = contentRaw.select("body p");

		StringBuilder contentBuffer = new StringBuilder();

		contentElements.forEach(e -> {
			contentBuffer.append(e.text());
		});

		this.content = formatContent(contentBuffer.toString());
	}

	private static String formatContent(String text) {

		//text = text.replaceAll("&nbsp;", " ");
		//text = text.replaceAll("&gt", ">");
		//text = text.replaceAll("&quot;", "\"");
		text = text.replaceAll("[\\\"\\]\\[\\}\\{\\\\•]", "");
		text = text.replaceAll("\\\"\\w+", "“");
		text = text.replaceAll("\\w+\\\"", "”");
		//text = text.replaceAll("\\,", "");
		//text = text.replaceAll("\\.", "");

		return text;
	}

	/**
	 * Extrai as informacoes contidas no json de resposta
	 * do ElasticSearch
	 * 
	 * @param source
	 * @throws ParseException
	 */
	@JsonProperty("_source")
	private void unpackSourceFromJson(JsonNode source) throws ParseException {
		if (source != null) {
			this.id = source.get("id").asInt();
			this.title = source.get("title").textValue();
			this.content = source.get("content").textValue();
			this.link = source.get("helpcenter_url").textValue();

			this.sectionName = source.get("section_name").textValue();
			this.sectionVisibility = source.get("section_visibility").textValue();

			String dataString = source.get("last_update").textValue().replaceAll("\\.000Z", "");

			this.updatedAt = yyyyMMddHHmmss.parse(dataString);
		}
	}

	@Override
	public String toString() {
		return "\n{\n id: " + this.getId() + ",\n title:" + this.getTitle() + ",\n content: " + this.getContent() + ",\n last_update:  " + this.getUpdatedAt() + ",\n link: " + this.getLink() + ",\n section_name: " + this.getSectionName() + ",\n section_visibility: " + this.getSectionVisibility() + ",\n elastic_id: " + this.getIdElastic() + "\n}";
	}

}