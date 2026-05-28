package com.skillmatch.validator;

import com.skillmatch.annotation.ValidTag;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;
import java.util.regex.Pattern;

public class TagValidator implements ConstraintValidator<ValidTag, List<String>> {
    private static final Pattern PATTERN = Pattern.compile("^#[一-龥a-zA-Z0-9]{1,9}$");

    @Override
    public boolean isValid(List<String> tags, ConstraintValidatorContext context) {
        if (tags == null || tags.isEmpty()) return true;
        return tags.stream().allMatch(t -> t != null && PATTERN.matcher(t).matches());
    }
}
