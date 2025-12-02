package whitetail.timing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FrameTimingAnalyzer extends JFrame {

    // Configuration variables - hardcoded for now
    private static final float MIN_Y_AXIS = 0.0f;      // Minimum ms for Y axis
    private static final float MAX_Y_AXIS = 20.0f;     // Maximum ms for Y axis
    private static final String SET1_STR = "totalMs";   // First data set identifier
    private static final String SET2_STR = "swapMs";    // Second data set identifier

    // Data storage
    private List<Float> totalMsList;
    private List<Float> swapMsList;

    // GUI Components
    private JButton pickFileButton;
    private JPanel mainPanel;
    private JPanel graphPanel;

    public FrameTimingAnalyzer() {
        totalMsList = new ArrayList<Float>();
        swapMsList = new ArrayList<Float>();
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("Whitetail Frame Timing Analyzer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create main panel
        mainPanel = new JPanel(new BorderLayout());

        // Create top control panel with button
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pickFileButton = new JButton("Pick File");
        pickFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onPickFile();
            }
        });
        controlPanel.add(pickFileButton);

        // Create graph panel (placeholder for now - will draw graph here later)
        graphPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (totalMsList.isEmpty() && swapMsList.isEmpty()) {
                    drawPlaceholder(g);
                } else {
                    drawGraph(g);
                }
            }
        };
        graphPanel.setBackground(Color.WHITE);
        graphPanel.setPreferredSize(new Dimension(800, 400));
        graphPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // Assemble the layout
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(graphPanel, BorderLayout.CENTER);

        // Add info panel at bottom
        JPanel infoPanel = new JPanel(new GridLayout(2, 2, 10, 5));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        infoPanel.add(new JLabel("Y-Axis Range: " + MIN_Y_AXIS + " - " + MAX_Y_AXIS + " ms"));
        infoPanel.add(new JLabel("Data Set 1 (red): " + SET1_STR));
        infoPanel.add(new JLabel(""));  // Empty cell for alignment
        infoPanel.add(new JLabel("Data Set 2 (blue): " + SET2_STR));

        mainPanel.add(infoPanel, BorderLayout.SOUTH);

        // Add main panel to frame
        add(mainPanel);

        // Set window size and center it
        setSize(900, 600);
        setLocationRelativeTo(null);
    }

    private void drawPlaceholder(Graphics g) {
        // Draw a simple placeholder to show where the graph will be
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int width = graphPanel.getWidth();
        int height = graphPanel.getHeight();

        // Draw axes
        g2d.setColor(Color.BLACK);
        int margin = 40;
        // Y axis
        g2d.drawLine(margin, margin, margin, height - margin);
        // X axis
        g2d.drawLine(margin, height - margin, width - margin, height - margin);

        // Draw message
        g2d.setColor(Color.LIGHT_GRAY);
        if (totalMsList == null || totalMsList.isEmpty()) {
            g2d.drawString("Frame timing graph will appear here",
                    width / 2 - 100, height / 2);
        } else {
            g2d.drawString(totalMsList.size() + " frames loaded - ready to graph",
                    width / 2 - 100, height / 2);
        }
    }

    private void drawGraph(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int width = graphPanel.getWidth();
        int height = graphPanel.getHeight();
        int leftMargin = 50;
        int rightMargin = 20;
        int topMargin = 20;
        int bottomMargin = 40;

        int graphWidth = width - leftMargin - rightMargin;
        int graphHeight = height - topMargin - bottomMargin;

        // Clear background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Draw grid lines first (so they appear behind the data)
        g2d.setColor(new Color(230, 230, 230));
        int gridLines = 9;
        for (int i = 0; i <= gridLines; i++) {
            // Horizontal grid lines
            int y = topMargin + (graphHeight * i / gridLines);
            g2d.drawLine(leftMargin, y, leftMargin + graphWidth, y);

            // Vertical grid lines (every 100 frames)
            if (totalMsList.size() > 0) {
                int frameStep = Math.max(1, totalMsList.size() / 10);
                for (int frame = 0; frame < totalMsList.size(); frame += frameStep) {
                    int x = leftMargin + (frame * graphWidth / totalMsList.size());
                    g2d.drawLine(x, topMargin, x, topMargin + graphHeight);
                }
            }
        }

        // Draw axes
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        // Y axis
        g2d.drawLine(leftMargin, topMargin, leftMargin, topMargin + graphHeight);
        // X axis
        g2d.drawLine(leftMargin, topMargin + graphHeight,
                leftMargin + graphWidth, topMargin + graphHeight);

        // Draw Y axis labels
        g2d.setColor(Color.BLACK);
        for (int i = 0; i <= 9; i++) {
            float value = MIN_Y_AXIS + (MAX_Y_AXIS - MIN_Y_AXIS) * (1.0f - i / 9.0f);
            int y = topMargin + (graphHeight * i / 9);
            g2d.drawString(String.format("%.1f", value), leftMargin - 35, y + 5);
            // Add tick marks
            g2d.drawLine(leftMargin - 5, y, leftMargin, y);
        }

        // Draw Y axis title
        g2d.drawString("Time (ms)", 5, height / 2);

        // Draw X axis labels
        if (totalMsList.size() > 0) {
            int labelCount = Math.min(10, totalMsList.size());
            for (int i = 0; i <= labelCount; i++) {
                int frame = (totalMsList.size() - 1) * i / labelCount;
                int x = leftMargin + (frame * graphWidth / totalMsList.size());
                g2d.drawString(String.valueOf(frame), x - 10, topMargin + graphHeight + 20);
            }
        }

        // Draw X axis title
        g2d.drawString("Frame", width / 2 - 20, height - 5);

        // Draw the data lines
        g2d.setStroke(new BasicStroke(1.5f));

        // Draw totalMs in red
        if (!totalMsList.isEmpty()) {
            g2d.setColor(Color.RED);
            drawDataLine(g2d, totalMsList, leftMargin, topMargin, graphWidth, graphHeight);
        }

        // Draw swapMs in blue
        if (!swapMsList.isEmpty()) {
            g2d.setColor(Color.BLUE);
            drawDataLine(g2d, swapMsList, leftMargin, topMargin, graphWidth, graphHeight);
        }

        // Draw legend
        int legendX = leftMargin + graphWidth - 120;
        int legendY = topMargin + 10;
        g2d.setColor(Color.WHITE);
        g2d.fillRect(legendX - 5, legendY - 5, 110, 45);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(legendX - 5, legendY - 5, 110, 45);

        g2d.setColor(Color.RED);
        g2d.drawLine(legendX, legendY + 7, legendX + 20, legendY + 7);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Total Time", legendX + 25, legendY + 12);

        g2d.setColor(Color.BLUE);
        g2d.drawLine(legendX, legendY + 22, legendX + 20, legendY + 22);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Swap Time", legendX + 25, legendY + 27);

        // Draw statistics
        if (!totalMsList.isEmpty()) {
            drawStatistics(g2d, leftMargin + 10, topMargin + 10);
        }
    }

    private void drawDataLine(Graphics2D g2d, List<Float> data,
                              int leftMargin, int topMargin,
                              int graphWidth, int graphHeight) {
        if (data.size() < 2) return;

        int prevX = leftMargin;
        int prevY = topMargin + graphHeight -
                (int)((data.get(0) - MIN_Y_AXIS) / (MAX_Y_AXIS - MIN_Y_AXIS) * graphHeight);

        for (int i = 1; i < data.size(); i++) {
            int x = leftMargin + (i * graphWidth / (data.size() - 1));
            float value = data.get(i);

            // Clamp value to axis range for display
            value = Math.max(MIN_Y_AXIS, Math.min(MAX_Y_AXIS, value));

            int y = topMargin + graphHeight -
                    (int)((value - MIN_Y_AXIS) / (MAX_Y_AXIS - MIN_Y_AXIS) * graphHeight);

            g2d.drawLine(prevX, prevY, x, y);

            // Highlight outliers (values near max or significantly above average)
            if (value > MAX_Y_AXIS * 0.9f) {
                Color oldColor = g2d.getColor();
                g2d.setColor(Color.ORANGE);
                g2d.fillOval(x - 3, y - 3, 6, 6);
                g2d.setColor(oldColor);
            }

            prevX = x;
            prevY = y;
        }
    }

    private void drawStatistics(Graphics2D g2d, int x, int y) {
        g2d.setColor(Color.WHITE);
        g2d.fillRect(x - 5, y - 5, 150, 80);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x - 5, y - 5, 150, 80);

        g2d.setFont(new Font("Monospaced", Font.PLAIN, 11));

        // Calculate stats for total time
        float avgTotal = calculateAverage(totalMsList);
        float minTotal = findMin(totalMsList);
        float maxTotal = findMax(totalMsList);

        // Calculate stats for swap time
        float avgSwap = calculateAverage(swapMsList);
        float minSwap = findMin(swapMsList);
        float maxSwap = findMax(swapMsList);

        g2d.drawString("Statistics:", x, y + 12);
        g2d.drawString(String.format("Total: %.2f ms avg", avgTotal), x, y + 28);
        g2d.drawString(String.format("  %.2f-%.2f range", minTotal, maxTotal), x, y + 42);
        g2d.drawString(String.format("Swap: %.2f ms avg", avgSwap), x, y + 58);
        g2d.drawString(String.format("  %.2f-%.2f range", minSwap, maxSwap), x, y + 72);
    }

    private float findMin(List<Float> values) {
        if (values.isEmpty()) return 0.0f;
        float min = values.get(0);
        for (Float value : values) {
            if (value < min) min = value;
        }
        return min;
    }

    private float findMax(List<Float> values) {
        if (values.isEmpty()) return 0.0f;
        float max = values.get(0);
        for (Float value : values) {
            if (value > max) max = value;
        }
        return max;
    }

    private void onPickFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Log File");
        fileChooser.setCurrentDirectory(new File("."));

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            parseLogFile(selectedFile);
        }
    }

    private void parseLogFile(File file) {
        // Clear existing data
        totalMsList.clear();
        swapMsList.clear();

        BufferedReader reader = null;
        int linesRead = 0;
        int entriesParsed = 0;

        try {
            reader = new BufferedReader(new FileReader(file));
            String line;

            while ((line = reader.readLine()) != null) {
                linesRead++;

                // Check if line contains our timing data
                if (line.startsWith(SET1_STR + ",")) {
                    try {
                        String valueStr = line.substring(SET1_STR.length() + 1);
                        float value = Float.parseFloat(valueStr);
                        totalMsList.add(value);
                        entriesParsed++;
                    } catch (NumberFormatException e) {
                        System.err.println("Failed to parse totalMs value at line " + linesRead + ": " + line);
                    }
                } else if (line.startsWith(SET2_STR + ",")) {
                    try {
                        String valueStr = line.substring(SET2_STR.length() + 1);
                        float value = Float.parseFloat(valueStr);
                        swapMsList.add(value);
                        entriesParsed++;
                    } catch (NumberFormatException e) {
                        System.err.println("Failed to parse swapMs value at line " + linesRead + ": " + line);
                    }
                }
                // Ignore all other lines
            }

            // Check if we have matching pairs
            if (totalMsList.size() != swapMsList.size()) {
                JOptionPane.showMessageDialog(this,
                        "Warning: Mismatched data counts!\n" +
                                "Total entries: " + totalMsList.size() + "\n" +
                                "Swap entries: " + swapMsList.size(),
                        "Data Mismatch",
                        JOptionPane.WARNING_MESSAGE);
            }

            // Report results
            String message = String.format(
                    "File loaded successfully!\n" +
                            "Lines read: %d\n" +
                            "Entries parsed: %d\n" +
                            "Frames detected: %d",
                    linesRead, entriesParsed, Math.min(totalMsList.size(), swapMsList.size())
            );

            JOptionPane.showMessageDialog(this, message, "File Loaded", JOptionPane.INFORMATION_MESSAGE);

            // Print some basic stats to console
            if (!totalMsList.isEmpty()) {
                float avgTotal = calculateAverage(totalMsList);
                float avgSwap = calculateAverage(swapMsList);
                System.out.printf("Average total: %.3f ms, Average swap: %.3f ms%n", avgTotal, avgSwap);
            }

            // Trigger graph redraw
            graphPanel.repaint();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error reading file: " + e.getMessage(),
                    "File Error",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Ignore close errors
                }
            }
        }
    }

    private float calculateAverage(List<Float> values) {
        if (values.isEmpty()) return 0.0f;

        float sum = 0.0f;
        for (Float value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    public static void main(String[] args) {
        // Use the Event Dispatch Thread for Swing
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                FrameTimingAnalyzer analyzer = new FrameTimingAnalyzer();
                analyzer.setVisible(true);
            }
        });
    }
}