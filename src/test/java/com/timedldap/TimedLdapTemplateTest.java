package com.timedldap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ldap.core.DirContextProcessor;
import org.springframework.ldap.core.NameClassPairCallbackHandler;
import org.springframework.ldap.core.support.LdapContextSource;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link TimedLdapTemplate} class.
 * <p>
 * This test class demonstrates:
 * - Mocking Spring LDAP components to test functionality in isolation.
 * - Verifying the behavior of {@code executeReadOnly()} and {@code search()} methods.
 * - Handling exceptions and validating performance metrics collection.
 */
public class TimedLdapTemplateTest {
    @Mock
    private LdapContextSource contextSource;
    @Mock
    private DirContext dirContext;
    @Mock
    private NameClassPairCallbackHandler nameClassPairCallbackHandler;
    @Mock
    private DirContextProcessor processor;
    @InjectMocks
    private TimedLdapTemplate timedLdapTemplate;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Tests the {@code executeReadOnly()} method to ensure it executes a read-only LDAP operation
     * and collects performance metrics.
     *
     * @throws NamingException if an error occurs while interacting with the LDAP context.
     */
    @Test
    public void testExecuteReadOnly() throws NamingException {
        when(contextSource.getReadOnlyContext()).thenReturn(dirContext);

        String result = timedLdapTemplate.executeReadOnly(context -> "testResult");

        assertEquals("testResult", result);
        verify(contextSource).getReadOnlyContext();
        verify(dirContext).close();

        Map<String, Long> metrics = TimedLdapTemplate.getMetrics();
        assertTrue(metrics.containsKey("acquire"));
        assertTrue(metrics.containsKey("search"));
        assertTrue(metrics.containsKey("release"));
    }

    /**
     * Tests the {@code search()} method to ensure it throws an exception when the context source fails.
     *
     * @throws NamingException if an error occurs during the search operation.
     */
    @Test
    public void testSearchThrowsException() throws NamingException {
        when(contextSource.getReadOnlyContext()).thenThrow(new RuntimeException("Execution of Ldap callback failed: {}"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            timedLdapTemplate.search("base", "filter", new SearchControls(), nameClassPairCallbackHandler, processor);
        });

        assertEquals("Execution of Ldap callback failed: {}", exception.getMessage());
    }
}
