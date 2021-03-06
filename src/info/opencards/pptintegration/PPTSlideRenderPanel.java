package info.opencards.pptintegration;

import info.opencards.Utils;
import info.opencards.core.CardFile;
import org.apache.poi.hslf.record.TextHeaderAtom;
import org.apache.poi.hslf.usermodel.HSLFAutoShape;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideMaster;
import org.apache.poi.sl.draw.DrawFactory;
import org.apache.poi.sl.usermodel.AutoShape;
import org.apache.poi.sl.usermodel.Slide;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;


/**
 * Allows to render incomplete powerpoint slides (given as poi-objects)
 *
 * @author Holger Brandl
 */
public class PPTSlideRenderPanel extends JPanel {


    private HSLFSlide slide;
    private boolean showTitleShape;
    private boolean showContent;

    private Dimension baseSize;
    private CardFile curCardFile;


    public PPTSlideRenderPanel() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2) {
                    // show the slide in the system's ppt-editor
                    OpenCurrentSlide.showCurrentSlideInPPTEditor(curCardFile, slide);
                }
            }
        });
    }


    private void drawSlidesPartially(Graphics2D graphics, HSLFSlide slide) {
        HSLFSlideMaster master = (HSLFSlideMaster) slide.getMasterSheet();

        if (slide.getFollowMasterBackground() && master.getBackground() != null) {
//            master.getBackground().draw(graphics, null);
            factoryDraw(graphics, master.getBackground());
        }

        if (slide.getFollowMasterObjects()) {

            java.util.List<HSLFShape> sh = master.getShapes();
            for (HSLFShape aSh : sh) {
                if (aSh.isPlaceholder()) continue;

                aSh.draw(graphics, null);
            }
        }


        HSLFShape titleShape = getTitleShape(slide);

        for (HSLFShape shape : slide.getShapes()) {
            boolean isTitleShape = shape.getShapeId() == titleShape.getShapeId();

            if (isTitleShape && showTitleShape) {
//                shape.draw(graphics);
                factoryDraw(graphics, shape);
            }

            if (!isTitleShape && showContent) {
//                shape.draw(graphics);
                factoryDraw(graphics, shape);
            }
        }
    }


    private void factoryDraw(Graphics2D graphics, HSLFShape shape) {
        DrawFactory.getInstance(graphics).getDrawable(shape).draw(graphics);
    }


    private HSLFShape getTitleShape(HSLFSlide slide) {
        String slideTitle = slide.getTitle();

        for (HSLFShape shape : slide.getShapes()) {
            if (shape instanceof AutoShape) {
                HSLFAutoShape autoShape = (HSLFAutoShape) shape;
                if (autoShape.getText() != null && autoShape.getText().equals(slideTitle)) {
                    int type = autoShape.getRunType();

                    if (type == TextHeaderAtom.CENTER_TITLE_TYPE || type == TextHeaderAtom.TITLE_TYPE) {
                        return shape;
                    }
                }
            }
        }

//  When you have a XSLFSlide object you can use .getShapes() to get all shapes in the slide. If the shape is a
// XSLFTextShape you can use .getTextType() to check if it's a title, .getTextParagraphs() to get the paragraphs and
// .getTextRuns() on the paragraphs to get the text runs with the text. That should give you

        return null;

        // can not work as we don't have a slide title for slides without a title element
//        // if we don't find a title shape than use the most topwards element as question
//        if(slide.getShapes().length ==0)
//            return null;
//
//        return Collections.max(Arrays.asList(slide.getShapes()), new Comparator<Shape>() {
//            @Override
//            public int compare(Shape o1, Shape o2) {
//                return o1.getAnchor().getCenterY() - o2.getAnchor().getCenterY() < 0 ? -1 : 1;
//            }
//        });
    }


    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2d = (Graphics2D) graphics;

        if (!Utils.isMacOSX()) {
            RenderingHints renderHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            renderHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHints(renderHints);
        }

        super.paintComponent(graphics);

        if (slide == null) {
            return;
        }

//        System.err.println("dimension: " + getSize());
//        System.err.println("baseSize: " + baseSize);
//        System.err.println("scaling: " + slideScaleTransform);


        double slideRatio = baseSize.getWidth() / baseSize.getHeight();
        double containerRatio = getSize().getWidth() / getSize().getHeight();

        AffineTransform slideScaleTransform = new AffineTransform();

        if (slideRatio < containerRatio) {
//            scaleDim = new Dimension(getWidth(), (int) (getSize().getHeight() / slideRatio));
            // y is limitting expansionhere
            double yScale = getSize().getHeight() / baseSize.getHeight();
            slideScaleTransform.scale(yScale, yScale);
            slideScaleTransform.translate((getSize().getWidth() - yScale * baseSize.getWidth()) * 0.5 / yScale, 0);

        } else {
            double xScale = getSize().getWidth() / baseSize.getWidth();
            slideScaleTransform.scale(xScale, xScale);
            slideScaleTransform.translate(0, (getSize().getHeight() - xScale * baseSize.getHeight()) * 0.5 / xScale);
        }


        // ...or adjust to full width
//        double xScale = scaleDim.getWidth() / getWidth();
//        double yScale = scaleDim.getHeight() / getHeight();
//
//        slideScaleTransform.scale(xScale, yScale);
//        slideScaleTransform.translate(xScale, yScale);

        g2d.setTransform(slideScaleTransform);

        if (showContent && showTitleShape) {
            slide.draw(g2d);
        } else {
            drawSlidesPartially(g2d, slide);
        }
    }


    public void configure(Slide slide, boolean showTitle, boolean showContent) {
        this.slide = (HSLFSlide) slide;

        this.showTitleShape = showTitle;
        this.showContent = showContent;

        repaint();
    }


    public void setBaseSize(Dimension baseSize) {
        this.baseSize = baseSize;
    }


    public void setCardFile(CardFile cardFile) {
        curCardFile = cardFile;
    }
}
