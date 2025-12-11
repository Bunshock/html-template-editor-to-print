package com.bunshock.service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReportGenerator {
    
    // Process simple fields (Strings and dates)
    public String processSimpleFields(String html, Map<String, String> data) {
        String result = html;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    // Process tables
    public String processTables(String html, String tableName, List<Map<String, String>> rows) {
        String regex = "\\{\\{#" + tableName + "\\}\\}(.*?)\\{\\{/" + tableName + "\\}\\}";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);

        if (matcher.find()) {
            String rowTemplate = matcher.group(1);
            StringBuilder allRows = new StringBuilder();

            for (Map<String, String> rowData : rows) {
                String tempRow = rowTemplate;
                for (Map.Entry<String, String> col : rowData.entrySet()) {
                    String val = col.getValue() == null ? "" : col.getValue();
                    tempRow = tempRow.replace("{{" + col.getKey() + "}}", val);
                }
                allRows.append(tempRow).append("\n");
            }
            return matcher.replaceFirst(allRows.toString());
        }
        return html;
    }
}
