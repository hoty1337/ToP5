import javafx.util.Pair;
import org.jfree.chart.*;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.*;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.jfree.chart.ChartFactory.createXYLineChart;

public class Main {
	private static JFreeChart createStairsChart(List<Double> x, List<Double> y) {
		XYSeries series = new XYSeries("");
		for (int i = 0; i < x.size(); i++) {
			series.add(x.get(i), y.get(i));
		}
		XYSeriesCollection dataset = new XYSeriesCollection(series);
		JFreeChart chart = createXYLineChart(
				null, "X", "F(X)", dataset, PlotOrientation.VERTICAL, false, true, false);
		XYPlot plot = chart.getXYPlot();
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setSeriesPaint(0, new Color(132, 0, 50));
		renderer.setSeriesShapesVisible(0, false);
		plot.setRenderer(renderer);
		plot.setBackgroundPaint(Color.WHITE);
		plot.setDomainGridlinePaint(Color.BLACK);
		plot.setRangeGridlinePaint(Color.BLACK);
		return chart;
	}

	private static ChartPanel createHistogramChart(List<Double> bars_x, List<Double> bars_y) {
		XYSeries series = new XYSeries("");
		for (int i = 0; i < bars_x.size(); i++) {
			series.add(bars_x.get(i), bars_y.get(i));
		}

		XYSeriesCollection dataset = new XYSeriesCollection(series);
		JFreeChart chart = ChartFactory.createXYBarChart("Гистограмма приведенных частот", "X", false, "Y", dataset, PlotOrientation.VERTICAL, false, false, false);

		XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.white);
		plot.setRangeGridlinePaint(Color.black);
		plot.setDomainGridlinePaint(Color.black);

		NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
		domainAxis.setTickUnit(new NumberTickUnit(1.0));

		return new ChartPanel(chart);
	}

	public static void main(String[] args) {
		// Начальные параметры
		double[] xs = {1.07, -1.49, 0.11, 0.35, 1.07, -0.26,-0.35, 1.01, 0.28,-1.10,
									 1.59, -0.10, 1.18,-0.73, 0.31, -1.20, 0.73,-0.12,-1.32,-0.26};
		System.out.printf("Исходная выборка: %s%n", Arrays.toString(xs));

		Arrays.sort(xs);
		System.out.printf("Вариационный ряд: %s%n%n", Arrays.toString(xs));

		// Статистический ряд и распределение
		double min_x = xs[0];
		double max_x = xs[xs.length - 1];
		double range_x = max_x - min_x;
		System.out.printf("Минимальный элемент: %s%nМаксимальный элемент: %s%nРазмах выборки: %s%n%n", min_x, max_x, range_x);

		Map<Double, Integer> statisticSeries = IntStream.range(0, xs.length)
				.boxed()
				.collect(Collectors.toMap(i -> xs[i], i -> 1, Integer::sum));
		System.out.printf("Статистический ряд: %s%n", statisticSeries);

		Map<Double, Double> statisticalDistribution = statisticSeries.entrySet()
				.stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() / (double) xs.length));
		System.out.printf("Статистическое распределение выборки: %s%n%n", statisticalDistribution);

		double mathematicalExpectation = statisticalDistribution.entrySet()
				.stream()
				.mapToDouble(e -> e.getKey() * e.getValue())
				.sum();
		double dispersion = statisticSeries.entrySet()
				.stream()
				.mapToDouble(e -> Math.pow(e.getKey() - mathematicalExpectation, 2) * e.getValue())
				.sum() / xs.length;
		double standardDeviation = Math.sqrt(dispersion);
		System.out.printf("Математическое ожидание: %s%nДисперсия: %s%nCреднеквадратичное отклонение: %s%n%n", mathematicalExpectation, dispersion, standardDeviation);

		// Интервальный ряд
		int n = (int) (1 + Math.log(xs.length) / Math.log(2));
		double h = range_x / n;

		Map<Pair<Double, Double>, Integer> intervalSeries = new LinkedHashMap<>();
		double right = min_x - h / 2;
		double left = right + h;

		for (int i = 0; i <= n; i++) {
			right = Math.round(right * 10000.0) / 10000.0;
			left = Math.round(left * 10000.0) / 10000.0;
			intervalSeries.put(new Pair<>(right, left), 0);
			right += h;
			left += h;
		}

		for (Pair<Double, Double> interval : intervalSeries.keySet()) {
			left = interval.getKey();
			right = interval.getValue();
			for (double x : xs) {
				if (left <= x && x < right) {
					intervalSeries.put(interval, intervalSeries.get(interval) + 1);
				}
			}
		}

		System.out.println("Интервальный ряд:");
		for (Map.Entry<Pair<Double, Double>, Integer> entry : intervalSeries.entrySet()) {
			System.out.printf("[%f;%f)\t: %d\n", entry.getKey().getKey(), entry.getKey().getValue(), entry.getValue());
		}
		System.out.println();

		// Эмпирическая функция
		// Создаем массивы для координат точек графика эмпирической функции
		List<Double> graphicX = new ArrayList<>();
		List<Double> graphicY = new ArrayList<>();
