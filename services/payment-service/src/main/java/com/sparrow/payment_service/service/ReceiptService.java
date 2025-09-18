package com.sparrow.payment_service.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.sparrow.payment_service.model.Payment;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class ReceiptService {

    public String generateReceipt(Payment payment) {
        // In a real implementation, this would save the PDF to storage
        // and return a URL. For simplicity, we'll just return a placeholder
        return "/api/payments/" + payment.getId() + "/receipt";
    }

    public byte[] getReceiptPdf(Payment payment) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            document.add(new Paragraph("PARCEL PAYMENT RECEIPT")
                    .setBold().setFontSize(16));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Transaction ID: " + payment.getTransactionId()));
            document.add(new Paragraph("Parcel ID: " + payment.getParcelId()));
            document.add(new Paragraph("User ID: " + payment.getUserId()));
            document.add(new Paragraph("Amount: $" + payment.getAmount()));
            document.add(new Paragraph("Payment Method: " + payment.getPaymentMethod()));
            document.add(new Paragraph("Status: " + payment.getPaymentStatus()));
            document.add(new Paragraph("Date: " + payment.getCreatedAt()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Thank you for your payment!"));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating receipt", e);
        }
    }
}