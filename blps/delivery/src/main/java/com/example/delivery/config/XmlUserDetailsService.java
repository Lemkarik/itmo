package com.example.delivery.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.example.delivery.exception.ConflictException;
import com.example.delivery.model.UserRole;
import com.example.delivery.repository.UserRepository;

@Service
public class XmlUserDetailsService {

    private static final Pattern BCRYPT_HASH = Pattern.compile("\\$2[aby]\\$\\d{2}\\$.{53}");

    private final UserRepository userRepository;
    private final Path usersXmlPath;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public XmlUserDetailsService(
            UserRepository userRepository,
            @Value("${app.security.users-xml:}") String usersXmlPath) {
        this.userRepository = userRepository;
        this.usersXmlPath = resolveUsersXmlPath(usersXmlPath);
        initializeUsersXml();
        migratePlaintextPasswords();
    }

    public XmlUser findByUsername(String username) {
        return loadUsers().stream()
                .filter(user -> user.username().equals(username))
                .findFirst()
                .orElse(null);
    }

    public List<XmlUser> getAllUsers() {
        return loadUsers();
    }

    public synchronized void addUser(String username, String password, UserRole role) {
        Path xmlPath = writableUsersXmlPath();
        Path lockPath = xmlPath.resolveSibling(xmlPath.getFileName() + ".lock");
        try (FileChannel channel = FileChannel.open(
                lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {
            Document document = parse(xmlPath);
            if (find(document, username) != null) {
                throw new ConflictException("Username is already registered");
            }

            Element user = document.createElement("user");
            appendText(document, user, "username", username);
            appendText(document, user, "password", passwordEncoder.encode(password));
            appendText(document, user, "role", role.name());
            document.getDocumentElement().appendChild(user);
            writeAtomically(document, xmlPath);
        } catch (ConflictException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot update users.xml", exception);
        }
    }

    public Long resolveDatabaseUserId(String username) {
        return userRepository.findByUsername(username)
                .map(com.example.delivery.model.User::getId)
                .orElse(null);
    }

    public boolean passwordMatches(String rawPassword, String passwordHash) {
        return rawPassword != null
                && passwordHash != null
                && BCRYPT_HASH.matcher(passwordHash).matches()
                && passwordEncoder.matches(rawPassword, passwordHash);
    }

    private List<XmlUser> loadUsers() {
        try (InputStream input = openUsersXml()) {
            return readUsers(parse(input));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load users.xml", exception);
        }
    }

    private InputStream openUsersXml() throws IOException {
        return Files.newInputStream(usersXmlPath);
    }

    private Path writableUsersXmlPath() {
        if (!Files.isRegularFile(usersXmlPath)) {
            throw new IllegalStateException("Configured users.xml does not exist: " + usersXmlPath);
        }
        return usersXmlPath;
    }

    private Path resolveUsersXmlPath(String configuredPath) {
        if (configuredPath != null && !configuredPath.isBlank()) {
            return Path.of(configuredPath).toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("user.home"), ".delivery", "users.xml")
                .toAbsolutePath()
                .normalize();
    }

    private void initializeUsersXml() {
        if (Files.isRegularFile(usersXmlPath)) {
            return;
        }
        try {
            Files.createDirectories(usersXmlPath.getParent());
            try (InputStream input = new ClassPathResource("users.xml").getInputStream()) {
                Files.copy(input, usersXmlPath);
            } catch (FileAlreadyExistsException ignored) {
                return;
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot initialize users.xml: " + usersXmlPath, exception);
        }
    }

    private synchronized void migratePlaintextPasswords() {
        Path lockPath = usersXmlPath.resolveSibling(usersXmlPath.getFileName() + ".lock");
        try (FileChannel channel = FileChannel.open(
                lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {
            Document document = parse(usersXmlPath);
            NodeList passwords = document.getElementsByTagName("password");
            boolean changed = false;
            for (int index = 0; index < passwords.getLength(); index++) {
                Element password = (Element) passwords.item(index);
                String value = password.getTextContent().trim();
                if (!BCRYPT_HASH.matcher(value).matches()) {
                    password.setTextContent(passwordEncoder.encode(value));
                    changed = true;
                }
            }
            if (changed) {
                writeAtomically(document, usersXmlPath);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot migrate users.xml passwords to BCrypt", exception);
        }
    }

    private Document parse(Path path) throws Exception {
        try (InputStream input = Files.newInputStream(path)) {
            return parse(input);
        }
    }

    private Document parse(InputStream input) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(input);
    }

    private List<XmlUser> readUsers(Document document) {
        NodeList nodes = document.getElementsByTagName("user");
        List<XmlUser> result = new ArrayList<>();
        for (int index = 0; index < nodes.getLength(); index++) {
            Element element = (Element) nodes.item(index);
            result.add(new XmlUser(
                    text(element, "username"),
                    text(element, "password"),
                    text(element, "role")));
        }
        return List.copyOf(result);
    }

    private XmlUser find(Document document, String username) {
        return readUsers(document).stream()
                .filter(user -> user.username().equals(username))
                .findFirst()
                .orElse(null);
    }

    private String text(Element element, String tagName) {
        return element.getElementsByTagName(tagName).item(0).getTextContent().trim();
    }

    private void appendText(Document document, Element parent, String name, String value) {
        Element element = document.createElement(name);
        element.setTextContent(value);
        parent.appendChild(element);
    }

    private void writeAtomically(Document document, Path target) throws Exception {
        removeWhitespace(document.getDocumentElement());
        Path temporary = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            try (OutputStream output = Files.newOutputStream(temporary, StandardOpenOption.TRUNCATE_EXISTING)) {
                transformer.transform(new DOMSource(document), new StreamResult(output));
            }
            moveAtomically(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void removeWhitespace(Node parent) {
        Node child = parent.getFirstChild();
        while (child != null) {
            Node next = child.getNextSibling();
            if (child.getNodeType() == Node.TEXT_NODE && child.getTextContent().isBlank()) {
                parent.removeChild(child);
            } else {
                removeWhitespace(child);
            }
            child = next;
        }
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public record XmlUser(String username, String password, String role) {
    }
}
