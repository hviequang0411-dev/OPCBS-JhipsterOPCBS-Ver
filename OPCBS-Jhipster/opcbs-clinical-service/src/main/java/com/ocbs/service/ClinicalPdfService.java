package com.ocbs.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.ocbs.domain.Appointment;
import com.ocbs.domain.DoctorProfile;
import com.ocbs.domain.PatientProfile;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for generating comprehensive Clinical / Medical Record PDF documents.
 */
@Service
public class ClinicalPdfService {

    private static final Logger LOG = LoggerFactory.getLogger(ClinicalPdfService.class);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy").withZone(ZoneId.systemDefault());

    // Primary Colors
    private static final Color TEAL_PRIMARY = new Color(13, 148, 136); // #0d9488
    private static final Color TEAL_DARK = new Color(15, 118, 110);    // #0f766e
    private static final Color SLATE_DARK = new Color(30, 41, 59);     // #1e293b
    private static final Color SLATE_MUTED = new Color(100, 116, 139); // #64748b
    private static final Color BG_LIGHT = new Color(248, 250, 252);    // #f8fafc
    private static final Color BORDER_GRAY = new Color(226, 232, 240); // #e2e8f0
    private static final Color DANGER_RED = new Color(225, 29, 72);    // #e11d48

    public byte[] generatePatientClinicalRecordPdf(
        PatientProfile patient,
        List<Appointment> appointments,
        DoctorProfile currentDoctor
    ) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, out);

            // Page Event for Header / Footer
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter w, Document doc) {
                    PdfPTable footer = new PdfPTable(1);
                    footer.setTotalWidth(doc.getPageSize().getWidth() - 72);
                    Font footerFont = getFont(8, Font.ITALIC, SLATE_MUTED);

                    PdfPCell cell = new PdfPCell(new Phrase(
                        "MindBridge Healthcare - Hồ Sơ Bệnh Án & Tham Vấn Tâm Lý | Trang " + doc.getPageNumber(),
                        footerFont
                    ));
                    cell.setBorder(Rectangle.TOP);
                    cell.setBorderColor(BORDER_GRAY);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setPaddingTop(5);
                    footer.addCell(cell);

                    footer.writeSelectedRows(0, -1, 36, 30, w.getDirectContent());
                }
            });

            document.open();

            // 1. BRAND & CLINIC HEADER
            addClinicHeader(document);

            // 2. DOCUMENT TITLE
            Paragraph title = new Paragraph("HỒ SƠ BỆNH ÁN & QUÁ TRÌNH TRỊ LIỆU TÂM LÝ", getFont(16, Font.BOLD, TEAL_DARK));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(10);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph docSubtitle = new Paragraph(
                "Mã hồ sơ: MB-EHR-" + patient.getId() + "  |  Ngày xuất: " + DATETIME_FMT.format(java.time.Instant.now()),
                getFont(9, Font.NORMAL, SLATE_MUTED)
            );
            docSubtitle.setAlignment(Element.ALIGN_CENTER);
            docSubtitle.setSpacingAfter(15);
            document.add(docSubtitle);

            // 3. PATIENT DEMOGRAPHICS SECTION
            addSectionHeading(document, "1. THÔNG TIN BỆNH NHÂN (PATIENT PROFILE)");
            addPatientDemographics(document, patient);

            // 4. MEDICAL & PSYCHOLOGICAL BACKGROUND
            addSectionHeading(document, "2. TIỀN SỬ Y KHOA & LƯU Ý ĐẶC BIỆT");
            addMedicalBackground(document, patient);

            // 5. CLINICAL CONSULTATION SESSIONS
            addSectionHeading(document, "3. LỊCH SỬ THAM VẤN & PHÁC ĐỒ ĐIỀU TRỊ (" + (appointments != null ? appointments.size() : 0) + " Ca khám)");
            addConsultationHistory(document, appointments);

            // 6. SIGN-OFF & DOCTOR ATTESTATION
            addSignOffSection(document, currentDoctor);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            LOG.error("Error generating clinical record PDF for patient ID {}", patient != null ? patient.getId() : null, e);
            throw new RuntimeException("Không thể tạo file PDF bệnh án: " + e.getMessage(), e);
        }
    }

    private void addClinicHeader(Document document) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{60, 40});

        // Left Header: Brand
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        Paragraph brand = new Paragraph("MINDBRIDGE HEALTHCARE", getFont(13, Font.BOLD, TEAL_PRIMARY));
        Paragraph clinicDesc = new Paragraph("Hệ Thống Y Tế & Tham Vấn Tâm Lý Trực Tuyến OPCBS\nHotline: 1900-6868  |  Website: mindbridge.vn", getFont(8, Font.NORMAL, SLATE_MUTED));
        left.addElement(brand);
        left.addElement(clinicDesc);
        table.addCell(left);

        // Right Header: Certification
        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph rightText = new Paragraph("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM\nĐộc lập - Tự do - Hạnh phúc", getFont(8, Font.BOLD, SLATE_DARK));
        rightText.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(rightText);
        table.addCell(right);

        document.add(table);

        // Decorative teal line
        LineSeparator line = new LineSeparator(1.5f, 100, TEAL_PRIMARY, Element.ALIGN_CENTER, -4);
        document.add(new Chunk(line));
    }

    private void addSectionHeading(Document document, String title) throws DocumentException {
        Paragraph p = new Paragraph(title, getFont(11, Font.BOLD, TEAL_DARK));
        p.setSpacingBefore(12);
        p.setSpacingAfter(6);
        document.add(p);
    }

    private void addPatientDemographics(Document document, PatientProfile patient) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{20, 30, 20, 30});

        String dobStr = patient.getDateOfBirth() != null ? DATE_FMT.format(patient.getDateOfBirth().atStartOfDay(ZoneId.systemDefault())) : "Chưa cập nhật";
        String genderStr = patient.getGender() != null ? (patient.getGender().name().equalsIgnoreCase("MALE") ? "Nam" : "Nữ") : "Chưa cập nhật";

        addTableCell(table, "Họ và tên:", true);
        addTableCell(table, patient.getFullName() != null ? patient.getFullName() : "--", false);

        addTableCell(table, "Số điện thoại:", true);
        addTableCell(table, patient.getPhone() != null ? patient.getPhone() : "--", false);

        addTableCell(table, "Ngày sinh:", true);
        addTableCell(table, dobStr, false);

        addTableCell(table, "Giới tính:", true);
        addTableCell(table, genderStr, false);

        addTableCell(table, "Nhóm máu:", true);
        addTableCell(table, patient.getBloodGroup() != null ? patient.getBloodGroup() : "Chưa xác định", false);

        addTableCell(table, "SĐT Khẩn cấp:", true);
        addTableCell(table, patient.getEmergencyContactPhone() != null ? patient.getEmergencyContactPhone() : "--", false);

        addTableCell(table, "Địa chỉ:", true);
        PdfPCell addrCell = createCell(patient.getAddress() != null ? patient.getAddress() : "--", false);
        addrCell.setColspan(3);
        table.addCell(addrCell);

        document.add(table);
    }

    private void addMedicalBackground(Document document, PatientProfile patient) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{25, 75});

        addTableCell(table, "Tiền sử bệnh lý / Tâm lý:", true);
        addTableCell(table, patient.getMedicalHistory() != null && !patient.getMedicalHistory().isBlank() 
            ? patient.getMedicalHistory() 
            : "Chưa ghi nhận tiền sử đặc biệt.", false);

        addTableCell(table, "Dị ứng & Cảnh báo:", true);
        PdfPCell allergyCell = createCell(patient.getAllergies() != null && !patient.getAllergies().isBlank()
            ? patient.getAllergies()
            : "Không có tiền sử dị ứng thuốc hay thực phẩm.", false);
        if (patient.getAllergies() != null && !patient.getAllergies().isBlank()) {
            allergyCell.setBackgroundColor(new Color(254, 242, 242));
        }
        table.addCell(allergyCell);

        document.add(table);
    }

    private void addConsultationHistory(Document document, List<Appointment> appointments) throws DocumentException {
        if (appointments == null || appointments.isEmpty()) {
            Paragraph p = new Paragraph("Chưa có ca tham vấn lâm sàng nào được ghi nhận cho bệnh nhân này.", getFont(9, Font.ITALIC, SLATE_MUTED));
            p.setSpacingBefore(4);
            document.add(p);
            return;
        }

        int count = 1;
        for (Appointment app : appointments) {
            PdfPTable card = new PdfPTable(1);
            card.setWidthPercentage(100);
            card.setSpacingBefore(8);
            card.setSpacingAfter(4);

            PdfPCell cell = new PdfPCell();
            cell.setBorderColor(BORDER_GRAY);
            cell.setBackgroundColor(BG_LIGHT);
            cell.setPadding(8);

            // Session Title Header
            String startTime = app.getScheduledStart() != null ? DATETIME_FMT.format(app.getScheduledStart()) : "--";
            String doctorName = app.getDoctor() != null && app.getDoctor().getFullName() != null ? app.getDoctor().getFullName() : "Bác sĩ phụ trách";
            String status = app.getStatus() != null ? app.getStatus().name() : "";

            Paragraph sessionHead = new Paragraph(
                "Buổi #" + count + " - Ca khám: " + (app.getBookingCode() != null ? app.getBookingCode() : ("ID:" + app.getId())) + 
                "  (" + startTime + ") - Trạng thái: " + status,
                getFont(9.5f, Font.BOLD, TEAL_DARK)
            );
            cell.addElement(sessionHead);

            Paragraph docInfo = new Paragraph("Bác sĩ điều trị: " + doctorName + "  |  Hình thức: " + (app.getConsultationMode() != null ? app.getConsultationMode().name() : "TRỰC TIẾP"), getFont(8.5f, Font.NORMAL, SLATE_MUTED));
            docInfo.setSpacingAfter(5);
            cell.addElement(docInfo);

            // Diagnosis
            if (app.getDiagnosis() != null && !app.getDiagnosis().isBlank()) {
                Paragraph diag = new Paragraph("• Chẩn đoán lâm sàng: " + app.getDiagnosis(), getFont(9, Font.BOLD, SLATE_DARK));
                cell.addElement(diag);
            }

            // Summary
            if (app.getConsultationSummary() != null && !app.getConsultationSummary().isBlank()) {
                Paragraph summ = new Paragraph("• Tóm tắt buổi khám: " + app.getConsultationSummary(), getFont(8.5f, Font.NORMAL, SLATE_DARK));
                cell.addElement(summ);
            }

            // Therapy Plan
            if (app.getTherapyPlan() != null && !app.getTherapyPlan().isBlank()) {
                Paragraph plan = new Paragraph("• Phác đồ trị liệu: " + app.getTherapyPlan(), getFont(8.5f, Font.NORMAL, TEAL_DARK));
                cell.addElement(plan);
            }

            // Recommendations
            if (app.getRecommendations() != null && !app.getRecommendations().isBlank()) {
                Paragraph rec = new Paragraph("• Lời khuyên & Bài tập: " + app.getRecommendations(), getFont(8.5f, Font.NORMAL, SLATE_DARK));
                cell.addElement(rec);
            }

            // Next Appointment / Follow up
            if (app.getNextAppointmentRecommendedDate() != null) {
                Paragraph next = new Paragraph("• Đề xuất hẹn tái khám: " + DATETIME_FMT.format(app.getNextAppointmentRecommendedDate()), getFont(8.5f, Font.BOLD, DANGER_RED));
                cell.addElement(next);
            }

            card.addCell(cell);
            document.add(card);
            count++;
        }
    }

    private void addSignOffSection(Document document, DoctorProfile currentDoctor) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(20);
        table.setWidths(new float[]{50, 50});

        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.addElement(new Paragraph(" "));
        table.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph placeDate = new Paragraph("Hà Nội, ngày " + DateTimeFormatter.ofPattern("dd 'tháng' MM 'năm' yyyy").withZone(ZoneId.systemDefault()).format(java.time.Instant.now()), getFont(9, Font.ITALIC, SLATE_DARK));
        placeDate.setAlignment(Element.ALIGN_CENTER);
        right.addElement(placeDate);

        Paragraph docTitle = new Paragraph("BÁC SĨ / CHUYÊN GIA PHỤ TRÁCH", getFont(9.5f, Font.BOLD, SLATE_DARK));
        docTitle.setAlignment(Element.ALIGN_CENTER);
        docTitle.setSpacingAfter(45); // Space for signature
        right.addElement(docTitle);

        String docName = currentDoctor != null && currentDoctor.getFullName() != null ? currentDoctor.getFullName() : "BS. MindBridge Healthcare";
        Paragraph docNamePara = new Paragraph(docName, getFont(10, Font.BOLD, TEAL_DARK));
        docNamePara.setAlignment(Element.ALIGN_CENTER);
        right.addElement(docNamePara);

        table.addCell(right);
        document.add(table);
    }

    private void addTableCell(PdfPTable table, String text, boolean isLabel) {
        table.addCell(createCell(text, isLabel));
    }

    private PdfPCell createCell(String text, boolean isLabel) {
        Font font = isLabel ? getFont(8.5f, Font.BOLD, SLATE_DARK) : getFont(8.5f, Font.NORMAL, SLATE_DARK);
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBorderColor(BORDER_GRAY);
        cell.setPadding(5);
        if (isLabel) {
            cell.setBackgroundColor(BG_LIGHT);
        }
        return cell;
    }

    private Font getFont(float size, int style, Color color) {
        try {
            File fontFile = new File("C:/Windows/Fonts/arial.ttf");
            if (fontFile.exists()) {
                BaseFont bf = BaseFont.createFont("C:/Windows/Fonts/arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                return new Font(bf, size, style, color);
            }
        } catch (Exception ignored) {
            // fallback
        }
        return FontFactory.getFont(FontFactory.HELVETICA, size, style, color);
    }
}
