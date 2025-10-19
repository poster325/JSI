import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import javax.swing.JPanel;

public class JSICanvas2D extends JPanel {
    
    private static final Color COLOR_PT_CURVE_DEFAULT = Color.BLACK;
    private static final Color COLOR_SELECTION_BOX = new Color(255, 0, 0, 64);
    private static final Color COLOR_SELECTED_PT_CURVE = Color.ORANGE;
    private static final Color COLOR_INFO = new Color(255, 0, 0, 128);
    private static final Color COLOR_CROSS_HAIR = new Color(255, 0, 0, 64);
    
    private static final Stroke STROKE_PT_CURVE_DEFAULT = new BasicStroke(5f);
    private static final Stroke STROKE_SELECTION_BOX = new BasicStroke(3f);
    private static final Stroke STROKE_CROSS_HAIR = new BasicStroke(5f);
    
    private static final Font FONT_INFO = 
            new Font("Monospaced", Font.PLAIN, 24);
    
    private static final float INFO_TOP_ALIGNMENT_X = 20f;
    private static final float INFO_TOP_ALIGNMENT_Y = 30f;
    private static final double CROSS_HAIR_RADIUS = 30.0;
    private static final double PEN_TIP_OFFSET = 30.0;
    
    public static final float STROKE_WIDTH_INCREMENT = 1f;
    private static final float STROKE_MIN_WIDTH = 1f;
    
    private JSI mJSI = null;
    private Color mCurColorForPtCurve = null;
    private Stroke mCurStrokeForPtCurve = null;
    
    public Color getCurColorForPtCurve() {
        return this.mCurColorForPtCurve;
    }
    
    public void setCurColorForPtCurve(Color c) {
        this.mCurColorForPtCurve = c;
    }
    
    public Stroke getCurStrokeForPtCurve() {
        return this.mCurStrokeForPtCurve;
    }
    
    public JSICanvas2D(JSI jsi) {
        this.mJSI = jsi;
        this.mCurStrokeForPtCurve = JSICanvas2D.STROKE_PT_CURVE_DEFAULT;
        this.mCurColorForPtCurve = JSICanvas2D.COLOR_PT_CURVE_DEFAULT;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING, 
                RenderingHints.VALUE_ANTIALIAS_ON);
        
