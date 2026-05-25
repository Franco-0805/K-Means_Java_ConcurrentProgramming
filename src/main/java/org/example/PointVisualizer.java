package org.example;

import javax.swing.*;
import java.awt.*;
import java.util.Random;


public class PointVisualizer extends JPanel {
    private double[][] data;
    // 绘图参数（可按需调整）
    private final int SCALE = 5;       // 坐标放大倍数
    private final int LEFT_MARGIN = 60; // 左边距（给y轴刻度留空间）
    private final int BOTTOM_MARGIN = 60; // 底边距（给x轴刻度留空间）
    private final int MAX_VALUE = 100;  // 点的坐标最大值（和生成点的范围一致）

    public PointVisualizer(double[][] data) {
        this.data = data;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // ========== 1. 先画坐标轴和刻度 ==========
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2)); // 坐标轴加粗

        // 画X轴（底部）
        int xAxisY = getHeight() - BOTTOM_MARGIN;
        g2d.drawLine(LEFT_MARGIN, xAxisY, LEFT_MARGIN + MAX_VALUE * SCALE, xAxisY);

        // 画Y轴（左侧）
        int yAxisX = LEFT_MARGIN;
        g2d.drawLine(yAxisX, BOTTOM_MARGIN, yAxisX, getHeight() - BOTTOM_MARGIN);

        // 画X轴刻度 + 标签
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        for (int i = 0; i <= MAX_VALUE; i += 10) { // 每10个单位一个刻度
            int x = LEFT_MARGIN + i * SCALE;
            // 刻度线
            g2d.drawLine(x, xAxisY, x, xAxisY - 5);
            // 刻度数字
            g2d.drawString(String.valueOf(i), x - 10, xAxisY + 20);
        }
        g2d.drawString("X axis", LEFT_MARGIN + MAX_VALUE * SCALE / 2 - 20, xAxisY + 40);

        // 画Y轴刻度 + 标签
        for (int i = 0; i <= MAX_VALUE; i += 10) {
            int y = getHeight() - BOTTOM_MARGIN - i * SCALE;
            // 刻度线
            g2d.drawLine(yAxisX, y, yAxisX + 5, y);
            // 刻度数字
            g2d.drawString(String.valueOf(i), yAxisX - 40, y + 5);
        }
        // 旋转Y轴标签（和Python一样竖排）
        Graphics2D g2dRotated = (Graphics2D) g2d.create();
        g2dRotated.rotate(-Math.PI / 2, LEFT_MARGIN - 30, getHeight() / 2);
        g2dRotated.drawString("Y axi", LEFT_MARGIN - 30, getHeight() / 2);
        g2dRotated.dispose();

        // ========== 2. 再画所有点（蓝色小圆点） ==========
        g2d.setColor(Color.BLUE);
        for (double[] point : data) {
            // 坐标转换：适配边距和坐标轴
            double x = LEFT_MARGIN + point[0] * SCALE;
            double y = getHeight() - BOTTOM_MARGIN - point[1] * SCALE;
            g2d.fillOval((int) x - 3, (int) y - 3, 6, 6);
        }
    }

    // 生成随机点（和之前完全一致，兼容你的代码）
    public static double[][] generatePoints(int size) {
        Random rand = new Random();
        double[][] data = new double[size][2];
        for (int i = 0; i < size; i++) {
            data[i][0] = rand.nextDouble() * 100;
            data[i][1] = rand.nextDouble() * 100;
        }
        return data;
    }

    // 主方法：直接运行
    public static void main(String[] args) {
        double[][] data = generatePoints(1000); // 改数字控制点的数量

        JFrame frame = new JFrame("带坐标轴的随机点分布");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 700); // 窗口稍微加高，适配坐标轴
        frame.add(new PointVisualizer(data));
        frame.setVisible(true);
    }
}

