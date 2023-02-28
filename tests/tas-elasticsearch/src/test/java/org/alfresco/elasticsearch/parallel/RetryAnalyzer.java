package org.alfresco.elasticsearch.parallel;

import java.util.ConcurrentModificationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Sometimes REST tests fail when run in parallel with:
 * <pre>
 *     java.lang.IllegalStateException: Invalid use of BasicClientConnManager: connection still allocated.
 *     Make sure to release the connection before allocating another one.
 * </pre>
 * Other times restassured fails with <code>java.util.ConcurrentModificationException</code>.
 * If we detect a test failed due to these timing issues then we rerun it up to three times.
 */
public class RetryAnalyzer implements IRetryAnalyzer
{
    private static Logger LOGGER = LoggerFactory.getLogger(RetryAnalyzer.class);
    private static int RETRY_LIMIT = 3;
    private int retryNumber = 0;

    @Override
    public boolean retry(ITestResult testResult) {
        retryNumber++;
        if (retryNumber == RETRY_LIMIT)
        {
            return false;
        }
        Throwable throwable = testResult.getThrowable();
        boolean shouldRetry = throwable != null
                                && (throwable instanceof IllegalStateException
                                    && throwable.getMessage().contains("connection still allocated"))
                                || (throwable instanceof ConcurrentModificationException);
        LOGGER.info("Retry: {}, shouldRetry: {}", retryNumber, shouldRetry, throwable);
        return shouldRetry;
    }
}
