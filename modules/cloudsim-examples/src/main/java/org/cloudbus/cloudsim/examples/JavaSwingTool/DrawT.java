package org.cloudbus.cloudsim.examples.JavaSwingTool;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.core.ContainerCloudlet;
import org.cloudbus.cloudsim.container.core.UserSideDatacenter;
import org.cloudbus.cloudsim.examples.CloudletRequestDistribution.BaseRequestDistribution;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.*;
import java.util.List;

public class DrawT extends JFrame{
    private JTabbedPane tabPane ;
    private static int terminated_time = 86400;
    private static int interval_length = 3600;

    public static Map<Integer, Map<Double, Integer>> DataCenterSeries = new HashMap<Integer, Map<Double, Integer>>();

    public DrawT(Map<Integer, Map<Double, Integer>> DataCenterSeriesTmp){
        DataCenterSeries = DataCenterSeriesTmp;
    }

    /*
    * 数据中心时间类
    * */
    class containerEvent implements Comparable<containerEvent>{
        double eventTime;
        int eventTag; //1代表创建，-1代表销毁
        int datacenterId;
        containerEvent(double eventTime, int eventTag, int datacenterId){
            this.eventTime = eventTime;
            this.eventTag = eventTag;
            this.datacenterId = datacenterId;
        }
        @Override
        public int compareTo(containerEvent o) {
            if(this.eventTime == o.eventTime)
                return 0;
            else
                return this.eventTime < o.eventTime ? -1: 1;
        }
    }


