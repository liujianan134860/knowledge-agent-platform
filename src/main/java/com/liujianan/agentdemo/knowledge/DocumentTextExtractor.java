package com.liujianan.agentdemo.knowledge;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class DocumentTextExtractor {
    public String extract(MultipartFile file) {
        return extractWithMetadata(file).text();
    }

    public ExtractedDocument extractWithMetadata(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String lowerName = filename.toLowerCase(Locale.ROOT);
        try {
            if (lowerName.endsWith(".pdf")) {
                return extractPdf(file);
            }
            if (lowerName.endsWith(".docx")) {
                return singlePage(extractDocx(file), "docx");
            }
            if (lowerName.endsWith(".txt") || lowerName.endsWith(".md")) {
                return singlePage(new String(file.getBytes(), StandardCharsets.UTF_8),
                        lowerName.endsWith(".md") ? "markdown" : "text");
            }
            throw new IllegalArgumentException("unsupported file type: " + filename);
        } catch (Exception exception) {
            throw new IllegalArgumentException("failed to extract text from " + filename + ": " + exception.getMessage());
        }
    }

    private ExtractedDocument extractPdf(MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream(); PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            StringBuilder text = new StringBuilder();
            List<PageSpan> pages = new ArrayList<>();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                int start = text.length();
                String pageText = stripper.getText(document);
                text.append(pageText);
                int end = text.length();
                pages.add(new PageSpan(page, start, end));
            }
            return new ExtractedDocument(text.toString(), "pdf", pages);
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

    private ExtractedDocument singlePage(String text, String sourceType) {
        String safeText = text == null ? "" : text;
        return new ExtractedDocument(safeText, sourceType, List.of(new PageSpan(1, 0, safeText.length())));
    }

    public record ExtractedDocument(String text, String sourceType, List<PageSpan> pages) {
        public Integer pageForOffset(int offset) {
            if (pages == null || pages.isEmpty()) {
                return null;
            }
            for (PageSpan page : pages) {
                if (offset >= page.startOffset() && offset < page.endOffset()) {
                    return page.pageNumber();
                }
            }
            return pages.get(pages.size() - 1).pageNumber();
        }
    }

    public record PageSpan(Integer pageNumber, int startOffset, int endOffset) {
    }
}
