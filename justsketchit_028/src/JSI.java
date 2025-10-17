import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class JSI implements MouseListener, MouseMotionListener, 
        KeyListener, WindowFocusListener {
    
    public enum Mode {
        DRAW, SELECT, SELECTED, PAN, ZOOM_ROTATE, COLOR
    }
    
    private static final int MAX_HISTORY_SIZE = 5;
    private static final int DEFAULT_WINDOW_WIDTH = 800;
    private static final int DEFAULT_WINDOW_HEIGHT = 600;
    
    private Mode mMode = JSI.Mode.DRAW;
    private JFrame mFrame = null;
    private JSICanvas2D mCanvas2D = null;
    private JSIPtCurve mCurPtCurve = null;
    private ArrayList<JSIPtCurve> mPtCurves = null;
    private ArrayList<JSIPtCurve> mSelectedPtCurves = null;
    private JSISelectionBox mSelectionBox = null;
    private JSIXform mXform = null;
    private JSIColorChooser mColorChooser = null;
    
    private ArrayList<ArrayList<JSIPtCurve>> mHistory;
    private int mHistoryIndex;
    private int mHistorySize;
    
    public Mode getMode() {
        return this.mMode;
    }
    
    public JSIPtCurve getCurPtCurve() {
        return this.mCurPtCurve;
    }
    
    public ArrayList<JSIPtCurve> getPtCurves() {
        return this.mPtCurves;
    }
    
    public ArrayList<JSIPtCurve> getSelectedPtCurves() {
        return this.mSelectedPtCurves;
    }
    
    public JSISelectionBox getSelectionBox() {
        return this.mSelectionBox;
    }
    
    public JSIXform getXform() {
        return this.mXform;
    }
    
    public JSIColorChooser getColorChooser() {
        return this.mColorChooser;
    }
    
    public JSI() {
        this.initializeFrame();
        this.initializeCanvas();
        this.initializeDataStructures();
    }
    
    private void initializeFrame() {
        this.mFrame = new JFrame("JustSketchIt");
        this.mFrame.setSize(DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);
        this.mFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.mFrame.addWindowFocusListener(this);
        this.mFrame.setLocationRelativeTo(null);
        this.mFrame.setVisible(true);
    }
    
    private void initializeCanvas() {
        this.mCanvas2D = new JSICanvas2D(this);
        this.mFrame.add(this.mCanvas2D);
        this.mCanvas2D.addMouseListener(this);
        this.mCanvas2D.addMouseMotionListener(this);
        this.mCanvas2D.setFocusable(true);
        this.mCanvas2D.addKeyListener(this);
        this.mCanvas2D.requestFocusInWindow();
    }
    
    private void initializeDataStructures() {
        this.mPtCurves = new ArrayList<JSIPtCurve>();
        this.mSelectedPtCurves = new ArrayList<JSIPtCurve>();
        this.mXform = new JSIXform();
        this.mColorChooser = new JSIColorChooser();
        
        this.mHistory = 
                new ArrayList<ArrayList<JSIPtCurve>>(MAX_HISTORY_SIZE);
        this.mHistoryIndex = -1;
        this.mHistorySize = 0;
        this.saveToHistory();
    }
    
    public static void main(String[] args) {
        new JSI();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        this.mCanvas2D.requestFocusInWindow();
        
        Point pt = e.getPoint();
        switch (this.mMode) {
            case DRAW:
                this.handleDrawModePress(pt);
                break;
            case SELECT:
                this.mSelectionBox = new JSISelectionBox(pt);
                break;
            case SELECTED:
                this.handleSelectedModePress(e, pt);
                break;
            case PAN:
            case ZOOM_ROTATE:
                this.mXform.setStartScreenPt(pt);
                break;
            case COLOR:
                break;
        }
        
        this.mCanvas2D.repaint();
    }
    
    private void handleDrawModePress(Point screenPt) {
        Point2D.Double worldPt = 
                this.mXform.calcPtFromScreenToWorld(screenPt);
        this.mCurPtCurve = new JSIPtCurve(
                worldPt,
                this.mCanvas2D.getCurColorForPtCurve(),
                this.mCanvas2D.getCurStrokeForPtCurve());
    }
    
    private void handleSelectedModePress(MouseEvent e, Point pt) {
        boolean isShiftPressed = 
                (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;
        
        if (isShiftPressed) {
            this.mMode = JSI.Mode.SELECT;
            this.mSelectionBox = new JSISelectionBox(pt);
        } else {
            this.deselectAllCurves();
            this.mMode = JSI.Mode.DRAW;
        }
    }
    
    private void deselectAllCurves() {
        this.mPtCurves.addAll(this.mSelectedPtCurves);
        this.mSelectedPtCurves.clear();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        switch (this.mMode) {
            case DRAW:
                this.handleDrawModeRelease();
                break;
            case SELECT:
            case SELECTED:
                this.mSelectionBox = null;
                break;
            case PAN:
            case ZOOM_ROTATE:
                this.mXform.setStartScreenPt(null);
                break;
            case COLOR:
                this.handleColorModeRelease(e.getPoint());
                break;
        }
        this.mCanvas2D.repaint();
    }
    
    private void handleDrawModeRelease() {
        if (this.mCurPtCurve != null && 
                this.mCurPtCurve.getPts().size() >= 2) {
            this.mPtCurves.add(this.mCurPtCurve);
            this.saveToHistory();
        } else {
            this.deselectAllCurves();
            this.saveToHistory();
        }
        this.mCurPtCurve = null;
    }
    
    private void handleColorModeRelease(Point pt) {
        Color c = this.mColorChooser.calcColor(
                pt, 
                this.mCanvas2D.getWidth(), 
                this.mCanvas2D.getHeight());
        
        if (c != null) {
            if (!this.mSelectedPtCurves.isEmpty()) {
                this.setColorForSelectedCurves(c);
            } else {
                this.mCanvas2D.setCurColorForPtCurve(c);
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point pt = e.getPoint();
        switch (this.mMode) {
            case DRAW:
                this.handleDrawModeDrag(pt);
                break;
            case SELECT:
            case SELECTED:
                if (this.mSelectionBox != null) {
                    this.mSelectionBox.update(pt);
                    this.updateSelectedPtCurves();
                }
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
    
    private void handleDrawModeDrag(Point screenPt) {
        if (this.mCurPtCurve == null) {
            return;
        }
        
        int size = this.mCurPtCurve.getPts().size();
        Point2D.Double lastWorldPt = 
                this.mCurPtCurve.getPts().get(size - 1);
        Point lastScreenPt = 
                this.mXform.calcPtFromWorldToScreen(lastWorldPt);
        
        if (screenPt.distance(lastScreenPt) < 
                JSIPtCurve.MIN_DIST_BTWN_PTS) {
            return;
        }
        
        Point2D.Double worldPt = 
                this.mXform.calcPtFromScreenToWorld(screenPt);
        this.mCurPtCurve.addPt(worldPt);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
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
            case KeyEvent.VK_Z:
                if (this.isCtrlPressed(e)) {
                    this.undo();
                }
                break;
            case KeyEvent.VK_Y:
                if (this.isCtrlPressed(e)) {
                    this.redo();
                }
                break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_CLOSE_BRACKET:
                this.increaseStrokeWidth();
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_OPEN_BRACKET:
                this.decreaseStrokeWidth();
                break;
        }
        this.mCanvas2D.repaint();
    }
    
    private boolean isCtrlPressed(KeyEvent e) {
        return (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0;
    }
    
    private void increaseStrokeWidth() {
        if (this.mMode == JSI.Mode.SELECTED) {
            this.adjustStrokeWidthForSelectedCurves(
                    JSICanvas2D.STROKE_WIDTH_INCREMENT);
        } else {
            this.mCanvas2D.increaseStrokeWidthForCurPtCurve(
                    JSICanvas2D.STROKE_WIDTH_INCREMENT);
        }
    }
    
    private void decreaseStrokeWidth() {
        if (this.mMode == JSI.Mode.SELECTED) {
            this.adjustStrokeWidthForSelectedCurves(
                    -JSICanvas2D.STROKE_WIDTH_INCREMENT);
        } else {
            this.mCanvas2D.increaseStrokeWidthForCurPtCurve(
                    -JSICanvas2D.STROKE_WIDTH_INCREMENT);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        
        switch (code) {
            case KeyEvent.VK_SHIFT:
                this.mSelectionBox = null;
                this.switchModeBasedOnSelection();
                break;
            case KeyEvent.VK_CONTROL:
            case KeyEvent.VK_ALT:
            case KeyEvent.VK_C:
                this.switchModeBasedOnSelection();
                break;
            case KeyEvent.VK_ESCAPE:
                this.deselectAllCurves();
                this.saveToHistory();
                this.mMode = JSI.Mode.DRAW;
                break;
            case KeyEvent.VK_DELETE:
                this.deleteSelectedCurves();
                break;
            case KeyEvent.VK_H:
                this.mXform.home();
                break;
        }
        this.mCanvas2D.repaint();
    }
    
    private void switchModeBasedOnSelection() {
        if (!this.mSelectedPtCurves.isEmpty()) {
            this.mMode = JSI.Mode.SELECTED;
        } else {
            this.mMode = JSI.Mode.DRAW;
        }
    }
    
    private void deleteSelectedCurves() {
        if (!this.mSelectedPtCurves.isEmpty()) {
            this.mSelectedPtCurves.clear();
            this.saveToHistory();
            this.mMode = JSI.Mode.DRAW;
        }
    }
    
    private void updateSelectedPtCurves() {
        AffineTransform at = this.mXform.getCurXformFromScreenToWorld();
        Shape worldSelectionBoxShape = 
                at.createTransformedShape(this.mSelectionBox);
        
        ArrayList<JSIPtCurve> newlySelectedPtCurves = 
                new ArrayList<JSIPtCurve>();
        
        for (JSIPtCurve ptCurve : this.mPtCurves) {
            if (this.isCurveInSelection(ptCurve, worldSelectionBoxShape)) {
                newlySelectedPtCurves.add(ptCurve);
            }
        }
        
        this.mPtCurves.removeAll(newlySelectedPtCurves);
        this.mSelectedPtCurves.addAll(newlySelectedPtCurves);
    }
    
    private boolean isCurveInSelection(
            JSIPtCurve ptCurve, 
            Shape selectionShape) {
        if (!selectionShape.intersects(ptCurve.getBoundingBox()) && 
                !ptCurve.getBoundingBox().isEmpty()) {
            return false;
        }
        
        for (Point2D.Double pt : ptCurve.getPts()) {
            if (selectionShape.contains(pt)) {
                return true;
            }
        }
        return false;
    }
    
    private void adjustStrokeWidthForSelectedCurves(float delta) {
        for (JSIPtCurve curve : this.mSelectedPtCurves) {
            curve.adjustStrokeWidth(delta);
        }
        this.saveToHistory();
        this.forceCanvasRepaint();
    }
    
    private void setColorForSelectedCurves(Color color) {
        for (JSIPtCurve curve : this.mSelectedPtCurves) {
            curve.setColor(color);
        }
        this.saveToHistory();
        this.forceCanvasRepaint();
    }
    
    private void forceCanvasRepaint() {
        SwingUtilities.invokeLater(() -> {
            this.mCanvas2D.repaint();
            this.mCanvas2D.invalidate();
            this.mCanvas2D.revalidate();
        });
    }
    
    private void saveToHistory() {
        this.clearFutureHistory();
        
        ArrayList<JSIPtCurve> currentState = 
                this.deepCopyPtCurves(this.mPtCurves);
        
        if (this.mHistorySize < MAX_HISTORY_SIZE) {
            this.mHistory.add(currentState);
            this.mHistorySize++;
        } else {
            this.mHistory.remove(0);
            this.mHistory.add(currentState);
        }
        
        this.mHistoryIndex = this.mHistorySize - 1;
    }
    
    private void clearFutureHistory() {
        while (this.mHistorySize > this.mHistoryIndex + 1) {
            this.mHistory.remove(this.mHistorySize - 1);
            this.mHistorySize--;
        }
    }
    
    private ArrayList<JSIPtCurve> deepCopyPtCurves(ArrayList<JSIPtCurve> original) {
        ArrayList<JSIPtCurve> copy = new ArrayList<JSIPtCurve>();
        for (JSIPtCurve curve : original) {
            copy.add(this.deepCopyPtCurve(curve));
        }
        return copy;
    }
    
    private JSIPtCurve deepCopyPtCurve(JSIPtCurve original) {
        ArrayList<Point2D.Double> originalPts = original.getPts();
        if (originalPts.isEmpty()) {
            return null;
        }
        
        Point2D.Double firstPt = new Point2D.Double(
                originalPts.get(0).x, 
                originalPts.get(0).y);
        JSIPtCurve copy = new JSIPtCurve(
                firstPt, 
                original.getColor(), 
                original.getStroke());
        
        for (int i = 1; i < originalPts.size(); i++) {
            Point2D.Double pt = originalPts.get(i);
            copy.addPt(new Point2D.Double(pt.x, pt.y));
        }
        
        return copy;
    }
    
    private void undo() {
        if (this.mHistoryIndex > 0) {
            this.mHistoryIndex--;
            this.restoreFromHistory();
        }
    }
    
    private void redo() {
        if (this.mHistoryIndex < this.mHistorySize - 1) {
            this.mHistoryIndex++;
            this.restoreFromHistory();
        }
    }
    
    private void restoreFromHistory() {
        if (this.mHistoryIndex < 0 || 
                this.mHistoryIndex >= this.mHistorySize) {
            return;
        }
        
        this.mPtCurves.clear();
        this.mSelectedPtCurves.clear();
        
        ArrayList<JSIPtCurve> historyState = 
                this.mHistory.get(this.mHistoryIndex);
        this.mPtCurves.addAll(this.deepCopyPtCurves(historyState));
        
        this.mMode = JSI.Mode.DRAW;
        this.mCanvas2D.repaint();
    }
    
    @Override
    public void windowGainedFocus(WindowEvent e) {
        this.mCanvas2D.requestFocusInWindow();
    }
    
    @Override
    public void windowLostFocus(WindowEvent e) {
    }
}
