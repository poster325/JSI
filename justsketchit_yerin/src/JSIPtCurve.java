
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class JSIPtCurve {
    // constants
    public static final double MIN_DIST_BTWN_PTS = 5.0;
    
    // fields
    private ArrayList<Point2D.Double> mPts = null;
    public ArrayList<Point2D.Double> getPts () { //getter
        return this.mPts;
    }
    
    private Rectangle2D.Double mBoundingBox = null;
    public Rectangle2D.Double getBoundingBox() { 
        return this.mBoundingBox;
    }
    
    private Color mColor = null;
    public Color getColor(){
        return this.mColor;
    }
    
    private Stroke mStroke = null;
    public Stroke getStroke(){
        return this.mStroke;
    }
    
    // constructor
    public JSIPtCurve (Point2D.Double pt, Color c, Stroke s) {
        this.mPts = new ArrayList<Point2D.Double>();
        this.mPts.add(pt);
        this.mBoundingBox = new Rectangle2D.Double(pt.x, pt.y, 0.0, 0.0);
        
        this.mColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
        BasicStroke bs = (BasicStroke) s;
        this.mStroke = new BasicStroke(bs.getLineWidth(), bs.getEndCap(),
            bs.getLineJoin());
        
    }
    
    // methods
    public void addPt(Point2D.Double pt) {
        this.mPts.add(pt);
        this.mBoundingBox.add(pt);
    }

}
