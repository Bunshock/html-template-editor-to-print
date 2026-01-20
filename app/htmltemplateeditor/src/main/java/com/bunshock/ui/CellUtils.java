package com.bunshock.ui;

import java.util.List;
import java.util.Map;

import com.bunshock.model.FieldConfig;

public class CellUtils {

    public static boolean isFieldEnabled(Map<String, String> rowData, FieldConfig config) {
        if (config.getEnabledIf() == null) {
            return true; // No restrictions
        }

        String dependencyTag = config.getEnabledIf().tag;
        List<String> allowedValues = config.getEnabledIf().values;

        // Get the value of the controlling field (e.g., "type") from the current row
        String actualValue = rowData.getOrDefault(dependencyTag, "");

        // Check if the actual value is in the allowed list
        return allowedValues.contains(actualValue);
    }
}