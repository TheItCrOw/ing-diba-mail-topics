package org.example;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasIOUtils;
import org.dkpro.core.api.resources.CompressionMethod;
import org.dkpro.core.io.xmi.XmiReader;
import org.dkpro.core.io.xmi.XmiWriter;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIDockerDriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIUIMADriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;
import org.texttechnologylab.annotation.DocumentAnnotation;
import org.xml.sax.SAXException;

public class Main {
    private static final String COMMA_DELIMITER = ",";

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java App <emails-csv-file>");
            System.exit(1);
        }
        System.out.println("Starting ING DiBa DUUI Pipeline");

        var rootPath = args[0];
        var emailsCsvPath = Paths.get(rootPath, "emails.csv");
        var rawXmiOutput = Paths.get(rootPath, "xmi-raw");
        var annotatedXmiOutput = Paths.get(rootPath, "xmi-annotated/input");

        System.out.println("Trying to read in all mails and transforming them to UIMA XMIs...");
        writeEmailsAsXMI(emailsCsvPath.toString(), rawXmiOutput.toString());
        System.out.println("Done!");

        System.out.println("Running the DUUI Pipeline now...");
        runDUUIPipeline(rawXmiOutput.toString(), annotatedXmiOutput.toString());
    }

    private static void runDUUIPipeline(String inputDir, String outputDir) throws Exception {
        var composer = new DUUIComposer()
                .withLuaContext(
                        new DUUILuaContext()
                                .withJsonLibrary()
                )
                .withSkipVerification(true)
                .addDriver(new DUUIUIMADriver())
                .addDriver(new DUUIDockerDriver());

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                XmiReader.class,
                XmiReader.PARAM_SOURCE_LOCATION, inputDir,
                XmiReader.PARAM_PATTERNS, new String[]{"[+]*.xmi*", "[-]10*.xmi*", "[-]11*.xmi*"},
                XmiReader.PARAM_LANGUAGE, "de",
                XmiReader.PARAM_LENIENT, true,
                XmiReader.PARAM_ADD_DOCUMENT_METADATA, false
        );

        composer.add(
                new DUUIDockerDriver.Component("docker.texttechnologylab.org/duui-spacy-de_core_news_sm:latest")
                        .withName("duui-spacy")
                        //.withImageFetching()
                        .withGPU(false)
        );

        composer.add(
                new DUUIDockerDriver.Component("docker.texttechnologylab.org/duui-transformers-berttopic:latest")
                        //.withImageFetching()
                        .withParameter("timeout", "9000")
                        .withParameter("selection", "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence")
        );

        composer.add(
                new DUUIDockerDriver.Component("docker.texttechnologylab.org/duui-transformers-sentiment-atomar-twitter-xlm-roberta-base-sentiment:latest")
                        .withParameter("selection", "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence")
                        .withParameter("timeout", "900")
                        .withImageFetching()
        );

        composer.add(
                new DUUIDockerDriver.Component("docker.texttechnologylab.org/duui-transformers-emotion-finetuned-twitter-xlm-roberta-base-emotion:latest")
                        .withImageFetching()
                        .withParameter("timeout", "900")
                        .withParameter("selection", "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence")
        );

        composer.add(
                new DUUIUIMADriver.Component(
                        AnalysisEngineFactory.createEngineDescription(
                                XmiWriter.class,
                                XmiWriter.PARAM_TARGET_LOCATION, outputDir,
                                XmiWriter.PARAM_SANITIZE_ILLEGAL_CHARACTERS, true,
                                XmiWriter.PARAM_PRETTY_PRINT, true,
                                XmiWriter.PARAM_COMPRESSION, CompressionMethod.GZIP,
                                XmiWriter.PARAM_VERSION, "1.1",
                                XmiWriter.PARAM_OVERWRITE, true
                        )
                )
        );

        composer.run(reader, "ing-diba-pipeline");
    }

    private static void writeEmailsAsXMI(String csvFilePath, String outputDir) throws IOException {
        try (var reader = Files.newBufferedReader(Paths.get(csvFilePath));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT)) {
            var skipFirst = true;
            for (var record : csvParser) {
                // Not interested in the header.
                if(skipFirst) {
                    skipFirst = false;
                    continue;
                }
                var id = record.get(0);
                var text = record.get(1);
                var outputPath = Paths.get(String.valueOf(outputDir), "mail_" + id + ".xmi");

                // Create the JCas from the mail
                var jCas = JCasFactory.createJCas();
                jCas.setDocumentText(text);
                jCas.setDocumentLanguage("de");

                var documentAnnotation = new DocumentMetaData(jCas);
                documentAnnotation.setDocumentId(id);
                documentAnnotation.setDocumentTitle("Mail_" + id);
                documentAnnotation.setDocumentUri(outputPath.toUri().toString());
                documentAnnotation.setBegin(0);
                documentAnnotation.setEnd(text.length() - 1);
                documentAnnotation.setDocumentBaseUri(outputPath.getParent().toUri().toString());
                documentAnnotation.addToIndexes();

                // Now write the xmi
                try (var os = Files.newOutputStream(outputPath)) {
                    CasIOUtils.save(jCas.getCas(), os, SerialFormat.XMI);
                }
            }
        } catch (Exception e) {
            System.out.println("Error reading a mail and transforming it to a JCas.");
            e.printStackTrace();
        }
    }

}