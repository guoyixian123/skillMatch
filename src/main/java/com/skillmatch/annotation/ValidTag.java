package com.skillmatch.annotation;

import com.skillmatch.validator.TagValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TagValidator.class)
public @interface ValidTag {
    String message() default "标签格式错误: 必须以#开头, 长度1-9, 只能包含中文/字母/数字";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
