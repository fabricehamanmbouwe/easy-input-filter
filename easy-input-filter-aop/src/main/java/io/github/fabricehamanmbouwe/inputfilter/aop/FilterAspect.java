package io.github.fabricehamanmbouwe.inputfilter.aop;

import io.github.fabricehamanmbouwe.inputfilter.core.FilterEngine;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Intercepts every Spring MVC controller method and runs the {@link FilterEngine}
 * against each argument before the controller method executes.
 * <p>
 * Two cases are handled:
 * <ul>
 *   <li>{@code @RequestBody} arguments — the whole DTO is scanned for field-level annotations.</li>
 *   <li>Plain {@code String} parameters carrying filter annotations from the
 *       {@code io.github.fabricehamanmbouwe.inputfilter.annotation} package directly
 *       (e.g. {@code @RequestParam @NoPhone String phone}) — the value is filtered in place.</li>
 * </ul>
 */
@Aspect
public class FilterAspect {

    private static final String FILTER_ANNOTATION_PACKAGE =
            "io.github.fabricehamanmbouwe.inputfilter.annotation";

    private final FilterEngine filterEngine;

    public FilterAspect(FilterEngine filterEngine) {
        this.filterEngine = filterEngine;
    }

    @Around("execution(* (@org.springframework.web.bind.annotation.RestController *).*(..)) || " +
            "execution(* (@org.springframework.stereotype.Controller *).*(..))")
    public Object filterRequestBodies(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = method.getParameters();

        for (int i = 0; i < parameters.length; i++) {
            if (isRequestBody(parameters[i]) && args[i] != null) {
                filterEngine.process(args[i]);
            } else if (!isRequestBody(parameters[i])
                    && args[i] instanceof String s
                    && hasFilterAnnotations(parameters[i])) {
                args[i] = filterEngine.processValue(s, parameters[i].getAnnotations());
            }
        }

        return joinPoint.proceed(args);
    }

    private boolean isRequestBody(Parameter parameter) {
        return AnnotatedElementUtils.hasAnnotation(parameter, RequestBody.class)
                || parameter.getAnnotation(RequestBody.class) != null;
    }

    private boolean hasFilterAnnotations(Parameter parameter) {
        for (Annotation annotation : parameter.getAnnotations()) {
            if (annotation.annotationType().getPackageName().startsWith(FILTER_ANNOTATION_PACKAGE)) {
                return true;
            }
        }
        return false;
    }
}
