
import java.awt.Point;
import java.awt.Rectangle;

public class JSISelectionBox extends Rectangle {
    // fields 
    private Point mAnchorPt = null;
    
    // constructor
    public JSISelectionBox(Point pt){
        super(pt);
        this.mAnchorPt = pt;
        
    }
    
    // update with a new point
    public void update(Point pt){
        this.setRect(this.mAnchorPt.x, this.mAnchorPt.y, 0, 0);
        this.add(pt);
    }
    
}
