import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

public class JSIXform {
    // constants
    public static final Point PIVOT_PT = new Point(100,100);
    public static final double MIN_START_ARM_LENGTH_FOR_SCALING = 100.0;
    
    // fields
    private AffineTransform mCurXformFromWorldToScreen = null;
    public AffineTransform getCurXformFromWorldToScreen () {
        return this.mCurXformFromWorldToScreen;
    }
    private AffineTransform mCurXformFromScreenToWorld = null;
    public AffineTransform getCurXformFromScreenToWorld () {
        return this.mCurXformFromScreenToWorld;
    }
    private AffineTransform mStartXformFromWorldToScreen = null;
    private Point mStartScreenPt = null;
    public void setStartScreenPt(Point pt) {
        this.mStartScreenPt = pt;
        this.mStartXformFromWorldToScreen.setTransform(
                this.mCurXformFromWorldToScreen);
        
    }
    
    // consturctor
    public JSIXform() {
        this.mCurXformFromWorldToScreen = new AffineTransform();
        this.mCurXformFromScreenToWorld = new AffineTransform();
        this.mStartXformFromWorldToScreen = new AffineTransform();
    }
    
    // call wheneer mCurXformFromWorldToScreen changes to have 
    // its corresponding mCurXformFromScreenToWorld
    public void updateCurXformFromScreenToWorld() {
        try {
            this. mCurXformFromScreenToWorld =
                    this.mCurXformFromWorldToScreen.createInverse();
        } catch (NoninvertibleTransformException ex) {
            System.out.println("NoninvertibleTransformException");
        }
    }
    
    public Point calcPtFromWorldToScreen (Point2D.Double worldPt){
        Point screenPt = new Point();
        this.mCurXformFromWorldToScreen.transform(worldPt, screenPt);
        return screenPt;
    }
    
    public Point2D.Double calcPtFromScreenToWorld (Point screenPt) {
        Point2D.Double worldPt = new Point2D.Double();
        this.mCurXformFromScreenToWorld.transform(screenPt, worldPt);
        return worldPt;
    }

    

    public boolean translateTo(Point pt) {
        if (this.mStartScreenPt == null) {
            return false;
        }
        
        this.mCurXformFromWorldToScreen.setTransform(
                this.mStartXformFromWorldToScreen);
        // call whenever mCurXformFromWorldToScreen changes
        this.updateCurXformFromScreenToWorld();
        
        
        Point2D.Double worldPt0 = this.calcPtFromScreenToWorld(this.mStartScreenPt);
        Point2D.Double worldPt1 = this.calcPtFromScreenToWorld(pt);
        double dx = worldPt1.x - worldPt0.x;
        double dy = worldPt1.y- worldPt0.y;
        
        this.mCurXformFromWorldToScreen.translate(dx, dy);
        // call whenever mCurXformFromWorldToScreen changes
        this.updateCurXformFromScreenToWorld();
        
        return true;
    }

    public boolean rotateTo(Point pt) {
        if (this.mStartScreenPt == null) {
            return false;
        }
        
        this.mCurXformFromWorldToScreen.setTransform(
                this.mStartXformFromWorldToScreen);
        // call whenever mCurXformFromWorldToScreen changes
        this.updateCurXformFromScreenToWorld();
        
        double ang0 = StrictMath.atan2(
            this.mStartScreenPt.y - JSIXform.PIVOT_PT.y,
            this.mStartScreenPt.x - JSIXform.PIVOT_PT.x);
        double ang1 = StrictMath.atan2(
            pt.y - JSIXform.PIVOT_PT.y, pt.x - JSIXform.PIVOT_PT.x);
        double ang = ang1 - ang0;
        
        Point2D.Double worldPivotPt = this.calcPtFromScreenToWorld(JSIXform.PIVOT_PT);
        this.mCurXformFromWorldToScreen.translate(worldPivotPt.x, worldPivotPt.x);
        this.mCurXformFromWorldToScreen.rotate(ang);
        this.mCurXformFromWorldToScreen.translate(-worldPivotPt.x, -worldPivotPt.y);
        
        // call whenever mCurXformFromWorldToScreen changes
        this.updateCurXformFromScreenToWorld();
        
        return true;
    }
    
    public boolean zoomRotateTo(Point pt) {
        if (this.mStartScreenPt == null) {
            return false;
        }
        
        this.mCurXformFromWorldToScreen.setTransform(
                this.mStartXformFromWorldToScreen);
        // call whenever mCurXformFromWorldToScreen changes
        this.updateCurXformFromScreenToWorld();
        
        double d0 = JSIXform.PIVOT_PT.distance(this.mStartScreenPt);
        if(d0 < JSIXform.MIN_START_ARM_LENGTH_FOR_SCALING){
            return false;
        }
        double d1 = JSIXform.PIVOT_PT.distance(pt);
        double s = d1 / d0;
        
        double ang0 = StrictMath.atan2(
            this.mStartScreenPt.y - JSIXform.PIVOT_PT.y,
            this.mStartScreenPt.x - JSIXform.PIVOT_PT.x);
        double ang1 = StrictMath.atan2(
            pt.y - JSIXform.PIVOT_PT.y, pt.x - JSIXform.PIVOT_PT.x);
        double ang = ang1 - ang0;
        
        Point2D.Double worldPivotPt = this.calcPtFromScreenToWorld(JSIXform.PIVOT_PT);
        this.mCurXformFromWorldToScreen.translate(worldPivotPt.x, worldPivotPt.y);
        this.mCurXformFromWorldToScreen.rotate(ang);
        this.mCurXformFromWorldToScreen.scale(s, s);
        this.mCurXformFromWorldToScreen.translate(-worldPivotPt.x, -worldPivotPt.y);
        
        // call whenever mCurXformFromWorldToScreen changes
        this.updateCurXformFromScreenToWorld();
        
        return true;
    }

    public boolean home() {
        this.mCurXformFromWorldToScreen.setToIdentity();
        this.updateCurXformFromScreenToWorld();
        
        return true;
    }
    
}
