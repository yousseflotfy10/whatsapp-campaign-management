package com.wcm.campaign.service;

import com.wcm.campaign.dto.BulkUploadResult;
import com.wcm.campaign.dto.ContactRequest;
import com.wcm.campaign.model.Contact;
import com.wcm.campaign.repository.ContactRepository;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ContactService {

    private final ContactRepository contactRepository;

    public ContactService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    @Transactional
    public Contact createContact(ContactRequest request) {

        if (request.getContactName() == null || request.getContactName().trim().isEmpty()) {
            throw new IllegalArgumentException("Contact name is required");
        }

        if (request.getContactNumbers() == null || request.getContactNumbers().trim().isEmpty()) {
            throw new IllegalArgumentException("Contact phone number is required");
        }

        Contact contact = new Contact();

        contact.setContactName(request.getContactName().trim());

        contact.setContactNumbers(normalizeNumbers(request.getContactNumbers()));

        contact.setListId(request.getListId());
        contact.setListHash(request.getListHash());
        contact.setIsActive(true);

        return contactRepository.save(contact);
    }

    @Transactional(readOnly = true)
    public List<Contact> getAllContacts() {
        return contactRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Contact> getActiveContacts() {
        return contactRepository.findByIsActive(true);
    }

    @Transactional(readOnly = true)
    public Contact getContactById(Long id) {

        return contactRepository.findById(id).orElseThrow(() -> new RuntimeException("Contact not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Contact> searchContacts(String name) {

        if (name == null || name.trim().isEmpty()) {
            return getAllContacts();
        }
        return contactRepository.findByContactNameContainingIgnoreCase(name.trim());
    }

    @Transactional
    public Contact updateContact(Long id, ContactRequest request) {

        Contact contact = getContactById(id);

        if (request.getContactName() != null && !request.getContactName().trim().isEmpty()) {
            contact.setContactName(request.getContactName().trim());
        }

        if (request.getContactNumbers() != null && !request.getContactNumbers().trim().isEmpty()) {
            contact.setContactNumbers(normalizeNumbers(request.getContactNumbers()));
        }

        if (request.getListId() != null) {
            contact.setListId(request.getListId());
        }

        if (request.getListHash() != null) {
            contact.setListHash(request.getListHash());
        }

        return contactRepository.save(contact);
    }

    @Transactional
    public void deleteContact(Long id) {

        Contact contact = getContactById(id);

        contact.setIsActive(false);

        contactRepository.save(contact);
    }

    @Transactional
    public BulkUploadResult uploadBulk(MultipartFile file) {

        validateFile(file);

        try {
            List<BulkContactRow> rows = readCsv(file.getInputStream());

            if (rows.isEmpty()) {
                throw new IllegalArgumentException("The uploaded file is empty");
            }

            Map<String, BulkContactRow> uniqueContacts = new LinkedHashMap<>();

            for (BulkContactRow row : rows) {
                if (row.getPhoneNumbers() == null || row.getPhoneNumbers().trim().isEmpty()) {
                    continue;
                }

                String normalizedNumbers = normalizeNumbers(row.getPhoneNumbers());

                if (normalizedNumbers.isEmpty()) {
                    continue;
                }

                row.setPhoneNumbers(normalizedNumbers);

                if (!uniqueContacts.containsKey(normalizedNumbers)) {
                    uniqueContacts.put(normalizedNumbers, row);
                }
            }

            if (uniqueContacts.isEmpty()) {
                throw new IllegalArgumentException("No valid contacts were found in the uploaded file");
            }

            List<BulkContactRow> contacts = new ArrayList<>(uniqueContacts.values());

            String listHash = generateListHash(contacts);

            Optional<Contact> existing = contactRepository.findFirstByListHash(listHash);

            if (existing.isPresent()) {
                return new BulkUploadResult(listHash, rows.size(), 0, true, "Duplicate contact list. " + "The uploaded file was not saved.");
            }

            for (BulkContactRow row : contacts) {

                Contact contact = new Contact();

                contact.setContactName(row.getContactName() == null ? null : row.getContactName().trim());

                contact.setContactNumbers(row.getPhoneNumbers());

                contact.setListId(null);

                contact.setListHash(listHash);

                contact.setIsActive(true);

                contactRepository.save(contact);
            }

            return new BulkUploadResult(listHash, rows.size(), contacts.size(), false, "Contact list uploaded successfully.");

        } catch (IOException e) {throw new IllegalArgumentException("Unable to read the uploaded file", e);
        }
    }

    private List<BulkContactRow> readCsv(InputStream inputStream) throws IOException {

        List<BulkContactRow> rows = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String line;

        boolean firstLine = true;

        while ((line = reader.readLine()) != null) {

            if (line.trim().isEmpty()) {
                continue;
            }

            if (firstLine) {

                firstLine = false;

                String lower = line.toLowerCase();

                if (lower.contains("name") || lower.contains("phone") || lower.contains("number")) {
                    continue;
                }
            }

            String[] columns = line.split(",", -1);

            if (columns.length < 2) {
                continue;
            }

            String name = columns[0].trim();

            String numbers = columns[1].trim();

            rows.add(new BulkContactRow(name, numbers));
        }

        return rows;
    }

    private String generateListHash(
            List<BulkContactRow> contacts) {

        List<String> normalizedContacts = contacts.stream()
                        .map(row -> (row.getContactName() == null ? "" : row.getContactName().trim()) + "|" + row.getPhoneNumbers())
                        .sorted()
                        .toList();

        String canonicalData = String.join("\n", normalizedContacts);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonicalData.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();

            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }

            return result.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please upload a CSV file");
        }
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Invalid file name");
        }
        if (!filename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Only CSV files are supported");
        }
    }

    private String normalizeNumbers(String numbers) {
        if (numbers == null || numbers.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        String[] parts = numbers.split(",");

        List<String> normalized = new ArrayList<>();

        for (String part : parts) {
            String number = normalizeNumber(part.trim());
            if (!normalized.contains(number)) {
                normalized.add(number);
            }
        }
        return String.join(",", normalized);
    }

    private String normalizeNumber(String number) {
        if (number == null || number.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }

        number = number.trim();

        number = number.replaceAll("[\\s\\-()]+", "");

        if (number.startsWith("+")) {
            String digits = number.substring(1);

            if (!digits.matches("\\d+")) {
                throw new IllegalArgumentException("Invalid phone number: " + number);
            }

            if (digits.length() < 10 || digits.length() > 15) {
                throw new IllegalArgumentException("Invalid phone number length: " + number);
            }

            return "+" + digits;
        }

        if (number.startsWith("00")) {

            String digits = number.substring(2);

            if (!digits.matches("\\d+")) {
                throw new IllegalArgumentException("Invalid phone number: " + number);
            }

            if (digits.length() < 10 || digits.length() > 15) {
                throw new IllegalArgumentException("Invalid phone number length: " + number);
            }

            return "+" + digits;
        }

        if (!number.matches("\\d+")) {
            throw new IllegalArgumentException("Invalid phone number: " + number);
        }

        if (number.length() == 10 || number.length() == 11) {
            return number;
        }
        throw new IllegalArgumentException("Invalid phone number: " + number);
    }

    private static class BulkContactRow {

        private String contactName;
        private String phoneNumbers;

        public BulkContactRow(String contactName, String phoneNumbers) {
            this.contactName = contactName;
            this.phoneNumbers = phoneNumbers;
        }

        public String getContactName() {
            return contactName;
        }

        public String getPhoneNumbers() {
            return phoneNumbers;
        }

        public void setPhoneNumbers(String phoneNumbers) {
            this.phoneNumbers = phoneNumbers;
        }
    }
}