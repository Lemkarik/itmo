package com.example.delivery.config;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.example.delivery.repository.UserRepository;

@Service
public class XmlUserDetailsService {

    private final List<XmlUser> users = new ArrayList<>();
    private final UserRepository userRepository;
    private final String usersXmlPath;

    public XmlUserDetailsService(
            UserRepository userRepository,
            @Value("${app.security.users-xml:}") String usersXmlPath) {
        this.userRepository = userRepository;
        this.usersXmlPath = usersXmlPath;
    }

    @PostConstruct
    public void init() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            try (InputStream is = openUsersXml()) {
                Document doc = builder.parse(is);
                NodeList userNodes = doc.getElementsByTagName("user");
                for (int i = 0; i < userNodes.getLength(); i++) {
                    Element userElement = (Element) userNodes.item(i);
                    String username = userElement.getElementsByTagName("username").item(0).getTextContent();
                    String password = userElement.getElementsByTagName("password").item(0).getTextContent();
                    String role = userElement.getElementsByTagName("role").item(0).getTextContent();
                    users.add(new XmlUser(username, password, role));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load users.xml", e);
        }
    }

    private InputStream openUsersXml() throws Exception {
        if (usersXmlPath != null && !usersXmlPath.isBlank()) {
            return Files.newInputStream(Path.of(usersXmlPath));
        }
        return new ClassPathResource("users.xml").getInputStream();
    }

    public XmlUser findByUsername(String username) {
        return users.stream()
                .filter(u -> u.username().equals(username))
                .findFirst()
                .orElse(null);
    }

    public List<XmlUser> getAllUsers() {
        return new ArrayList<>(users);
    }

    public Long resolveDatabaseUserId(String username) {
        return userRepository.findByUsername(username)
                .map(com.example.delivery.model.User::getId)
                .orElse(null);
    }

    public record XmlUser(String username, String password, String role) {
    }
}
