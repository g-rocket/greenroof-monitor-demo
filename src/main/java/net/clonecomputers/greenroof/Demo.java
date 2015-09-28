package net.clonecomputers.greenroof;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import info.monitorenter.gui.chart.*;
import info.monitorenter.gui.chart.traces.*;
import jssc.*;

public class Demo extends JPanel implements Runnable {
	private final Chart2D mainChart;
	private final ITrace2D mainTrace;
	private final Chart2D scrollingChart;
	private final ITrace2D scrollingTrace;
	private final SerialPort port;

	public static void main(String[] args) {
		JFrame window = new JFrame("Greenroof monitor demo");
		Demo app = new Demo();
		window.setContentPane(app);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setVisible(true);
		app.run();
	}
	
	public Demo() {
		mainChart = new Chart2D();
		scrollingChart = new Chart2D();
		this.setLayout(new GridLayout(2,1));
		add(mainChart);
		add(scrollingChart);
		mainTrace = new Trace2DSimple("Data");
		mainChart.addTrace(mainTrace);
		scrollingTrace = new Trace2DLtd(200, "Data (last 20s)");
		scrollingChart.addTrace(scrollingTrace);
		System.out.println(Arrays.toString(SerialPortList.getPortNames()));
		String portName = null;
		for(String name: SerialPortList.getPortNames()) {
			if(!name.contains("Bluetooth")) {
				portName = name;
				break;
			}
		}
		port = new SerialPort(portName);
		try {
			port.openPort();
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				port.closePort();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}));
	}
	
	private void addPoint(double y) {
		mainTrace.addPoint(System.currentTimeMillis(), y);
		scrollingTrace.addPoint(System.currentTimeMillis(), y);
	}

	@Override
	public void run() {
		while(true) {
			short data = 0;
			try {
				port.writeInt(0x0f);
				byte[] bytes = port.readBytes(2, 1000);
				data |= bytes[0] << 0;
				data |= bytes[1] << 6;
				addPoint(processData(data));
			} catch (SerialPortException | SerialPortTimeoutException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	private static final double etape_m = -1/40D, etape_b = 16;
	private static final double ord_b = .0908, ord_e = 5.1327;
	private static final double other_resistor = 560; // ohms
	private static final double v_in = 5; // volts
	
	public static double processData(short rawData){
		double v_out = rawData*(v_in/1024D); // volts
		double resistance = (other_resistor/((v_in/v_out) - 1)); // ohms
		double levelI = etape_m*resistance + etape_b; // inches
		double levelF = levelI/12D; // feet
		double rate = ord_b*Math.exp(levelF*ord_e); //   gallons / minuite
		System.out.printf("%d,%.2f,%.2f,%.2f,%.2f,%.2f\n",
				rawData,v_out,resistance,levelI,levelF,rate);
		return rate;
	}

}
