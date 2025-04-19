package my.project.qri3a.utils;

import java.util.Locale;
import java.util.regex.Pattern;

public class SlugGenerator {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGESDHASHES = Pattern.compile("(^-|-$)");
    private static final Pattern MULTIPLEDASHES = Pattern.compile("-+");

    /**
     * Generates a slug from the given title and ID.
     * Format: words-with-hyphens-id
     * Example: pc-ba9i-ne9i-[id]
     *
     * @param title The product title
     * @param id The product ID to append
     * @return A formatted slug
     */
    public static String generateProductSlug(String title, String id) {
        if (title == null || title.isEmpty()) {
            return id;
        }

        // Convert to lowercase and normalize
        String slug = title.toLowerCase(Locale.ROOT);

        // Replace spaces with hyphens
        slug = WHITESPACE.matcher(slug).replaceAll("-");

        // Replace non-Latin characters
        slug = NONLATIN.matcher(slug).replaceAll("");

        // Replace multiple consecutive hyphens with a single hyphen
        slug = MULTIPLEDASHES.matcher(slug).replaceAll("-");

        // Remove hyphens from the beginning and end
        slug = EDGESDHASHES.matcher(slug).replaceAll("");

        // Truncate slug if it's too long (to leave room for the ID)
        int maxLength = 200; // Adjust based on your column length (255 - ID length - 1)
        if (slug.length() > maxLength) {
            slug = slug.substring(0, maxLength);
        }

        // Append the ID
        return slug + "-" + id;
    }
}