// Добавляем начальную точку (min_x - h / 2, 0)
		graphicX.add(min_x - h / 2);
		graphicY.add(0.0);

// Определяем начальное значение правой границы интервала
		right = min_x - h / 2;

// Создаем список для эмпирической функции
		List<Pair<String, Double>> empiricalFunction = new ArrayList<>();
// Добавляем начальное значение (x < right, 0)
		empiricalFunction.add(new Pair<>("x < " + right, 0.0));

// Обходим все интервалы интервального ряда
		for (Map.Entry<Pair<Double, Double>, Integer> entry : intervalSeries.entrySet()) {
			left = entry.getKey().getKey();
			right = entry.getKey().getValue();

			// Если достигли последнего интервала, выходим из цикла
			if (right == max_x + h / 2) {
				break;
			}

			// Вычисляем значение функции в точке right
			double y = (double) entry.getValue() / xs.length + empiricalFunction.get(empiricalFunction.size() - 1).getValue();
			empiricalFunction.add(new Pair<>(String.format("%.4f <= x < %.4f", left, right), y));

			// Добавляем точку (right, y) в массивы для координат точек графика
			graphicX.add(right);
			graphicY.add(Math.round(y * 10000.0) / 10000.0);
		}

// Добавляем конечную точку (max_x + h / 2, 1)
		empiricalFunction.add(new Pair<>("x > " + String.format("%.4f", max_x - h / 2), 1.0));
		graphicX.add(Math.round(max_x + h / 2.0 * 10000.0) / 10000.0);
		graphicY.add(1.0);

// Выводим эмпирическую функцию на экран
		System.out.println("Эмпирическая функция:");
		for (Pair<String, Double> entry : empiricalFunction) {
			System.out.printf("%s: %.4f\n", entry.getKey(), entry.getValue());
		}

		// Графики
