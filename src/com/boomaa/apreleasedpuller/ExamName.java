package com.boomaa.apreleasedpuller;

public class ExamName {
    private final String formatted;
    private final String unformatted;

    public ExamName(String input, boolean isFormatted) {
        if (isFormatted) {
            this.formatted = input;
            this.unformatted = artFormatCheck(input.toLowerCase().replaceAll(" ", "-"));
        } else {
            this.unformatted = input;
            this.formatted = toFormatted(input);
        }
    }

    private String artFormatCheck(String intermediate) {
        return intermediate.contains("art") ? artFormatCheck(artFormatCheck(intermediate, "3"), "2") : intermediate;
    }

    private String artFormatCheck(String intermediate, String search) {
        int ios = intermediate.indexOf(search);
        if (ios != -1) {
            return intermediate.substring(0, ios + 1) + "-" + intermediate.substring(ios + 1);
        }
        return intermediate;
    }

    public String getFormatted() {
        return formatted;
    }

    public String getUnformatted() {
        return unformatted;
    }

    public static String toFormatted(String exam) {
        StringBuilder builder = new StringBuilder();
        String[] words = exam.split("-");
        for (int i = 0; i < words.length; i++) {
            switch (words[i]) {
                case "ap":
                    builder.append("AP");
                    break;
                case "ab":
                    builder.append("AB");
                    break;
                case "bc":
                    builder.append("BC");
                    break;
                case "and":
                    builder.append("and");
                    break;
                default:
                    builder.append(words[i].substring(0, 1).toUpperCase()).append(words[i].substring(1));
                    break;
            }
            if (i != words.length - 1 && Character.isAlphabetic(words[i].charAt(0))) {
                builder.append(' ');
            }
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return formatted;
    }
}