        this.drawWorldSpaceElements(g2);
        this.drawScreenSpaceElements(g2);
    }
    
    private void drawWorldSpaceElements(Graphics2D g2) {
        g2.transform(this.mJSI.getXform().getCurXformFromWorldToScreen());
        this.drawPtCurves(g2);
        this.drawSelectedPtCurve(g2);
        this.drawCurPtCurve(g2);
        g2.transform(this.mJSI.getXform().getCurXformFromScreenToWorld());
    }
    
    private void drawScreenSpaceElements(Graphics2D g2) {
        this.drawSelectionBox(g2);
        this.drawCrossHair(g2);
        this.drawColorChooser(g2);
        this.drawPenTip(g2);
        this.drawInfo(g2);
    }

    private void drawPtCurves(Graphics2D g2) {
        for (JSIPtCurve ptCurve : this.mJSI.getPtCurves()) {
            this.drawPtCurve(
                    g2, 
                    ptCurve, 
                    ptCurve.getColor(), 
                    ptCurve.getStroke());
        }
    }

    private void drawCurPtCurve(Graphics2D g2) {
        JSIPtCurve ptCurve = this.mJSI.getCurPtCurve();
        if (ptCurve != null) {
            this.drawPtCurve(
                    g2, 
                    ptCurve, 
                    ptCurve.getColor(),
                    ptCurve.getStroke());
        }
    }
    
    private void drawPtCurve(
            Graphics2D g2, JSIPtCurve ptCurve, Color c, Stroke s) {
        ArrayList<Point2D.Double> pts = ptCurve.getPts();
        if (pts.size() < 2) {
            return;
        }
        
        Path2D.Double path = new Path2D.Double();
        Point2D.Double pt0 = pts.get(0);
        path.moveTo(pt0.x, pt0.y);
        
        for (int i = 1; i < pts.size(); i++) {
            Point2D.Double pt = pts.get(i);
            path.lineTo(pt.x, pt.y);
        }
        
        g2.setColor(c);
        g2.setStroke(s);
        g2.draw(path);
    }

    private void drawSelectionBox(Graphics2D g2) {
        if (this.mJSI.getSelectionBox() != null) {
            g2.setColor(JSICanvas2D.COLOR_SELECTION_BOX);
            g2.setStroke(JSICanvas2D.STROKE_SELECTION_BOX);
            g2.draw(this.mJSI.getSelectionBox());
        }
    }

    private void drawSelectedPtCurve(Graphics2D g2) {
        for (JSIPtCurve selectedPtCurve : this.mJSI.getSelectedPtCurves()) {
            this.drawPtCurve(
                    g2, 
                    selectedPtCurve, 
                    JSICanvas2D.COLOR_SELECTED_PT_CURVE, 
                    selectedPtCurve.getStroke());
        }
    }

    private void drawInfo(Graphics2D g2) {
        String modeText = String.valueOf(this.mJSI.getMode());
        g2.setColor(JSICanvas2D.COLOR_INFO);
        g2.setFont(JSICanvas2D.FONT_INFO);
        g2.drawString(
                modeText, 
                JSICanvas2D.INFO_TOP_ALIGNMENT_X, 
                JSICanvas2D.INFO_TOP_ALIGNMENT_Y);
    }

    private void drawCrossHair(Graphics2D g2) {
        if (this.mJSI.getMode() != JSI.Mode.ZOOM_ROTATE) {
            return;
        }
        
        double r = JSICanvas2D.CROSS_HAIR_RADIUS;
        Point ctr = JSIXform.PIVOT_PT;
        Line2D hline = new Line2D.Double(
                ctr.x - r, ctr.y, ctr.x + r, ctr.y);
        Line2D vline = new Line2D.Double(
                ctr.x, ctr.y - r, ctr.x, ctr.y + r);

        g2.setColor(COLOR_CROSS_HAIR);
        g2.setStroke(STROKE_CROSS_HAIR);
        g2.draw(hline);
        g2.draw(vline);
    }

    void increaseStrokeWidthForCurPtCurve(float delta) {
        BasicStroke bs = (BasicStroke) this.mCurStrokeForPtCurve;
        float newWidth = bs.getLineWidth() + delta;
        
        if (newWidth < STROKE_MIN_WIDTH) {
            newWidth = STROKE_MIN_WIDTH;
        }
        
        this.mCurStrokeForPtCurve = new BasicStroke(
                newWidth, 
                bs.getEndCap(),
                bs.getLineJoin());
    }

    private void drawPenTip(Graphics2D g2) {
        BasicStroke bs = (BasicStroke) this.mCurStrokeForPtCurve;
        double diameter = this.calcPenTipDiameter(bs.getLineWidth());
        double radius = diameter / 2.0;
        
        Point2D.Double center = new Point2D.Double(
                this.getWidth() - JSICanvas2D.PEN_TIP_OFFSET,
                JSICanvas2D.PEN_TIP_OFFSET);
        
        Ellipse2D.Double penTip = new Ellipse2D.Double(
                center.x - radius, 
                center.y - radius, 
                diameter, 
                diameter);
        
        g2.setColor(this.mCurColorForPtCurve);
        g2.fill(penTip);
    }
    
    private double calcPenTipDiameter(float strokeWidth) {
        Point2D.Double worldPt0 = new Point2D.Double(0.0, 0.0);
        Point2D.Double worldPt1 = new Point2D.Double(strokeWidth, 0.0);
        Point screenPt0 = 
                this.mJSI.getXform().calcPtFromWorldToScreen(worldPt0);
        Point screenPt1 = 
                this.mJSI.getXform().calcPtFromWorldToScreen(worldPt1);
        return screenPt0.distance(screenPt1);
    }

    private void drawColorChooser(Graphics2D g2) {
        if (this.mJSI.getMode() == JSI.Mode.COLOR) {
            this.mJSI.getColorChooser().drawCells(
                    g2, 
                    this.getWidth(), 
                    this.getHeight());
        }
    }
}
