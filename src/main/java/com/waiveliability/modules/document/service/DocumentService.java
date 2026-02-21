package com.waiveliability.modules.document.service;

import com.waiveliability.common.storage.S3Service;
import com.waiveliability.modules.forms.domain.FormField;
import com.waiveliability.modules.forms.repository.FormFieldRepository;
import com.waiveliability.modules.submissions.domain.Submission;
import com.waiveliability.modules.submissions.repository.SubmissionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final SubmissionRepository submissionRepository;
    private final FormFieldRepository formFieldRepository;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;

    private static final float MARGIN = 50f;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;
    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a z")
            .withZone(ZoneId.of("UTC"));

    @Async
    @Transactional
    public void generatePdfAsync(UUID submissionId) {
        try {
            Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + submissionId));

            byte[] pdfBytes = buildPdf(submission);

            String key = String.format("pdfs/%s/%s.pdf",
                submission.getTenant().getId(), submissionId);
            s3Service.upload(key, new ByteArrayInputStream(pdfBytes), pdfBytes.length, "application/pdf");

            submission.setPdfS3Key(key);
            submissionRepository.save(submission);

            log.info("PDF generated for submission {}", submissionId);
        } catch (Exception e) {
            log.error("Failed to generate PDF for submission {}: {}", submissionId, e.getMessage(), e);
        }
    }

    private byte[] buildPdf(Submission submission) throws Exception {
        Map<String, Object> answers = deserializeAnswers(submission.getFormData());
        List<FormField> fields = formFieldRepository.findByFormIdOrderByFieldOrder(
            submission.getForm().getId());

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontSmall = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                // Title
                y = drawText(cs, submission.getForm().getName(), fontBold, 18, MARGIN, y);
                y -= 6;

                // Description
                if (submission.getForm().getDescription() != null) {
                    y = drawText(cs, submission.getForm().getDescription(), fontRegular, 11, MARGIN, y);
                    y -= 4;
                }

                // Divider line
                cs.setLineWidth(0.5f);
                cs.moveTo(MARGIN, y);
                cs.lineTo(PAGE_WIDTH - MARGIN, y);
                cs.stroke();
                y -= 16;

                // Field answers
                for (FormField field : fields) {
                    String answer = getAnswerString(answers, field);
                    y = drawText(cs, field.getLabel() + ":", fontBold, 10, MARGIN, y);
                    y -= 2;
                    y = drawText(cs, answer != null ? answer : "(not provided)", fontRegular, 10, MARGIN + 12, y);
                    y -= 10;

                    if (y < MARGIN + 60) break; // avoid running off page
                }

                y -= 10;
                // Divider
                cs.moveTo(MARGIN, y);
                cs.lineTo(PAGE_WIDTH - MARGIN, y);
                cs.stroke();
                y -= 14;

                // Footer: submission metadata
                String footerText = String.format("Submitted: %s  |  ID: %s",
                    DATE_FMT.format(submission.getSubmittedAt()), submission.getId());
                drawText(cs, footerText, fontSmall, 8, MARGIN, y);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private float drawText(PDPageContentStream cs, String text, PDType1Font font,
                            float fontSize, float x, float y) throws Exception {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        // Sanitize: replace non-WinAnsi characters with '?'
        String safe = text.chars()
            .map(c -> c < 256 ? c : '?')
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();
        cs.showText(safe);
        cs.endText();
        return y - fontSize - 4;
    }

    private String getAnswerString(Map<String, Object> answers, FormField field) {
        Object val = answers.get(field.getId().toString());
        if (val == null) return null;
        if (val instanceof Boolean b) return b ? "Yes" : "No";
        return val.toString();
    }

    private Map<String, Object> deserializeAnswers(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
