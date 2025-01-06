package com.timedldap;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldif.LDIFReader;
import org.springframework.ldap.core.ContextExecutor;
import org.springframework.ldap.core.support.LdapContextSource;

import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Example test class to demonstrate the usage of {@link TimedLdapTemplate#executeReadOnly(ContextExecutor)}
 * with an in-memory LDAP server.
 * <p>
 * This test illustrates:
 * - Setting up an in-memory LDAP server for testing purposes.
 * - Using the {@code executeReadOnly} method of {@link TimedLdapTemplate} to perform read-only LDAP operations.
 * - Collecting and displaying performance metrics for LDAP operations.
 */
public class TimedLdapTemplateExecuteReadOnlyTest {

    /**
     * Main method to execute the test.
     *
     * @param args command-line arguments (not used).
     * @throws LDAPException if an error occurs while setting up the LDAP server or executing LDAP operations.
     */
    public static void main(String[] args) throws LDAPException, LDAPException {
        // Set up an in-memory LDAP server
        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=example,dc=com");
        config.addAdditionalBindCredentials("cn=admin", "password");
        InMemoryDirectoryServer server = new InMemoryDirectoryServer(config);

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
        server.startListening();
        System.out.println("In-memory LDAP server started...");

        // Set up the Spring LDAP Context Source to leverage the TimeLdapTemplate
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl("ldap://localhost:" + server.getListenPort());
        contextSource.setBase("dc=example,dc=com");
        contextSource.setUserDn("cn=admin");
        contextSource.setPassword("password");
        contextSource.afterPropertiesSet();

        TimedLdapTemplate ldapTemplate = new TimedLdapTemplate(contextSource);

        // Execute the "executeReadOnly" method
        try {
            String filter = "(uid=john.doe)";
            String searchBase = "ou=users";

            String result = ldapTemplate.executeReadOnly(ctx -> {
                SearchControls controls = new SearchControls();
                controls.setSearchScope(SearchControls.SUBTREE_SCOPE);

                NamingEnumeration<SearchResult> results = ctx.search(searchBase, filter, controls);
                if (results.hasMore()) {
                    SearchResult searchResult = results.next();
                    return searchResult.getNameInNamespace();
                }
                return null;
            });

            if (result != null) {
                System.out.println("Search Result: " + result);
            } else {
                System.out.println("No result found.");
            }

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