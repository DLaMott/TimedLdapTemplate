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

    @Test
    public void testSearchThrowsException() throws NamingException {
        when(contextSource.getReadOnlyContext()).thenThrow(new RuntimeException("Execution of Ldap callback failed: {}"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            timedLdapTemplate.search("base", "filter", new SearchControls(), nameClassPairCallbackHandler, processor);
        });

        assertEquals("Execution of Ldap callback failed: {}", exception.getMessage());
    }

}