// F(x)
		double eps = 0.0001;
		List<Double> stairs_x = new ArrayList<>();
		List<Double> stairs_y = new ArrayList<>();
		stairs_y.add(0.0);
		for (double i : statisticalDistribution.keySet()) {
			stairs_x.add(i);
			stairs_y.add(stairs_y.get(stairs_y.size() - 1));
			stairs_x.add(i + eps);
			stairs_y.add(stairs_y.get(stairs_y.size() - 1) + statisticalDistribution.get(i));
		}
		Collections.sort(stairs_x);
		stairs_x.add(0, stairs_x.get(0) - h / 2);
		stairs_x.add(stairs_x.size(), stairs_x.get(stairs_x.size() - 1) + h / 2);
		stairs_y.add(1.0);

		JFrame frame = new JFrame();
		frame.setTitle("Функция распределения");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 600);
		frame.setLocationRelativeTo(null);

		ChartPanel chartPanel = new ChartPanel(createStairsChart(stairs_x, stairs_y));
		frame.setContentPane(chartPanel);
		frame.pack();
		frame.setVisible(true);

		// Эмпирическая функция
		XYSeries empiricalFunctionSeries = new XYSeries("Эмпирическая функция распределения");
		List<Double> stairsX = new ArrayList<>();
		List<Double> stairsY = new ArrayList<>();
		stairsX.add(min_x - h);
		stairsY.add(0.0);
		stairsX.add(graphicX.get(0));
		stairsY.add(graphicY.get(0));

		for (int i = 1; i < graphicX.size(); i++) {
			stairsX.add(graphicX.get(i - 1));
			stairsY.add(graphicY.get(i - 1));
			empiricalFunctionSeries.add(graphicX.get(i - 1), graphicY.get(i - 1));
			stairsX.add(graphicX.get(i - 1) + eps);
			stairsY.add(graphicY.get(i));
		}

		stairsX.add(stairsX.get(stairsX.size() - 1) + h);
		stairsX.add(stairsX.get(stairsX.size() - 1) + h / 2);
		stairsY.add(1.0);
		stairsY.add(1.0);
		empiricalFunctionSeries.add(stairsX.get(stairsX.size() - 1) - h / 2, stairsY.get(stairsY.size() - 1));

		XYSeries stairsSeries = new XYSeries("");
		for (int i = 0; i < stairsX.size(); i++) {
			stairsSeries.add(stairsX.get(i), stairsY.get(i));
		}
		XYDataset dataset = new XYSeriesCollection();
		JFreeChart chart = createXYLineChart("Эмпирическая функция распределения", "X", "F*(X)", dataset, PlotOrientation.VERTICAL, true, true, false);
		XYPlot plot = chart.getXYPlot();
		plot.setDataset(0, new XYSeriesCollection(stairsSeries));
		plot.setDataset(1, new XYSeriesCollection(empiricalFunctionSeries));
		plot.setBackgroundPaint(Color.WHITE);
		plot.setRangeGridlinePaint(Color.BLACK);
		plot.setDomainGridlinePaint(Color.BLACK);
		plot.setRenderer(new XYLineAndShapeRenderer(true, true));
		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
		renderer.setSeriesStroke(0, new BasicStroke(2.0f));
		renderer.setSeriesPaint(0, new Color(150, 0, 50));
		renderer.setSeriesStroke(1, new BasicStroke(3.0f));
		renderer.setSeriesPaint(1, new Color(229, 149, 0));

		ChartPanel panel = new ChartPanel(chart);
		panel.setPreferredSize(new java.awt.Dimension(800, 600));
		panel.setMouseZoomable(true);
		frame = new JFrame("Эмпирическая функция распределения");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(panel);
		frame.pack();
		frame.setVisible(true);

		// Гистограмма частот
		stairsX.subList(0, 3).clear();
		stairsX.remove(stairsX.size() - 1);
		stairsY.subList(0, 3).clear();
		stairsY.remove(stairsY.size() - 1);

		// Изменяем значения элементов в массиве stairs_y
		for (int i = stairsY.size() - 1; i > 1; i--) {
			stairsY.set(i, stairsY.get(i) - stairsY.get(i - 2));
		}

		// Создаем новый массив stairs_x
		ArrayList<Double> new_stairs_x = new ArrayList<>();
		h = stairsX.get(1) - stairsX.get(0);
		for (int i = 0; i < stairsX.size(); i += 2) {
			new_stairs_x.add(stairsX.get(i) + h / 2);
		}

		// Создаем новый массив stairs_y
		ArrayList<Double> new_stairs_y = new ArrayList<>();
		for (int i = 0; i < stairsY.size(); i += 2) {
			new_stairs_y.add(stairsY.get(i));
		}

		frame = new JFrame("Гистограмма приведенных частот");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		panel = new ChartPanel(chart);
		panel.setBackground(Color.white);
		panel.setLayout(new BorderLayout());

		chartPanel = createHistogramChart(new_stairs_x, new_stairs_y);
		panel.add(chartPanel, BorderLayout.CENTER);

		frame.setContentPane(panel);
		frame.pack();
		frame.setVisible(true);

		// Полигон частот
		frame = new JFrame("Полигон приведенных частот");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());

		XYSeries series = new XYSeries("Частоты");
		for (int i = 0; i < stairsX.size(); i+=2) {
			series.add(stairsX.get(i), stairsY.get(i));
		}

		dataset = new XYSeriesCollection(series);
		chart = ChartFactory.createXYLineChart(
				"Полигон приведенных частот",
				"Классы",
				"Частоты",
				dataset,
				PlotOrientation.VERTICAL,
				true,
				true,
				false
		);

		plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setRangeGridlinePaint(Color.BLACK);
		plot.setDomainGridlinePaint(Color.BLACK);
		plot.setRenderer(new XYLineAndShapeRenderer(true, true));
		renderer = (XYLineAndShapeRenderer) plot.getRenderer();
		renderer.setSeriesStroke(0, new BasicStroke(2.0f));
		renderer.setSeriesPaint(0, new Color(150, 0, 50));

		panel = new ChartPanel(chart);
		panel.setPreferredSize(new java.awt.Dimension(800, 600));
		panel.setMouseZoomable(true);
		frame = new JFrame("Полигон приведенных частот");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(panel);
		frame.pack();
		frame.setVisible(true);
	}
}
