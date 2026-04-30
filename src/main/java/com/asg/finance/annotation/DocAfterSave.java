package com.asg.finance.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DocAfterSave {

    /**
     * The document ID (e.g. "400-101" for Petty Cash).
     * Falls back to UserContext.getDocumentId() when blank.
     */
    String docId() default "";
}
