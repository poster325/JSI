import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

public class JSIXform {
    
    public static final Point PIVOT_PT = new Point(100, 100);
    public static final double MIN_START_ARM_LENGTH_FOR_SCALING = 100.0;
    
    private AffineTransform mCurXformFromWorldToScreen = null;
    private AffineTransform mCurXformFromScreenToWorld = null;
    private AffineTransform mStartXformFromWorldToScreen = null;
    private Point mStartScreenPt = null;
    
    public AffineTransform getCurXformFromWorldToScreen() {
        return this.mCurXformFromWorldToScreen;
    }
    
    public AffineTransform getCurXformFromScreenToWorld() {
        return this.mCurXformFromScreenToWorld;
    }
    
    public void setStartScreenPt(Point pt) {
        this.mStartScreenPt = pt;
        this.mStartXformFromWorldToScreen.setTransform(
                this.mCurXformFromWorldToScreen);
    }
    
    public JSIXform() {
        this.mCurXformFromWorldToScreen = new AffineTransform();
        this.mCurXformFromScreenToWorld = new AffineTransform();
        this.mStartXformFromWorldToScreen = new AffineTransform();
    }
    
    public void updateCurXformFromScreenToWorld() {
        try {
            this.mCurXformFromScreenToWorld =
                    this.mCurXformFromWorldToScreen.createInverse();
        } catch (NoninvertibleTransformException ex) {
            System.err.println(
                    "Failed to invert transformation: " + ex.getMessage());
        }
    }
    
    public Point calcPtFromWorldToScreen(Point2D.Double worldPt) {
        Point screenPt = new Point();
        this.mCurXformFromWorldToScreen.transform(worldPt, screenPt);
        return screenPt;
    }
    
    public Point2D.Double calcPtFromScreenToWorld(Point screenPt) {
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
        this.updateCurXformFromScreenToWorld();
        
        Point2D.Double worldPt0 = 
                this.calcPtFromScreenToWorld(this.mStartScreenPt);
        Point2D.Double worldPt1 = this.calcPtFromScreenToWorld(pt);
        double dx = worldPt1.x - worldPt0.x;
        double dy = worldPt1.y - worldPt0.y;
        
        this.mCurXformFromWorldToScreen.translate(dx, dy);
        this.updateCurXformFromScreenToWorld();
        
        return true;
    }

    public boolean rotateTo(Point pt) {
        if (this.mStartScreenPt == null) {
            return false;
        }
        
        this.mCurXformFromWorldToScreen.setTransform(
                this.mStartXformFromWorldToScreen);
        this.updateCurXformFromScreenToWorld();
        
        double startAngle = this.calcAngleFromPivot(this.mStartScreenPt);
        double endAngle = this.calcAngleFromPivot(pt);
        double deltaAngle = endAngle - startAngle;
        
        Point2D.Double worldPivotPt = 
                this.calcPtFromScreenToWorld(JSIXform.PIVOT_PT);
        
        this.mCurXformFromWorldToScreen.translate(
                worldPivotPt.x, worldPivotPt.y);
        this.mCurXformFromWorldToScreen.rotate(deltaAngle);
        this.mCurXformFromWorldToScreen.translate(
                -worldPivotPt.x, -worldPivotPt.y);
        
        this.updateCurXformFromScreenToWorld();
        
        return true;
    }
    
    private double calcAngleFromPivot(Point pt) {
        return StrictMath.atan2(
                pt.y - JSIXform.PIVOT_PT.y,
                pt.x - JSIXform.PIVOT_PT.x);
    }
    
    public boolean zoomRotateTo(Point pt) {
        if (this.mStartScreenPt == null) {
            return false;
        }
        
        this.mCurXformFromWorldToScreen.setTransform(
                this.mStartXformFromWorldToScreen);
        this.updateCurXformFromScreenToWorld();
        
        double startDistance = JSIXform.PIVOT_PT.distance(this.mStartScreenPt);
        if (startDistance < JSIXform.MIN_START_ARM_LENGTH_FOR_SCALING) {
            return false;
        }
        
        double endDistance = JSIXform.PIVOT_PT.distance(pt);
        double scaleFactor = endDistance / startDistance;
        
        double startAngle = this.calcAngleFromPivot(this.mStartScreenPt);
        double endAngle = this.calcAngleFromPivot(pt);
        double deltaAngle = endAngle - startAngle;
        
        Point2D.Double worldPivotPt = 
                this.calcPtFromScreenToWorld(JSIXform.PIVOT_PT);
        
        this.mCurXformFromWorldToScreen.translate(
                worldPivotPt.x, worldPivotPt.y);
        this.mCurXformFromWorldToScreen.rotate(deltaAngle);
        this.mCurXformFromWorldToScreen.scale(scaleFactor, scaleFactor);
        this.mCurXformFromWorldToScreen.translate(
                -worldPivotPt.x, -worldPivotPt.y);
        
        this.updateCurXformFromScreenToWorld();
        
        return true;
    }

    public boolean home() {
        this.mCurXformFromWorldToScreen.setToIdentity();
        this.updateCurXformFromScreenToWorld();
        return true;
    }
}
