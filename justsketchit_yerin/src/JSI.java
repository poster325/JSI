import java.awt.Color;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import javax.swing.JFrame;

public class JSI implements MouseListener, MouseMotionListener, KeyListener {
    
    public enum Mode { DRAW, SELECT, SELECTED, PAN, ZOOM_ROTATE, COLOR};
    private Mode mMode = JSI.Mode.DRAW;
    
    public Mode getMode() {
        return this.mMode;
    }
    
    // Fields
    private JFrame mFrame = null;
    private JSICanvas2D mCanvas2D = null;
    private JSIPtCurve mCurPtCurve = null;
    public JSIPtCurve getCurPtCurve () {
        return this.mCurPtCurve;
    }
    
    private ArrayList<JSIPtCurve> mPtCurves = null;
    public ArrayList<JSIPtCurve> getPtCurves () {
        return this.mPtCurves;
    }
    
    
    private ArrayList<JSIPtCurve> mSelectedPtCurves = null;
    public ArrayList<JSIPtCurve> getSelectedPtCurves() {
        return this.mSelectedPtCurves;
    }
            
    
    private JSISelectionBox mSelectionBox = null;
    public JSISelectionBox getSelectionBox() {
        return this.mSelectionBox;
    }
    
    private JSIXform mXform = null;
    public JSIXform getXform() {
        return this.mXform;
    }
    
    private JSIColorChooser mColorChooser = null;
    public JSIColorChooser getColorChooser () {
        return this.mColorChooser;
    }
    
    // Constructor
    public JSI () {
        this.mFrame = new JFrame("JustSketchIt");
        this.mFrame.setSize(800,600);
        this.mFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.mFrame.setVisible(true);
        
        
        this.mCanvas2D = new JSICanvas2D(this);
        this.mFrame.add(this.mCanvas2D);

        this.mFrame.setVisible(true);
        
        
        this.mCanvas2D.addMouseListener(this);
        this.mCanvas2D.addMouseMotionListener(this);
        this.mCanvas2D.setFocusable(true);
        this.mCanvas2D.addKeyListener(this);
        
        
        this.mPtCurves = new ArrayList<JSIPtCurve>();
        
        this.mSelectedPtCurves = new ArrayList<JSIPtCurve>();
        
        this.mXform = new JSIXform();
        
        this.mColorChooser = new JSIColorChooser();
        
    }
    
    public static void main(String[] args) {
        new JSI();
    }
    
    // Mouse Event Action helper functions
    
    
    // Key Event Action helper functions
    private void deleteSelectedCurve() {
        this.mSelectedPtCurves.clear();
        this.mMode = JSI.Mode.DRAW;
    }
    
    

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
          Point pt = e.getPoint();
          Point2D.Double worldPt = this.mXform.calcPtFromScreenToWorld(pt);
          switch (this.mMode) {
              case DRAW:
                  this.mCurPtCurve = new JSIPtCurve(worldPt,
                    this.mCanvas2D.getCurColorForPtCurve(),
                    this.mCanvas2D.getCurStrokeForPtCurve());
                  break;
              case SELECT:
                  this.mSelectionBox = new JSISelectionBox(pt);
                  break;
              case SELECTED:
                  this.mCurPtCurve = new JSIPtCurve(worldPt,
                    this.mCanvas2D.getCurColorForPtCurve(),
                    this.mCanvas2D.getCurStrokeForPtCurve());
                break;
              case PAN:
                this.mXform.setStartScreenPt(pt);
                break;
              case ZOOM_ROTATE:
                  this.mXform.setStartScreenPt(pt);
                  break;
              case COLOR:
                  
                  break;
          }
          
