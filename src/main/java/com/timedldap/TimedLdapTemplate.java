package com.timedldap;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.*;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A custom extension of the {@link LdapTemplate} class that tracks the time taken for various LDAP operations.
 * <p>
 * The {@code TimedLdapTemplate} class is designed to provide metrics for LDAP operations such as acquiring the
 * context, executing searches, and releasing the context. These metrics are useful for debugging and monitoring
 * the performance of LDAP operations in real-time.
 * <p>
 * Key Features:
 * - Tracks the time taken to acquire the LDAP context.
 * - Tracks the time taken for the actual LDAP operation.
 * - Tracks the time taken to release the LDAP context.
 * - Provides a mechanism to retrieve and reset metrics for analysis.
 */
public class TimedLdapTemplate extends LdapTemplate {

    private static final Logger LOG = LoggerFactory.getLogger(TimedLdapTemplate.class);

    /**
     * Constructs a new {@code TimedLdapTemplate} instance using the provided {@link ContextSource}.
     *
     * @param contextSource the {@link ContextSource} to use for LDAP operations.
     */
    public TimedLdapTemplate(ContextSource contextSource){
        super(contextSource);
    }

    /**
     * Executes a read-only LDAP operation and tracks the time taken for acquiring the context,
     * executing the operation, and releasing the context.
     *
     * @param action the {@link ContextExecutor} that encapsulates the LDAP operation to be performed.
     * @param <T>    the type of the result returned by the operation.
     * @return the result of the LDAP operation.
     * @throws RuntimeException if the LDAP operation fails.
     */
    @Override
    public<T> T executeReadOnly(ContextExecutor<T> action){
        Stopwatch stopwatch = Stopwatch.createStarted();
        DirContext context = null;
        long acquireTime = 0;
        long searchTime = 0;
        long releaseTime = 0;

        try{

            context = getContextSource().getReadOnlyContext();
            acquireTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            addMetrics("acquire", acquireTime);

            stopwatch.reset().start();
            T result = action.executeWithContext(context);
            searchTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            addMetrics("search", searchTime);

            return result;

        }catch (Exception e){
            LOG.error("Execution of LDAP callback failed: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }finally {
            if (context != null){
                stopwatch.reset().start();
                closeContext(context);
                releaseTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
                LOG.info("Released context in {} ms", releaseTime);
                addMetrics("release", releaseTime);
            }
        }
    }

    /**
     * Performs an LDAP search operation and tracks the time taken for acquiring the context,
     * executing the search, and releasing the context.
     *
     * @param base      the base directory for the search.
     * @param filter    the LDAP search filter.
     * @param controls  the {@link SearchControls} that specify the scope and other search parameters.
     * @param handler   the {@link NameClassPairCallbackHandler} to handle search results.
     * @param processor the {@link DirContextProcessor} to process the directory context.
     * @throws RuntimeException if the LDAP search fails.
     */
    @Override
    public void search(final String base, final String filter, final SearchControls controls,
                       NameClassPairCallbackHandler handler, DirContextProcessor processor){
        Stopwatch stopwatch = Stopwatch.createStarted();
        DirContext context = null;
        long acquireTime = 0;
        long searchTime = 0;
        long releaseTime = 0;

        try{

            context = getContextSource().getReadOnlyContext();
            acquireTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            addMetrics("acquire", acquireTime);

            stopwatch.reset().start();
            super.search(base,filter,controls,handler,processor);
            searchTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            addMetrics("search", searchTime);
        }catch (Exception e){
            LOG.error("Execution of LDAP callback failed: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }finally {
            if (context != null){
                stopwatch.reset().start();
                closeContext(context);
                releaseTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
                LOG.info("Released context in {} ms", releaseTime);
                addMetrics("release", releaseTime);
            }
        }
    }

    /**
     * Closes the provided LDAP {@link DirContext}.
     *
     * @param context the {@link DirContext} to be closed.
     */
    private void closeContext(DirContext context) {
        try {
            context.close();
        }catch (NamingException e){
            LOG.error("Failed to close context: {}", e.getMessage());
        }
    }

    private static ThreadLocal<Map<String, Long>> metrics = ThreadLocal.withInitial(HashMap::new);

    /**
     * Adds a metric entry with the specified key and value.
     *
     * @param key   the key representing the metric (e.g., "acquire", "search", "release").
     * @param value the time value (in milliseconds) associated with the metric.
     */
    private void addMetrics(String key, long value){
        metrics.get().put(key,value);
    }

    /**
     * Retrieves the current metrics as an immutable map.
     *
     * @return a map containing the metrics tracked for LDAP operations.
     */
    public static Map<String, Long> getMetrics(){
        return new HashMap<>(metrics.get());
    }

    /**
     * Resets the metrics, clearing all tracked values.
     */
    public static void resetMetrics(){
        metrics.get().clear();
    }
}
