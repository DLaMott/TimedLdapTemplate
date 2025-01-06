package com.timedldap;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldif.LDIFReader;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.support.LdapContextSource;

import javax.naming.directory.SearchControls;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Example test class to demonstrate the usage of the {@link TimedLdapTemplate} with an in-memory LDAP server.
 * <p>
 * This test class sets up an in-memory LDAP server using the UnboundID LDAP SDK, populates it with sample data,
 * and performs LDAP operations while collecting metrics on the performance of the operations.
 * <p>
 * Key Features:
 * - Demonstrates how to configure and use {@link TimedLdapTemplate}.
 * - Provides an example of setting up an in-memory LDAP server for testing.
 * - Illustrates how to measure performance metrics for LDAP operations.
 */
public class TimedLdapTemplateExampleTest {

    /**
     * Main method to execute the test.
     *
     * @param args command-line arguments (not used).
     * @throws LDAPException if an error occurs while setting up the LDAP server.
     * @throws IOException   if an error occurs while importing LDIF data.
     */
    public static void main(String[] args) throws LDAPException, IOException {

        // Set up an in-memory LDAP server
        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=example,dc=com");
        config.addAdditionalBindCredentials("cn=admin", "password");
        InMemoryDirectoryServer server = new InMemoryDirectoryServer(config);

        // Sample data
        String ldifData = """
                dn: dc=example,dc=com
                objectClass: top
                objectClass: domain
                dc: example
                
                dn: ou=users,dc=example,dc=com
                objectClass: top
                objectClass: organizationalUnit
                ou: users
                
                dn: uid=john.doe,ou=users,dc=example,dc=com
                objectClass: inetOrgPerson
                cn: John Doe
                sn: Doe
                uid: john.doe
                userPassword: password123
                
                dn: uid=jane.doe,ou=users,dc=example,dc=com
                objectClass: inetOrgPerson
                cn: Jane Doe
                sn: Doe
                uid: jane.doe
                userPassword: password123
                """;

        InputStream ldifStream = new ByteArrayInputStream(ldifData.getBytes(StandardCharsets.UTF_8));

        server.importFromLDIF(true, new LDIFReader(ldifStream));
        server.startListening(); // Start the LDAP server

        System.out.println("In-memory LDAP server started...");

        // Set up the Spring LDAP Context Source to leverage the TimeLdapTemplate
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl("ldap://localhost:" + server.getListenPort()); // Use the server's dynamic port
        contextSource.setBase("dc=example,dc=com");
        contextSource.setUserDn("cn=admin");
        contextSource.setPassword("password");
        contextSource.afterPropertiesSet();
        TimedLdapTemplate ldapTemplate = new TimedLdapTemplate(contextSource);

        // Perform a search
        String base = "ou=users";
        String filter = "(uid=john.doe)";
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        try {
            List<String> results = ldapTemplate.search(base, filter, searchControls, (AttributesMapper<String>) attributes -> {
                String cn = (String) attributes.get("cn").get();
                return "User: " + cn;
            });

            results.forEach(System.out::println);

            Map<String, Long> metrics = TimedLdapTemplate.getMetrics();
            System.out.println("LDAP Metrics:");
            metrics.forEach((key, value) -> System.out.println(key + ": " + value + " ms"));

        } catch (Exception e) {
            System.err.println("Error during LDAP operation: " + e.getMessage());
        } finally {

            TimedLdapTemplate.resetMetrics();

            server.shutDown(true);
            System.out.println("In-memory LDAP server stopped...");
        }
    }
}
