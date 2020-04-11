package com.boomaa.apreleasedpuller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class DownloadReleasedFRQs {
	private static String examName;
	private static String baseUrl = "https://apcentral.collegeboard.org";
	private static int prevYear = 2019;
	
	public static void main(String[] args) throws IOException {
		examName = args[0];
		String connectUrl = baseUrl + "/courses/ap-" + examName + "/exam";
		
		int startYear = 1980;
		int endYear = prevYear;
		if (args[1] != null) {
			startYear = Integer.parseInt(args[1]);
		}
		if (args[2] != null) {
			endYear = Integer.parseInt(args[2]);
		}
		
		
		setupFilestructure();
		
		Document past = Jsoup.connect(connectUrl + "/past-exam-questions").get();
		Elements tables = past.body().getElementsByClass("cb-accordion").first().getElementsByClass("panel");
		for (int i = 0;i < tables.size();i++) {
			Element table = tables.get(i).getElementsByClass("table").get(0);
			int year = Integer.parseInt(table.getElementsByTag("caption").first().text().replaceAll("[^\\d]", ""));
			if (year < startYear) {
				continue;
			} else if (year > endYear) {
				break;
			}
			Elements hrefList = table.getElementsByTag("a");
			downloadAllFromTable(hrefList, year);
		}
		
		if (endYear >= prevYear) {
			Document current = Jsoup.connect(connectUrl).get();
			Elements tableLinks = current.body().getElementById("main-content").getElementsByClass("node").get(7).getElementsByTag("a");
			downloadAllFromTable(tableLinks, prevYear);
		}
	}
	
	private static void downloadAllFromTable(Elements tableLinks, int year) {
		for (int i = 0;i < tableLinks.size();i++) {
			String href = tableLinks.get(i).attributes().get("href");
			if (!href.contains("collegeboard.org")) {
				href = baseUrl + href;
			}
			String name = tableLinks.get(i).text();
			downloadFile(href, getPathFromDocName(name, year));
		}
	}
	
	private static String getPathFromDocName(String linkName, int year) {
		linkName = checkNameIrregularities(linkName);
		switch (linkName) {
			case "Free-Response Questions":
				return examName + "/Questions/" + year + ".pdf";
			case "Scoring Guidelines":
			case "Student Performance Q&A":
			case "Scoring Statistics":
			case "Score Distributions":
				return examName + "/" + linkName + "/" + year + ".pdf";
			default: 
				linkName = linkName.replaceAll("[^\\d]", "");
				makeFolder(examName + "/Samples & Commentary/" + year);
				return examName + "/Samples & Commentary/" + year + "/Question " + linkName + ".pdf";
		}
	}
	
	private static String checkNameIrregularities(String linkName) {
		switch (linkName) {
			case "Chief Reader Report":
				return "Student Performance Q&A";
			case "Scoring Distribution":
				return "Score Distributions";
			case "All Questions":
				return "Free-Response Questions";
			default:
				return linkName;
		}
	}
	
	private static void setupFilestructure() {
		makeFolder(examName);
		makeFolder(examName + "/Questions");
		makeFolder(examName + "/Samples & Commentary");
		makeFolder(examName + "/Score Distributions");
		makeFolder(examName + "/Scoring Guidelines");
		makeFolder(examName + "/Scoring Statistics");
		makeFolder(examName + "/Student Performance Q&A");
	}
	
	private static File makeFolder(String name) {
		File folder = new File(name);
		if (!folder.exists()) {
			folder.mkdir();
		}
		return folder;
	}
	
	private static void downloadFile(String url, String outputPath) {
		try (BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
			FileOutputStream fileOutputStream = new FileOutputStream(outputPath)) {
		    byte dataBuffer[] = new byte[1024];
		    int bytesRead;
		    while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
		        fileOutputStream.write(dataBuffer, 0, bytesRead);
		    }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