    //mips,CloudLetPesNumber  cloudlet的数量,以及设置整个窗口的大小
    public DrawT() {

        this.tabPane = createTabPanel();
        this.tabPane.setPreferredSize(new Dimension(1200, 900));
        setContentPane(this.tabPane);
        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    /**
     * Create the tab panel.  创建一个Panel对象，一个窗口有两个，无关紧要
     */
    public JTabbedPane createTabPanel() {
        // crate the tab panel
        final JTabbedPane tabbedPane = new JTabbedPane();

        // crate the first tab
        tabbedPane.addTab("Input Data", new JPanel(new GridLayout(1, 1)));

        tabbedPane.addTab("Result", new JPanel(new GridLayout(1, 1)));
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

    //主函数里调用，传入连接数据
    public void setInputDataPanel(Map<Integer, Map<Double, Integer>> DataCenterSeries) {
        CustomPanel panel = createInputDataPanel(DataCenterSeries);
        this.tabPane.setComponentAt(0, panel);
    }

    public void setResultPanel(List<ContainerCloudlet> resultCloudletList) {
        CustomPanel panel = createResultPanel(resultCloudletList);
        this.tabPane.setComponentAt(1, panel);
    }

    /**
     * Create the input panel. 创建基础的panel   被setInputDataPanel调用
     */
    public CustomPanel createInputDataPanel(Map<Integer, Map<Double, Integer>> DataCenterSeries) {


        CustomPanel panel = new CustomPanel(new GridLayout(1, 1));
        JFreeChart chart1 = createRequestNumberChartOfAll(DataCenterSeries, terminated_time, interval_length);

        panel.add((Component)new ChartPanel(chart1, false));


        return panel;
    }

    //++将list传进去并且设置格式 被创建panel调用  createInputDataPanel    terminated_time是整个时长，是需要的，也不用变，还是72*20=1440，
    private JFreeChart createRequestNumberChartOfAll(Map<Integer, Map<Double, Integer>> DataCenterSeries, int terminated_time, int interval_length) {
        int interval_nums = terminated_time / interval_length + 1;
        int[] requestNumbers = new int[interval_nums];
        XYSeriesCollection dataset = new XYSeriesCollection();

        int max_num = -1, min_num = 100000;

        for(Map.Entry<Integer, Map<Double, Integer>> DatacenterEntry : DataCenterSeries.entrySet()){
                Integer mapKey = DatacenterEntry.getKey();
            Map<Double, Integer>  eventmap = DatacenterEntry.getValue();

            XYSeries series = new XYSeries("DataCenter"+mapKey);
//            if (mapKey==3){
                Set entries = eventmap.entrySet( );

                if(entries != null) {
                    Iterator iterator = entries.iterator( );
                    while(iterator.hasNext( )) {
                        Map.Entry entry = (Map.Entry) iterator.next( );
                        double key = (Double) entry.getKey( );
                        Integer value =(Integer) entry.getValue();
                        if (value>max_num) max_num =value;
                        if (value<min_num) min_num = value;
                        series.add(key,value);
                    }
                }
                dataset.addSeries(series);
//            }
        }
        //以下是表格格式

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Real-time Quantity of Containers in Each Datacenter",
                "Time",
                "Container Numbers",
                dataset,
                PlotOrientation.VERTICAL,
                true,          //是否显示图例
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

        NumberAxis xAxis = (NumberAxis)plot.getDomainAxis();
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(terminated_time);
        xAxis.setNumberFormatOverride(getNumberFormat());
        xAxis.setTickUnit(new NumberTickUnit(terminated_time/24));

        NumberAxis yAxis = (NumberAxis)plot.getRangeAxis();
        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        yAxis.setRange(0, 100);
        return chart;

    }

    //++横纵坐标格式
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

    //++将时间格式变掉
    private String timeNumberToString(int time) {
        time = time % 86400;
        int hour = time / 3600;
        int minute = (time - hour * 3600) / 60;
        return String.format("%02d:%02d", hour, minute);
    }
    //++把时间格式变掉，变为时间段那种
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

    //result
    /**
     * Create the result panel.
     */
    public CustomPanel createResultPanel(List<ContainerCloudlet> resultCloudletList) {
        CustomPanel panel = new CustomPanel(new GridLayout(3, 1));
        CustomPanel field1 = new CustomPanel(new GridLayout(1, 2));
        JFreeChart chart1 = createDelayChartOfCDF(resultCloudletList);
        JFreeChart chart2 = createDelayChartOfDistribution(resultCloudletList);
        field1.add((Component)new ChartPanel(chart1, false));
        field1.add((Component)new ChartPanel(chart2, false));
        panel.add(field1);
        JScrollPane field2 = createResultTable(resultCloudletList);
        JScrollPane field3 = createCostTable();
        panel.add(field2);
        panel.add(field3);
        return panel;
    }

    private JFreeChart createDelayChartOfCDF(List<ContainerCloudlet> resultCloudletList) {
        int length = resultCloudletList.size();
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series = new XYSeries("Delay Factor");
        // createDataset
        double[] dataArr = new double[length];
        for (int i=0;i<resultCloudletList.size();i++) {
            double current_value = resultCloudletList.get(i).getDelayFactor();
            dataArr[i] = current_value;
        }
        Arrays.sort(dataArr);
        int pos = 0;
        series.add(dataArr[pos]*0.99, 0);
        while(pos < length) {
            double current = dataArr[pos];
            while(pos < length && current == dataArr[pos]) {
                pos++;
            }
            Log.printLine(current + " " + pos);
            series.add(current, (double) pos / length);
        }

        dataset.addSeries(series);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Delay Distribution",
                "Delay Factor",
                "CDF",
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

        ChartUtils.applyCurrentTheme(chart);

        return chart;
    }

    private JFreeChart createDelayChartOfDistribution(List<ContainerCloudlet> resultCloudletList) {
        int length = resultCloudletList.size();
        double max_value = 0, min_value = 1000;
        double[] dataArr = new double[length];
        for (int i=0;i<resultCloudletList.size();i++) {
            double current_value = resultCloudletList.get(i).getDelayFactor();
            dataArr[i] = current_value;
            max_value = Math.max(max_value, current_value);
            min_value = Math.min(min_value, current_value);
        }
        HistogramDataset dataset = new HistogramDataset();
        dataset.addSeries("", dataArr, 20, min_value * 0.99, max_value * 1.01);
        JFreeChart chart = ChartFactory.createHistogram(
                "Delay Distribution",
                "Delay Factor",
                "Number",
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
        // renderer.setDefaultToolTipGenerator(new MyXYToolTipGenerator());
        return chart;
    }

    private JScrollPane createResultTable(List<ContainerCloudlet> resultCloudletList) {
        int length = resultCloudletList.size();
        double max_value = 0, min_value = 1000, total_value = 0;
        double[] dataArr = new double[length];
        for (int i=0;i<resultCloudletList.size();i++) {
            double current_value = resultCloudletList.get(i).getDelayFactor();
            dataArr[i] = current_value;
            max_value = Math.max(max_value, current_value);
            min_value = Math.min(min_value, current_value);
            total_value += current_value;
        }
        //定义二维数组作为表格数据
        Object[][] tableData = {
                new Object[]{"Delay Factor" , length, min_value , max_value, total_value / length},
        };
        //定义一维数据作为列标题
        Object[] columnTitle = {"" , "CloudLet number", "Min" , "Max", "Avg"};
        JTable table = new JTable(tableData , columnTitle);
        return new JScrollPane(table);
    }

    private JScrollPane createCostTable(){
        Object[][] tableData = {
                new Object[]{"Cost" , UserSideDatacenter.TotalContainerCost},
        };
        Object[] columnTitle = {"" , "Total cost"};
        JTable table = new JTable(tableData , columnTitle);
        return new JScrollPane(table);
    }


    public static void main(String[] args) {
        try {
            int brokerId = 1;
            int terminated_time_test = 24 * 60 * 60; //24 hours
            int interval_length_test = 20 * 60; // 20 minutes
            int gaussian_mean_test = 1000;
            int gaussian_var_test = 1000;
            int poisson_lambda_test = 10000;
            int mips = 10;
            int CloudPesNumber = 8;
            BaseRequestDistribution self_design_distribution = new BaseRequestDistribution(terminated_time_test, interval_length_test, poisson_lambda_test, gaussian_mean_test, gaussian_var_test);
            List<ContainerCloudlet>  cloudletList_test = self_design_distribution.GetWorkloads();
            for(ContainerCloudlet cl : cloudletList_test){
                cl.setUserId(brokerId);
            }

            EventQueue.invokeLater(() -> {
                //change the default font
                Font font = new Font("Arial", Font.PLAIN, 13);
                Enumeration keys = UIManager.getDefaults().keys();
                while (keys.hasMoreElements()) {
                    Object key = keys.nextElement();
                    Object value = UIManager.get(key);
                    if (value instanceof javax.swing.plaf.FontUIResource) {
                        UIManager.put(key, font);
                    }
                }
                DrawT ex = new DrawT();
                ex.setInputDataPanel(DataCenterSeries);   //重中之重要改
                ex.setResultPanel(cloudletList_test);
                ex.setVisible(true);
            });
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }
    }

}
