import javax.swing.*;
import java.awt.*;

/**
 * 単純な縦方向グラデーションパネル。
 * 色1 -> 色2 の線形グラデーションを描画します。
 */
public class GradientPanel extends JPanel {
    private final Color colorTop;
    private final Color colorBottom;

    public GradientPanel(Color top, Color bottom) {
        this(top, bottom, null);
    }

    public GradientPanel(Color top, Color bottom, LayoutManager lm) {
        super(lm == null ? new FlowLayout() : lm);
        this.colorTop = top == null ? Color.WHITE : top;
        this.colorBottom = bottom == null ? Color.WHITE : bottom;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int w = getWidth();
            int h = getHeight();
            GradientPaint gp = new GradientPaint(0, 0, colorTop, 0, h, colorBottom);
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);
        } finally {
            g2.dispose();
        }
    }
}
