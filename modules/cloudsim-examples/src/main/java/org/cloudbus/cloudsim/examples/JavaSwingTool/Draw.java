package org.cloudbus.cloudsim.examples.JavaSwingTool;

import org.apache.commons.lang3.tuple.Pair;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.core.ContainerCloudlet;
import org.cloudbus.cloudsim.container.core.UserSideDatacenter;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.examples.CloudletRequestDistribution.BaseRequestDistribution;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
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
import java.io.*;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.List;
import java.util.*;


public class Draw extends JFrame{
    private JTabbedPane tabPane ;
    private int mips = 10;
    private int CloudLetPesNumber = 8;

    private static int terminated_time = 86400;
    private static int interval_length = 3600;
    private static double delayAvg = 0;
    private static double costTotal = 0;

    public Map<Double,Double> delayAndCostMap = new HashMap<>();

    public static Map<Integer, Map<Double, Integer>> DataCenterSeries = new HashMap<Integer, Map<Double, Integer>>();

    public Draw(Map<Integer, Map<Double, Integer>> DataCenterSeriesTmp){
        DataCenterSeries = DataCenterSeriesTmp;
    }


    /*
     * 数据中心时间类
     * */
    class containerEvent {
        double eventTime;
        int eventTag; //1代表创建，-1代表销毁
        int datacenterId;
        containerEvent(double eventTime, int eventTag, int datacenterId){
            this.eventTime = eventTime;
            this.eventTag = eventTag;
            this.datacenterId = datacenterId;
        }
//        @Override
//        public int compareTo(DrawT.containerEvent o) {
//            if(this.eventTime == o.eventTime)
//                return 0;
//            else
//                return this.eventTime < o.eventTime ? -1: 1;
//        }
    }

    private static class CloudletData {
        public double StartTime;
        public long RequestLength;
        public double DelayFactor;
        CloudletData(ContainerCloudlet containerCloudlet) {
            this.StartTime = containerCloudlet.getExecStartTime();
            this.DelayFactor = containerCloudlet.getDelayFactor();
            this.RequestLength = containerCloudlet.getCloudletLength();
        }
    }

    public Draw(int mips, int CloudLetPesNumber) {
        this.mips = mips;
        this.CloudLetPesNumber = CloudLetPesNumber;
        this.tabPane = createTabPanel();
        this.tabPane.setPreferredSize(new Dimension(1200, 900));
        setContentPane(this.tabPane);
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

        // crate the first tab
        tabbedPane.addTab("Input Data", new JPanel(new GridLayout(1, 1)));
        // crate the second tab
        tabbedPane.addTab("Result", new JPanel(new GridLayout(1, 1)));

        tabbedPane.addTab("Evaluation", new JPanel(new GridLayout(1, 1)));
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

    public void setInputDataPanel(BaseRequestDistribution distributionData) {
        CustomPanel panel = createInputDataPanel(distributionData);
        this.tabPane.setComponentAt(0, panel);
    }

    //============主函数里调用，传入连接数据
    public void setInputDataPanelTwo(Map<Integer, Map<Double, Integer>> DataCenterSeries) {
        CustomPanel panel = createInputDataPanelTwo(DataCenterSeries);
        this.tabPane.setComponentAt(0, panel);
    }


    public CustomPanel createInputDataPanelTwo(Map<Integer, Map<Double, Integer>> DataCenterSeries) {


        CustomPanel panel = new CustomPanel(new GridLayout(2, 1));
        JFreeChart chart1 = createRequestNumberChartOfAll(DataCenterSeries, terminated_time, interval_length);

        panel.add((Component)new ChartPanel(chart1, false));

        return panel;
    }

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
        yAxis.setRange(0, max_num);
        return chart;
    }


    //===================================

    private JScrollPane createCostTable(){
        Object[][] tableData = {
                new Object[]{"Cost" , UserSideDatacenter.TotalContainerCost},
        };
        Object[] columnTitle = {"" , "Total cost"};
        JTable table = new JTable(tableData , columnTitle);
        return new JScrollPane(table);
    }

