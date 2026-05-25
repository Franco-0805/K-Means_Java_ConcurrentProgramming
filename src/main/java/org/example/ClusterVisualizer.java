package org.example;

import javax.swing.*;
import java.awt.*;

// 带完整坐标轴的聚类结果查看器
public class ClusterVisualizer extends JPanel {
    private double[][] data;
    private int[] labels;
    private double[][] centroids;
    // 绘图参数（和上面的查看器保持一致）
    private final int SCALE = 5;
    private final int LEFT_MARGIN = 60;
    private final int BOTTOM_MARGIN = 60;
    private final int MAX_VALUE = 100;

    public ClusterVisualizer(double[][] data, int[] labels, double[][] centroids) {
        this.data = data;
        this.labels = labels;
        this.centroids = centroids;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // ========== 1. 先画坐标轴和刻度（和上面完全一致） ==========
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));

        int xAxisY = getHeight() - BOTTOM_MARGIN;
        int yAxisX = LEFT_MARGIN;
        // X轴
        g2d.drawLine(LEFT_MARGIN, xAxisY, LEFT_MARGIN + MAX_VALUE * SCALE, xAxisY);
        // Y轴
        g2d.drawLine(yAxisX, BOTTOM_MARGIN, yAxisX, getHeight() - BOTTOM_MARGIN);

        // X轴刻度
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        for (int i = 0; i <= MAX_VALUE; i += 10) {
            int x = LEFT_MARGIN + i * SCALE;
            g2d.drawLine(x, xAxisY, x, xAxisY - 5);
            g2d.drawString(String.valueOf(i), x - 10, xAxisY + 20);
        }
        g2d.drawString("X 坐标", LEFT_MARGIN + MAX_VALUE * SCALE / 2 - 20, xAxisY + 40);

        // Y轴刻度
        for (int i = 0; i <= MAX_VALUE; i += 10) {
            int y = getHeight() - BOTTOM_MARGIN - i * SCALE;
            g2d.drawLine(yAxisX, y, yAxisX + 5, y);
            g2d.drawString(String.valueOf(i), yAxisX - 40, y + 5);
        }
        Graphics2D g2dRotated = (Graphics2D) g2d.create();
        g2dRotated.rotate(-Math.PI / 2, LEFT_MARGIN - 30, getHeight() / 2);
        g2dRotated.drawString("Y 坐标", LEFT_MARGIN - 30, getHeight() / 2);
        g2dRotated.dispose();

        // ========== 2. 画聚类点（按簇上色） ==========
        Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA};
        for (int i = 0; i < data.length; i++) {
            int clusterId = labels[i];
            g2d.setColor(colors[clusterId % colors.length]);

            double x = LEFT_MARGIN + data[i][0] * SCALE;
            double y = getHeight() - BOTTOM_MARGIN - data[i][1] * SCALE;
            g2d.fillOval((int) x - 3, (int) y - 3, 6, 6);
        }

        // ========== 3. 画簇心（黑色大方块，最顶层） ==========
        g2d.setColor(Color.BLACK);
        for (double[] c : centroids) {
            double x = LEFT_MARGIN + c[0] * SCALE;
            double y = getHeight() - BOTTOM_MARGIN - c[1] * SCALE;
            g2d.fillRect((int) x - 5, (int) y - 5, 10, 10);
        }
    }

    // 聚类完成后，调用这一行就能显示图像
    public static void showClusters(double[][] data, int[] labels, double[][] centroids) {
        JFrame frame = new JFrame("带坐标轴的K-Means聚类结果");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(800, 700);
        frame.add(new ClusterVisualizer(data, labels, centroids));
        frame.setVisible(true);
    }
}