package com.waiveliability.modules.forms.service;

import com.waiveliability.modules.forms.dto.DetectedField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentImportService {

    private static final Pattern QUESTION_PATTERN = Pattern.compile(".+\\?$");
    private static final Pattern CHECKBOX_PATTERN = Pattern.compile("^\\s*[\\[\\]☐☑]\\s*.+");
    private static final Pattern SIGNATURE_PATTERN = Pattern.compile("(?i)(signature|signed):?\\s*.*");

    public List<DetectedField> parseDocument(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Filename is required");
        }

        String content;
        if (filename.endsWith(".txt")) {
            content = parseTextFile(file);
        } else if (filename.endsWith(".docx")) {
            content = parseDocxFile(file);
        } else if (filename.endsWith(".pdf")) {
            content = parsePdfFile(file);
        } else {
            throw new IllegalArgumentException("Unsupported file type. Supported: .txt, .docx, .pdf");
        }

        return detectFields(content);
    }

    private String parseTextFile(MultipartFile file) {
        try {
            return new String(file.getBytes());
        } catch (Exception e) {
            log.error("Failed to parse text file", e);
            throw new IllegalArgumentException("Failed to parse text file");
        }
    }

    private String parseDocxFile(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             XWPFDocument doc = new XWPFDocument(is)) {

            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph para : doc.getParagraphs()) {
                String text = para.getText().trim();
                if (!text.isEmpty()) {
                    sb.append(text).append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Failed to parse docx file", e);
            throw new IllegalArgumentException("Failed to parse docx file");
        }
    }

    private String parsePdfFile(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             PDDocument doc = Loader.loadPDF(file.getBytes())) {

            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        } catch (Exception e) {
            log.error("Failed to parse pdf file", e);
            throw new IllegalArgumentException("Failed to parse pdf file");
        }
    }

    private List<DetectedField> detectFields(String content) {
        List<DetectedField> fields = new ArrayList<>();
        String[] lines = content.split("\n");

        int fieldOrder = 0;
        StringBuilder contentBlock = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                // Empty line - flush content block if exists
                if (contentBlock.length() > 0) {
                    fields.add(DetectedField.builder()
                            .label("Terms & Conditions")
                            .fieldType("content")
                            .content(contentBlock.toString().trim())
                            .required(true)
                            .fieldOrder(fieldOrder++)
                            .build());
                    contentBlock = new StringBuilder();
                }
                continue;
            }

            // Check for signature field
            if (SIGNATURE_PATTERN.matcher(trimmed).matches()) {
                // Flush content block first
                if (contentBlock.length() > 0) {
                    fields.add(DetectedField.builder()
                            .label("Terms & Conditions")
                            .fieldType("content")
                            .content(contentBlock.toString().trim())
                            .required(true)
                            .fieldOrder(fieldOrder++)
                            .build());
                    contentBlock = new StringBuilder();
                }
                fields.add(DetectedField.builder()
                        .label("Signature")
                        .fieldType("text")
                        .placeholder("Type your full name as signature")
                        .required(true)
                        .fieldOrder(fieldOrder++)
                        .build());
                continue;
            }

            // Check for checkbox
            if (CHECKBOX_PATTERN.matcher(trimmed).matches() || trimmed.toLowerCase().startsWith("i agree") ||
                    trimmed.toLowerCase().startsWith("i accept") || trimmed.toLowerCase().contains("agree to")) {
                // Flush content block first
                if (contentBlock.length() > 0) {
                    fields.add(DetectedField.builder()
                            .label("Terms & Conditions")
                            .fieldType("content")
                            .content(contentBlock.toString().trim())
                            .required(true)
                            .fieldOrder(fieldOrder++)
                            .build());
                    contentBlock = new StringBuilder();
                }
                // Clean up checkbox marker for label
                String label = trimmed.replaceFirst("^\\s*[\\[\\]☐☑]\\s*", "");
                fields.add(DetectedField.builder()
                        .label(label.isEmpty() ? "I agree to the terms" : label)
                        .fieldType("checkbox")
                        .required(true)
                        .fieldOrder(fieldOrder++)
                        .build());
                continue;
            }

            // Check for question (text field)
            if (QUESTION_PATTERN.matcher(trimmed).matches()) {
                // Flush content block first
                if (contentBlock.length() > 0) {
                    fields.add(DetectedField.builder()
                            .label("Terms & Conditions")
                            .fieldType("content")
                            .content(contentBlock.toString().trim())
                            .required(true)
                            .fieldOrder(fieldOrder++)
                            .build());
                    contentBlock = new StringBuilder();
                }
                String label = trimmed.substring(0, trimmed.length() - 1).trim(); // Remove ?
                fields.add(DetectedField.builder()
                        .label(label.isEmpty() ? "Question" : label)
                        .fieldType("text")
                        .required(false)
                        .fieldOrder(fieldOrder++)
                        .build());
                continue;
            }

            // Otherwise, add to content block
            if (contentBlock.length() > 0) {
                contentBlock.append("\n");
            }
            contentBlock.append(trimmed);
        }

        // Flush remaining content block
        if (contentBlock.length() > 0) {
            fields.add(DetectedField.builder()
                    .label("Terms & Conditions")
                    .fieldType("content")
                    .content(contentBlock.toString().trim())
                    .required(true)
                    .fieldOrder(fieldOrder++)
                    .build());
        }

        // If no fields detected, create a default content field
        if (fields.isEmpty()) {
            fields.add(DetectedField.builder()
                    .label("Document Content")
                    .fieldType("content")
                    .content(content)
                    .required(false)
                    .fieldOrder(0)
                    .build());
        }

        return fields;
    }
}
