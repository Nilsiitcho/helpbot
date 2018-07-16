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

@JsonIgnoreProperties(ignoreUnknown = true)
public class ArticleData {

	private static DateFormat	yyyyMMddHHmmss	= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	static {
		yyyyMMddHHmmss.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	@JsonProperty("id")
	private Integer				id;

	@JsonProperty("_id")
	private String				idElastic;

	private String				title;

	private String				searchContent;

	private Date				updatedAt;

	@JsonProperty("helpcenter_url")
	private String				link;

	@JsonProperty("views")
	private Integer				views;

	@JsonProperty("upvote_count")
	private Integer				upvoteCount;

	@JsonProperty("keywords")
	private String				keywords;

	private String				sectionName;

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

	public String getSearchContent() {
		return searchContent;
	}

	public void setSearchContent(String searchContent) {
		this.searchContent = searchContent;
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

	public String getIdElastic() {
		return idElastic;
	}

	public void setIdElastic(String idElastic) {
		this.idElastic = idElastic;
	}

	public Integer getViews() {
		return views;
	}

	public void setViews(Integer views) {
		this.views = views;
	}

	public Integer getUpvoteCount() {
		return upvoteCount;
	}

	public void setUpvoteCount(Integer upvoteCount) {
		this.upvoteCount = upvoteCount;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	@JsonProperty("updated_at")
	private void formatDate(String dataString) throws ParseException {
		dataString = dataString.replaceAll("\\+00:00", "");
		this.updatedAt = yyyyMMddHHmmss.parse(dataString);
	}

	@JsonProperty("titles")
	private void unpackTitleFromJson(ArrayList<JsonNode> titles) {
		JsonNode jsonTitle = titles.get(0);
		this.title = jsonTitle.get("translation").textValue().replaceAll("\\\"", "'");
	}

	@JsonProperty("section")
	private void unpackSectionFromJson(JsonNode section) {
		JsonNode json = section.get("titles");
		String sourceSectionName = json.get(0).get("translation").textValue();

		this.sectionName = sourceSectionName.replaceAll(" ", "-").toLowerCase();
	}

	@JsonProperty("contents")
	private void unpackContentFromJson(ArrayList<JsonNode> contents) {
		JsonNode jsonContent = contents.get(0);
		String contentStringRaw = jsonContent.get("translation").textValue().replaceAll("A última edição deste tópico foi na[\\s+]\\D+[\\d+\\/*]+,\\s+\\D+[\\d{2}:\\d{2}:\\d{2}]+ \\DM", "");

		Document contentRaw = Jsoup.parse(contentStringRaw, "UTF-8");
		Elements contentElements = contentRaw.select("body p");
		StringBuilder contentBuffer = new StringBuilder();

		contentElements.forEach(e -> {
			contentBuffer.append(e.text());
		});

		this.searchContent = formatSearchContent(contentBuffer.toString());
	}

	private static String formatSearchContent(String text) {
		text = text.replaceAll("[\\\"\\]\\[\\}\\{\\\\•]", " ");
		text = text.replaceAll("\\\"", "'");
		text = text.replaceAll("A última edição deste tópico foi na[\\s+]\\D+[\\d+\\/*]+,\\s+\\D+[\\d{2}:\\d{2}:\\d{2}]+ \\DM", "");

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
			this.searchContent = source.get("search_content").textValue();
			this.link = source.get("helpcenter_url").textValue();
			this.sectionName = source.get("section_name").textValue();

			String dataString = source.get("last_update").textValue().replaceAll("\\.000Z", "");
			this.updatedAt = yyyyMMddHHmmss.parse(dataString);
		}
	}

	@Override
	public String toString() {
		return "\n{\n id: " + this.getId() + ",\n title:" + this.getTitle() + ",\n last_update:  " + this.getUpdatedAt() + ",\n link: " + this.getLink() + ",\n section_name: " + this.getSectionName() + ",\n elastic_id: " + this.getIdElastic() + "\n}";
	}

}