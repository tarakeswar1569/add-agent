package com.cgi.addagent.router;

import com.cgi.addagent.model.Paragraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Routes paragraphs in the ADD into backend and frontend module sections.
 *
 * The supplied BBD DOCM uses:
 *   - Heading2 "Interfaces" as the start of the FE design area.
 *   - Heading4 entries inside that area as FE modules.
 *   - Heading2 "Application Design Internal to the System" as the start of
 *     the BE design area, with Heading4 entries nested under several
 *     Heading2 subsections.
 */
public class SectionRouter {

    public static final Map<String, String> CANONICAL_NAMES = new HashMap<>();

    static {
        CANONICAL_NAMES.put("interace transfer", "Interac eTransfer");
        CANONICAL_NAMES.put("interace-transfer", "Interac eTransfer");
        CANONICAL_NAMES.put("interac e-transfer", "Interac eTransfer");
        CANONICAL_NAMES.put("message centre", "Message Centre");
        CANONICAL_NAMES.put("message center", "Message Centre");
    }

    public Map<String, List<Paragraph>> routeBeSections(List<Paragraph> all) {
        int start = findSectionHeading(all, "Application Design Internal to the System");
        if (start < 0) {
            return new LinkedHashMap<>();
        }

        // BE design contains multiple Heading2 subsections before/around the
        // actual Heading4 module headings, so do not stop at Heading2.
        return collectUntilHeading1(all, start + 1);
    }

    public Map<String, List<Paragraph>> routeFeSections(List<Paragraph> all) {
        int start = findSectionHeading(all, "Interfaces");
        if (start < 0) {
            return new LinkedHashMap<>();
        }

        // FE design is the Heading2 Interfaces section. Its Heading4 entries
        // end when the next Heading2/Heading1 section begins.
        return collectUntilMajorSection(all, start + 1);
    }

    private Map<String, List<Paragraph>> collectUntilHeading1(
            List<Paragraph> all, int start) {

        Map<String, List<Paragraph>> out = new LinkedHashMap<>();
        String current = null;

        for (int i = start; i < all.size(); i++) {
            Paragraph p = all.get(i);

            if (isHeading1(p)) {
                break;
            }

            if (isH4(p)) {
                String name = canonical(p.text());
                if (!isModuleHeading(name)) {
                    continue;
                }
                if (!name.isBlank()) {
                    current = name;
                    out.putIfAbsent(name, new ArrayList<>());
                }
            } else if (current != null) {
                out.get(current).add(p);
            }
        }

        return out;
    }

    private Map<String, List<Paragraph>> collectUntilMajorSection(
            List<Paragraph> all, int start) {

        Map<String, List<Paragraph>> out = new LinkedHashMap<>();
        String current = null;

        for (int i = start; i < all.size(); i++) {
            Paragraph p = all.get(i);

            if (isHeading1(p) || isHeading2(p)) {
                break;
            }

            if (isH4(p)) {
                String name = canonical(p.text());
                if (!isModuleHeading(name)) {
                    continue;
                }
                if (!name.isBlank()) {
                    current = name;
                    out.putIfAbsent(name, new ArrayList<>());
                }
            } else if (current != null) {
                out.get(current).add(p);
            }
        }

        return out;
    }

    /**
     * Prefer a Heading2 section with the requested title. This matters for
     * this DOCM because "Interfaces" appears earlier as a Heading3.
     */
    private int findSectionHeading(List<Paragraph> all, String section) {
        for (int i = 0; i < all.size(); i++) {
            Paragraph p = all.get(i);
            if (isHeading2(p) && equalsText(p.text(), section)) {
                return i;
            }
        }

        // Fallback for documents that use a different heading style.
        for (int i = 0; i < all.size(); i++) {
            if (equalsText(all.get(i).text(), section)) {
                return i;
            }
        }

        return -1;
    }

    private boolean equalsText(String value, String expected) {
        return value != null && value.trim().equalsIgnoreCase(expected);
    }

    private boolean isH4(Paragraph p) {
        String style = normaliseStyle(p.style());
        return "header4".equals(style)
                || "heading4".equals(style);
    }

    private boolean isHeading2(Paragraph p) {
        String style = normaliseStyle(p.style());
        return "heading2".equals(style)
                || "header2".equals(style);
    }

    private boolean isHeading1(Paragraph p) {
        String style = normaliseStyle(p.style());
        return "heading1".equals(style)
                || "header1".equals(style);
    }

    private String normaliseStyle(String style) {
        if (style == null) {
            return "";
        }
        return style.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    public static String normalise(String s) {
        return s == null
                ? ""
                : s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private boolean isModuleHeading(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String normalized = normalise(value);

        // The DOCM contains one Heading4 sentence that is documentation text,
        // not a module name. Avoid treating prose/file-path descriptions as
        // modules.
        if (normalized.startsWith("openapispecificationunder")) {
            return false;
        }

        return value.length() <= 100;
    }

    private String canonical(String s) {
        String value = s == null ? "" : s.trim();
        String normalised = normalise(value);

        for (Map.Entry<String, String> entry : CANONICAL_NAMES.entrySet()) {
            if (normalise(entry.getKey()).equals(normalised)) {
                return entry.getValue();
            }
        }

        return value;
    }
}
