package com.wcm.campaign.service;

import com.wcm.campaign.dto.BulkUploadResult;
import com.wcm.campaign.dto.ContactListResponse;
import com.wcm.campaign.dto.ContactRequest;
import com.wcm.campaign.model.Contact;
import com.wcm.campaign.model.ContactList;
import com.wcm.campaign.repository.ContactListRepository;
import com.wcm.campaign.repository.ContactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(ContactService.class);

    private final ContactRepository contactRepository;
    private final ContactListRepository contactListRepository;

    public ContactService(
            ContactRepository contactRepository,
            ContactListRepository contactListRepository
    ) {
        this.contactRepository = contactRepository;
        this.contactListRepository = contactListRepository;
    }

    @Transactional
    public Contact createContact(ContactRequest request) {

        logger.info("Creating contact");

        if (request.getContactName() == null || request.getContactName().trim().isEmpty()) {
            logger.warn("Contact creation failed: contact name is required");
            throw new IllegalArgumentException("Contact name is required");
        }

        if (request.getContactNumbers() == null || request.getContactNumbers().trim().isEmpty()) {
            logger.warn("Contact creation failed: phone number is required");
            throw new IllegalArgumentException("Contact phone number is required");
        }

        String normalizedNumbers = normalizeNumbers(request.getContactNumbers());

        Optional<Contact> existing = contactRepository.findFirstByContactNumbersAndIsActiveTrue(normalizedNumbers);

        Contact contact;

        if (existing.isPresent()) {
            contact = existing.get();
            logger.info("Existing contact found with id {}", contact.getContactId());
        } else {
            contact = new Contact();

            contact.setContactName(request.getContactName().trim());

            contact.setContactNumbers(normalizedNumbers);
            contact.setIsActive(true);

            contact = contactRepository.save(contact);

            logger.info("Contact created successfully with id {}", contact.getContactId());
        }

        if (request.getListId() != null && !request.getListId().isBlank()) {

            ContactList contactList = contactListRepository.findById(request.getListId())
                    .orElseThrow(() -> new IllegalArgumentException("Contact list not found: " + request.getListId()));

            contact.addContactList(contactList);

            contact = contactRepository.save(contact);

            logger.info("Contact {} added to contact list {}", contact.getContactId(), request.getListId());
        }

        return contact;
    }

    @Transactional(readOnly = true)
    public List<Contact> getAllContacts() {
        logger.info("Fetching all contacts");
        return contactRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Contact> getActiveContacts() {
        logger.info("Fetching active contacts");
        return contactRepository.findByIsActive(true);
    }

    @Transactional(readOnly = true)
    public List<ContactListResponse> getActiveListIds() {

        logger.info("Fetching contact lists");

        return contactListRepository.findAll()
                .stream()
                .map(list -> new ContactListResponse(list.getListId(), list.getListName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Contact getContactById(Long id) {

        return contactRepository.findById(id).orElseThrow(() -> {
            logger.warn("Contact not found with id {}", id);
            return new RuntimeException("Contact not found with id: " + id);
        });
    }

    @Transactional(readOnly = true)
    public List<Contact> searchContacts(String name) {
        if (name == null || name.trim().isEmpty()) {
            return getAllContacts();
        }

        logger.info("Searching contacts by name");

        return contactRepository.findByContactNameContainingIgnoreCase(name.trim());
    }

    @Transactional
    public Contact updateContact(Long id, ContactRequest request) {

        logger.info("Updating contact with id {}", id);

        Contact contact = getContactById(id);

        if (request.getContactName() != null && !request.getContactName().trim().isEmpty()) {
            contact.setContactName(request.getContactName().trim());
        }

        if (request.getContactNumbers() != null && !request.getContactNumbers().trim().isEmpty()) {
            String normalizedNumbers = normalizeNumbers(request.getContactNumbers());
            contact.setContactNumbers(normalizedNumbers);
        }

        if (request.getListId() != null && !request.getListId().isBlank()) {

            ContactList contactList = contactListRepository.findById(request.getListId())
                    .orElseThrow(() -> new IllegalArgumentException("Contact list not found: " + request.getListId()));

            contact.addContactList(contactList);

            logger.info("Contact {} added to contact list {}", id, request.getListId());
        }

        Contact updatedContact = contactRepository.save(contact);

        logger.info("Contact {} updated successfully", id);

        return updatedContact;
    }

    @Transactional
    public void deleteContact(Long id) {

        logger.info("Deleting contact with id {}", id);

        Contact contact = getContactById(id);

        contact.setIsActive(false);

        contactRepository.save(contact);

        logger.info("Contact {} deleted successfully", id);
    }

    @Transactional
    public BulkUploadResult uploadBulk(MultipartFile file) {

        validateFile(file);

        logger.info("Starting contact list upload");

        try {
            List<BulkContactRow> rows = readCsv(file.getInputStream());

            if (rows.isEmpty()) {
                logger.warn("Contact list upload failed: uploaded file is empty");
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

                uniqueContacts.putIfAbsent(normalizedNumbers, row);
            }

            if (uniqueContacts.isEmpty()) {
                logger.warn("Contact list upload failed: no valid contacts found");
                throw new IllegalArgumentException("No valid contacts were found in the uploaded file");
            }

            List<BulkContactRow> contacts = new ArrayList<>(uniqueContacts.values());

            String listName = getFileListName(file);

            String listHash = generateListHash(contacts);

            if (contactListRepository.existsByListHash(listHash)) {
                logger.warn("Duplicate contact list detected: {}", listName);
                return new BulkUploadResult(listHash, rows.size(), 0, true, "Duplicate contact list. The uploaded file was not saved."
                );
            }

            ContactList contactList = new ContactList(listHash, listName, listHash);

            contactList = contactListRepository.save(contactList);

            int savedContacts = 0;

            for (BulkContactRow row : contacts) {
                String normalizedNumbers = row.getPhoneNumbers();
                Optional<Contact> existing = contactRepository.findFirstByContactNumbersAndIsActiveTrue(normalizedNumbers);

                Contact contact;
                if (existing.isPresent()) {
                    contact = existing.get();
                } else {
                    contact = new Contact();

                    contact.setContactName(row.getContactName() == null ? null : row.getContactName().trim());

                    contact.setContactNumbers(normalizedNumbers);

                    contact.setIsActive(true);

                    contact = contactRepository.save(contact);

                    savedContacts++;
                }
                contact.addContactList(contactList);

                contactRepository.save(contact);
            }

            logger.info("Contact list uploaded successfully. List name: {}, contacts: {}", listName, savedContacts);

            return new BulkUploadResult(listHash, rows.size(), savedContacts, false, "Contact list uploaded successfully.");

        } catch (IOException e) {
            logger.error("Unable to read uploaded contact list", e);
            throw new IllegalArgumentException("Unable to read the uploaded file", e);
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

            List<String> columns = parseCsvLine(line);

            if (columns.size() < 2) {
                continue;
            }

            String name = columns.get(0).trim();

            String numbers = String.join(",", columns.subList(1, columns.size())).trim();

            rows.add(new BulkContactRow(name, numbers));
        }

        return rows;
    }

    private String generateListHash(List<BulkContactRow> contacts) {

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
            logger.error("SHA-256 algorithm is not available", e);
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            logger.warn("Contact list upload failed: no file uploaded");
            throw new IllegalArgumentException("Please upload a CSV file");
        }

        String filename = file.getOriginalFilename();

        if (filename == null) {
            logger.warn("Contact list upload failed: invalid file name");
            throw new IllegalArgumentException("Invalid file name");
        }

        if (!filename.toLowerCase().endsWith(".csv")) {
            logger.warn("Contact list upload failed: unsupported file type");
            throw new IllegalArgumentException("Only CSV files are supported");
        }
    }

    private String getFileListName(MultipartFile file) {

        String filename = file.getOriginalFilename();

        if (filename == null || filename.isBlank()) {
            return "Imported contact list";
        }

        int extensionIndex = filename.lastIndexOf('.');

        return (extensionIndex > 0 ? filename.substring(0, extensionIndex) : filename).trim();
    }

    private String normalizeNumbers(String numbers) {

        if (numbers == null || numbers.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        String[] parts = numbers.split("\\s*(?:,|;|\\||/|\\r?\\n)+\\s*");

        List<String> normalized = new ArrayList<>();

        for (String part : parts) {
            String number = normalizeNumber(part.trim());
            if (!normalized.contains(number)) {
                normalized.add(number);
            }
        }

        return String.join(",", normalized);
    }

    private List<String> parseCsvLine(String line) {

        List<String> columns = new ArrayList<>();

        StringBuilder value = new StringBuilder();

        boolean quoted = false;

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);

            if (character == '"') {

                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                columns.add(value.toString());
                value.setLength(0);
            } else {
                value.append(character);
            }
        }

        columns.add(value.toString());

        return columns;
    }

    private String normalizeNumber(String number) {

        if (number == null || number.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }

        number = number.trim().replaceAll("[\\s\\-()]+", "");

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

        if (number.startsWith("20") && number.length() == 12) {

            String localPart = number.substring(2);

            if (!localPart.matches("1\\d{9}")) {
                throw new IllegalArgumentException("Invalid Egyptian phone number: " + number);
            }

            return "+" + number;
        }

        if (number.length() == 11 && number.startsWith("01")) {

            String localPart = number.substring(1);

            if (!localPart.matches("1\\d{9}")) {
                throw new IllegalArgumentException("Invalid Egyptian phone number: " + number);
            }

            return "+20" + localPart;
        }

        if (number.length() == 10 && number.startsWith("1")) {
            return "+20" + number;
        }

        if (number.matches("\\d+") && number.length() >= 10 && number.length() <= 15) {
            return "+" + number;
        }

        throw new IllegalArgumentException("Invalid phone number: " + number);
    }

    private static class BulkContactRow {

        private String contactName;
        private String phoneNumbers;

        public BulkContactRow(
                String contactName,
                String phoneNumbers
        ) {

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