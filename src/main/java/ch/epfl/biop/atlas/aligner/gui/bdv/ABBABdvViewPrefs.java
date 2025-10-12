package ch.epfl.biop.atlas.aligner.gui.bdv;

import java.awt.*;

public class ABBABdvViewPrefs {
   public static Font slice_info_font;// = new Font("TimesRoman", Font.BOLD, 16);
   public static Font action_font;
   public static Color slice_info_color;// = new Color(32, 125, 49, 220); // When slice info is shown
   public static Color done; // = new Color(0, 255, 0, 200);
   public static Color done_export;
   public static Color locked; // = new Color(255, 0, 0, 200);
   public static Color pending; // = new Color(255, 255, 0, 200);
   public static Color unmirror_small_scale = new Color(128, 128, 128, 200);
   public static Color raster_small_scale = new Color(128, 128, 128, 200);
   public static Color invalid_action_string_color = new Color(205, 1, 106, 199);
   public static Color delete_action_string_color = new Color(255, 255, 255, 200);
   public static Color raster_action_string_color = new Color(255, 255, 255, 200);
   public static Color register_small_scale = new Color(128, 128, 128, 200);
   public static Color stroke_color_left = new Color(0,255,0);
   public static Color stroke_color_right = new Color(255,0,255);
   public static Color text_action_raster_deformation;
   public static Color text_action_register = new Color(255, 255, 255, 200);
   public static Color text_action_unmirror;
   public static Color text_action_export_to_roimanager = new Color(255, 255, 255, 200);
   public static Color text_action_export_deformation_field_action = new Color(255, 255, 255, 200); // text_action_export_deformation_field_action
   public static Color text_action_export_atlas_to_image_plus = new Color(255, 255, 255, 200);
   public static Color text_action_export_slice_region_to_file = new Color(255, 255, 255, 200);
   public static Color text_action_export_slice_regions_to_qupath = new Color(255, 255, 255, 200);
   public static Color text_action_export_slice_to_image_plus = new Color(255, 255, 255, 200);
   public static Color color_cite_command_bg = new Color(245, 245, 245);
   public static Color task_counter_color = new Color(128,112,50,200);
   public static Stroke task_counter_stroke = new BasicStroke(4);
   public static Color mouse_atlas_coordinates_color = new Color(255, 255, 255, 67);
   public static Font mouse_atlas_coordinates_font = new Font("TimesRoman", Font.BOLD, 16);
   public static Color line_between_selected_slices_color = new Color(255, 0, 255, 200);
   public static Color color_slice_handle_not_selected = new Color(255, 255, 0, 64);
   public static Color color_slice_handle_selected = new Color(0, 255, 0, 180);
   public static Stroke dashed_stroke_slice_handle_to_atlas = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
   public static Color current_slice_handle_color = new Color(255,255,255,128);
   public static Font action_summary_current_slice_font = new Font("TimesRoman", Font.PLAIN, 16);
   public static Font arrow_on_current_slice_font = new Font("TimesRoman", Font.BOLD, 18);
   public static Color action_summary_current_slice_color = new Color(255,255,255,128);
   public static Color rectangle_dnd_color = new Color(120,250,50,128);
   public static Color current_slice_circle_color = new Color(255, 255, 255, 128);
   public static Stroke current_slice_circle_stroke = new BasicStroke(5);

   public static Color selection_back_color = new Color(0xF7BF18);
   public static Stroke selection_stroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0); //new BasicStroke(2);
   public static Stroke line_between_selected_slices_stroke;

   public static String title_suffix = "", title_prefix = "";
}
