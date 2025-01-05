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

public class TimedLdapTemplate extends LdapTemplate {

    private static final Logger LOG = LoggerFactory.getLogger(TimedLdapTemplate.class);

    public TimedLdapTemplate(ContextSource contextSource){
        super(contextSource);
    }

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

    private void closeContext(DirContext context) {
        try {
            context.close();
        }catch (NamingException e){
            LOG.error("Failed to close context: {}", e.getMessage());
        }
    }

    private static ThreadLocal<Map<String, Long>> metrics = ThreadLocal.withInitial(HashMap::new);

    private void addMetrics(String key, long value){
        metrics.get().put(key,value);
    }

    public static Map<String, Long> getMetrics(){
        return new HashMap<>(metrics.get());
    }

    public static void resetMetrics(){
        metrics.get().clear();
    }
}
