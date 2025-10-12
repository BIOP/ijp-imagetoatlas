package ch.epfl.biop.atlas.aligner.gui.bdv;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

    public static ABBATheme createLightTheme() {
        // WHITE BACKGROUND - use DARKER colors for contrast
        ABBATheme theme = new ABBATheme();

        // Fonts (same for both themes)
        theme.slice_info_font = new Font("TimesRoman", Font.BOLD, 16);
        theme.mouse_atlas_coordinates_font = new Font("TimesRoman", Font.BOLD, 16);
        theme.action_summary_current_slice_font = new Font("TimesRoman", Font.PLAIN, 16);
        theme.arrow_on_current_slice_font = new Font("TimesRoman", Font.BOLD, 18);
        theme.action_font = new Font("TimesRoman", Font.BOLD, 10);

        // Strokes (same for both themes)
        theme.selection_stroke = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
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
        theme.slice_info_font = new Font("TimesRoman", Font.BOLD, 16);
        theme.mouse_atlas_coordinates_font = new Font("TimesRoman", Font.BOLD, 16);
        theme.action_summary_current_slice_font = new Font("TimesRoman", Font.PLAIN, 16);
        theme.arrow_on_current_slice_font = new Font("TimesRoman", Font.BOLD, 20);
        theme.action_font = new Font("TimesRoman", Font.BOLD, 10);

        // Strokes (same as light theme)
        theme.selection_stroke = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        theme.dashed_stroke_slice_handle_to_atlas = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        theme.task_counter_stroke = new BasicStroke(4);
        theme.current_slice_circle_stroke = new BasicStroke(5);

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
        theme.color_slice_handle_not_selected = new Color(255, 255, 100, 150);    // Bright yellow
        theme.color_slice_handle_selected = new Color(100, 255, 100, 220);        // Lime green
        theme.current_slice_handle_color = new Color(200, 200, 200, 200);         // Light gray
        theme.action_summary_current_slice_color = new Color(220, 220, 220, 200); // Light gray
        theme.rectangle_dnd_color = new Color(150, 255, 100, 180);                // Bright lime
        theme.current_slice_circle_color = new Color(200, 200, 200, 200);         // Light gray
        theme.selection_back_color = new Color(0xFFD700);                         // Gold

        return theme;
    }

}