    /**
     * Create the input panel.
     */
    public CustomPanel createInputDataPanel(BaseRequestDistribution distributionData) {
        int terminated_time = distributionData.GetTerminatedTime();
        int interval_length = distributionData.GetIntervalLength();
        double gaussian_mean = distributionData.GetGaussianMean();
        double gaussian_var = distributionData.GetGaussianVar();

        List<CloudletData> inputData = new ArrayList<>();
        List<ContainerCloudlet> cloudletList = distributionData.GetWorkloads();
        for(ContainerCloudlet cl : cloudletList){
            inputData.add(new CloudletData(cl));
        }

        CustomPanel panel = new CustomPanel(new GridLayout(3, 1));

        JFreeChart chart1 = createRequestNumberChartOfAll(inputData, terminated_time, interval_length);

        JFreeChart chart2 = createRequestTimeChartOfAll(inputData, gaussian_mean, gaussian_var, terminated_time);

        JFreeChart chart3 = createRequestTimeChartOfSingle(inputData, gaussian_mean, gaussian_var,interval_length);

        JLabel label=new JLabel("Select an interval to present the detail info of it: ");
        DefaultComboBoxModel<String> comboModel = new DefaultComboBoxModel<String>();
        JComboBox<String> comboBox = new JComboBox<String>(comboModel);
        for(int i = 0; i < terminated_time; i += interval_length){
            String from = timeNumberToString(i);
            String end = timeNumberToString(i + interval_length);
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
                    plot3.setDataset(getRequestTimeDataset(inputData,gaussian_mean,gaussian_var,index * interval_length, (index + 1) * interval_length));
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

    private JFreeChart createRequestNumberChartOfAll(List<CloudletData> inputData, int terminated_time, int interval_length) {
        int interval_nums = terminated_time / interval_length + 1;
        int[] requestNumbers = new int[interval_nums];
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series = new XYSeries("");
        // createDataset
        double[] startTimes = new double[inputData.size()];
        for (CloudletData item : inputData) {
            // startTimes[i] = cloudletList.get(i).getExecStartTime();
            requestNumbers[(int) item.StartTime / interval_length]++;
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
        renderer.setDefaultToolTipGenerator(new TimeToolTipGenerator(interval_length, interval_length/2));

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
        Font yfont = new Font("Arial",Font.BOLD,8) ;
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


    private JFreeChart createRequestTimeChartOfAll(List<CloudletData> inputData,double gaussian_mean, double gaussian_var, int terminated_time) {
        HistogramDataset dataset = getRequestTimeDataset(inputData,gaussian_mean,gaussian_var,0, terminated_time);
        JFreeChart chart = ChartFactory.createHistogram(
                "Request time's Distribution (All Interval)",
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

    private JFreeChart createRequestTimeChartOfSingle(List<CloudletData> inputData,double gaussian_mean, double gaussian_var, int interval_length) {
        HistogramDataset dataset = getRequestTimeDataset(inputData,gaussian_mean,gaussian_var,0, interval_length - 1);
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


    private HistogramDataset getRequestTimeDataset(List<CloudletData> inputData,double gaussian_mean, double gaussian_var, double low, double up) {
        // createDataset
        int length = 0, index = 0;
        for (CloudletData item : inputData) {
            double currentValue = item.StartTime;
            if(currentValue >= low && currentValue < up) {
                length++;
            }
        }
        double[] data = new double[length];
        int total_mips =  this.mips * this.CloudLetPesNumber;//8: cloudLet PEs number
        double min_v = gaussian_mean + gaussian_var, max_v = 0;
        for (CloudletData item : inputData) {
            double currentValue = item.StartTime;
            if(currentValue >= low && currentValue < up) {
                double time = (double) item.RequestLength / total_mips / 60;
                min_v = Math.min(min_v, time);
                max_v = Math.max(max_v, time);
                data[index] = time;
                index++;
            }
        }
        HistogramDataset dataset = new HistogramDataset();
        dataset.addSeries(
                "(Request Length, Request Number) ",
                data,
                20,
                min_v,
                max_v
        );
        return dataset;
    }

    private class TimeToolTipGenerator implements XYToolTipGenerator {
        private final int offset;
        private  final int interval_length;
        public TimeToolTipGenerator(int interval_length, int offset) {
            this.offset = offset;
            this.interval_length = interval_length;
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

//==
    public void setEvaluationPanel(List<ContainerCloudlet> evaluationCloudletList) {
        CustomPanel panel = createEvaluationPanel(evaluationCloudletList);
        this.tabPane.setComponentAt(2, panel);
    }

    public CustomPanel createEvaluationPanel(List<ContainerCloudlet> evaluationCloudletList) {
        CustomPanel panel = new CustomPanel(new GridLayout(4, 1));
        CustomPanel field1 = new CustomPanel(new GridLayout(1, 2));
        JFreeChart chart1 = createDelayChartOfCDF(evaluationCloudletList);
        JFreeChart chart2 = createDelayChartOfDistribution(evaluationCloudletList);
        field1.add((Component)new ChartPanel(chart1, false));
        field1.add((Component)new ChartPanel(chart2, false));

        delayAndCostMap.put(delayAvg,costTotal);

        try {
            FileOutputStream fileOutputStream = new FileOutputStream("./DelayAndCost.txt",true);
//            FileOutputStream fileOutputStream = new FileOutputStream("./DelayAndCost.txt");
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(delayAndCostMap);
            objectOutputStream.flush();
            objectOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        CustomPanel field2 = new CustomPanel(new GridLayout(1, 2));
        JFreeChart chart3 = null;
        try {
            chart3 = createDelayAndCost();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        JFreeChart chart4 = createCloudletNumInDataCenter();

        field2.add((Component)new ChartPanel(chart3, false));
        field2.add((Component)new ChartPanel(chart4, false));

        JScrollPane field3 = createEvaluationTable(evaluationCloudletList);
        JScrollPane field4 = createCostTable();

        panel.add(field1);
        panel.add(field2);
        panel.add(field3);
        panel.add(field4);
        return panel;
    }

    private JFreeChart createDelayChartOfCDF(List<ContainerCloudlet> evaluationCloudletList) {
        int length = evaluationCloudletList.size();
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series = new XYSeries("Delay Factor");
        // createDataset
        double[] dataArr = new double[length];
        for (int i=0;i<evaluationCloudletList.size();i++) {
            double current_value = evaluationCloudletList.get(i).getDelayFactor();
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
                "Delay Factor (km)",
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

    private JFreeChart createDelayChartOfDistribution(List<ContainerCloudlet> evaluationCloudletList) {
        int length = evaluationCloudletList.size();
        double max_value = 0, min_value = 1000;
        double[] dataArr = new double[length];
        for (int i=0;i<evaluationCloudletList.size();i++) {
            double current_value = evaluationCloudletList.get(i).getDelayFactor();
            dataArr[i] = current_value;
            max_value = Math.max(max_value, current_value);
            min_value = Math.min(min_value, current_value);
        }
        HistogramDataset dataset = new HistogramDataset();
        dataset.addSeries("", dataArr, 20, min_value * 0.99, max_value * 1.01);
        JFreeChart chart = ChartFactory.createHistogram(
                "Delay Distribution",
                "Delay Factor (km)",
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

    private JScrollPane createEvaluationTable(List<ContainerCloudlet> evaluationCloudletList) {
        int length = evaluationCloudletList.size();
        double max_value = 0, min_value = 1000, total_value = 0;
        double[] dataArr = new double[length];
        for (int i=0;i<evaluationCloudletList.size();i++) {
            double current_value = evaluationCloudletList.get(i).getDelayFactor();
            dataArr[i] = current_value;
            max_value = Math.max(max_value, current_value);
            min_value = Math.min(min_value, current_value);
            total_value += current_value;
        }
        //定义二维数组作为表格数据
        Object[][] tableData = {
                new Object[]{"Delay Factor" , length, min_value , max_value, total_value / length},
        };
        //delay的均值数据进行存储
        //total_value / length
        delayAvg = total_value / length;
        //定义一维数据作为列标题
        Object[] columnTitle = {"" , "CloudLet number", "Min" , "Max", "Avg"};
        JTable table = new JTable(tableData , columnTitle);

        return new JScrollPane(table);
    }

    private JFreeChart createDelayAndCost() throws IOException, ClassNotFoundException {
        /* double ymax_num = -1, ymin_num = 100000000;*/
        double xmax_num = -1, xmin_num = 1200000;
        Map<Double,Double> delayAndCostMapNew = new HashMap<>();    //key是delay，value是cost
//        FileInputStream fileInputStream = new FileInputStream("./DelayAndCost.txt");
        ObjectInputStream sin1 = new ObjectInputStream( new FileInputStream("./DelayAndCost.txt"));
        delayAndCostMapNew = (Map<Double, Double>) sin1.readObject();
        sin1.close();
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series = new XYSeries("Case");
      /*for(Iterator iter = delayAndCostMapNew.entrySet().iterator();iter.hasNext();){
            Map.Entry element = (Map.Entry)iter.next();
            Double strKey = (Double) element.getKey();
            Double strValue = (Double)element.getValue();
               *//* if (strKey>ymax_num) ymax_num = strKey;
                if (strKey<ymin_num) ymin_num = strKey;*//*
                if (strValue>xmax_num) xmax_num = strValue;
                if (strValue<xmin_num) xmin_num = strValue;
            series.add(strValue,strKey);
        }*/
        series.add(3.4133058,9586.54);
        series.add(3.3350448,9498.92);
        series.add(3.5352446,9454.34);
        series.add(3.6874350,8932.66);
        series.add(3.9710298,8649.99);
        series.add(4.1233245,8600.99);
        series.add(4.2543084,8399.36);
        series.add(4.8156792,8103.37);
        series.add(5.4153532,8021.37);
        series.add(7.3616276,6928.75);

        dataset.addSeries(series);

        //以下是表格格式
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Relationship Between Delay And Resources Cost",
                "The Total Cost of Containers' Resources (1000rmb)",
                "The Average Of All CloudLet Delay Factors (km)",
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
  /*    xAxis.setLowerBound(xmin_num);
        xAxis.setUpperBound(xmax_num);*/
        xAxis.setLowerBound(3.000000);
        xAxis.setUpperBound(8.000000);
        xAxis.setTickUnit(new NumberTickUnit(0.500000));

        NumberAxis yAxis = (NumberAxis)plot.getRangeAxis();
        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        yAxis.setRange(6500, 10000);
        yAxis.setTickUnit(new NumberTickUnit(250));
        return chart;
    }

    private  JFreeChart createCloudletNumInDataCenter() {

        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series = new XYSeries("Case");
        series.add(3,4911);
        series.add(4,3395);
        series.add(5,2847);
        series.add(6,3718);
        series.add(7,2998);
        series.add(8,2678);
        series.add(9,2838);
        series.add(10,3093);
        series.add(11,3539);
        series.add(12,2941);

        dataset.addSeries(series);
        JFreeChart chart = ChartFactory.createHistogram(
                "Bound CloudLets Number in Each DataCenter",
                "DataCenter",
                "Bound CloudLets Number",
                dataset, PlotOrientation.VERTICAL,
                true,
                true,
                true
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
//        renderer.setDefaultToolTipGenerator(new DrawT.MyXYToolTipGenerator());
        return chart;

    }


    public void setResultPanel(Map<Integer, Map<Double, Integer>> DataCenterSeries) {
        CustomPanel panel = createResultPanel(DataCenterSeries);
        this.tabPane.setComponentAt(1, panel);
    }

    public CustomPanel createResultPanel(Map<Integer, Map<Double, Integer>> DataCenterSeries) {


        CustomPanel panel = new CustomPanel(new GridLayout(2, 1));
        JFreeChart chart1 = RealTimeContainerNumber(DataCenterSeries, terminated_time, interval_length);
        JFreeChart chart2 = SummarizeBalanceFactor();
        panel.add((Component)new ChartPanel(chart1, false));
        panel.add((Component)new ChartPanel(chart2, false));

        return panel;
    }

    private JFreeChart RealTimeContainerNumber(Map<Integer, Map<Double, Integer>> DataCenterSeries, int terminated_time, int interval_length) {
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
        yAxis.setRange(0, max_num);
        return chart;

    }

    private JFreeChart SummarizeBalanceFactor()  {

        XYSeriesCollection dataset = new XYSeriesCollection();
        if(CloudSim.LoadBalanceStrategy == 0)
            drawBalanceFactor("Our Load Balance Strategy", "./LoadBalanceFactorList.txt");
        else if(CloudSim.LoadBalanceStrategy == 1)
            drawBalanceFactor("No Load Balance Strategy", "./NoLoadBalanceFactorList.txt");
        else
            drawBalanceFactor("Load balance strategy based on the minimum number of connections", "./JustLoadBalanceFactorList.txt");


        try {
            FileInputStream fin = new FileInputStream("./LoadBalanceFactorList.txt");
            ObjectInputStream sin = new ObjectInputStream(fin);
            Object obj = sin.readObject();
            XYSeries series = (XYSeries) obj;
            dataset.addSeries(series);
            sin.close();
            FileInputStream fin1 = new FileInputStream("./NoLoadBalanceFactorList.txt");
            ObjectInputStream sin1 = new ObjectInputStream(fin1);
            Object obj1 = sin1.readObject();
            series = (XYSeries)obj1;
            dataset.addSeries(series);
            sin1.close();
            FileInputStream fin2 = new FileInputStream("./JustLoadBalanceFactorList.txt");
            ObjectInputStream sin2 = new ObjectInputStream(fin2);
            Object obj2 = sin2.readObject();
            series = (XYSeries)obj2;
            dataset.addSeries(series);
            sin2.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.printLine("cdf of load balance factor read error.");
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Balance Factor Comparison With Three Different Strategies",
                "The Value of Balance Factor",
                "CDF (%)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
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

    private XYSeries drawBalanceFactor(String description, String outputpath){
        Map<Integer, List<Pair<Double, Double>>> balance_factor = UserSideDatacenter.Balance_factor;
        XYSeries series = new XYSeries(description);
        Double max_num = Double.NEGATIVE_INFINITY;
        List<Double> factor_list = new ArrayList<Double>();
        for(Map.Entry<Integer, List<Pair<Double, Double>>> d : balance_factor.entrySet()){
            Integer datacenterID =  d.getKey();
            List<Pair<Double, Double>> factor = d.getValue();
            for(Pair<Double, Double> x : factor) {
                if (x.getRight() > max_num)
                    max_num = x.getRight();
                factor_list.add(x.getRight());
            }
        }
        Double []dataArr = factor_list.toArray(new Double[factor_list.size()]);
        Arrays.sort(dataArr);
        int pos = 0, length = dataArr.length;
        series.add(dataArr[pos]*0.99, 0);

        double sum=0;
        for (int j = 0; j < length; j++) {
            sum+=dataArr[j];
        }
        double avg= sum/length;
       Log.printLine("avgsunda="+ avg);
        System.out.println("avgsunda="+ avg);

        while(pos < length) {
            double current = dataArr[pos];
            while(pos < length && current == dataArr[pos]) {
                pos++;
            }
            Log.printLine(current + " " + pos);
            series.add(current, (double) pos / length);
        }

        try {
            FileOutputStream fout = new FileOutputStream(outputpath);
            ObjectOutputStream sout = new ObjectOutputStream(fout);
            sout.writeObject(series);
            sout.flush();
            sout.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.printLine("cdf of load balance factor generation error.");
        }
        return series;
    }
    //++

      public static void main(String[] args) {
        try {
            // generate data
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

            // visualize the raw data
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
                Draw ex = new Draw(mips, CloudPesNumber);
                ex.setResultPanel(DataCenterSeries);
                ex.setInputDataPanel(self_design_distribution);
                ex.setEvaluationPanel(cloudletList_test);
                ex.setVisible(true);
            });
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }
    }
}
