package com.wcm.campaign.service;

import com.wcm.campaign.model.Campaign;
import com.wcm.campaign.model.ContactList;
import com.wcm.campaign.model.Message;
import com.wcm.campaign.repository.CampaignRepository;
import com.wcm.campaign.repository.ContactListRepository;
import com.wcm.campaign.repository.MessageRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    private final CampaignRepository campaignRepository;
    private final MessageRepository messageRepository;
    private final ContactListRepository contactListRepository;

    public ReportService(
            CampaignRepository campaignRepository,
            MessageRepository messageRepository,
            ContactListRepository contactListRepository
    ) {
        this.campaignRepository = campaignRepository;
        this.messageRepository = messageRepository;
        this.contactListRepository = contactListRepository;
    }

    @Transactional(readOnly = true)
    public byte[] exportCampaignReport(String range) {

        logger.info("Generating campaign report for range {}", range);

        List<Campaign> campaigns = campaignRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Campaign::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<Campaign> selectedCampaigns = switch (normalizeRange(range)) {
            case "LAST" -> campaigns.stream().limit(1).toList();
            case "LAST_10" -> campaigns.stream().limit(10).toList();
            default -> campaigns;
        };

        Set<Long> campaignIds = selectedCampaigns.stream()
                .map(Campaign::getCampaignId)
                .collect(Collectors.toSet());

        List<Message> messages = messageRepository.findAll()
                .stream()
                .filter(message -> message.getCampaign() != null
                        && campaignIds.contains(message.getCampaign().getCampaignId()))
                .toList();

        Map<String, String> listNames = new LinkedHashMap<>();

        for (ContactList contactList : contactListRepository.findAll()) {
            if (contactList.getListId() != null
                    && contactList.getListName() != null
                    && !contactList.getListName().isBlank()) {
                listNames.put(contactList.getListId(), contactList.getListName());
            }
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            writeCampaignSummary(workbook.createSheet("Campaign Summary"), selectedCampaigns, messages, listNames);
            writeMessageDetails(workbook.createSheet("Message Details"), messages);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);

            logger.info("Campaign report generated successfully. Campaigns: {}, Messages: {}",
                    selectedCampaigns.size(), messages.size());

            return output.toByteArray();

        } catch (IOException exception) {
            logger.error("Unable to create campaign Excel report", exception);
            throw new IllegalStateException("Unable to create Excel report", exception);
        }
    }

    private void writeCampaignSummary(
            Sheet sheet,
            List<Campaign> campaigns,
            List<Message> messages,
            Map<String, String> listNames
    ) {

        String[] headers = {
                "Campaign ID", "Campaign", "Status", "Sender", "Lists", "Template",
                "Created", "Approved", "Messages", "Delivered", "Sent", "Failed", "Delivery Rate"
        };

        writeHeader(sheet, headers);
        int rowIndex = 1;

        for (Campaign campaign : campaigns) {
            List<Message> campaignMessages = messages.stream()
                    .filter(message -> message.getCampaign() != null
                            && message.getCampaign().getCampaignId().equals(campaign.getCampaignId()))
                    .toList();

            int total = campaignMessages.size();
            int delivered = countStatus(campaignMessages, "DELIVERED");
            int sent = countStatus(campaignMessages, "SENT");
            int failed = countStatus(campaignMessages, "FAILED");

            Row row = sheet.createRow(rowIndex++);
            write(row, 0, campaign.getCampaignId());
            write(row, 1, campaign.getCampaignName());
            write(row, 2, campaign.getStatus());
            write(row, 3, campaign.getSenderName());
            write(row, 4, formatListNames(campaign.getListId(), listNames));
            write(row, 5, campaign.getTemplate() == null ? "" : campaign.getTemplate().getTemplateName());
            write(row, 6, campaign.getCreatedAt());
            write(row, 7, campaign.getApprovedAt());
            write(row, 8, total);
            write(row, 9, delivered);
            write(row, 10, sent);
            write(row, 11, failed);
            write(row, 12, total == 0 ? "0%" : Math.round(delivered * 100.0 / total) + "%");
        }

        autosize(sheet, headers.length);
    }

    private String formatListNames(String listIds, Map<String, String> listNames) {

        if (listIds == null || listIds.isBlank()) {
            return "";
        }

        return Arrays.stream(listIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .map(id -> listNames.getOrDefault(id, "Unknown list"))
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private void writeMessageDetails(Sheet sheet, List<Message> messages) {

        String[] headers = {
                "Message ID", "Campaign", "Contact", "Phone Numbers", "Status",
                "Message Content", "Sent At", "Delivered At", "Failed At", "Failure Reason"
        };

        writeHeader(sheet, headers);
        int rowIndex = 1;

        for (Message message : messages) {
            Row row = sheet.createRow(rowIndex++);
            write(row, 0, message.getMessageId());
            write(row, 1, message.getCampaign() == null ? "" : message.getCampaign().getCampaignName());
            write(row, 2, message.getContact() == null ? "" : message.getContact().getContactName());
            write(row, 3, message.getContact() == null ? "" : message.getContact().getContactNumbers());
            write(row, 4, message.getStatus());
            write(row, 5, message.getMessageContent());
            write(row, 6, message.getSentAt());
            write(row, 7, message.getDeliveredAt());
            write(row, 8, message.getFailedAt());
            write(row, 9, message.getFailureReason());
        }

        autosize(sheet, headers.length);
    }

    private int countStatus(List<Message> messages, String status) {
        return (int) messages.stream()
                .filter(message -> status.equals(message.getStatus()))
                .count();
    }

    private String normalizeRange(String range) {

        if (range == null) {
            return "ALL";
        }

        return switch (range.trim().toUpperCase()) {
            case "LAST", "LAST_10", "ALL" -> range.trim().toUpperCase();
            default -> throw new IllegalArgumentException("Report range must be LAST, LAST_10, or ALL");
        };
    }

    private void writeHeader(Sheet sheet, String[] headers) {
        Row row = sheet.createRow(0);
        for (int index = 0; index < headers.length; index++) {
            row.createCell(index).setCellValue(headers[index]);
        }
    }

    private void write(Row row, int column, Object value) {

        Cell cell = row.createCell(column);

        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value instanceof LocalDateTime dateTime) {
            cell.setCellValue(dateTime.toString());
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private void autosize(Sheet sheet, int columnCount) {
        for (int index = 0; index < columnCount; index++) {
            sheet.autoSizeColumn(index);
        }
    }
}