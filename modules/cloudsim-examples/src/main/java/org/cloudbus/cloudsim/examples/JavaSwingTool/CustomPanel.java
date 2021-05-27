package org.cloudbus.cloudsim.examples.JavaSwingTool;

import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import org.jfree.chart.JFreeChart;

public class CustomPanel extends JPanel {
    List<JFreeChart> charts;

    public CustomPanel(LayoutManager layout) {
        super(layout);
        this.charts = new ArrayList<>();
    }

    public void addChart(JFreeChart chart) {
        this.charts.add(chart);
    }

    public JFreeChart[] getCharts() {
        int chartCount = this.charts.size();
        JFreeChart[] charts = new JFreeChart[chartCount];
        for (int i = 0; i < chartCount; i++)
            charts[i] = this.charts.get(i);
        return charts;
    }
}

