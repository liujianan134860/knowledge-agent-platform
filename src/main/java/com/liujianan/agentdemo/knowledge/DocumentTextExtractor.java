package com.liujianan.agentdemo.knowledge;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class DocumentTextExtractor {
    public String extract(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String lowerName = filename.toLowerCase(Locale.ROOT);
        try {
            if (lowerName.endsWith(".pdf")) {
                return extractPdf(file);
            }
            if (lowerName.endsWith(".docx")) {
                return extractDocx(file);
            }
            if (lowerName.endsWith(".txt") || lowerName.endsWith(".md")) {
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            }
            throw new IllegalArgumentException("unsupported file type: " + filename);
        } catch (Exception exception) {
            throw new IllegalArgumentException("failed to extract text from " + filename + ": " + exception.getMessage());
        }
    }

    private String extractPdf(MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream(); PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractDocx(MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream(); XWPFDocument document = new XWPFDocument(inputStream)) {
            return document.getParagraphs()
                    .stream()
                    .map(XWPFParagraph::getText)
                    .filter(text -> text != null && !text.isBlank())
                    .collect(Collectors.joining("\n"));
        }
    }
}
