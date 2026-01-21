package com.bunshock.service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReportGenerator {

    /**
     * Replaces simple top-level fields (e.g. {{NOMBRE}}, {{DNI}}).
     */
    public String processSimpleFields(String html, Map<String, String> data) {
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() == null ? "" : entry.getValue();

            // Case-insensitive replacement for {{KEY}}
            String regex = "(?i)\\{\\{" + key + "\\}\\}";
            html = html.replaceAll(regex, Matcher.quoteReplacement(value));
        }
        return html;
    }

    /**
     * Processes the {{#ITEMS}}...{{/ITEMS}} block.
     * For each item:
     * 1. Copies the HTML block.
     * 2. Fills in data.
     * 3. Removes lines/divs for empty fields.
     */
    public String processRepeatedItems(String html, List<Map<String, String>> items) {
        // 1. Find the {{#ITEMS}} block
        Pattern blockPattern = Pattern.compile("(?i)\\{\\{#ITEMS\\}\\}(.*?)\\{\\{/ITEMS\\}\\}", Pattern.DOTALL);
        Matcher blockMatcher = blockPattern.matcher(html);

        if (blockMatcher.find()) {
            String itemTemplate = blockMatcher.group(1); 
            StringBuilder allItemsHtml = new StringBuilder();

            for (Map<String, String> itemData : items) {
                String rowHtml = itemTemplate;

                for (Map.Entry<String, String> entry : itemData.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    boolean isEmpty = (value == null || value.trim().isEmpty());

                    if (isEmpty) {
                        // --- THE FIX ---
                        // 1. We match the OPENING tag of the container: <div ...>
                        // 2. We allow ANY content (.*? including newlines/spans) 
                        // 3. Until we hit {{KEY}}
                        // 4. Then we find the CLOSING tag of that same container: </div>
                        
                        // "dotall" mode (?s) allows matching across newlines
                        // We target 'div', 'p', 'span', or 'li' tags specifically to avoid accidental deletion of larger blocks
                        String removeRegex = "(?is)<(div|p|span|li)[^>]*>(?:(?!<\\/\\1>).)*?\\{\\{" + key + "\\}\\}.*?<\\/\\1>";
                        
                        rowHtml = rowHtml.replaceAll(removeRegex, "");
                        
                        // Cleanup: Also remove the bare placeholder if the regex didn't catch a wrapper
                        rowHtml = rowHtml.replaceAll("(?i)\\{\\{" + key + "\\}\\}", "");
                    } else {
                        // Standard Replacement
                        String placeholderRegex = "(?i)\\{\\{" + key + "\\}\\}";
                        rowHtml = rowHtml.replaceAll(placeholderRegex, Matcher.quoteReplacement(value));
                    }
                }
                // Cleanup empty lines
                rowHtml = rowHtml.replaceAll("(?m)^\\s+$", ""); 
                allItemsHtml.append(rowHtml);
            }

            html = html.substring(0, blockMatcher.start()) + 
                   allItemsHtml.toString() + 
                   html.substring(blockMatcher.end());
        }

        return html;
    }
}