package org.cloudbus.cloudsim.examples.JavaSwingTool;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.core.ContainerCloudlet;
import org.cloudbus.cloudsim.examples.CloudletRequestDistribution.BaseRequestDistribution;
import org.cloudbus.cloudsim.examples.container.ConstantsExamples;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import org.jfree.chart.axis.*;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.*;

import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.title.TextTitle;

import org.jfree.data.statistics.HistogramDataset;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.List;

import org.jfree.chart.plot.XYPlot;

import javax.swing.border.EmptyBorder;




public class Draw extends JFrame{
    private final List<ContainerCloudlet> cloudletList;
    private final int terminated_time;
    private final int interval_length;
    private final int gaussian_mean;
    private final int gaussian_var;

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

        // contentPane.add(comboBox);
        basis.setVisible(true);
        return basis;
    }

    /**
     * Create the input panel.
     */
    public CustomPanel createInputDataPanel() {
        CustomPanel panel = new CustomPanel(new GridLayout(3, 1));

        JFreeChart chart1 = createRequestNumberChartOfAll();


        JFreeChart chart2 = createRequestTimeChartOfAll();

        JFreeChart chart3 = createRequestTimeChartOfSingle();

        JLabel label=new JLabel("Select an interval to present the detail info of it: ");
        DefaultComboBoxModel<String> comboModel = new DefaultComboBoxModel<String>();
        JComboBox<String> comboBox = new JComboBox<String>(comboModel);
        for(int i = 0; i < this.terminated_time; i += this.interval_length){
            String from = timeNumberToString(i);
            String end = timeNumberToString(i + this.interval_length);
            String IntervalString;
            comboBox.addItem(from + " --> " + end);
        }

        comboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    // 选择的下拉框选项
                    System.out.println(comboBox.getSelectedIndex());
                    int index = comboBox.getSelectedIndex();
                    chart3.setTitle(String.format("Request time's Distribution (%s~%s)", timeNumberToString(index * interval_length), timeNumberToString((index + 1) * interval_length)));
                    XYPlot plot3 = (XYPlot)chart3.getPlot();
                    plot3.setDataset(getRequestTimeDataset(index * interval_length, (index + 1) * interval_length));
                }
            }
        });



        panel.add((Component)new ChartPanel(chart1, false));
        panel.add((Component)new ChartPanel(chart2, false));


        JPanel jP = new CustomPanel(new BorderLayout());
        jP.add((Component)new ChartPanel(chart3, false));
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(label);
        buttonPanel.add(comboBox);

        jP.add(buttonPanel,BorderLayout.SOUTH);

        panel.add(jP);

        return panel;
    }

    private JFreeChart createRequestNumberChartOfAll() {
        int interval_nums = terminated_time / interval_length + 1;
        int[] requestNumbers = new int[interval_nums];
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series = new XYSeries("");
        // createDataset
        double[] startTimes = new double[cloudletList.size()];
        for (ContainerCloudlet containerCloudlet : cloudletList) {
            // startTimes[i] = cloudletList.get(i).getExecStartTime();
            requestNumbers[(int) containerCloudlet.getExecStartTime() / interval_length]++;
        }

        int max_num = -1, min_num = 100000;
        for(int i=0;i<interval_nums;i++){
            if(requestNumbers[i] != 0) {
                series.add(i*interval_length + interval_length / 2, requestNumbers[i]);
                if(requestNumbers[i] > max_num) max_num = requestNumbers[i];
                if(requestNumbers[i] < min_num) min_num = requestNumbers[i];
            }
        }
        dataset.addSeries(series);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Request Start Time's Distribution",
                "Start Time",
                "Request Number",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false
        );
        chart = DecorateChart(chart);


        XYPlot plot = (XYPlot)chart.getPlot();
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setForegroundAlpha(0.85F);
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer)plot.getRenderer();
        renderer.setDefaultShapesVisible(true);
        renderer.setDefaultShapesFilled(true);
        renderer.setDefaultToolTipGenerator(new TimeToolTipGenerator(interval_length/2));

        NumberAxis xAxis = (NumberAxis)plot.getDomainAxis();
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(terminated_time);
        xAxis.setNumberFormatOverride(getNumberFormat());
        xAxis.setTickUnit(new NumberTickUnit(terminated_time/24));

        NumberAxis yAxis = (NumberAxis)plot.getRangeAxis();
        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        yAxis.setRange(min_num * 99 / 100, max_num * 100 / 99);
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


    private JFreeChart createRequestTimeChartOfAll() {

        HistogramDataset dataset = getRequestTimeDataset(0, terminated_time);
        JFreeChart chart = ChartFactory.createHistogram(
                "Request time's Distribution (ALl Interval)",
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

    private JFreeChart createRequestTimeChartOfSingle() {
        HistogramDataset dataset = getRequestTimeDataset(0, interval_length - 1);
        JFreeChart chart = ChartFactory.createHistogram(
                String.format("Request time's Distribution (%s~%s)", timeNumberToString(0), timeNumberToString(interval_length)),
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


    private HistogramDataset getRequestTimeDataset(double low, double up) {
        // createDataset
        int length = 0, index = 0;
        for (ContainerCloudlet containerCloudlet : cloudletList) {
            double currentValue = containerCloudlet.getExecStartTime();
            if(currentValue >= low && currentValue < up) {
                length++;
            }
        }
        double[] data = new double[length];
        int MIPS = 10;
        int total_mips = MIPS * ConstantsExamples.CLOUDLET_PES;
        for (ContainerCloudlet containerCloudlet : cloudletList) {
            double currentValue = containerCloudlet.getExecStartTime();
            if(currentValue >= low && currentValue < up) {
                data[index] = (double) containerCloudlet.getCloudletLength() / total_mips;
                index++;
            }
        }

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
            int time1 = (int)(xyDataset.getXValue(i, i1)) - offset;
            int time2 = (int)(xyDataset.getXValue(i, i1)) + interval_length - offset;
            return String.format("%s~%s, %.1f", timeNumberToString(time1), timeNumberToString(time2), xyDataset.getYValue(i,i1));
        }
    }

    private class MyXYToolTipGenerator implements XYToolTipGenerator {
        @Override
        public String generateToolTip(XYDataset xyDataset, int i, int i1) {
            return String.format("%s", xyDataset.getYValue(i,i1));
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
            int poisson_lambda_test = 10000;
            BaseRequestDistribution self_design_distribution = new BaseRequestDistribution(terminated_time_test, interval_length_test, poisson_lambda_test, gaussian_mean_test, gaussian_var_test);
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
