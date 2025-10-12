package ch.epfl.biop.atlas.aligner.gui.bdv;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Color;
import java.awt.Window;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ABBATheme {

    public static void setTheme(ABBATheme theme) {
        try {
            // Get all instance fields from ABBATheme
            Field[] themeFields = ABBATheme.class.getDeclaredFields();

            // Get all static fields from ABBABdvViewPrefs
            Field[] prefsFields = ABBABdvViewPrefs.class.getDeclaredFields();

            // Create maps for easy lookup and comparison
            Map<String, Field> themeFieldMap = new HashMap<>();
            for (Field f : themeFields) {
                if (!f.isSynthetic()) { // Skip compiler-generated fields
                    themeFieldMap.put(f.getName(), f);
                }
            }

            Map<String, Field> prefsFieldMap = new HashMap<>();
            for (Field f : prefsFields) {
                if (Modifier.isStatic(f.getModifiers()) && !f.isSynthetic()) {
                    prefsFieldMap.put(f.getName(), f);
                }
            }

            // Check for fields in theme but not in prefs (missing destination)
            Set<String> missingInPrefs = new HashSet<>(themeFieldMap.keySet());
            missingInPrefs.removeAll(prefsFieldMap.keySet());
            if (!missingInPrefs.isEmpty()) {
                System.err.println("Warning: Fields exist in ABBATheme but not as static fields in ABBABdvViewPrefs: " + missingInPrefs);
            }

            // Check for static fields in prefs but not in theme (missing source)
            Set<String> missingInTheme = new HashSet<>(prefsFieldMap.keySet());
            missingInTheme.removeAll(themeFieldMap.keySet());
            if (!missingInTheme.isEmpty()) {
                System.err.println("Warning: Static fields exist in ABBABdvViewPrefs but not in ABBATheme: " + missingInTheme);
            }

            // Copy matching fields from theme instance to static fields in prefs
            for (Map.Entry<String, Field> entry : themeFieldMap.entrySet()) {
                String fieldName = entry.getKey();
                Field themeField = entry.getValue();
                Field prefsField = prefsFieldMap.get(fieldName);

                if (prefsField != null) {
                    themeField.setAccessible(true);
                    prefsField.setAccessible(true);

                    Object value = themeField.get(theme);
                    prefsField.set(null, value); // null because it's a static field
                }
            }

        } catch (IllegalAccessException e) {
            System.err.println("Error setting theme via reflection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Fonts
    public Font slice_info_font;
    public Font mouse_atlas_coordinates_font;
    public Font action_summary_current_slice_font;
    public Font arrow_on_current_slice_font;
    public Font action_font;

    // Strokes
    public BasicStroke selection_stroke;
    public BasicStroke dashed_stroke_slice_handle_to_atlas;
    public BasicStroke task_counter_stroke;
    public BasicStroke current_slice_circle_stroke;
    public BasicStroke line_between_selected_slices_stroke;

    // Colors
    public Color slice_info_color;
    public Color done;
    public Color done_export;
    public Color locked;
    public Color pending;
    public Color unmirror_small_scale;
    public Color raster_small_scale;
    public Color invalid_action_string_color;
    public Color delete_action_string_color;
    public Color text_action_register;
    public Color raster_action_string_color;
    public Color register_small_scale;
    public Color stroke_color_left;
    public Color stroke_color_right;
    public Color text_action_export_to_roimanager;
    public Color text_action_export_deformation_field_action;
    public Color text_action_export_atlas_to_image_plus;
    public Color text_action_export_slice_region_to_file; //text_action_export_slice_region_to_file
    public Color text_action_export_slice_regions_to_qupath;
    public Color text_action_unmirror;
    public Color text_action_raster_deformation;

    public Color color_cite_command_bg;
    public Color task_counter_color;
    public Color text_action_export_slice_to_image_plus;
    public Color mouse_atlas_coordinates_color;
    public Color line_between_selected_slices_color;
    public Color color_slice_handle_not_selected;
    public Color color_slice_handle_selected;
    public Color current_slice_handle_color;
    public Color action_summary_current_slice_color;
    public Color rectangle_dnd_color;
    public Color current_slice_circle_color;
    public Color selection_back_color;

    public String title_suffix="", title_prefix = "";

    public static String default_font = "SansSerif";//"Inter";

    public static ABBATheme createLightTheme() {
        // WHITE BACKGROUND - use DARKER colors for contrast
        ABBATheme theme = new ABBATheme();

        // Fonts (same for both themes)
        theme.slice_info_font = new Font(default_font, Font.BOLD, 16);
        theme.mouse_atlas_coordinates_font = new Font(default_font, Font.BOLD, 16);
        theme.action_summary_current_slice_font = new Font(default_font, Font.PLAIN, 16);
        theme.arrow_on_current_slice_font = new Font(default_font, Font.BOLD, 18);
        theme.action_font = new Font(default_font, Font.BOLD, 10);

        // Strokes (same for both themes)
        theme.selection_stroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        theme.line_between_selected_slices_stroke = new BasicStroke(2);
        theme.dashed_stroke_slice_handle_to_atlas = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        theme.task_counter_stroke = new BasicStroke(4);
        theme.current_slice_circle_stroke = new BasicStroke(5);

        // Colors - DARKER for white background
        theme.slice_info_color = new Color(255-180, 255-180, 255-180, 220);              // Dark green
        theme.done = new Color(25, 100, 40, 220);                            // Forest green
        theme.locked = new Color(180, 0, 0, 220);                          // Dark red
        theme.pending = new Color(200, 150, 0, 220);                       // Gold/amber
        theme.unmirror_small_scale = new Color(90, 90, 90, 200);          // Dark gray
        theme.raster_small_scale = new Color(90, 90, 90, 200);            // Dark gray
        theme.invalid_action_string_color = new Color(180, 0, 80, 220);   // Dark magenta
        theme.delete_action_string_color = new Color(20, 20, 20, 220);     // Near black
        theme.raster_action_string_color = new Color(40, 40, 120, 220);   // Dark blue
        theme.done_export = new Color(40, 40, 120, 220);
        theme.register_small_scale = new Color(90, 90, 90, 200);          // Dark gray
        theme.stroke_color_left = new Color(0, 150, 0);                   // Forest green
        theme.stroke_color_right = new Color(180, 0, 180);                // Dark magenta
        Color text_action = new Color(255, 255, 255, 200);
        theme.text_action_register = text_action;
        theme.text_action_export_to_roimanager = text_action;
        theme.text_action_export_deformation_field_action = text_action;
        theme.text_action_export_atlas_to_image_plus = text_action;
        theme.text_action_export_slice_region_to_file = text_action;
        theme.text_action_export_slice_regions_to_qupath = text_action;
        theme.text_action_export_slice_to_image_plus = text_action;
        theme.text_action_unmirror = text_action;
        theme.text_action_raster_deformation = text_action;

        theme.color_cite_command_bg = new Color(245, 245, 245);           // Very light gray
        theme.task_counter_color = new Color(0xF7BF18);          // Dark o
        theme.mouse_atlas_coordinates_color = new Color(60, 60, 60, 180); // Dark gray
        theme.line_between_selected_slices_color = new Color(180, 0, 180, 220); // Dark magenta
        theme.color_slice_handle_not_selected = new Color(200, 150, 0, 128);    // Gold
        theme.color_slice_handle_selected = new Color(0, 150, 0, 220);          // Forest green
        theme.current_slice_handle_color = new Color(80, 80, 80, 180);          // Dark gray
        theme.action_summary_current_slice_color = new Color(50, 50, 50, 180);  // Dark gray
        theme.rectangle_dnd_color = new Color(80, 180, 30, 180);                // Dark lime
        theme.current_slice_circle_color = new Color(60, 60, 60, 180);          // Dark gray
        theme.selection_back_color = new Color(0xF7BF18);                       // Orange/gold

        return theme;
    }

    public static ABBATheme createDarkTheme() {
        // BLACK BACKGROUND - use BRIGHTER colors for contrast
        ABBATheme theme = new ABBATheme();

        // Fonts (same as light theme)
        theme.slice_info_font = new Font(default_font, Font.BOLD, 16);
        theme.mouse_atlas_coordinates_font = new Font(default_font, Font.BOLD, 16);
        theme.action_summary_current_slice_font = new Font(default_font, Font.PLAIN, 16);
        theme.arrow_on_current_slice_font = new Font(default_font, Font.BOLD, 20);
        theme.action_font = new Font("TimesRoman", Font.BOLD, 10);

        // Strokes (same as light theme)
        theme.selection_stroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        theme.dashed_stroke_slice_handle_to_atlas = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        theme.task_counter_stroke = new BasicStroke(4);
        theme.current_slice_circle_stroke = new BasicStroke(5);
        theme.line_between_selected_slices_stroke = new BasicStroke(2);

        // Colors - BRIGHTER for black background
        theme.slice_info_color = new Color(180, 180, 180, 220);            // Bright green
        theme.done = new Color(100, 220, 120, 220);                     // Lime green
        theme.locked = new Color(255, 100, 100, 220);                      // Light red/coral
        theme.pending = new Color(255, 255, 100, 220);                     // Bright yellow
        theme.unmirror_small_scale = new Color(180, 180, 180, 220);        // Light gray
        theme.raster_small_scale = new Color(180, 180, 180, 220);          // Light gray
        theme.invalid_action_string_color = new Color(255, 100, 180, 220); // Pink/magenta
        theme.delete_action_string_color = new Color(255, 255, 255, 220);  // White
        theme.raster_action_string_color = new Color(150, 200, 255, 220);  // Light blue
        theme.done_export = new Color(150, 200, 255, 220);
        theme.register_small_scale = new Color(180, 180, 180, 220);        // Light gray
        theme.stroke_color_left = new Color(100, 255, 100);                // Lime green
        theme.stroke_color_right = new Color(255, 100, 255);               // Bright magenta
        Color text_action = new Color(0, 0, 0, 200);
        theme.text_action_register = text_action;
        theme.text_action_export_to_roimanager = text_action;
        theme.text_action_export_deformation_field_action = text_action;
        theme.text_action_export_atlas_to_image_plus = text_action;
        theme.text_action_export_slice_region_to_file = text_action;
        theme.text_action_export_slice_regions_to_qupath = text_action;
        theme.text_action_export_slice_to_image_plus = text_action;
        theme.text_action_unmirror = text_action;
        theme.text_action_raster_deformation = text_action;

        theme.color_cite_command_bg = new Color(40, 40, 40);               // Dark gray bg
        theme.task_counter_color = new Color(0xFFD700);          // Light ochre/gold
        theme.mouse_atlas_coordinates_color = new Color(200, 200, 200, 180); // Light gray
        theme.line_between_selected_slices_color = new Color(255, 100, 255, 220); // Bright magenta
        theme.color_slice_handle_not_selected = new Color(180, 200, 100, 150);    // Bright yellow
        theme.color_slice_handle_selected = new Color(120, 220, 140, 220);        // Lime green
        theme.current_slice_handle_color = new Color(200, 200, 200, 200);         // Light gray
        theme.action_summary_current_slice_color = new Color(220, 220, 220, 200); // Light gray
        theme.rectangle_dnd_color = new Color(120, 220, 140, 220);                // Bright lime
        theme.current_slice_circle_color = new Color(200, 200, 200, 200);         // Light gray
        theme.selection_back_color = new Color(0xFFD700);                         // Gold

        return theme;
    }

    /*public static ABBATheme createHalloweenTheme() {
        UIManager.put("Panel.background", new Color(60, 40, 20)); // Strong orange tint
        UIManager.put("Table.background", new Color(70, 45, 20));
        UIManager.put("TabbedPane.background", new Color(60, 40, 20));

        // BRIGHT orange accents
        UIManager.put("TabbedPane.selectedBackground", new Color(120, 70, 20)); // Very orange
        UIManager.put("Table.selectionBackground", new Color(255, 140, 0)); // Pure pumpkin orange!

        // VIBRANT purple accents for borders/highlights
        UIManager.put("Component.focusColor", new Color(186, 85, 211)); // Bright purple
        UIManager.put("Component.borderColor", new Color(153, 50, 204)); // Dark orchid purple
        UIManager.put("TabbedPane.selectedForeground", new Color(255, 140, 0)); // Orange text on selected tabs

        // Additional Halloween touches
        UIManager.put("Separator.foreground", new Color(255, 140, 0)); // Orange separators
        UIManager.put("ScrollBar.thumb", new Color(255, 140, 0, 180)); // Orange scrollbars
        UIManager.put("Button.background", new Color(80, 50, 20)); // Orange-tinted buttons
        UIManager.put("TextField.background", new Color(70, 50, 30)); // Orange-tinted fields

// Force update all existing components
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
        }

        // HALLOWEEN THEME - Spooky colors for dark background! 🎃
        ABBATheme theme = new ABBATheme();

        // Fonts (same as other themes)
        theme.slice_info_font = new Font(default_font, Font.BOLD, 16);
        theme.mouse_atlas_coordinates_font = new Font(default_font, Font.BOLD, 16);
        theme.action_summary_current_slice_font = new Font(default_font, Font.PLAIN, 16);
        theme.arrow_on_current_slice_font = new Font(default_font, Font.BOLD, 20);
        theme.action_font = new Font("TimesRoman", Font.BOLD, 10);

        // Strokes (same as other themes)
        theme.selection_stroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        theme.dashed_stroke_slice_handle_to_atlas = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        theme.task_counter_stroke = new BasicStroke(4);
        theme.current_slice_circle_stroke = new BasicStroke(5);
        theme.line_between_selected_slices_stroke = new BasicStroke(2);

        theme.title_prefix = "\uD83C\uDF83\uD83D\uDC7B\uD83E\uDD87  |  ";
        theme.title_suffix = "  |  \uD83C\uDF83\uD83D\uDC7B\uD83E\uDD87";

        // Halloween Colors! 🎃👻🦇
        theme.slice_info_color = new Color(255, 140, 0, 220);              // Pumpkin orange
        theme.done = new Color(255, 128, 0, 220);                            // Toxic slime green
        theme.locked = new Color(139, 0, 0, 220);                          // Blood red
        theme.pending = new Color(153, 50, 204, 220);                       // Jack-o'-lantern orange
        theme.unmirror_small_scale = new Color(153, 50, 204, 220);         // Dark purple
        theme.raster_small_scale = new Color(153, 50, 204, 220);           // Dark purple
        theme.invalid_action_string_color = new Color(255, 0, 0, 220);     // Scary red
        theme.delete_action_string_color = new Color(255, 255, 255, 220);  // Ghost white
        theme.raster_action_string_color = new Color(186, 85, 211, 220);   // Medium orchid purple
        theme.done_export = new Color(50, 205, 50, 220);                   // Lime green (slime)
        theme.register_small_scale = new Color(148, 0, 211, 220);          // Dark violet
        theme.stroke_color_left = new Color(0, 255, 0);                    // Toxic green
        theme.stroke_color_right = new Color(255, 0, 255);                 // Neon purple

        // Action text colors - on colored backgrounds
        Color text_action = new Color(0, 0, 0, 220);                       // Black text
        theme.text_action_register = text_action;
        theme.text_action_export_to_roimanager = text_action;
        theme.text_action_export_deformation_field_action = text_action;
        theme.text_action_export_atlas_to_image_plus = text_action;
        theme.text_action_export_slice_region_to_file = text_action;
        theme.text_action_export_slice_regions_to_qupath = text_action;
        theme.text_action_export_slice_to_image_plus = text_action;
        theme.text_action_unmirror = text_action;
        theme.text_action_raster_deformation = text_action;

        theme.color_cite_command_bg = new Color(25, 0, 51);                // Midnight purple
        theme.task_counter_color = new Color(255, 140, 0);                 // Pumpkin orange
        theme.mouse_atlas_coordinates_color = new Color(186, 85, 211, 180); // Purple haze
        theme.line_between_selected_slices_color = new Color(255, 20, 147, 220); // Deep pink/magenta
        theme.color_slice_handle_not_selected = new Color(255, 140, 0, 150);     // Faded pumpkin
        theme.color_slice_handle_selected = new Color(255, 106, 0, 220);           // Radioactive green
        theme.current_slice_handle_color = new Color(138, 43, 226, 200);         // Blue-violet
        theme.action_summary_current_slice_color = new Color(255, 215, 0, 200);  // Golden glow
        theme.rectangle_dnd_color = new Color(255, 69, 0, 180);                  // Orange-red
        theme.current_slice_circle_color = new Color(186, 85, 211, 200);         // Medium orchid
        theme.selection_back_color = new Color(255, 140, 0);                     // Pumpkin orange

        return theme;
    }

    public static ABBATheme createAprilFoolsTheme() {
        // APRIL FOOLS THEME - Comic Sans and Chaos! 🤡
        ABBATheme theme = new ABBATheme();

        // The WORST fonts! 🎨
        String terribleFont = "Comic Sans MS"; // Try Comic Sans first
        theme.slice_info_font = new Font(terribleFont, Font.BOLD, 16);
        theme.mouse_atlas_coordinates_font = new Font(terribleFont, Font.ITALIC, 16); // Italic for extra "style"
        theme.action_summary_current_slice_font = new Font(terribleFont, Font.PLAIN, 16);
        theme.arrow_on_current_slice_font = new Font(terribleFont, Font.BOLD, 20);
        theme.action_font = new Font(terribleFont, Font.BOLD, 10);

        // Strokes (same as other themes)
        theme.selection_stroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        theme.dashed_stroke_slice_handle_to_atlas = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        theme.task_counter_stroke = new BasicStroke(4);
        theme.current_slice_circle_stroke = new BasicStroke(5);
        theme.line_between_selected_slices_stroke = new BasicStroke(2);

        // CHAOTIC COLORS - Fixed seed for consistency, but totally random! 🌈
        Random rng = new Random(20240401); // April 1st seed

        theme.slice_info_color = randomBrightColor(rng);
        theme.done = randomBrightColor(rng);
        theme.locked = randomBrightColor(rng);
        theme.pending = randomBrightColor(rng);
        theme.unmirror_small_scale = randomBrightColor(rng);
        theme.raster_small_scale = randomBrightColor(rng);
        theme.invalid_action_string_color = randomBrightColor(rng);
        theme.delete_action_string_color = randomBrightColor(rng);
        theme.raster_action_string_color = randomBrightColor(rng);
        theme.done_export = randomBrightColor(rng);
        theme.register_small_scale = randomBrightColor(rng);
        theme.stroke_color_left = randomBrightColor(rng, 255); // Full opacity for strokes
        theme.stroke_color_right = randomBrightColor(rng, 255);

        // Action text - make it hard to read for extra chaos! 😈
        Color text_action = randomBrightColor(rng);
        theme.text_action_register = text_action;
        theme.text_action_export_to_roimanager = text_action;
        theme.text_action_export_deformation_field_action = text_action;
        theme.text_action_export_atlas_to_image_plus = text_action;
        theme.text_action_export_slice_region_to_file = text_action;
        theme.text_action_export_slice_regions_to_qupath = text_action;
        theme.text_action_export_slice_to_image_plus = text_action;
        theme.text_action_unmirror = text_action;
        theme.text_action_raster_deformation = text_action;

        theme.color_cite_command_bg = randomBrightColor(rng);
        theme.task_counter_color = randomBrightColor(rng, 255);
        theme.mouse_atlas_coordinates_color = randomBrightColor(rng);
        theme.line_between_selected_slices_color = randomBrightColor(rng);
        theme.color_slice_handle_not_selected = randomBrightColor(rng);
        theme.color_slice_handle_selected = randomBrightColor(rng);
        theme.current_slice_handle_color = randomBrightColor(rng);
        theme.action_summary_current_slice_color = randomBrightColor(rng);
        theme.rectangle_dnd_color = randomBrightColor(rng);
        theme.current_slice_circle_color = randomBrightColor(rng);
        theme.selection_back_color = randomBrightColor(rng, 255);

        theme.title_prefix = "\uD83E\uDD21  |  ";
        theme.title_suffix = "  |  \uD83E\uDD21";

        return theme;
    }

    private static Color randomBrightColor(Random rng) {
        return randomBrightColor(rng, 220); // Default alpha
    }

    private static Color randomBrightColor(Random rng, int alpha) {
        // Generate bright, saturated colors (100-255 range)
        return new Color(
                rng.nextInt(156) + 100,  // Red: 100-255
                rng.nextInt(156) + 100,  // Green: 100-255
                rng.nextInt(156) + 100,  // Blue: 100-255
                alpha
        );
    }*/

}