          this.mCanvas2D.repaint();
        
    }
    @Override
    public void mouseReleased(MouseEvent e) {
          switch (this.mMode) {
              case DRAW:
                  if(this.mCurPtCurve != null && this.mCurPtCurve.getPts().size() >= 2) {
                    this.mPtCurves.add(this.mCurPtCurve); 
                  }
                  this.mCurPtCurve = null;
                  break;
              case SELECT:
                  this.mSelectionBox = null;
                  break;
              case SELECTED:
                  this.mMode = JSI.Mode.DRAW;
                  
                  this.mPtCurves.addAll(this.mSelectedPtCurves);
                  this.mSelectedPtCurves.clear();
                  
                  this.mCurPtCurve = null;
                  break;
              case PAN:
                  this.mXform.setStartScreenPt(null);
                  break;
              case ZOOM_ROTATE:
                  this.mXform.setStartScreenPt(null);
                  break;
              case COLOR:
                  Point pt = e.getPoint();
                  Color c = this.mColorChooser.calcColor(pt,
                          this.mCanvas2D.getWidth(), this.mCanvas2D.getHeight());
                  if (c!=null){
                      this.mCanvas2D.setCurColorForPtCurve(c);
                  }
                  break;
          }
          this.mCanvas2D.repaint();
    }
    

    @Override
    public void mouseEntered(MouseEvent e) {
//        System.out.println("mouseEntered");
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
//        System.out.println("mouseExited");
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point pt = e.getPoint();
        switch (this.mMode) {
            case DRAW:
                if (this.mCurPtCurve != null){
                    int size = this.mCurPtCurve.getPts().size();
                    Point2D.Double lastWorldPt = this.mCurPtCurve.getPts().get(size-1);
                    Point lastScreenPt = this.mXform.calcPtFromWorldToScreen(lastWorldPt);
                    if(pt.distance(lastScreenPt) < JSIPtCurve.MIN_DIST_BTWN_PTS){
                        return;
                    }
                    Point2D.Double worldPt = this.mXform.calcPtFromScreenToWorld(pt);
                    this.mCurPtCurve.addPt(worldPt);
                }
                break;
            case SELECT:
                if(this.mSelectionBox != null){
                   this.mSelectionBox.update(pt);
                   this.updateSelectedPtCurves();
                }
                break;
            case SELECTED:
                break;
            case PAN:
                this.mXform.translateTo(pt);
                break;
            case ZOOM_ROTATE:
                this.mXform.zoomRotateTo(pt);
                break;
            case COLOR:
                
                break;
        }
        this.mCanvas2D.repaint();

    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }
   
    
    

    @Override
    public void keyTyped(KeyEvent e) {
        
    }

    public void keyToMode(KeyEvent e){
        int code = e.getKeyCode();
        switch (code) {
            case KeyEvent.VK_SHIFT:
                this.mMode = JSI.Mode.SELECT;
                break;
            case KeyEvent.VK_CONTROL:
                this.mMode = JSI.Mode.PAN;
                break;
            case KeyEvent.VK_ALT:
                this.mMode = JSI.Mode.ZOOM_ROTATE;
                break;
            case KeyEvent.VK_C:
                this.mMode = JSI.Mode.COLOR;
                break;
            case KeyEvent.VK_UP:
                this.mCanvas2D.increaseStrokeWidthForCurPtCurve(
                        JSICanvas2D.STROKE_WIDTH_INCREMENT);
                break;
            case KeyEvent.VK_DOWN:
                this.mCanvas2D.increaseStrokeWidthForCurPtCurve(
                        -JSICanvas2D.STROKE_WIDTH_INCREMENT);
                break;
        }
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        switch (this.mMode) {
            case DRAW:
                this.keyToMode(e);
                break;
            case SELECT:
                
                break;
            case SELECTED:
                this.keyToMode(e);
                break;
            case PAN:
                break;
            case ZOOM_ROTATE:
                break;
            case COLOR:
                break;
        }
        
        this.mCanvas2D.repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        switch (this.mMode) {
            case DRAW:
                switch (code) {
                    case KeyEvent.VK_H:
                        this.mXform.home();
                        break;
                }
                break;
            case SELECT:
                switch (code) {
                    case KeyEvent.VK_SHIFT:
                        if (this.mSelectedPtCurves.isEmpty()) {
                            this.mMode = JSI.Mode.DRAW;
                        } else {
                            this.mMode = JSI.Mode.SELECTED;
                        }
                        
                        this.mSelectionBox = null;
                        break;
                }
                break;
            case SELECTED:
                switch (code){
                    case KeyEvent.VK_ESCAPE:
                        this.mPtCurves.addAll(this.mSelectedPtCurves); 
                        this.mSelectedPtCurves.clear(); 
                        this.mMode = JSI.Mode.DRAW;
                        break;
                    case KeyEvent.VK_DELETE:
                        this.deleteSelectedCurve();
                        break;
                    case KeyEvent.VK_H:
                        this.mXform.home();
                        break;
                }
                break;
            case PAN:
                switch (code) {
                    case KeyEvent.VK_CONTROL:
                        if (this.mSelectedPtCurves.isEmpty()){
                            this.mMode = JSI.Mode.DRAW;
                        } else{
                            this.mMode = JSI.Mode.SELECTED;
                        }
                        break;
                }
                break;
            case ZOOM_ROTATE:
                switch (code) {
                    case KeyEvent.VK_ALT:
                        if (this.mSelectedPtCurves.isEmpty()){
                            this.mMode = JSI.Mode.DRAW;
                        } else {
                            this.mMode = JSI.Mode.SELECTED;
                        }
                        break;
                }
                break;
            case COLOR:
                switch(code) {
                    case KeyEvent.VK_C:
                        if (this.mSelectedPtCurves.isEmpty()){
                            this.mMode = JSI.Mode.DRAW;
                        } else {
                            this.mMode = JSI.Mode.SELECTED;
                        }
                        break;
                }
                break;
        }
        
        switch (code) {
            case KeyEvent.VK_Z:
                if(this.mPtCurves != null){
                    this.mPtCurves.remove(this.mPtCurves.size()-1);
                    this.mCanvas2D.repaint();
                    
                } else{
                    System.out.println("no previous curve");
                }
                break;
                
        }
        this.mCanvas2D.repaint();
    }
    
    private void updateSelectedPtCurves(){
        AffineTransform at = this.mXform.getCurXformFromScreenToWorld();
        Shape worldSelectionBoxShape = at.createTransformedShape(this.mSelectionBox);
        
        ArrayList<JSIPtCurve> newlySelectedPtCurves = 
                new ArrayList<JSIPtCurve>();
        
        for(JSIPtCurve ptCurve : this.mPtCurves) {
            if (worldSelectionBoxShape.intersects(ptCurve.getBoundingBox()) || 
                    ptCurve.getBoundingBox().isEmpty() ) {
                for (Point2D.Double pt: ptCurve.getPts()) {
                    if (worldSelectionBoxShape.contains(pt)) {
                        newlySelectedPtCurves.add(ptCurve);
                        break;
                    }
                }    
            }
        }
        this.mPtCurves.removeAll(newlySelectedPtCurves);
        this.mSelectedPtCurves.addAll(newlySelectedPtCurves);
        
    
    }
    
    
    
}
