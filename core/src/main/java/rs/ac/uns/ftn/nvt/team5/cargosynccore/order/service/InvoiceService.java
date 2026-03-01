package rs.ac.uns.ftn.nvt.team5.cargosynccore.order.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.order.model.Order;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.order.model.OrderItem;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class InvoiceService {

    private static final Color BRAND_COLOR = new Color(103, 58, 183); // #673AB7
    private static final Color HEADER_BG   = new Color(237, 231, 246);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public byte[] generateInvoicePdf(Order order) {
        try (var baos = new ByteArrayOutputStream()) {
            var doc = new Document(PageSize.A4, 50, 50, 60, 50);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            addHeader(doc, order);
            addBillingInfo(doc, order);
            addItemsTable(doc, order);
            addTotal(doc, order);
            addFooter(doc);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate invoice PDF for order {}", order.getId(), e);
            throw new RuntimeException("Invoice generation failed", e);
        }
    }

    private void addHeader(Document doc, Order order) throws DocumentException {
        Font titleFont = new Font(Font.HELVETICA, 22, Font.BOLD, BRAND_COLOR);
        Font subFont  = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);

        var title = new Paragraph("CargoSync", titleFont);
        title.setAlignment(Element.ALIGN_LEFT);
        doc.add(title);

        doc.add(new Paragraph("Smart Logistics Platform", subFont));
        doc.add(Chunk.NEWLINE);

        Font invoiceFont = new Font(Font.HELVETICA, 16, Font.BOLD, Color.DARK_GRAY);
        var invoiceTitle = new Paragraph("INVOICE", invoiceFont);
        invoiceTitle.setAlignment(Element.ALIGN_LEFT);
        doc.add(invoiceTitle);

        Font metaFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
        doc.add(new Paragraph("Order ID: " + order.getId(), metaFont));
        doc.add(new Paragraph("Date: " + order.getCreatedDate().format(DATE_FMT), metaFont));
        doc.add(Chunk.NEWLINE);
    }

    private void addBillingInfo(Document doc, Order order) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 1});

        Font labelFont = new Font(Font.HELVETICA, 9, Font.BOLD, BRAND_COLOR);
        Font valueFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        var customer = order.getCustomer();
        var company  = order.getCompany();

        PdfPCell fromCell = new PdfPCell();
        fromCell.setBorder(Rectangle.NO_BORDER);
        fromCell.addElement(new Paragraph("BILLED TO", labelFont));
        fromCell.addElement(new Paragraph(customer.getFirstName() + " " + customer.getLastName(), valueFont));
        fromCell.addElement(new Paragraph(customer.getEmail(), valueFont));

        PdfPCell toCell = new PdfPCell();
        toCell.setBorder(Rectangle.NO_BORDER);
        toCell.addElement(new Paragraph("DELIVERY TO", labelFont));
        toCell.addElement(new Paragraph(company.getName(), valueFont));
        if (company.getAddress() != null) {
            toCell.addElement(new Paragraph(company.getAddress(), valueFont));
        }

        table.addCell(fromCell);
        table.addCell(toCell);
        doc.add(table);
        doc.add(Chunk.NEWLINE);
    }

    private void addItemsTable(Document doc, Order order) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4, 1, 2, 1.5f, 2});

        Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        String[] headers = {"Product", "Qty", "Unit Price", "Weight", "Line Total"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(BRAND_COLOR);
            cell.setPadding(6);
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }

        Font rowFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
        boolean alternate = false;
        for (OrderItem item : order.getItems()) {
            Color bg = alternate ? HEADER_BG : Color.WHITE;
            String[] values = {
                item.getProduct().getName(),
                String.valueOf(item.getQuantity()),
                "€" + item.getUnitPrice(),
                item.getProduct().getWeight() + " kg",
                "€" + item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity()))
            };
            for (String v : values) {
                PdfPCell cell = new PdfPCell(new Phrase(v, rowFont));
                cell.setBackgroundColor(bg);
                cell.setPadding(5);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
            }
            alternate = !alternate;
        }
        doc.add(table);
    }

    private void addTotal(Document doc, Order order) throws DocumentException {
        doc.add(Chunk.NEWLINE);
        Font totalFont = new Font(Font.HELVETICA, 13, Font.BOLD, BRAND_COLOR);
        var total = new Paragraph("Grand Total: €" + order.getTotalAmount(), totalFont);
        total.setAlignment(Element.ALIGN_RIGHT);
        doc.add(total);
    }

    private void addFooter(Document doc) throws DocumentException {
        doc.add(Chunk.NEWLINE);
        Font footerFont = new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY);
        var footer = new Paragraph(
                "Thank you for your order. This is an automatically generated document.", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }
}
