package org.cloudbus.cloudsim.examples.JavaSwingTool;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.core.ContainerCloudlet;
import org.cloudbus.cloudsim.examples.CloudletRequestDistribution.BaseRequestDistribution;
import org.cloudbus.cloudsim.examples.container.ConstantsExamples;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import org.jfree.chart.axis.*;
import org.jfree.chart.labels.CategorySeriesLabelGenerator;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.labels.StandardCategorySeriesLabelGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.*;

import org.jfree.chart.renderer.category.BarPainter;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.*;
import java.util.List;


import org.jfree.util.ShapeUtilities;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import javax.swing.border.EmptyBorder;




public class Draw extends JFrame{
    static JSlider slider1;
    static JSlider slider2;
    private List<ContainerCloudlet> cloudletList;
    private final int terminated_time;
    private final int interval_length;
    private final int gaussian_mean;
    private final int gaussian_var;
    private int MIPS = 10;

    public Draw(List<ContainerCloudlet> cloudletList, int terminated_time, int interval_length, int gaussian_mean, int gaussian_var) {
        this.cloudletList = cloudletList;
        this.terminated_time = terminated_time;
        this.interval_length = interval_length;
        this.gaussian_mean = gaussian_mean;
        this.gaussian_var = gaussian_var;
        JTabbedPane panel = createTabPanel();
        panel.setPreferredSize(new Dimension(1200, 900));
        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
        JFrame interval_list = createDropdownList();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }


