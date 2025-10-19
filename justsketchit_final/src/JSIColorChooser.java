import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Rectangle2D;

public class JSIColorChooser {
    
    private static final int CELL_NUM_HUE = 40;
    private static final int CELL_NUM_BRIGHTNESS = 10;
    private static final float SATURATION_DEFAULT = 1f;
    private static final float OPAQUENESS_DEFAULT = 0.75f;
    private static final double VERTICAL_START_FRACTION = 1.0 / 3.0;
    private static final double VERTICAL_END_FRACTION = 2.0 / 3.0;
    
    private Color[][] mColors = null;
    private float mSaturation = Float.NaN;
    private float mOpaqueness = Float.NaN;
    
    public JSIColorChooser() {
        this.mColors = new Color[CELL_NUM_BRIGHTNESS][CELL_NUM_HUE];
        this.mSaturation = SATURATION_DEFAULT;
        this.mOpaqueness = OPAQUENESS_DEFAULT;
        this.createCellColors();
    }
    
    private void createCellColors() {
        float brightnessStep = 1f / (float) (CELL_NUM_BRIGHTNESS - 1);
        float hueStep = 1f / (float) (CELL_NUM_HUE - 1);
        
        for (int i = 0; i < CELL_NUM_BRIGHTNESS; i++) {
            float brightness = brightnessStep * (float) i;
            for (int j = 0; j < CELL_NUM_HUE; j++) {
                float hue = hueStep * (float) j;
                Color hsb = Color.getHSBColor(
                        hue, 
                        this.mSaturation, 
                        brightness);
                this.mColors[i][j] = new Color(
                        hsb.getRed(), 
                        hsb.getGreen(),
                        hsb.getBlue(), 
                        (int) (this.mOpaqueness * 255f));
            }
        }
    }
    
    public void drawCells(Graphics2D g2, int canvasWidth, int canvasHeight) {
        double yStart = canvasHeight * VERTICAL_START_FRACTION;
        double yEnd = canvasHeight * VERTICAL_END_FRACTION;
        double cellWidth = (double) canvasWidth / (double) CELL_NUM_HUE;
        double cellHeight = (yEnd - yStart) / (double) CELL_NUM_BRIGHTNESS;
        
        for (int i = 0; i < CELL_NUM_BRIGHTNESS; i++) {
            double y = yStart + cellHeight * (double) i;
            for (int j = 0; j < CELL_NUM_HUE; j++) {
                double x = cellWidth * (double) j;
                Rectangle2D rect = new Rectangle2D.Double(
                        x, y, cellWidth, cellHeight);
                g2.setColor(this.mColors[i][j]);
                g2.fill(rect);
            }
        }
    }

    public Color calcColor(Point pt, int canvasWidth, int canvasHeight) {
        double yStart = canvasHeight * VERTICAL_START_FRACTION;
        double yEnd = canvasHeight * VERTICAL_END_FRACTION;
        double cellWidth = (double) canvasWidth / (double) CELL_NUM_HUE;
        double cellHeight = (yEnd - yStart) / (double) CELL_NUM_BRIGHTNESS;
        
        int brightnessIndex = (int) (((double) pt.y - yStart) / cellHeight);
        int hueIndex = (int) ((double) pt.x / cellWidth);
        
        if (brightnessIndex < 0 || brightnessIndex >= CELL_NUM_BRIGHTNESS) {
            return null;
        }
        
        return this.mColors[brightnessIndex][hueIndex];
    }
}
