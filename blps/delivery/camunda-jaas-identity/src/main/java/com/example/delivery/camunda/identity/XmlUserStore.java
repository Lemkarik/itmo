package com.example.delivery.camunda.identity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

final class XmlUserStore {

    static final String USERS_PATH_PROPERTY = "delivery.users.xml";
    static final String USERS_PATH_ENV = "DELIVERY_USERS_XML";

    private final Path usersXmlPath;
    private final String source;

    XmlUserStore() {
        String path = System.getProperty(USERS_PATH_PROPERTY);
        if (path == null || path.isBlank()) {
            path = System.getenv(USERS_PATH_ENV);
        }
        usersXmlPath = path == null || path.isBlank()
                ? Path.of(System.getProperty("user.home"), ".delivery", "users.xml").toAbsolutePath()
                : Path.of(path).toAbsolutePath();
        initializeUsersXml();
        source = usersXmlPath.toString();
    }

    List<UserAccount> users() {
        return loadUsers();
    }

    String source() {
        return source;
    }

    UserAccount find(String username) {
        return loadUsers().stream()
                .filter(user -> user.username().equals(username))
                .findFirst()
                .orElse(null);
    }

    private List<UserAccount> loadUsers() {
        try (InputStream input = openUsersXml()) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            NodeList nodes = factory.newDocumentBuilder().parse(input).getElementsByTagName("user");
            List<UserAccount> result = new ArrayList<>();
            for (int index = 0; index < nodes.getLength(); index++) {
                Element element = (Element) nodes.item(index);
                result.add(new UserAccount(
                        text(element, "username"),
                        text(element, "password"),
                        text(element, "role")));
            }
            return List.copyOf(result);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load delivery users.xml", exception);
        }
    }

    private InputStream openUsersXml() throws IOException {
        return Files.newInputStream(usersXmlPath);
    }

    private void initializeUsersXml() {
        if (Files.isRegularFile(usersXmlPath)) {
            return;
        }
        try {
            Files.createDirectories(usersXmlPath.getParent());
            try (InputStream resource = XmlUserStore.class.getClassLoader().getResourceAsStream("users.xml")) {
                if (resource == null) {
                    throw new IOException("users.xml is absent from the plugin classpath");
                }
                Files.copy(resource, usersXmlPath);
            } catch (FileAlreadyExistsException ignored) {
                return;
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot initialize delivery users.xml", exception);
        }
    }

    private String text(Element element, String tagName) {
        return element.getElementsByTagName(tagName).item(0).getTextContent().trim();
    }

    record UserAccount(String username, String password, String role) {
    }
}
