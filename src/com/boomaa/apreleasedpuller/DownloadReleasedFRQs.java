package com.boomaa.apreleasedpuller;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.*;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class DownloadReleasedFRQs {
    private static ExamName examName;
    private static String baseUrl = "https://apcentral.collegeboard.org";
    private static int prevYear = 2019;
    private static JFrame frame;

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            Document examListPage = null;
            try {
                examListPage = Jsoup.connect(baseUrl + "/courses").get();
            } catch (UnknownHostException e) {
                JOptionPane.showMessageDialog(new JFrame(), "No Internet Connection", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }

            //TODO add support for "language and culture" exams
            String[] noExamArray = {
                    "AP 2D Art and Design", "AP 3D Art and Design", "AP Capstone",
                    "AP Computer Science Principles", "AP Drawing", "AP Research", "AP Seminar"
            };
            List<String> noExamList = Arrays.asList(noExamArray);

            Elements courseLinks = examListPage.body().getElementById("main-content").getElementsByTag("a");
            List<String> examLinks = new ArrayList<>();
            String[] examLinksArr = new String[0];
            for (int i = 0;i < courseLinks.size();i++) {
                String link = courseLinks.get(i).attributes().get("href");
                if (link.contains("/courses")) {
                    int start = link.indexOf("/courses/") + 9;
                    int end = link.indexOf("?");
                    String name = ExamName.toFormatted(link.substring(start, end));
                    if (!noExamList.contains(name)) {
                        examLinks.add(name);
                    }
                }
            }
            Collections.sort(examLinks);
            initFrame(examLinks.toArray(examLinksArr));
        } else {
            int startYear = 1980;
            int endYear = prevYear;
            if (args[1] != null) {
                startYear = Integer.parseInt(args[1]);
            }
            if (args[2] != null) {
                endYear = Integer.parseInt(args[2]);
            }
            mainFunction(args[0], startYear, endYear);
        }
    }

    private static void mainFunction(String passedExamName, int startYear, int endYear) throws IOException {
        examName = new ExamName(passedExamName, true);
        String connectUrl = baseUrl + "/courses/" + examName.getUnformatted() + "/exam/";

        setupFilestructure();

        Document past = null;
        try {
            past = Jsoup.connect(connectUrl + "past-exam-questions").followRedirects(true).get();
        } catch (HttpStatusException e) {
            //TODO remove AP Gov hardcoding for differing URL schema (united states -> us)
            past = Jsoup.connect(connectUrl + "ap-us-government-and-politics-past-exam-questions").followRedirects(true).get();
        }
        Elements tables = past.body().getElementsByClass("cb-accordion").first().getElementsByClass("panel");
        for (int i = 0; i < tables.size(); i++) {
            Element table = tables.get(i).getElementsByClass("table").get(0);
            Elements title = table.getElementsByTag("caption");
            if (title.size() == 0) {
                title = tables.get(i).getElementsByClass("panel-title");
            }
            int year = Integer.parseInt(title.first().text().replaceAll("[^\\d]", ""));
            if (year < startYear) {
                continue;
            } else if (year > endYear) {
                break;
            }
            Elements hrefList = table.getElementsByTag("a");
            downloadAllFromTable(hrefList, year);
        }

        if (endYear >= prevYear) {
            Document current = Jsoup.connect(connectUrl).followRedirects(true).get();
            Elements allNodes = current.body().getElementById("main-content").getElementsByClass("node");
            Elements tableLinks = null;
            for (int i = 0;i < allNodes.size();i++) {
                int compare = allNodes.get(i).getElementsByTag("a").size();
                if (compare >= 9) {
                    tableLinks = allNodes.get(i).getElementsByTag("a");
                    break;
                }
            }
            downloadAllFromTable(tableLinks, prevYear);
        }
    }

    private static void initFrame(String[] exams) throws IOException {
        frame = new JFrame("AP Released FRQ Puller");
        frame.setIconImage(ImageIO.read(new URL(
                "https://4f7fdkogwz-flywheel.netdna-ssl.com/bigpicture/wp-content/uploads/sites/10/2018/01/AP-logo.png")));
        frame.setLayout(new FlowLayout());
        frame.getContentPane().add(new JComboBox<>(exams));
        frame.getContentPane().add(new OverlayField("Start Year", 6));
        frame.getContentPane().add(new OverlayField("End Year", 6));
        JButton download = new JButton("Download");

        download.addActionListener((e) -> {
            try {
                mainFunction(
                        getExamName(0),
                        getTextFieldInt(1),
                        getTextFieldInt(2)
                );
            } catch (NumberFormatException | IOException e1) {
                e1.printStackTrace();
            }
        });

        frame.getContentPane().add(download);
        JLabel href = new JLabel("<html><a href=''>github.com/Boomaa23/APReleasedPuller</a></html>");
        frame.getContentPane().add(href);
        href.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://github.com/Boomaa23/APReleasedPuller"));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }
        });

        frame.getRootPane().setDefaultButton(download);
        frame.setSize(300, 150);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private static int getTextFieldInt(int n) {
        return Integer.parseInt(((JTextField) frame.getContentPane().getComponent(n)).getText());
    }

    @SuppressWarnings("unchecked")
    private static String getExamName(int n) {
        return (String) ((JComboBox<String>) frame.getContentPane().getComponent(n)).getSelectedItem();
    }

    private static void downloadAllFromTable(Elements tableLinks, int year) {
        for (int i = 0;i < tableLinks.size();i++) {
            String href = tableLinks.get(i).attributes().get("href");
            if (!href.contains("collegeboard.org")) {
                href = baseUrl + href;
            }
            String name = tableLinks.get(i).text();
            if (checkExcludedFile(href)) {
                downloadFile(href, getPathFromDocName(name, year));
            }
            combineFile("Score Distributions", "subscore_temp.pdf", year);
            combineFile("Questions", "quest_temp.pdf", year);
            combineFile("Scoring Guidelines", "sg_temp.pdf", year);
        }
    }

    private static boolean checkExcludedFile(String href) {
        return href.contains(".pdf") && !href.contains("course-and-exam") && !href.contains("ced") && !href.contains("?") && !href.contains("quick-reference");
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
            case "Subscore Distribution":
            case "Nonaural Score Distributions":
                return "subscore_temp.pdf";
            case "Sight-Singing Questions":
                return "quest_temp.pdf";
            case "Sight-Singing Scoring Guidelines":
                return "sg_temp.pdf";
            default:
                makeFolder(examName + "/Samples & Commentary/" + year);
                String mod = "";
                if (linkName.contains("SA")) {
                    mod += " SAQ";
                } else if (linkName.contains("LEQ")) {
                    mod += " LEQ";
                } else if (linkName.contains("DBQ")) {
                    mod += " DBQ";
                } else if (linkName.contains("Sight-Singing")) {
                    mod += " Sight-Singing";
                }
                linkName = linkName.replaceAll("[^\\d]", "") + mod;
                return examName + "/Samples & Commentary/" + year + "/Question " + linkName + ".pdf";
        }
    }

    private static String checkNameIrregularities(String linkName) {
        switch (linkName) {
            case "Chief Reader Report":
                return "Student Performance Q&A";
            case "Scoring Distribution":
            case "Scoring Distributions":
            case "Aural Score Distributions":
                return "Score Distributions";
            case "All Questions":
            case "Theory Questions":
                return "Free-Response Questions";
            default:
                return linkName;
        }
    }

    private static void setupFilestructure() {
        makeFolder(examName.toString());
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
        System.out.println("Downloading from: " + url);
        try (BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(outputPath)) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void combineFile(String folderName, String tempFileName, int year) {
        String sdPath = examName + "/" + folderName + "/" + year + ".pdf";
        PDFMergerUtility merger = new PDFMergerUtility();
        try {
            File tempFile = new File(tempFileName);
            if (!tempFile.exists()) {
                return;
            }
            merger.addSource(new File(sdPath));
            merger.addSource(tempFile);
            merger.setDestinationFileName(sdPath);
            merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
            tempFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
