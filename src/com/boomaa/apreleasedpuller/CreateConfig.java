package com.boomaa.apreleasedpuller;

import com.boomaa.s3uploader.NEOAWS;
import org.update4j.Configuration;
import org.update4j.FileMetadata;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CreateConfig {
    public static void main(String[] args) {
        Configuration config = Configuration.builder()
                .baseUri("https://raw.githubusercontent.com/Boomaa23/APReleasedPuller/master/")
                .basePath("${user.dir}")
                .property("default.launcher.main.class", "com.boomaa.apreleasedpuller.DownloadReleasedFRQs")
                .file(FileMetadata
                        .readFrom("ap-pull.jar")
                        .uri("ap-pull.jar")
                        .classpath())
                .build();
        try (Writer out = Files.newBufferedWriter(Paths.get("config.xml"))) {
            config.write(out);
        } catch (IOException e) {
            e.printStackTrace();
        }

        NEOAWS.create(args[0], args[1]);
        NEOAWS.AWS_EXECUTOR.uploadFile("config.xml", false);
    }
}