    /**
     * Create the tab panel.
     */
    public JTabbedPane createTabPanel() {
        // crate the tab panel
        final JTabbedPane tabbedPane = new JTabbedPane();
        // crate the input panel
        CustomPanel panel1 = createInputDataPanel();

        // create the first tab(set tab name and content)
        tabbedPane.addTab("Input Data", panel1);

        // crate the second tab
        tabbedPane.addTab("Result", new ImageIcon("bb.jpg"), new JPanel(new GridLayout(1, 1)));


        // add listener
        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                System.out.println("current tab: " + tabbedPane.getSelectedIndex());
            }
        });

        // set the default tab
        tabbedPane.setSelectedIndex(0);
        return tabbedPane;
    }

    /**
     * create a drop down list to present the detail info of one specific interval.
     */
    public JFrame createDropdownList(){
        JFrame basis = new JFrame();
        basis.setTitle("Interval Selection");
        basis.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        basis.setBounds(10,10,400,300);
        JPanel contentPane=new JPanel();
        contentPane.setBorder(new EmptyBorder(5,5,5,5));
        basis.setContentPane(contentPane);
        contentPane.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
        JLabel label=new JLabel("Select an interval to present the detail info of it.");
        contentPane.add(label);
        JComboBox comboBox=new JComboBox();
        for(int i = 0; i < this.terminated_time; i += this.interval_length){
            String from = timeNumberToString(i);
            String end = timeNumberToString(i + this.interval_length);
            String IntervalString;
            comboBox.addItem(from + " --> " + end);
        }
        contentPane.add(comboBox);
        basis.setVisible(true);
        return basis;
    }

    /**
     * Create the input panel.
     */
    public CustomPanel createInputDataPanel() {
        CustomPanel panel = new CustomPanel(new GridLayout(5, 1));
        JPanel controlPanel = new JPanel(new GridLayout(2, 1));

        JFreeChart chart1 = createBarChart1();


        JFreeChart chart2 = createScatterChart();


        JFreeChart chart3 = createBarChart2();


        JFreeChart chart4 = createBarChart3();


        slider1 = createJSlider();
        slider2 = createJSlider();
        ChangeListener listener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                XYPlot plot1 = (XYPlot)chart1.getPlot();
                XYPlot plot2 = (XYPlot)chart2.getPlot();
                CategoryPlot plot3 = (CategoryPlot)chart3.getPlot();
                XYPlot plot4 = (XYPlot)chart4.getPlot();
                ValueAxis axis1 = (ValueAxis)plot1.getDomainAxis();
                ValueAxis axis2 = (ValueAxis)plot2.getDomainAxis();
                double center = slider1.getValue();
                double radius = slider2.getValue();
                double low = center - radius < 0 ? 0 : center - radius;
                double up = center + radius > terminated_time ? terminated_time : center + radius;
                axis1.setLowerBound(low);
                axis1.setUpperBound(up);
                axis2.setLowerBound(low);
                axis2.setUpperBound(up);

                plot3.setDataset(getBarChart2Dataset(low, up));
                plot4.setDataset(getBarChart3Dataset(low, up));
            }
        };

        slider1.addChangeListener(listener);
        slider2.addChangeListener(listener);

        JPanel axisPanel1 = new JPanel(new BorderLayout());
        axisPanel1.add(slider1);
        axisPanel1.setBorder(new TitledBorder("The Center Of The Visible Range:"));
        JPanel axisPanel2 = new JPanel(new BorderLayout());
        axisPanel2.add(slider2);
        axisPanel2.setBorder(new TitledBorder("The Radius Of The Visible Range:"));

        panel.add((Component)new ChartPanel(chart2, false));
        panel.add((Component)new ChartPanel(chart1, false));
        panel.add((Component)new ChartPanel(chart4, false));
        panel.add((Component)new ChartPanel(chart3, false));
        controlPanel.add(axisPanel1);
        controlPanel.add(axisPanel2);
        panel.add(controlPanel);

        return panel;
    }

    private JSlider createJSlider() {
        JSlider slider = new JSlider(0, terminated_time, terminated_time / 2);

        int major_tick_interval = terminated_time / 12;
        slider.setMajorTickSpacing(major_tick_interval);
        slider.setMinorTickSpacing(interval_length);
        slider.setPaintLabels(true);
        slider.setPaintTicks(true);
        slider.setSnapToTicks(true);

        Hashtable<Integer, JComponent> hashtable = new Hashtable<Integer, JComponent>();
        for(int i=0;i<=12;i++){
            int current_time = major_tick_interval * i;
            int hour = current_time /3600;
            int minute = current_time % 60;
            hashtable.put(current_time, new JLabel(String.format("%02d:%02d", hour, minute)));      //  0  刻度位置，显示 "Start"
        }
        slider.setLabelTable(hashtable);

        return slider;
    }


    private JFreeChart createScatterChart() {
        XYSeries series1 = new XYSeries("(Start Time, Request Time) ");
        for(ContainerCloudlet cl : cloudletList){
            double time = (double)cl.getCloudletLength() /(this.MIPS * ConstantsExamples.CLOUDLET_PES);
            series1.add(cl.getExecStartTime(), time);
        }
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series1);

        JFreeChart chart = ChartFactory.createScatterPlot(
                "New Request aside by time clock",
                "Start Time",
                "Request Time (min)",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false
        );
        chart = DecorateChart(chart);


        XYPlot plot = (XYPlot)chart.getPlot();
        NumberAxis xAxis = (NumberAxis)plot.getDomainAxis();
        xAxis.setNumberFormatOverride(getNumberFormat());
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(terminated_time);
        xAxis.setTickUnit(new NumberTickUnit(terminated_time / 24));
        //modification
        Shape circle = ShapeUtilities.createDownTriangle(1);
        //Shape cross = ShapeUtilities.createDiagonalCross(3,1);
        XYItemRenderer renderer = plot.getRenderer();
        renderer.setDefaultToolTipGenerator(new TimeToolTipGenerator(0));
        renderer.setSeriesShape(0,circle);
        return chart;
    }

    private JFreeChart createBarChart1() {

        // createDataset
        double[] startTimes = new double[cloudletList.size()];
        for(int i=0;i<cloudletList.size();i++){
            startTimes[i] = cloudletList.get(i).getExecStartTime();
        }

        HistogramDataset dataset = new HistogramDataset();
        dataset.addSeries("(Start Time, Request Number) ", startTimes, (int)(terminated_time / interval_length), 0D, (double) terminated_time);

        JFreeChart chart = ChartFactory.createHistogram(
                "Request Start Time's Distribution",
                "Start Time",
                "Request Number",
                dataset, PlotOrientation.VERTICAL,
                false,
                true,
                false
        );
        chart = DecorateChart(chart);


        XYPlot plot = (XYPlot)chart.getPlot();
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setForegroundAlpha(0.85F);
        NumberAxis yAxis = (NumberAxis)plot.getRangeAxis();
        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        XYBarRenderer renderer = (XYBarRenderer)plot.getRenderer();
        renderer.setDrawBarOutline(false);
        renderer.setBarPainter((XYBarPainter)new StandardXYBarPainter());
        renderer.setShadowVisible(false);
        renderer.setDefaultToolTipGenerator(new TimeToolTipGenerator(interval_length/2));

        NumberAxis xAxis = (NumberAxis)plot.getDomainAxis();
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(terminated_time);
        xAxis.setNumberFormatOverride(getNumberFormat());
        xAxis.setTickUnit(new NumberTickUnit(terminated_time/24));
        return chart;
    }

    private JFreeChart DecorateChart(JFreeChart chart) {
        Font titleFont = new Font("Arial", Font.BOLD , 18) ;
        Font font = new Font("Arial",Font.PLAIN,12) ;
        Font yfont = new Font("Arial",Font.BOLD,12) ;
        chart.setTitle(new TextTitle(chart.getTitle().getText(),titleFont));

        XYPlot plot = chart.getXYPlot();
        ValueAxis domainAxis = plot.getDomainAxis();
        domainAxis.setLabelFont(font);
        //domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

        ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setLabelFont(yfont);
        rangeAxis.setTickLabelFont(yfont);
        return chart;

    }

    private JFreeChart createBarChart2() {
        CategoryDataset dataset = getBarChart2Dataset(0, terminated_time);

        JFreeChart chart = ChartFactory.createBarChart(
                "Request Number of Each Interval's Distribution",
                "Request Number",
                "Interval Number",
                (CategoryDataset)dataset
        );
        //This chart's type is different from the others.
        Font titleFont = new Font("Arial", Font.BOLD , 18) ;
        Font font = new Font("Arial",Font.PLAIN,12) ;
        Font yfont = new Font("Arial",Font.BOLD,12) ;
        chart.setTitle(new TextTitle(chart.getTitle().getText(),titleFont));

        CategoryPlot plot = (CategoryPlot)chart.getPlot();
        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setLabelFont(font);
        //domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

        ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setLabelFont(yfont);
        rangeAxis.setTickLabelFont(yfont);



        plot.setDomainGridlinesVisible(true);
        plot.setRangeCrosshairVisible(true);
        plot.setRangeCrosshairPaint(Color.BLUE);
        plot.getDomainAxis().setCategoryMargin(0.2D);
       // NumberAxis rangeAxis = (NumberAxis)plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        BarRenderer renderer = (BarRenderer)plot.getRenderer();
        renderer.setDrawBarOutline(false);
        renderer.setBarPainter((BarPainter)new StandardBarPainter());
        renderer.setItemMargin(0.06D);
        // renderer.setLegendItemToolTipGenerator((CategorySeriesLabelGenerator)new StandardCategorySeriesLabelGenerator("Tooltip: {0}"));
        renderer.setDefaultToolTipGenerator(new MyCategoryToolTipGenerator());
        return chart;
    }

    private JFreeChart createBarChart3() {

        HistogramDataset dataset = getBarChart3Dataset(0, terminated_time);
        JFreeChart chart = ChartFactory.createHistogram(
                "Request time's Distribution",
                "Request Time (min)",
                "Request Number",
                dataset, PlotOrientation.VERTICAL,
                false,
                true,
                false
        );
        chart = DecorateChart(chart);
        XYPlot plot = (XYPlot)chart.getPlot();
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setForegroundAlpha(0.85F);
        NumberAxis yAxis = (NumberAxis)plot.getRangeAxis();
        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        XYBarRenderer renderer = (XYBarRenderer)plot.getRenderer();
        renderer.setDrawBarOutline(false);
        renderer.setBarPainter((XYBarPainter)new StandardXYBarPainter());
        renderer.setShadowVisible(false);
        renderer.setDefaultToolTipGenerator(new MyXYToolTipGenerator());
        return chart;
    }

    private CategoryDataset getBarChart2Dataset(double low, double up) {
        int interval_num = (terminated_time / interval_length);
        // createDataset
        int[] interval_num_arr = new int[interval_num];
        int max_num = -1;
        for (ContainerCloudlet containerCloudlet : cloudletList) {
            double currentValue = containerCloudlet.getExecStartTime();
            if(currentValue >= low && currentValue < up) {
                int index = (int) (currentValue / interval_length);
                interval_num_arr[index]++;
                max_num = Math.max(max_num, (int) (interval_num_arr[index]));
            }
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int[] count = new int[max_num];
        for (int v : interval_num_arr) {
            if(v > 0) {
                count[v - 1]++;
            }
        }

        for(int i=0;i<count.length;i++) {
            dataset.addValue(count[i], "Request Number", Integer.toString(i + 1));
        }

        return (CategoryDataset)dataset;
    }

    private HistogramDataset getBarChart3Dataset(double low, double up) {
        // createDataset
        int length = 0, index = 0;
        for (ContainerCloudlet containerCloudlet : cloudletList) {
            double currentValue = containerCloudlet.getExecStartTime();
            if(currentValue >= low && currentValue < up) {
                length++;
            }
        }
        double[] data = new double[length];
        int total_mips = this.MIPS * ConstantsExamples.CLOUDLET_PES;
        for (ContainerCloudlet containerCloudlet : cloudletList) {
            double currentValue = containerCloudlet.getExecStartTime();
            if(currentValue >= low && currentValue < up) {
                data[index] = (double) containerCloudlet.getCloudletLength() / total_mips;
                index++;
            }
        }

        Log.printLine("low:"+low+"\nup:"+up+"\nlength:"+length+"\n");
        HistogramDataset dataset = new HistogramDataset();
        dataset.addSeries("(Request Length, Request Number) ", data, gaussian_var / 5, (double)(gaussian_mean - gaussian_var/2) / total_mips, (double) (gaussian_mean + gaussian_var/2) / total_mips);
        return dataset;
    }

    private class TimeToolTipGenerator implements XYToolTipGenerator {
        private int offset;
        public TimeToolTipGenerator(int offset) {
            this.offset = offset;
        }
        @Override
        public String generateToolTip(XYDataset xyDataset, int i, int i1) {
            int time = (int)(xyDataset.getXValue(i, i1)) - offset;
            return String.format("%s, %.1f", timeNumberToString(time), xyDataset.getYValue(i,i1));
        }
    }

    private class MyXYToolTipGenerator implements XYToolTipGenerator {
        @Override
        public String generateToolTip(XYDataset xyDataset, int i, int i1) {
            return String.format("%s", xyDataset.getYValue(i,i1));
        }
    }

    private static class MyCategoryToolTipGenerator implements CategoryToolTipGenerator {

        @Override
        public String generateToolTip(CategoryDataset categoryDataset, int i, int i1) {
            return String.format("%s", categoryDataset.getValue(i, i1));
        }
    }

    private String timeNumberToString(int time) {
        time = time % 86400;
        int hour = time / 3600;
        int minute = (time - hour * 3600) / 60;
        return String.format("%02d:%02d", hour, minute);
    }

    private NumberFormat getNumberFormat() {
        return new NumberFormat(){
            @Override
            public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
                int time = (int)(number) % 86400;
                // Log.printLine(time);
                return new StringBuffer(timeNumberToString(time));
            }

            @Override
            public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
                return new StringBuffer(String.format("%s", number));
            }

            @Override
            public Number parse(String source, ParsePosition parsePosition) {
                return null;
            }
        };
    }

    public static void main(String[] args) {
        try {
            // generate data
            int brokerId = 1;
            int terminated_time_test = 24 * 60 * 60; //24 hours
            int interval_length_test = 20 * 60; // 20 minutes
            int gaussian_mean_test = 1000;
            int gaussian_var_test = 100;
            BaseRequestDistribution self_design_distribution = new BaseRequestDistribution(terminated_time_test, interval_length_test, 3, gaussian_mean_test, gaussian_var_test);
            List<ContainerCloudlet>  cloudletList_test = self_design_distribution.GetWorkloads();
            for(ContainerCloudlet cl : cloudletList_test){
                cl.setUserId(brokerId);
            }

            // visualize the raw data
            EventQueue.invokeLater(() -> {
                //change the default font
                Font font = new Font("Arial", Font.PLAIN, 13);
                java.util.Enumeration keys = UIManager.getDefaults().keys();
                while (keys.hasMoreElements()) {
                    Object key = keys.nextElement();
                    Object value = UIManager.get(key);
                    if (value instanceof javax.swing.plaf.FontUIResource) {
                        UIManager.put(key, font);
                    }
                }
                Draw ex = new Draw(cloudletList_test, terminated_time_test, interval_length_test, gaussian_mean_test, gaussian_var_test);
                ex.setVisible(true);
            });
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }
    }
}
