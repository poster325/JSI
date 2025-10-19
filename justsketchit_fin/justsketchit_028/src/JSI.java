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
    
    // ====================================================================
    // Constants
    // ====================================================================
    
    // Mode system:
    // Persistent modes: DRAW, SELECTED (stay until explicitly changed)
    // Quasi-modes: SELECT, PAN, ZOOM_ROTATE, COLOR (temporary, key-held)
    public enum Mode {
        DRAW, SELECT, SELECTED, PAN, ZOOM_ROTATE, COLOR
    }
    
    private static final int MAX_HISTORY_SIZE = 5;
    private static final int DEFAULT_WINDOW_WIDTH = 800;
    private static final int DEFAULT_WINDOW_HEIGHT = 600;
    
    // ====================================================================
    // Fields
    // Application state and UI components
    // ====================================================================
    
    private Mode mMode = JSI.Mode.DRAW;
    private JFrame mFrame = null;
    private JSICanvas2D mCanvas2D = null;
    
    // Drawing state
    private JSIPtCurve mCurPtCurve = null;
    private ArrayList<JSIPtCurve> mPtCurves = null;
    private ArrayList<JSIPtCurve> mSelectedPtCurves = null;
    
    // Tools
    private JSISelectionBox mSelectionBox = null;
    private JSIXform mXform = null;
    private JSIColorChooser mColorChooser = null;
    
    // History system for undo/redo
    private ArrayList<ArrayList<JSIPtCurve>> mHistory;
    private int mHistoryIndex;
    private int mHistorySize;
    
    // ====================================================================
    // Constructor & Main
    // Entry point and initialization
    // ====================================================================
    
    public JSI() {
        this.initializeFrame();
        this.initializeCanvas();
        this.initializeDataStructures();
    }
    
    public static void main(String[] args) {
        new JSI();
    }
    
    // ====================================================================
    // Public Getters
    // Provide access to internal state for canvas rendering
    // ====================================================================
    
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
    
    // ====================================================================
    // Initialization Helpers
    // Break down constructor into focused setup functions
    // ====================================================================
    
    private void initializeFrame() {
        this.mFrame = new JFrame("JustSketchIt");
        this.mFrame.setSize(DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);
        this.mFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.mFrame.setLocationRelativeTo(null);
    }
    
    private void initializeCanvas() {
        this.mCanvas2D = new JSICanvas2D(this);
        this.mFrame.add(this.mCanvas2D);
        this.mCanvas2D.addMouseListener(this);
        this.mCanvas2D.addMouseMotionListener(this);
        this.mCanvas2D.setFocusable(true);
        this.mCanvas2D.addKeyListener(this);
        
        // Add window focus listener AFTER canvas is created
        this.mFrame.addWindowFocusListener(this);
        
        // Make frame visible and request focus
        this.mFrame.setVisible(true);
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
    
    // ====================================================================
    // Mouse Event Handlers
    // Implement MouseListener and MouseMotionListener interfaces
    // ====================================================================
    
    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        this.mCanvas2D.requestFocusInWindow();
        
        Point pt = e.getPoint();
        switch (this.mMode) {
            case DRAW:
                this.createNewPtCurve(pt);
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

    @Override
    public void mouseReleased(MouseEvent e) {
        switch (this.mMode) {
            case DRAW:
                this.finishPtCurve();
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
                this.applyColorFromChooser(e.getPoint());
                break;
        }
        this.mCanvas2D.repaint();
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
                this.continueDrawingPtCurve(pt);
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

    @Override
    public void mouseMoved(MouseEvent e) {
    }
    
    // ====================================================================
    // Mouse Event Helper Functions
    // Support functions for mouse event handling
    // ====================================================================
    
    // --- DRAW Mode Operations ---
    
    private void createNewPtCurve(Point screenPt) {
        Point2D.Double worldPt = 
                this.mXform.calcPtFromScreenToWorld(screenPt);
        this.mCurPtCurve = new JSIPtCurve(
                worldPt,
                this.mCanvas2D.getCurColorForPtCurve(),
                this.mCanvas2D.getCurStrokeForPtCurve());
    }
    
    private void continueDrawingPtCurve(Point screenPt) {
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
    
    private void finishPtCurve() {
        if (this.mCurPtCurve != null && 
                this.mCurPtCurve.getPts().size() >= 2) {
            this.mPtCurves.add(this.mCurPtCurve);
            this.saveToHistory();
        } else {
            this.deselectAll();
            this.saveToHistory();
        }
        this.mCurPtCurve = null;
    }
    
    // --- SELECTED Mode Operations ---
    
    private void handleSelectedModePress(MouseEvent e, Point pt) {
        boolean isShiftPressed = 
                (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;
        
        if (isShiftPressed) {
            this.mMode = JSI.Mode.SELECT;
            this.mSelectionBox = new JSISelectionBox(pt);
        } else {
            this.deselectAll();
            this.mMode = JSI.Mode.DRAW;
        }
    }
    
    private void deselectAll() {
        this.mPtCurves.addAll(this.mSelectedPtCurves);
        this.mSelectedPtCurves.clear();
    }
    
    private void deleteSelectedCurves() {
        if (!this.mSelectedPtCurves.isEmpty()) {
            this.mSelectedPtCurves.clear();
            this.saveToHistory();
            this.mMode = JSI.Mode.DRAW;
        }
    }
    
    // --- COLOR Mode Operations ---
    
    private void applyColorFromChooser(Point pt) {
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

    // ====================================================================
    // Key Event Handlers
    // Implement KeyListener interface for keyboard input
    // ====================================================================

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        
        // Handle mode switches and tool adjustments
        this.keyToMode(e);
        
        // Handle undo/redo
        if (code == KeyEvent.VK_Z && this.isCtrlPressed(e)) {
            this.undo();
        } else if (code == KeyEvent.VK_Y && this.isCtrlPressed(e)) {
            this.redo();
        }
        
        this.mCanvas2D.repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        
        switch (code) {
            case KeyEvent.VK_SHIFT:
                // Exit quasi-mode: return to persistent mode
                this.mSelectionBox = null;
                this.returnToPersistentMode();
                break;
            case KeyEvent.VK_CONTROL:
            case KeyEvent.VK_ALT:
            case KeyEvent.VK_C:
                // Exit quasi-modes: return to persistent mode
                this.returnToPersistentMode();
                break;
            case KeyEvent.VK_ESCAPE:
                this.deselectAll();
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
    
    // ====================================================================
    // Key Event Helper Functions
    // Support functions for keyboard event handling
    // ====================================================================
    
    // Map keys to quasi-modes and tool adjustments
    private void keyToMode(KeyEvent e) {
        int code = e.getKeyCode();
        switch (code) {
            case KeyEvent.VK_SHIFT:
                this.mMode = JSI.Mode.SELECT;  // Enter quasi-mode
                break;
            case KeyEvent.VK_CONTROL:
                this.mMode = JSI.Mode.PAN;  // Enter quasi-mode
                break;
            case KeyEvent.VK_ALT:
                this.mMode = JSI.Mode.ZOOM_ROTATE;  // Enter quasi-mode
                break;
            case KeyEvent.VK_C:
                this.mMode = JSI.Mode.COLOR;  // Enter quasi-mode
                break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_CLOSE_BRACKET:
                this.adjustStrokeWidth(JSICanvas2D.STROKE_WIDTH_INCREMENT);
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_OPEN_BRACKET:
                this.adjustStrokeWidth(-JSICanvas2D.STROKE_WIDTH_INCREMENT);
                break;
        }
    }
    
    private boolean isCtrlPressed(KeyEvent e) {
        return (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0;
    }
    
    private void adjustStrokeWidth(float delta) {
        if (this.mMode == JSI.Mode.SELECTED) {
            this.adjustStrokeWidthForSelectedCurves(delta);
        } else {
            this.mCanvas2D.increaseStrokeWidthForCurPtCurve(delta);
        }
    }
    
    private void returnToPersistentMode() {
        if (!this.mSelectedPtCurves.isEmpty()) {
            this.mMode = JSI.Mode.SELECTED;
        } else {
            this.mMode = JSI.Mode.DRAW;
        }
    }
    
    // ====================================================================
    // Selection Operations
    // Functions for selecting curves and modifying selected curves
    // ====================================================================
    
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
    
    // ====================================================================
    // History System (Undo/Redo)
    // Manage undo/redo functionality with deep copy snapshots
    // ====================================================================
    
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
    
    private ArrayList<JSIPtCurve> deepCopyPtCurves(
            ArrayList<JSIPtCurve> original) {
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
    
    // ====================================================================
    // Window Focus Handlers
    // Implement WindowFocusListener to maintain keyboard focus
    // ====================================================================
    
    @Override
    public void windowGainedFocus(WindowEvent e) {
        if (this.mCanvas2D != null) {
            this.mCanvas2D.requestFocusInWindow();
        }
    }
    
    @Override
    public void windowLostFocus(WindowEvent e) {
    }
}
