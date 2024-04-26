package org.alfresco.elasticsearch.utils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;

/**
 * A method interceptor that sorts test classes alphabetically.
 *
 * @author Damian Ujma
 */
public class AlphabeticalPriorityInterceptor implements IMethodInterceptor
{
    @Override
    public List<IMethodInstance> intercept(List<IMethodInstance> methods,
        ITestContext context)
    {
        return methods.stream()
            .sorted(Comparator.comparing(methodInstance -> methodInstance.getMethod().getTestClass().getName()))
            .collect(Collectors.toList());
    }
}
