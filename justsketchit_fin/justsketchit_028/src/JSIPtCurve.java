import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class JSIPtCurve {
    
    public static final double MIN_DIST_BTWN_PTS = 5.0;
    private static final float MIN_STROKE_WIDTH = 1f;
    
    private ArrayList<Point2D.Double> mPts = null;
    private Rectangle2D.Double mBoundingBox = null;
    private Color mColor = null;
    private Stroke mStroke = null;
    
    public ArrayList<Point2D.Double> getPts() {
        return this.mPts;
    }
    
    public Rectangle2D.Double getBoundingBox() {
        return this.mBoundingBox;
    }
    
    public Color getColor() {
        return this.mColor;
    }
    
    public void setColor(Color c) {
        this.mColor = new Color(
                c.getRed(), 
                c.getGreen(), 
                c.getBlue(), 
                c.getAlpha());
    }
    
    public Stroke getStroke() {
        return this.mStroke;
    }
    
    public void setStroke(Stroke s) {
        BasicStroke bs = (BasicStroke) s;
        this.mStroke = new BasicStroke(
                bs.getLineWidth(), 
                bs.getEndCap(),
                bs.getLineJoin());
    }
    
    public void adjustStrokeWidth(float delta) {
        BasicStroke bs = (BasicStroke) this.mStroke;
        float newWidth = bs.getLineWidth() + delta;
        
        if (newWidth < MIN_STROKE_WIDTH) {
            newWidth = MIN_STROKE_WIDTH;
        }
        
        this.mStroke = new BasicStroke(
                newWidth, 
                bs.getEndCap(), 
                bs.getLineJoin());
    }
    
    public JSIPtCurve(Point2D.Double pt, Color c, Stroke s) {
        this.mPts = new ArrayList<Point2D.Double>();
        this.mPts.add(pt);
        this.mBoundingBox = new Rectangle2D.Double(pt.x, pt.y, 0.0, 0.0);
        
        this.setColor(c);
        this.setStroke(s);
    }
    
    public void addPt(Point2D.Double pt) {
        this.mPts.add(pt);
        this.mBoundingBox.add(pt);
    }
}
