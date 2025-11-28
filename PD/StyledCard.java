import javax.swing.*;
import java.awt.*;

/**
 * 高級感のあるカードパネル：丸角、内部グラデーション、薄い影を描画します。
 * ホバー時に柔らかく暗くなるオーバーレイアニメーションを持ちます。
 */
public class StyledCard extends JPanel {
    private final Color topColor;
    private final Color bottomColor;
    private final int arc = 14;
    private final int shadowOffset = 6;

    // ホバー用オーバーレイ
    private float overlayAlpha = 0f;
    private final float overlayTarget = 0.10f; // 最終的なアルファ
    private javax.swing.Timer animTimer;
    private boolean hover = false;

    public StyledCard(Color top, Color bottom) {
        super(new BorderLayout());
        this.topColor = top == null ? Color.WHITE : top;
        this.bottomColor = bottom == null ? Color.WHITE : bottom;
        setOpaque(false);
        initHover();
    }

    private void initHover() {
        int fps = 60;
        int durationMs = 180;
        int delay = Math.max(4, 1000 / fps);
        int steps = Math.max(1, durationMs / delay);
        animTimer = new javax.swing.Timer(delay, e -> {
            float delta = overlayTarget / steps;
            if (hover) {
                overlayAlpha = Math.min(overlayTarget, overlayAlpha + delta);
            } else {
                overlayAlpha = Math.max(0f, overlayAlpha - delta);
            }
            repaint();
            if (!hover && overlayAlpha <= 0f) animTimer.stop();
            if (hover && overlayAlpha >= overlayTarget) animTimer.stop();
        });
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                hover = true;
                if (!animTimer.isRunning()) animTimer.start();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                hover = false;
                if (!animTimer.isRunning()) animTimer.start();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // シャドウ（薄い丸角長方形を下に描画）
            g2.setColor(Theme.CARD_SHADOW);
            g2.fillRoundRect(shadowOffset, shadowOffset, w - shadowOffset * 2, h - shadowOffset * 2, arc, arc);

            // メインの丸角グラデーション
            GradientPaint gp = new GradientPaint(0, 0, topColor, 0, h, bottomColor);
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, w - shadowOffset, h - shadowOffset, arc, arc);

            // ホバー時の半透明オーバーレイ
            if (overlayAlpha > 0f) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayAlpha));
                g2.setColor(Color.BLACK);
                g2.fillRoundRect(0, 0, w - shadowOffset, h - shadowOffset, arc, arc);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }
        } finally {
            g2.dispose();
        }
    }
}
